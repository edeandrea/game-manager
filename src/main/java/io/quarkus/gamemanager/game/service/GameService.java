package io.quarkus.gamemanager.game.service;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotAcceptableException;

import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.mapping.GameMapper;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class GameService {
  private final GameRepository gameRepository;
  private final EventRepository eventRepository;
  private final GameMapper gameMapper;

  public GameService(GameRepository gameRepository, EventRepository eventRepository, GameMapper gameMapper) {
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

  private GameDto saveGame(Event event, GameDto gameDto) {
    var game = this.gameMapper.toEntity(gameDto);
    game.setEvent(event);

    this.gameRepository.persistAndFlush(game);

    return this.gameMapper.toDto(game);
  }
}
