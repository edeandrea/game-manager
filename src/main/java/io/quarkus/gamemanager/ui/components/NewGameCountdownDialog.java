package io.quarkus.gamemanager.ui.components;


import java.time.Duration;

import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.service.GameService;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

public final class NewGameCountdownDialog extends Dialog {
  public NewGameCountdownDialog(PlayerDto player, GameService gameService) {
    setModality(ModalityMode.STRICT);
    setResizable(false);
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);

    var label = new NativeLabel("Get ready %s!".formatted(player.firstName()));
    label.setId("label");

    var progressBar = new ProgressBar(0, 5);
    progressBar.setValue(4);
    progressBar.getElement().setAttribute("aria-labelledby", "label");

    var countDownLabel = new Span("4");
    var layout = new HorizontalLayout(label, countDownLabel);
    layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

    add(layout, progressBar);

    UI ui = UI.getCurrent();

    gameService.countDown(4, Duration.ofSeconds(1))
        .subscribe()
        .with(
            count -> ui.access(() -> {
              countDownLabel.setText(String.valueOf(count));
              progressBar.setValue(count);

              switch (count) {
                case 3 -> label.setText("Ready!");
                case 2 -> label.setText("Set!");
                case 1 -> label.setText("GO!!!");
              }
            }),
            () -> ui.access(this::close)
        );
  }
}
