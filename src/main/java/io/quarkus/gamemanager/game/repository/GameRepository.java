package io.quarkus.gamemanager.game.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class GameRepository implements PanacheRepository<Game> {
  public List<Game> getGamesForEvent(Long eventId) {
    return list("event.id", eventId);
  }

  public List<Game> getGamesForEvent(Long eventId, Sort sort) {
    return list("event.id", sort, eventId);
  }

  public long countGamesForEvent(Long eventId) {
    return count("event.id", eventId);
  }
}
