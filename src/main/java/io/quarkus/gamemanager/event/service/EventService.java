package io.quarkus.gamemanager.event.service;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.EventQuery;
import io.quarkus.gamemanager.event.mapping.EventMapper;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.mapping.GameMapper;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.logging.Log;

@ApplicationScoped
public class EventService {
  private final EventRepository eventRepository;
  private final GameRepository gameRepository;
  private final EventMapper eventMapper;
  private final GameMapper gameMapper;

  public EventService(EventRepository eventRepository, GameRepository gameRepository, EventMapper eventMapper, GameMapper gameMapper) {
    this.eventRepository = eventRepository;
    this.gameRepository = gameRepository;
    this.eventMapper = eventMapper;
    this.gameMapper = gameMapper;
  }

  public List<EventDto> getAllEvents() {
    Log.info("Getting all events");
    return getAllEvents(EventQuery.empty());
  }

  @Transactional
  public List<EventDto> getAllEvents(EventQuery eventQuery) {
    Log.infof("Getting events with query: %s", eventQuery);
    return this.eventRepository.getEvents(eventQuery)
        .stream()
        .map(this.eventMapper::toDto)
        .toList();
  }

  @Transactional
  public Optional<EventDto> getEvent(@Valid @NotNull Long eventId) {
    Log.infof("Getting event with id: %s", eventId);
    return this.eventRepository.findByIdOptional(eventId)
        .map(this.eventMapper::toDto);
  }

  @Transactional
  public List<GameDto> getLeaderboard(@Valid @NotNull Long eventId) {
    Log.infof("Getting leaderboard for event with id: %s", eventId);
    return this.gameRepository.getLeaderboard(eventId)
        .stream()
        .map(this.gameMapper::toDto)
        .toList();
  }

  @Transactional
  public void deleteEvent(@Valid @NotNull Long eventId) {
    Log.infof("Deleting event with id: %s", eventId);
    this.eventRepository.deleteById(eventId);
  }

  @Transactional
  public void saveEvent(EventDto eventDto) {
    Log.infof("Saving event: %s", eventDto);
    var eventToSave = this.eventMapper.toEntity(eventDto);
    var updatedEvent = this.eventRepository.findById(eventToSave.getId()).from(eventToSave);
    this.eventRepository.persist(updatedEvent);
  }

  @Transactional
  public EventDto addEvent(@Valid @NotNull EventDto eventDto) {
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
