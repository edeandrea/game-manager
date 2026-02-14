package io.quarkus.gamemanager.event.domain;

import java.time.Instant;
import java.util.Optional;

import jakarta.ws.rs.QueryParam;

public record EventQuery(
    @QueryParam("start")
    Instant start,

    @QueryParam("end")
    Instant end,

    @QueryParam("name")
    String name
) {

  public static EventQuery empty() {
    return new EventQuery(null, null, null);
  }

  public Optional<Instant> getStart() {
    return Optional.ofNullable(start);
  }

  public Optional<Instant> getEnd() {
    return Optional.ofNullable(end);
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name)
        .filter(s -> !s.isBlank());
  }
}
