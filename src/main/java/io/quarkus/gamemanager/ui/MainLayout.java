package io.quarkus.gamemanager.ui;

import java.util.ArrayList;
import java.util.Optional;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.service.EventService;
import io.quarkus.gamemanager.game.service.GameService;
import io.quarkus.gamemanager.ui.components.AddEventDialog;
import io.quarkus.gamemanager.ui.views.GamesForEventView;
import io.quarkus.logging.Log;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
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
  private final GamesForEventView gamesForEventView;
  private final Button deleteEventButton = new Button(VaadinIcon.TRASH.create());
  private final Select<EventDto> eventSelector = new Select<>("Events");
  private ListDataProvider<EventDto> eventDataProvider;
  private boolean isDefaultDarkTheme = false;
  private String currentTheme = Lumo.LIGHT;

  public MainLayout(EventService eventService, GameBroadcaster gameBroadcaster, GameService gameService) {
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

    var ghIcon = new Html("<svg height='24' width='24' viewBox='0 0 16 16' fill='currentColor' style='transform: scale(1.8);'><path d='M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z'/></svg>");
    var anchorLayout = new HorizontalLayout();
    anchorLayout.setAlignItems(Alignment.CENTER);
    anchorLayout.setSpacing(false);
    anchorLayout.add(ghIcon);
    anchorLayout.add(new H1("Event Manager"));

    var anchor = new Anchor("https://github.com/edeandrea/game-manager", anchorLayout);

    anchor.setTarget(AnchorTarget.BLANK);

    var titleLayout = new HorizontalLayout(this.switchThemeButton, anchor);
    titleLayout.setSpacing(true);

    var spacer = new Div();
    spacer.getStyle().setFlexGrow("1");

    addToNavbar(titleLayout, spacer, createEventNavItem());

    this.gamesForEventView = new GamesForEventView(gameService, gameBroadcaster);
    setContent(this.gamesForEventView);
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
            event -> {
              this.deleteEventButton.setEnabled(true);
              this.gamesForEventView.setEvent(event);
            },
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
