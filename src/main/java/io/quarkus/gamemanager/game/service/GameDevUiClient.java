package io.quarkus.gamemanager.game.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "game")
@Path("/q")
@Produces(MediaType.APPLICATION_JSON)
public interface GameDevUiClient {
  @GET
  @Path("/health/started")
  Response healthStarted();

  @GET
  @Path("/health/ready")
  Response healthReady();

  @GET
  @Path("/health/live")
  Response healthLive();

  @GET
  @Path("/health")
  Response health();
}
