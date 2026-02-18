package io.quarkus.gamemanager.event.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.EventQuery;
import io.quarkus.gamemanager.event.mapping.EventMapper;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.logging.Log;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class EventService {
  private final EventRepository eventRepository;
  private final GameRepository gameRepository;
  private final EventMapper eventMapper;

  public EventService(EventRepository eventRepository, GameRepository gameRepository, EventMapper eventMapper) {
    this.eventRepository = eventRepository;
    this.gameRepository = gameRepository;
    this.eventMapper = eventMapper;
  }

  @WithSpan("EventService.getAllEvents")
  public List<EventDto> getAllEvents() {
    Log.info("Getting all events");
    return getAllEvents(EventQuery.empty());
  }

  @WithSpan("EventService.getAllEventsWithQuery")
  @Transactional
  public List<EventDto> getAllEvents(@SpanAttribute("arg.query") EventQuery eventQuery) {
    Log.infof("Getting events with query: %s", eventQuery);
    return this.eventRepository.getEvents(eventQuery)
        .stream()
        .map(this.eventMapper::toDto)
        .toList();
  }

  @WithSpan("EventService.deleteEvent")
  @Transactional
  public void deleteEvent(@Valid @NotNull @SpanAttribute("arg.eventId") Long eventId) {
    Log.infof("Deleting event with id: %s", eventId);
    this.eventRepository.deleteById(eventId);
  }

  @WithSpan("EventService.addEvent")
  @Transactional
  public EventDto addEvent(@Valid @NotNull @SpanAttribute("arg.event")
                             EventDto eventDto) {
    Log.infof("Adding event: %s", eventDto);
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
