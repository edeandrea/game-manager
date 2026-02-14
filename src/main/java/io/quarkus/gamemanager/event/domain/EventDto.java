package io.quarkus.gamemanager.event.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.quarkus.gamemanager.game.domain.GameDto;

public record EventDto(
    Long id,

    @NotNull(message = "Event date is required")
    Instant eventDate,

    @NotEmpty(message = "Name is required")
    String name,

    @NotEmpty(message = "Description is required")
    String description,
    List<GameDto> games
) {
  public EventDto(Long id, Instant eventDate, String name, String description) {
    this(id, eventDate, name, description, new ArrayList<>());
  }

  public EventDto(Instant eventDate, String name, String description) {
    this(null, eventDate, name, description);
  }
}
