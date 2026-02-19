package io.quarkus.gamemanager.game.repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class GameRepository implements PanacheRepository<Game> {
  private static final String EVENT_GAME_DATE_CLAUSE = "WHERE event.id = :eventId AND gameDate >= :startOfDay AND gameDate < :endOfDay";

  private static Parameters createGameDateParameters(LocalDate gameDate) {
    return Parameters.with("startOfDay", gameDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        .and("endOfDay", gameDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public List<Game> getGamesForEvent(Long eventId, Sort sort, Optional<LocalDate> gameDateFilter) {
    return gameDateFilter
        .map(filter -> list(EVENT_GAME_DATE_CLAUSE, sort, createGameDateParameters(filter).and("eventId", eventId)))
        .orElseGet(() -> list("event.id", sort, eventId));
  }

  public long countGamesForEvent(Long eventId, Optional<LocalDate> gameDateFilter) {
    return gameDateFilter
        .map(filter -> count(EVENT_GAME_DATE_CLAUSE, createGameDateParameters(filter).and("eventId", eventId)))
        .orElseGet(() -> count("event.id", eventId));
  }

  public long countGameDatesForEvent(Long eventId) {
    return find("SELECT COUNT(DISTINCT DATE(g.gameDate)) FROM Game g WHERE g.event.id = ?1", eventId)
        .project(Long.class)
        .firstResult();
  }

  public List<LocalDate> getGameDatesOrderedChronologically(Long eventId) {
    return find("SELECT DISTINCT DATE(g.gameDate) FROM Game g WHERE g.event.id = ?1 ORDER BY DATE(g.gameDate)", eventId)
        .project(LocalDate.class)
        .list();
  }
}
