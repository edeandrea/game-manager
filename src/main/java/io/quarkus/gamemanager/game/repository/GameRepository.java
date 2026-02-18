package io.quarkus.gamemanager.game.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class GameRepository implements PanacheRepository<Game> {
  private static final String EVENT_GAME_DATE_CLAUSE = "WHERE event.id = ?1 AND gameDate = ?2";

  public List<Game> getGamesForEvent(Long eventId, Sort sort, Optional<LocalDate> gameDateFilter) {
    return gameDateFilter
        .map(filter -> list(EVENT_GAME_DATE_CLAUSE, sort, eventId, filter))
        .orElseGet(() -> list("event.id", sort, eventId));
  }

  public long countGamesForEvent(Long eventId, Optional<LocalDate> gameDateFilter) {
    return gameDateFilter
        .map(filter -> count(EVENT_GAME_DATE_CLAUSE, eventId, filter))
        .orElseGet(() -> count("event.id", eventId));
  }

  public long countGameDatesForEvent(Long eventId) {
    return count("SELECT count(DISTINCT g.gameDate) FROM Game g WHERE g.event.id = ?1", eventId);
  }

  public List<LocalDate> getGameDatesOrderedChronologically(Long eventId) {
    return find("SELECT DISTINCT g.gameDate FROM Game g WHERE g.event.id = ?1", Sort.by("gameDate"), eventId)
        .project(LocalDate.class)
        .list();
  }
}
