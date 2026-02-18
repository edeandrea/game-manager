package io.quarkus.gamemanager.event.rest;

import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.EventQuery;
import io.quarkus.gamemanager.event.service.EventService;

import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class EventResource {
  private final EventService eventService;

  public EventResource(EventService eventService) {
    this.eventService = eventService;
  }

  @GET
  public List<EventDto> getAllEvents(@BeanParam EventQuery eventQuery) {
    return this.eventService.getAllEvents(eventQuery);
  }
}
