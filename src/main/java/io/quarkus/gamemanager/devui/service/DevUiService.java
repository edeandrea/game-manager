package io.quarkus.gamemanager.devui.service;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.gamemanager.game.config.GameConfig;
import io.quarkus.gamemanager.game.service.GameDevUiClient;
import io.quarkus.gamemanager.ide.IdeService;
import io.quarkus.logging.Log;

public class DevUiService implements IdeService {
  private final GameDevUiClient gameDevUiClient;
  private final GameConfig gameConfig;

  public DevUiService(@RestClient GameDevUiClient gameDevUiClient, GameConfig gameConfig) {
    this.gameDevUiClient = gameDevUiClient;
    this.gameConfig = gameConfig;
  }

  @Override
  public void startInIde(Path gameDir) {
    try {
      var appProcess = new ProcessBuilder(this.gameConfig.appStartupCommand().split(" "))
          .directory(gameDir.toFile())
          .start();

      appProcess.onExit()
          .thenAccept(p -> Log.infof("Dev mode process %d exited with code: %d", p.pid(), p.exitValue()));

      Log.infof("Started appProcess: %s", appProcess.pid());

      // Now wait for dev mode to start
      await("For app to start")
          .ignoreException(ProcessingException.class)
          .atMost(Duration.ofMinutes(1))
          .pollInterval(Duration.ofSeconds(3))
          .pollDelay(Duration.ofSeconds(3))
          .logging(log -> Log.infof("Checking to see if app is up: %s", log))
          .until(() -> appProcess.isAlive() && this.gameDevUiClient.health().getStatus() == Status.OK.getStatusCode());

      new ProcessBuilder("open", "%s/workspace".formatted(this.gameConfig.appDevUiUrl().toString()))
          .start()
          .onExit()
          .thenAccept(p -> Log.infof("External process %d exited with code: %d", p.pid(), p.exitValue()));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
