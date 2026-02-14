package io.quarkus.gamemanager.ui.components;

import java.time.LocalDate;
import java.time.ZoneId;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.quarkus.gamemanager.event.domain.EventDto;

public class EventForm {
  @NotNull(message = "Event date is required")
  private LocalDate eventDate;

  @NotEmpty(message = "Name is required")
  private String name;

  @NotEmpty(message = "Description is required")
  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public static EventForm fromDto(EventDto dto) {
    var form = new EventForm();
    form.setDescription(dto.description());
    form.setEventDate(dto.eventDate().atZone(ZoneId.systemDefault()).toLocalDate());
    form.setName(dto.name());
    return form;
  }

  public EventDto toDto() {
    return new EventDto(
        this.eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        this.name,
        this.description);
  }
}
