package io.quarkus.gamemanager.ui.events;

import io.quarkus.gamemanager.game.domain.GameDto;

import com.vaadin.flow.component.UI;

public record GameAddedEvent(UI source,  GameDto newGame, boolean isNewHighScore) implements UIEvent<GameAddedEvent> {
  public GameAddedEvent(UI source, GameDto newGame) {
    this(source, newGame, false);
  }
}
