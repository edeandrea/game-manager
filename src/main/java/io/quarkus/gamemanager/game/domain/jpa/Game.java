package io.quarkus.gamemanager.game.domain.jpa;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import io.quarkus.gamemanager.event.domain.jpa.Event;

@Entity
@Table(
    name = "games",
    indexes = {
        @Index(name = "idx_game_game_date", columnList = "game_date")
    }
)
public class Game {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "games_seq")
	@SequenceGenerator(name = "games_seq", allocationSize = 1, sequenceName = "games_seq")
  private Long id;

  @Embedded
  private Player player;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @CreationTimestamp(source = SourceType.DB)
	@Column(updatable = false, nullable = false)
	private Instant gameDate;

  @NotNull(message = "Time to complete is required")
  @Column(nullable = false)
  private Duration timeToComplete;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    withId(id);
  }

  public Game withId(Long id) {
    this.id = id;
    return this;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    withPlayer(player);
  }

  public Game withPlayer(Player player) {
    this.player = player;
    return this;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    withEvent(event);
  }

  public Game withEvent(Event event) {
    this.event = event;
    return this;
  }

  public Instant getGameDate() {
    return gameDate;
  }

  public void setGameDate(Instant gameDate) {
    withGameDate(gameDate);
  }

  public Game withGameDate(Instant gameDate) {
    this.gameDate = gameDate;
    return this;
  }

  public Duration getTimeToComplete() {
    return timeToComplete;
  }

  public void setTimeToComplete(Duration timeToComplete) {
    withTimeToComplete(timeToComplete);
  }

  public Game withTimeToComplete(Duration timeToComplete) {
    this.timeToComplete = timeToComplete;
    return this;
  }

  @Override
  public String toString() {
    return "Game{" +
        "gameDate=" + getGameDate() +
        ", id=" + getId() +
        ", player=" + getPlayer() +
        ", eventId=" + getEvent().getId() +
        ", timeToComplete=" + getTimeToComplete() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Game game)) {
      return false;
    }

    return Objects.equals(id, game.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
