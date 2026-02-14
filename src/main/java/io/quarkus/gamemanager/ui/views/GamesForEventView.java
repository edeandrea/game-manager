package io.quarkus.gamemanager.ui.views;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.service.EventService;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.service.GameService;
import io.quarkus.logging.Log;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.LocalDateRenderer;

public final class GamesForEventView extends VerticalLayout {
  private final EventService eventService;
  private final GameService gameService;
  private final Grid<GameDto> grid;
  private final ListDataProvider<GameDto> gridData;
  private EventDto currentEvent = null;

  public GamesForEventView(EventService eventService, GameService gameService) {
    this.eventService = eventService;
    this.gameService = gameService;

    this.gridData = new ListDataProvider<>(new ArrayList<>(0));
    this.grid = createGrid();
    add(this.grid);

    setPadding(true);
    setSizeFull();
  }

  public void setEvent(EventDto event) {
    this.currentEvent = event;

    Optional.ofNullable(event)
        .ifPresentOrElse(
            e -> {
              var leaderboard = this.eventService.getLeaderboard(e.id());
              Log.infof("Leaderboard for event '%s': %s", e.name(), leaderboard);
              this.gridData.getItems().addAll(this.eventService.getLeaderboard(e.id()));

              if (event.games().isEmpty()) {
                this.gridData.getItems().clear();
                this.grid.setEmptyStateText("No games found for event '%s'".formatted(event.name()));
              }
            },
            () -> {
              this.gridData.getItems().clear();
              this.grid.setEmptyStateText("No event selected.");
            }
        );

    this.gridData.refreshAll();
    this.grid.recalculateColumnWidths();
  }

  private Grid<GameDto> createGrid() {
    var grid = new Grid<>(GameDto.class, false);
    grid.setSelectionMode(SelectionMode.MULTI);
    grid.setColumnReorderingAllowed(true);
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setEmptyStateText("No event selected.");

    grid.addColumn(game -> game.player().firstName())
        .setHeader("First Name")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1);

    grid.addColumn(game -> game.player().lastName())
        .setHeader("Last Name")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1);

    grid.addColumn(new LocalDateRenderer<>(
            game -> game.gameDate().atZone(ZoneId.systemDefault()).toLocalDate(),
            () -> DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        ))
        .setHeader("Game Date")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1);

    var timeToCompleteColumn = grid.addColumn(game -> "%d minutes %d seconds".formatted(game.timeToComplete().toMinutesPart(), game.timeToComplete().toSecondsPart()))
        .setHeader("Time to Complete")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setComparator(Comparator.comparing(GameDto::timeToComplete));

    grid.sort(GridSortOrder.asc(timeToCompleteColumn).build());
    grid.setDataProvider(this.gridData);

    return grid;
  }
}
