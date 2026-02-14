package io.quarkus.gamemanager.event.rest;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.EventQuery;
import io.quarkus.gamemanager.event.service.EventService;
import io.quarkus.gamemanager.game.domain.GameDto;

import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Events")
@RunOnVirtualThread
public class EventResource {
  private final EventService eventService;

  public EventResource(EventService eventService) {
    this.eventService = eventService;
  }

  @GET
  @Transactional
  public List<EventDto> getAllEvents(@BeanParam EventQuery eventQuery) {
    return this.eventService.getAllEvents(eventQuery);
  }

  @Path("/{eventId}")
  @GET
  @Transactional
  public Response getEvent(@PathParam("eventId") @Valid @NotNull Long eventId) {
    return this.eventService.getEvent(eventId)
        .map(Response::ok)
        .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
        .build();
  }

  @Path("/{eventId}/leaderboard")
  @GET
  @Transactional
  public List<GameDto> getLeaderboard(@PathParam("eventId") @Valid @NotNull Long eventId) {
    return this.eventService.getLeaderboard(eventId);
  }

  @Path("/{eventId}")
  @DELETE
  @Transactional
  public void deleteEvent(@PathParam("eventId") @Valid @NotNull Long eventId) {
    this.eventService.deleteEvent(eventId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Transactional
  public EventDto addEvent(@Valid @NotNull EventDto eventDto) {
    return this.eventService.addEvent(eventDto);
  }
}
