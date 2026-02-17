package io.quarkus.gamemanager.game.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotAcceptableException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;

import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.config.GameConfig;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.mapping.GameMapper;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
public class GameService {
  private final GameConfig gameConfig;
  private final GameRepository gameRepository;
  private final EventRepository eventRepository;
  private final GameMapper gameMapper;

  public GameService(GameConfig gameConfig, GameRepository gameRepository, EventRepository eventRepository, GameMapper gameMapper) {
    this.gameConfig = gameConfig;
    this.gameRepository = gameRepository;
    this.eventRepository = eventRepository;
    this.gameMapper = gameMapper;
  }

  @Transactional
  public List<GameDto> getGames(@Valid @NotNull Long eventId) {
    Log.infof("Getting games for event with id: %s", eventId);
    return this.gameRepository.getGamesForEvent(eventId)
        .stream()
        .map(this.gameMapper::toDto)
        .toList();
  }

  @Transactional
  public List<GameDto> getGames(@Valid @NotNull Long eventId, Sort sort) {
    Log.infof("Getting games for event with id: %s, sorted by: %s", eventId, sort);
    return this.gameRepository.getGamesForEvent(eventId, sort)
        .stream()
        .map(this.gameMapper::toDto)
        .toList();
  }

  @Transactional
  public GameDto addGame(@Valid @NotNull GameDto gameDto) {
    Log.infof("Adding game: %s", gameDto);
    var eventId = gameDto.eventId();

    return this.eventRepository.findByIdOptional(eventId)
        .map(event -> saveGame(event, gameDto))
        .orElseThrow(() -> new NotAcceptableException("Game is not associated with any event, or event %d can not be found".formatted(eventId)));
  }

  public void deleteGames(Collection<GameDto> games) {
    games.forEach(this::deleteGame);
  }

  @Transactional
  public long countGamesForEvent(Long eventId) {
    Log.infof("Counting games for event with id: %s", eventId);
    return this.gameRepository.countGamesForEvent(eventId);
  }

  @Transactional
  public void deleteGame(GameDto gameDto) {
    Log.infof("Deleting game: %s", gameDto);
    this.gameRepository.deleteById(gameDto.id());
  }

  public Multi<Integer> countDown(Integer startingFrom, Duration every) {
    return Multi.createFrom()
        .range(0, startingFrom + 1)
        .map(i -> startingFrom - i)
        .call(() ->
            Uni.createFrom().nullItem()
                .onItem()
                .delayIt()
                .by(every)
        )
        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        .emitOn(Infrastructure.getDefaultExecutor());
  }

  public Multi<Long> timeGame() {
    return Multi.createFrom()
        .ticks()
        .startingAfter(Duration.ofSeconds(1))
        .every(Duration.ofSeconds(1))
        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        .emitOn(Infrastructure.getDefaultExecutor());
  }

  private void resetRepo(Path checkoutDir) throws IOException, GitAPIException {
    try (var git = Git.open(checkoutDir.toFile())) {
      git.fetch()
          .setRemote("origin")
          .setProgressMonitor(new TextProgressMonitor())
          .call();

      git.reset()
          .setMode(ResetType.HARD)
          .setRef("origin/%s".formatted(this.gameConfig.branch()))
          .setProgressMonitor(new TextProgressMonitor())
          .call();

      Log.info("Game repository reset successfully");
    }
  }

  private void cloneRepo(Path checkoutDir) throws GitAPIException {
    var repoUrl = this.gameConfig.repoUrl();

    Log.infof("Cloning branch [%s] of game repository [%s] into [%s]", this.gameConfig.branch(), repoUrl, checkoutDir);
    try (var _ = Git.cloneRepository()
        .setDirectory(checkoutDir.toFile())
        .setURI(repoUrl)
        .setCloneAllBranches(false)
        .setBranch(this.gameConfig.branch())
        .setProgressMonitor(new TextProgressMonitor())
        .call()) {

      Log.info("Game repository cloned successfully");
    }
  }

  public Uni<Void> setUpNewGame() {
    return Uni.createFrom().voidItem()
        .invoke(() -> {
          Log.info("Setting up new game");
          var rootCheckoutDir = this.gameConfig.rootCheckoutDir().normalize().toAbsolutePath();

          try {
            Log.infof("Creating checkout directory: %s", rootCheckoutDir);
            Files.createDirectories(rootCheckoutDir);

            var checkoutDir = this.gameConfig.checkoutDir().normalize().toAbsolutePath();

            if (Files.exists(checkoutDir)) {
              Log.infof("Checkout directory [%s] already exists", checkoutDir);
              resetRepo(checkoutDir);
            }
            else {
              Log.infof("Checkout directory [%s] doesn't exist");
              cloneRepo(checkoutDir);
              new ProcessBuilder("open", checkoutDir.toString())
                  .start()
                  .waitFor();
            }
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private GameDto saveGame(Event event, GameDto gameDto) {
    var game = this.gameMapper.toEntity(gameDto);
    game.setEvent(event);

    this.gameRepository.persistAndFlush(game);

    return this.gameMapper.toDto(game);
  }
}
