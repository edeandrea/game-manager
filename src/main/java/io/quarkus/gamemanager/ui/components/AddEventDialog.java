package io.quarkus.gamemanager.ui.components;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.service.EventService;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormRow;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Style.FlexWrap;

public final class AddEventDialog extends Dialog {
  private final EventService eventService;
  private final TextField nameField = new TextField("Event Name");
  private final DatePicker dateField = new DatePicker("Event Date");
  private final TextArea descriptionField = new TextArea("Event Description");
  private final Binder<EventForm> binder = new BeanValidationBinder<>(EventForm.class);
  private final EventForm eventForm = new EventForm();
  private EventDto newEvent;

  public AddEventDialog(EventService eventService) {
    super("Add new event");
    this.eventService = eventService;
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    setResizable(true);
    setDraggable(true);

    this.nameField.setRequired(true);
    this.nameField.setRequiredIndicatorVisible(true);
    this.nameField.setErrorMessage("Event name is required");
    this.dateField.setRequired(true);
    this.dateField.setRequiredIndicatorVisible(true);
    this.dateField.setErrorMessage("Event date is required");
    this.dateField.addFocusListener(e -> this.dateField.open());
    this.descriptionField.setRequired(true);
    this.descriptionField.setRequiredIndicatorVisible(true);
    this.descriptionField.setErrorMessage("Event description is required");

    var layout = new FormLayout();
    layout.setAutoResponsive(true);
    layout.setExpandColumns(true);
    layout.setExpandFields(true);

    var nameDateRow = new FormRow();
    nameDateRow.add(this.nameField, this.dateField);

    var descriptionRow = new FormRow();
    descriptionRow.add(this.descriptionField, 2);

    layout.add(nameDateRow, descriptionRow);

    add(layout);

    var cancelButton = new Button("Cancel", event -> close());
    var okButton = new Button("Ok", event -> handleOkButtonClick());
    okButton.addClickShortcut(Key.ENTER);
    okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    var buttonLayout = new HorizontalLayout(okButton, cancelButton);
    buttonLayout.getStyle().setFlexWrap(FlexWrap.WRAP);
    buttonLayout.setJustifyContentMode(JustifyContentMode.END);

    getFooter().add(buttonLayout);
    createBindings();

    this.nameField.focus();
  }

  private void createBindings() {
    this.binder.forField(this.nameField)
        .withValidator(name -> (name!=null) && !name.isBlank(), "Event name is required")
        .bind("name");

    this.binder.forField(this.dateField)
        .withValidator(date -> date!=null, "Event date is required")
        .bind("eventDate");

    this.binder.forField(this.descriptionField)
        .withValidator(description -> (description!=null) && !description.isBlank(), "Event description is required")
        .bind("description");
  }

  private void handleOkButtonClick() {
    if (this.binder.writeBeanIfValid(this.eventForm)) {
      this.newEvent = this.eventService.addEvent(this.eventForm.toDto());
      close();
    }
  }

  public Optional<EventDto> getEvent() {
    return Optional.ofNullable(this.newEvent);
  }

  public static final class EventForm {
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
      form.setEventDate(dto.eventDate());
      form.setName(dto.name());
      return form;
    }

    public EventDto toDto() {
      return new EventDto(
          this.eventDate,
          this.name,
          this.description);
    }
  }
}
