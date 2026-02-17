package io.quarkus.gamemanager.ui.components;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.service.GameService;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style.Display;
import com.vaadin.flow.dom.Style.FontWeight;
import com.vaadin.flow.dom.Style.TextAlign;

public final class GameUnderwayDialog extends Dialog {
  private final TimerSubscriber timer;
  private final Span elapsedTimeLabel = new Span();

  public GameUnderwayDialog(PlayerDto player, GameService gameService) {
    super("%s's game is underway!".formatted(player.firstName()));

    setModality(ModalityMode.STRICT);
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);
    setResizable(true);
    setDraggable(true);

    this.timer = new TimerSubscriber(UI.getCurrentOrThrow());
    gameService.timeGame().subscribe(this.timer);

    this.elapsedTimeLabel.getStyle().setFontWeight(FontWeight.BOLD);
    this.elapsedTimeLabel.getStyle().setTextAlign(TextAlign.CENTER);
    this.elapsedTimeLabel.getStyle().setDisplay(Display.BLOCK);

    add(this.elapsedTimeLabel);

    var cancelButton = new Button("Cancel", _ -> cancelGame());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    var completeGame = new Button("Complete Game", _ -> completeGame());
    completeGame.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    var footer = new HorizontalLayout(cancelButton, completeGame);
    footer.setSpacing(true);
    footer.setWidthFull();
    footer.setJustifyContentMode(JustifyContentMode.CENTER);
    getFooter().add(footer);
  }

  private void cancelGame() {
    this.timer.cancel();
    close();
  }

  private void completeGame() {
    this.timer.complete();
    close();
  }

  public Optional<Duration> getElapsedTime() {
    return this.timer.getElapsedTime();
  }

  private class TimerSubscriber implements Subscriber<Long> {
    private final UI ui;
    private Subscription subscription;
    private Duration elapsedTime;

    private TimerSubscriber(UI ui) {
      this.ui = ui;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      this.subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Long item) {
      this.elapsedTime = Duration.ofSeconds(item);
      this.ui.access(() -> elapsedTimeLabel.setText(DurationFormatter.format(this.elapsedTime)));
    }

    @Override
    public void onError(Throwable throwable) {
      cancelGame();
    }

    @Override
    public void onComplete() {
      // Should never be called
    }

    private void shutdownSubsription() {
      Optional.ofNullable(this.subscription)
          .ifPresent(Subscription::cancel);
    }

    public void cancel() {
      this.elapsedTime = null;
      shutdownSubsription();
    }

    public void complete() {
      shutdownSubsription();
    }

    public Optional<Duration> getElapsedTime() {
      return Optional.ofNullable(this.elapsedTime);
    }
  }
}
