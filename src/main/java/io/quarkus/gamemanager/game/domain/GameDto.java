package io.quarkus.gamemanager.game.domain;

import java.time.Duration;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record GameDto(
    Long id,

    @Valid
    @NotNull(message = "Player is required")
    PlayerDto player,

    @NotNull(message = "Event id is required")
    Long eventId,
    LocalDate gameDate,

    @NotNull(message = "Time to complete is required")
    Duration timeToComplete
) {
  public GameDto(PlayerDto player, Long eventId, Duration timeToComplete) {
    this(null, player, eventId, LocalDate.now(), timeToComplete);
  }
}
