package io.quarkus.gamemanager.ui;

import java.util.ArrayList;
import java.util.Optional;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.service.EventService;
import io.quarkus.gamemanager.ui.components.AddEventDialog;
import io.quarkus.logging.Log;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;

@PageTitle("Event Manager")
@Route("")
@JsModule("./prefers-color-scheme.js")
public class MainLayout extends AppLayout {
  private final Button switchThemeButton = new Button(VaadinIcon.SUN_O.create());
  private final EventService eventService;
  private final Button deleteEventButton = new Button(VaadinIcon.TRASH.create());
  private final Select<EventDto> eventSelector = new Select<>("Events");
  private ListDataProvider<EventDto> eventDataProvider;
  private boolean isDefaultDarkTheme = false;
  private String currentTheme = Lumo.LIGHT;

  public MainLayout(EventService eventService) {
    super();
    this.eventService = eventService;

    // Check the browser preference at set theme accordingly
    UI.getCurrent().getElement().executeJs("return document.documentElement.getAttribute('theme') == 'dark'")
        .then(
            Boolean.class,
            isDark -> {
              this.isDefaultDarkTheme = isDark;
              this.currentTheme = isDark ? Lumo.DARK : Lumo.LIGHT;

              this.switchThemeButton.setIcon(this.isDefaultDarkTheme ? VaadinIcon.SUN_O.create() : VaadinIcon.MOON_O.create());
              this.switchThemeButton.addClickListener(e -> toggleLightDarkTheme());
            }
        );

    var titleLayout = new HorizontalLayout(this.switchThemeButton, new H1("Event Manager"));
    titleLayout.setSpacing(true);

    var spacer = new Div();
    spacer.getStyle().setFlexGrow("1");

    addToNavbar(titleLayout, spacer, createEventNavItem());
  }

  private Component createEventNavItem() {
    createEventsList();
    var layout = new HorizontalLayout(this.eventSelector);

    var addEventButton = new Button(VaadinIcon.PLUS.create(), event -> handleAddNewEvent());
    addEventButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    addEventButton.setAriaLabel("Add event");
    addEventButton.setTooltipText("Add new event");

    this.deleteEventButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    this.deleteEventButton.setAriaLabel("Delete event");
    this.deleteEventButton.setTooltipText("Delete selected event");
    this.deleteEventButton.setEnabled(false);
    this.deleteEventButton.addClickListener(_ -> handleDeleteEvent());

    layout.add(addEventButton, deleteEventButton);
    layout.setAlignItems(Alignment.BASELINE);

    return layout;
  }

  private void handleDeleteEvent() {
    var currentEvent = this.eventSelector.getValue();
    var confirmDialog = new ConfirmDialog(
        "Really delete event?",
        "Are you sure you want to delete the event \"%s\"".formatted(currentEvent.name()),
        "Delete",
        _ -> {
          this.eventService.deleteEvent(currentEvent.id());
          this.eventDataProvider.getItems().remove(currentEvent);
          this.eventDataProvider.refreshAll();
        },
        "Cancel",
        _ -> {}
    );

    confirmDialog.setConfirmButtonTheme("error primary");
    confirmDialog.open();
  }

  private void handleAddNewEvent() {
    var dialog = new AddEventDialog(this.eventService);
    dialog.addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        dialog.getEvent()
            .ifPresent(newEvent -> {
              Log.infof("New event created: %s", newEvent);
              this.eventDataProvider.getItems().add(newEvent);
              this.eventDataProvider.refreshAll();
              this.eventSelector.setValue(newEvent);
            });
      }
    });

    dialog.open();
  }

  private void createEventsList() {
    this.eventDataProvider = new ListDataProvider<>(new ArrayList<>(this.eventService.getAllEvents()));
    this.eventDataProvider.setSortOrder(EventDto::name, SortDirection.ASCENDING);
    this.eventSelector.setItemLabelGenerator(event -> Optional.ofNullable(event).map(EventDto::name).orElse("No events found"));
    this.eventSelector.setDataProvider(this.eventDataProvider);
    this.eventSelector.setPlaceholder("Select an event");

    if (this.eventDataProvider.getItems().isEmpty()) {
      this.eventSelector.setEmptySelectionAllowed(true);
      this.eventSelector.setEmptySelectionCaption("No events found");
    }

    this.eventSelector.addValueChangeListener(event -> eventSelected(event.getValue()));
  }

  private void eventSelected(EventDto newEvent) {
    Optional.ofNullable(newEvent)
        .ifPresentOrElse(
            _ -> this.deleteEventButton.setEnabled(true),
            () -> this.deleteEventButton.setEnabled(false)
        );
  }

  private void toggleLightDarkTheme() {
    if (Lumo.DARK.equals(this.currentTheme)) {
      this.currentTheme = Lumo.LIGHT;
      this.switchThemeButton.setIcon(VaadinIcon.MOON_O.create());
    }
    else {
      this.currentTheme = Lumo.DARK;
      this.switchThemeButton.setIcon(VaadinIcon.SUN_O.create());
    }

    UI.getCurrent().getElement().executeJs("document.documentElement.setAttribute('theme', '" + this.currentTheme + "')");
  }

}
