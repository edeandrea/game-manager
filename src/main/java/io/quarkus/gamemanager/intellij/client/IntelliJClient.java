package io.quarkus.gamemanager.intellij.client;

import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.logging.Log;

@RegisterRestClient(configKey = "intellij")
@Path("/")
public interface IntelliJClient {
  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  Response endpoint();

  default void waitUntilAcceptingConnections() {
    await("IntelliJ MCP Server available")
        .ignoreExceptions()
        .atMost(Duration.ofMinutes(1))
        .pollInterval(Duration.ofSeconds(3))
        .pollDelay(Duration.ofSeconds(2))
        .logging(log -> Log.infof("Checking IntelliJ MCP Server availability: %s", log))
        .until(() -> {
          try (var response = endpoint()) {
            Log.debugf("IntelliJ MCP Server response: %s", response.getStatus());
            return response.getStatus() == Status.OK.getStatusCode();
          }
        });
  }
}
