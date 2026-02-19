package io.quarkus.gamemanager.game.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import io.quarkus.gamemanager.ide.IdeService;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
public class GameService {
  private static final Set<String> IGNORE_LINES_CONTAINING = Set.of(
      "@ToolBox",
      "@OutputGuardrails",
      "@UserMessage",
      "@RegisterAiService",
      "@Tool"
  );

  private final GameConfig gameConfig;
  private final GameRepository gameRepository;
  private final EventRepository eventRepository;
  private final GameMapper gameMapper;
  private final IdeService ideService;

  public GameService(GameConfig gameConfig, GameRepository gameRepository, EventRepository eventRepository, GameMapper gameMapper, IdeService ideService) {
    this.gameConfig = gameConfig;
    this.gameRepository = gameRepository;
    this.eventRepository = eventRepository;
    this.gameMapper = gameMapper;
    this.ideService = ideService;
  }

  @WithSpan("GameService.getGameDatesForEvent")
  public Stream<LocalDate> getGameDatesForEvent(@SpanAttribute("arg.eventId") Long eventId) {
    Log.infof("Getting game dates for event with id: %s", eventId);
    return this.gameRepository.getGameDatesOrderedChronologically(eventId)
        .stream();
  }

  @WithSpan("GameService.getGames")
  @Transactional
  public List<GameDto> getGames(@Valid @NotNull @SpanAttribute("arg.eventId") Long eventId, @SpanAttribute("arg.sort") Sort sort, @SpanAttribute("arg.gameDateFilter") Optional<LocalDate> gameDateFilter) {
    Log.infof("Getting games for event with id: %s, sorted by: %s, with date filter: %s", eventId, sort, gameDateFilter);

    return this.gameRepository.getGamesForEvent(eventId, sort, gameDateFilter)
        .stream()
        .map(this.gameMapper::toDto)
        .toList();
  }

  @WithSpan("GameService.addGame")
  @Transactional
  public GameDto addGame(@Valid @NotNull @SpanAttribute("arg.game") GameDto gameDto) {
    Log.infof("Adding game: %s", gameDto);
    var eventId = gameDto.eventId();

    return this.eventRepository.findByIdOptional(eventId)
        .map(event -> saveGame(event, gameDto))
        .orElseThrow(() -> new NotAcceptableException("Game is not associated with any event, or event %d can not be found".formatted(eventId)));
  }

  @WithSpan("GameService.deleteGames")
  public void deleteGames(@SpanAttribute("arg.games") Collection<GameDto> games) {
    games.forEach(this::deleteGame);
  }

  @WithSpan("GameService.countGamesForEvent")
  @Transactional
  public long countGamesForEvent(@SpanAttribute("arg.eventId") Long eventId, @SpanAttribute("arg.filter") Optional<LocalDate> gameDateFilter) {
    Log.infof("Counting games for event with id: %s", eventId);
    return this.gameRepository.countGamesForEvent(eventId, gameDateFilter);
  }

  @WithSpan("GameService.countGameDatesForEvent")
  @Transactional
  public long countGameDatesForEvent(@SpanAttribute("arg.eventId") Long eventId) {
    Log.infof("Counting game dates for event with id: %s", eventId);
    return this.gameRepository.countGameDatesForEvent(eventId);
  }

  @WithSpan("GameService.deleteGame")
  @Transactional
  public void deleteGame(@SpanAttribute("arg.game") GameDto gameDto) {
    Log.infof("Deleting game: %s", gameDto);
    this.gameRepository.deleteById(gameDto.id());
  }

  @WithSpan("GameService.countDown")
  public Multi<Integer> countDown(@SpanAttribute("arg.startingFrom") Integer startingFrom, @SpanAttribute("arg.every") Duration every) {
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

  @WithSpan("GameService.timeGame")
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

  @WithSpan("GameService.setUpNewGame")
  public Uni<Void> setUpNewGame() {
    return Uni.createFrom().voidItem()
        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        .emitOn(Infrastructure.getDefaultExecutor())
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
            }

            setUpGameStart(checkoutDir);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .invoke(() -> this.ideService.startInIde(this.gameConfig.checkoutDir().normalize().toAbsolutePath()));
  }

  private void setUpGameStart(Path gameDir) {
    Stream.of(
        "src/main/java/io/quarkus/game/Storyteller.java",
        "src/main/java/io/quarkus/game/Tools.java"
    )
        .map(gameDir::resolve)
        .forEach(this::rewriteFile);
  }

  private void rewriteFile(Path file) {
    Log.infof("Rewriting file: %s", file);
    var newFileContent = getAllLines(file)
        .filter(line -> IGNORE_LINES_CONTAINING.stream().noneMatch(line::contains))
        .collect(Collectors.joining("\n"));

    try {
      Files.writeString(file, newFileContent);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Stream<String> getAllLines(Path file) {
    try {
      return Files.readAllLines(file).stream();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private GameDto saveGame(Event event, GameDto gameDto) {
    var game = this.gameMapper.toEntity(gameDto);
    game.setEvent(event);

    this.gameRepository.persistAndFlush(game);

    return this.gameMapper.toDto(game);
  }
}
