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
import io.quarkus.gamemanager.event.mapping.EventMapper;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.mapping.GameMapper;
import io.quarkus.gamemanager.game.repository.GameRepository;

import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Events")
@RunOnVirtualThread
public class EventResource {
  private final EventRepository eventRepository;
  private final GameRepository gameRepository;
  private final EventMapper eventMapper;
  private final GameMapper gameMapper;

  public EventResource(EventRepository eventRepository, GameRepository gameRepository, EventMapper eventMapper, GameMapper gameMapper) {
    this.eventRepository = eventRepository;
    this.gameRepository = gameRepository;
    this.eventMapper = eventMapper;
    this.gameMapper = gameMapper;
  }

  @GET
  @Transactional
  public List<EventDto> getAllEvents(@BeanParam EventQuery eventQuery) {
    return this.eventRepository.getEvents(eventQuery)
        .stream()
        .map(this.eventMapper::toDto)
        .toList();
  }

  @Path("/{eventId}")
  @GET
  @Transactional
  public Response getEvent(@PathParam("eventId") @Valid @NotNull Long eventId) {
    return this.eventRepository.findByIdOptional(eventId)
        .map(this.eventMapper::toDto)
        .map(Response::ok)
        .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
        .build();
  }

  @Path("/{eventId}/leaderboard")
  @GET
  @Transactional
  public List<GameDto> getLeaderboard(@PathParam("eventId") @Valid @NotNull Long eventId) {
    return this.gameRepository.getLeaderboard(eventId)
        .stream()
        .map(this.gameMapper::toDto)
        .toList();
  }

  @Path("/{eventId}")
  @DELETE
  @Transactional
  public void deleteEvent(@PathParam("eventId") @Valid @NotNull Long eventId) {
    this.eventRepository.deleteById(eventId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Transactional
  public EventDto addEvent(@Valid @NotNull EventDto eventDto) {
    var event = (eventDto.id() == null) ?
        eventDto :
        new EventDto(null, eventDto.eventDate(), eventDto.name(), eventDto.description());

    // There shouldn't be any games in the event DTO when creating an event
    event.games().clear();

    var eventEntity = this.eventMapper.toEntity(event);
    this.eventRepository.persist(eventEntity);
    return this.eventMapper.toDto(eventEntity);
  }
}
