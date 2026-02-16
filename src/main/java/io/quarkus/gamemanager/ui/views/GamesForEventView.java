package io.quarkus.gamemanager.ui.views;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.service.EventService;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.service.GameService;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.selection.SelectionEvent;

public final class GamesForEventView extends VerticalLayout {
  private final EventService eventService;
  private final GameService gameService;
  private final Grid<GameDto> grid;
  private final Button removeGamesButton = new Button(VaadinIcon.TRASH.create());
  private EventDto currentEvent = null;

  public GamesForEventView(EventService eventService, GameService gameService) {
    this.eventService = eventService;
    this.gameService = gameService;

    var refreshGamesButton = new Button(VaadinIcon.REFRESH.create(), e -> refreshGrid());
    var addGameButton = new Button(VaadinIcon.PLUS.create(), e -> handleNewGame());
    addGameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    this.removeGamesButton.addClickListener(e -> handleRemoveSelectedGames());
    this.removeGamesButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    this.removeGamesButton.setEnabled(false);

    var topBar = new HorizontalLayout(refreshGamesButton, addGameButton, this.removeGamesButton);
    topBar.setWidthFull();
    add(topBar);

    this.grid = createGrid();
    this.grid.setDataProvider(DataProvider.fromCallbacks(new GameFetchCallback(), new GameSizeCallback()));
    this.grid.addSelectionListener(this::handleGridRowsSelected);
    add(this.grid);

    setPadding(true);
    setSizeFull();
  }

  private void handleGridRowsSelected(SelectionEvent<Grid<GameDto>, GameDto> event) {
    this.removeGamesButton.setEnabled(!event.getAllSelectedItems().isEmpty());
    this.grid.setEmptyStateText(this.currentEvent.games().isEmpty() ? "No games found for event '%s'".formatted(this.currentEvent.name()) : "");
  }

  private void handleRemoveSelectedGames() {
    var confirmDialog = new ConfirmDialog(
        "Delete games?",
        "Are you sure you want to delete the selected games?",
        "Yes",
        _ -> {
          var selectedGames = this.grid.getSelectedItems();
          var selectedGameIds = selectedGames
              .stream()
              .map(GameDto::id)
              .collect(Collectors.toSet());

          this.currentEvent.games().removeIf(game -> selectedGameIds.contains(game.id()));
          this.gameService.deleteGames(selectedGames);
          this.grid.getDataProvider().refreshAll();
          this.grid.deselectAll();
        },
        "No",
        _ -> {}
    );

    confirmDialog.setConfirmButtonTheme("error primary");
    confirmDialog.open();
  }

  private void handleNewGame() {

  }

  private void refreshGrid() {
    Optional.ofNullable(this.currentEvent)
        .ifPresentOrElse(
            e -> {
              var leaderboard = this.eventService.getLeaderboard(e.id());
              Log.infof("Leaderboard for event '%s': %s", e.name(), leaderboard);

              if (e.games().isEmpty()) {
                this.grid.setEmptyStateText("No games found for event '%s'".formatted(e.name()));
              }
            },
            () -> {
              this.grid.setEmptyStateText("No event selected.");
            }
        );

    this.grid.getDataProvider().refreshAll();
    this.grid.recalculateColumnWidths();
  }

  public void setEvent(EventDto event) {
    this.currentEvent = event;
    refreshGrid();
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
        .setFlexGrow(1)
        .setSortProperty("player.firstName");

    grid.addColumn(game -> game.player().lastName())
        .setHeader("Last Name")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortProperty("player.lastName");

    grid.addColumn(new LocalDateRenderer<>(
            game -> game.gameDate().atZone(ZoneId.systemDefault()).toLocalDate(),
            () -> DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        ))
        .setHeader("Game Date")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortProperty("gameDate");

    var timeToCompleteColumn = grid.addColumn(game -> "%d minutes %d seconds".formatted(game.timeToComplete().toMinutesPart(), game.timeToComplete().toSecondsPart()))
        .setHeader("Time to Complete")
        .setResizable(true)
        .setSortable(true)
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortProperty("timeToComplete");

    grid.sort(GridSortOrder.asc(timeToCompleteColumn).build());

    return grid;
  }

  private class GameFetchCallback implements FetchCallback<GameDto, Void> {
    @Override
    public Stream<GameDto> fetch(Query<GameDto, Void> query) {
      // Satisfy Vaadin's contract even though we aren't supporting filtering or pagination
      query.getLimit();
      query.getOffset();

      var stream = Optional.ofNullable(currentEvent)
          .map(e -> createSort(query.getSortOrders())
              .map(sort -> gameService.getGames(e.id(), sort))
              .orElseGet(() -> gameService.getGames(e.id()))
          )
          .map(List::stream)
          .orElseGet(Stream::empty);

      return stream;
    }

    private static Optional<Sort> createSort(List<QuerySortOrder> sortOrders) {
      if ((sortOrders != null) && !sortOrders.isEmpty()) {
        var sort = createSort(sortOrders.getFirst());

        return Optional.of(IntStream.range(1, sortOrders.size())
            .boxed()
            .map(sortOrders::get)
            .peek(sortOrder -> Log.infof("Processing sort order: %s", sortOrder.getSorted()))
            .reduce(sort, (s, sortOrder) -> s.and(sortOrder.getSorted(), getSortDirection(sortOrder)), ((sort1, _) -> sort1)));
      }

      return Optional.empty();
    }

    private static Sort createSort(QuerySortOrder sortOrder) {
      Log.infof("Creating first sort order: %s", sortOrder.getSorted());
      return Sort.by(sortOrder.getSorted(), getSortDirection(sortOrder));
    }

    private static Direction getSortDirection(QuerySortOrder sortOrder) {
      return switch (sortOrder.getDirection()) {
        case null -> Direction.Ascending;
        case ASCENDING -> Direction.Ascending;
        case DESCENDING -> Direction.Descending;
      };
    }
  }

  private class GameSizeCallback implements CountCallback<GameDto, Void> {
    @Override
    public int count(Query<GameDto, Void> query) {
      return Optional.ofNullable(currentEvent)
          .map(e -> (int) gameService.countGamesForEvent(e.id()))
          .orElse(0);
    }
  }
}
