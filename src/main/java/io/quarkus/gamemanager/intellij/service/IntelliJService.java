package io.quarkus.gamemanager.intellij.service;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.gamemanager.game.config.GameConfig;
import io.quarkus.gamemanager.game.service.GameDevUiClient;
import io.quarkus.gamemanager.ide.IdeService;
import io.quarkus.logging.Log;

@ApplicationScoped
public class IntelliJService implements IdeService {
  private final GameDevUiClient gameDevUiClient;
  private final GameConfig gameConfig;
  private final IntelliJActionService intelliJActionService;
  private final AtomicReference<Process> intellijProcess = new AtomicReference<>();

  public IntelliJService(@RestClient GameDevUiClient gameDevUiClient, GameConfig gameConfig, IntelliJActionService intelliJActionService) {
    this.gameDevUiClient = gameDevUiClient;
    this.gameConfig = gameConfig;
    this.intelliJActionService = intelliJActionService;
  }

  @Override
  public void startInIde(Path gameDir) {
    Log.infof("Opening new game in directory [%s]", gameDir);
    var intellijProcess = getIntellijProcess();
    this.intelliJActionService.executeRunConfiguration("booth-game", gameDir.toString());

    Log.infof("Started intellij process: %s", intellijProcess.pid());

    // Now wait for dev mode to start
    await("game startup")
        .ignoreException(ProcessingException.class)
        .atMost(Duration.ofMinutes(1))
        .pollInterval(Duration.ofSeconds(3))
        .pollDelay(Duration.ofSeconds(2))
        .logging(log -> Log.infof("Checking to see if game is up: %s", log))
        .until(() -> this.gameDevUiClient.health().getStatus() == Status.OK.getStatusCode());

    try {
      new ProcessBuilder("open", this.gameConfig.appDevUiUrl().toString())
          .start();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Process createIntellijProcess() {
    try {
      var process = new ProcessBuilder("intellij", this.gameConfig.checkoutDir().normalize().toAbsolutePath().toString())
          .start();

      process.onExit().thenAccept(p -> Log.infof("Intellij process %d exited with code: %d", p.pid(), p.exitValue()));

      return process;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized Process getIntellijProcess() {
    var process = Optional.ofNullable(this.intellijProcess.get())
        .filter(Process::isAlive)
        .orElseGet(this::createIntellijProcess);

    this.intellijProcess.set(process);

    return process;
  }
}
