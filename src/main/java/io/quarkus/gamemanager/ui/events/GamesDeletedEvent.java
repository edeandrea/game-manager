package io.quarkus.gamemanager.ui.events;

import java.util.List;

import io.quarkus.gamemanager.game.domain.GameDto;

import com.vaadin.flow.component.UI;

public record GamesDeletedEvent(UI source, List<GameDto> games) implements UIEvent<GamesDeletedEvent> {
  public GamesDeletedEvent(UI source, GameDto game) {
    this(source, List.of(game));
  }
}
