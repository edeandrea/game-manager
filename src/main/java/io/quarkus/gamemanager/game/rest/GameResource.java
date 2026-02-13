package io.quarkus.gamemanager.game.rest;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.mapping.GameMapper;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.logging.Log;

import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Games")
@RunOnVirtualThread
public class GameResource {
  private final GameRepository gameRepository;
  private final EventRepository eventRepository;
  private final GameMapper gameMapper;

  public GameResource(GameRepository gameRepository, EventRepository eventRepository, GameMapper gameMapper) {
    this.gameRepository = gameRepository;
    this.eventRepository = eventRepository;
    this.gameMapper = gameMapper;
  }

  @Path("/event/{eventId}")
  @GET
  @Transactional
  public List<GameDto> getGames(@PathParam("eventId") @Valid @NotNull Long eventId) {
    return this.gameRepository.getGamesForEvent(eventId)
        .stream()
        .map(this.gameMapper::toDto)
        .toList();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Transactional
  public GameDto addGame(@Valid @NotNull GameDto gameDto) {
    Log.infof("Adding game: %s", gameDto);
    var eventId = gameDto.eventId();

    return this.eventRepository.findByIdOptional(eventId)
        .map(event -> saveGame(event, gameDto))
        .orElseThrow(() -> new NotAcceptableException("Game is not associated with any event, or event %d can not be found".formatted(eventId)));
  }

  private GameDto saveGame(Event event, GameDto gameDto) {
    var game = this.gameMapper.toEntity(gameDto);
    game.setEvent(event);

    this.gameRepository.persistAndFlush(game);

    return this.gameMapper.toDto(game);
  }
}
