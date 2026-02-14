package io.quarkus.gamemanager.game.rest;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.service.GameService;
import io.quarkus.logging.Log;

import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Games")
@RunOnVirtualThread
public class GameResource {
  private final GameService gameService;

  public GameResource(GameService gameService) {
    this.gameService = gameService;
  }

  @Path("/event/{eventId}")
  @GET
  @Transactional
  public List<GameDto> getGames(@PathParam("eventId") @Valid @NotNull Long eventId) {
    return this.gameService.getGames(eventId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Transactional
  public GameDto addGame(@Valid @NotNull GameDto gameDto) {
    Log.infof("Adding game: %s", gameDto);
    return this.gameService.addGame(gameDto);
  }
}
