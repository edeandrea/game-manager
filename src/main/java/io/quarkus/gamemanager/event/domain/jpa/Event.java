package io.quarkus.gamemanager.event.domain.jpa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.quarkus.gamemanager.game.domain.jpa.Game;

@Entity
@Table(name = "events")
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "events_seq")
	@SequenceGenerator(name = "events_seq", allocationSize = 1, sequenceName = "events_seq")
  private Long id;

  @NotNull(message = "Event date is required")
  private LocalDate eventDate;

  @NotEmpty(message = "Name is required")
  private String name;

  @NotEmpty(message = "Description is required")
  @Column(columnDefinition = "TEXT")
  private String description;

  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Game> games = new ArrayList<>();

  public Event from(Event event) {
    return withEventDate(event.getEventDate())
        .withName(event.getName())
        .withDescription(event.getDescription())
        .withGames(event.getGames());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    withId(id);
  }

  public Event withId(Long id) {
    this.id = id;
    return this;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    withEventDate(eventDate);
  }

  public Event withEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    withDescription(description);
  }

  public Event withDescription(String description) {
    this.description = description;
    return this;
  }

  public String getName() {
    return name;
  }

  public Event withName(String name) {
    this.name = name;
    return this;
  }

  public void setName(String name) {
    withName(name);
  }

  public List<Game> getGames() {
    return games;
  }

  public void setGames(List<Game> games) {
    withGames(games);
  }

  public Event withGames(List<Game> games) {
    if (games != null) {
      this.games.clear();
      games.forEach(this::withGame);
    }

    return this;
  }

  public void addGame(Game game) {
    withGame(game);
  }

  public Event withGame(Game game) {
    if (game != null) {
      this.games.add(game);
    }

    game.setEvent(this);

    return this;
  }

  @Override
  public String toString() {
    return "Event{" +
        "description='" + getDescription() + '\'' +
        ", name='" + getName() + '\'' +
        ", id=" + getId() +
        ", eventDate=" + getEventDate() +
        ", games=" + getGames() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Event event)) {
      return false;
    }

    return Objects.equals(id, event.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
