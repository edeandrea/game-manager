package io.quarkus.gamemanager.game.domain;

public record GameTracker(Process appProcess, Process externalProcess) {
  public void shutdown() {
    this.appProcess.destroy();
    this.externalProcess.destroy();
  }
}
