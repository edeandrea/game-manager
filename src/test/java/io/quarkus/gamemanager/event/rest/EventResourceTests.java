package io.quarkus.gamemanager.event.rest;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.gamemanager.event.domain.jpa.EventTestHelper;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.http.ContentType;

@QuarkusTest
class EventResourceTests {
  @Inject
  EventRepository eventRepository;

  @BeforeEach
  @Transactional
  void beforeEach() {
    this.eventRepository.deleteAllWithCascade();
  }

  @Test
  void findAllEvents() {
    assertThat(this.eventRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent();
    var event2 = EventTestHelper.createEvent();

    QuarkusTransaction.requiringNew()
        .run(() -> {
          this.eventRepository.persist(event1, event2);
          this.eventRepository.flush();
        });

    assertThat(this.eventRepository.count()).isEqualTo(2);

    get("/events").then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("$.size()", is(2))
        .body("[0].id", is(event1.getId().intValue()))
        .body("[0].eventDate", is(event1.getEventDate().toString()))
        .body("[0].name", is(event1.getName()))
        .body("[0].description", is(event1.getDescription()))
        .body("[0].games.size()", is(2))
        .body("[0].games[0].id", is(event1.getGames().getFirst().getId().intValue()))
        .body("[0].games[0].player.firstName", is(event1.getGames().getFirst().getPlayer().firstName()))
        .body("[0].games[0].player.lastName", is(event1.getGames().getFirst().getPlayer().lastName()))
        .body("[0].games[0].player.email", is(event1.getGames().getFirst().getPlayer().email()))
        .body("[0].games[0].eventId", is(event1.getId().intValue()))
        .body("[0].games[0].gameDate", is(event1.getGames().getFirst().getGameDate().toString()))
        .body("[0].games[0].timeToComplete", is((float) event1.getGames().getFirst().getTimeToComplete().getSeconds()))
        .body("[0].games[1].id", is(event1.getGames().getLast().getId().intValue()))
        .body("[0].games[1].player.firstName", is(event1.getGames().getLast().getPlayer().firstName()))
        .body("[0].games[1].player.lastName", is(event1.getGames().getLast().getPlayer().lastName()))
        .body("[0].games[1].player.email", is(event1.getGames().getLast().getPlayer().email()))
        .body("[0].games[1].eventId", is(event1.getId().intValue()))
        .body("[0].games[1].gameDate", is(event1.getGames().getLast().getGameDate().toString()))
        .body("[0].games[1].timeToComplete", is((float) event1.getGames().getLast().getTimeToComplete().getSeconds()))
        .body("[1].id", is(event2.getId().intValue()))
        .body("[1].eventDate", is(event2.getEventDate().toString()))
        .body("[1].name", is(event2.getName()))
        .body("[1].description", is(event2.getDescription()))
        .body("[1].games.size()", is(2))
        .body("[1].games[0].id", is(event2.getGames().getFirst().getId().intValue()))
        .body("[1].games[0].player.firstName", is(event2.getGames().getFirst().getPlayer().firstName()))
        .body("[1].games[0].player.lastName", is(event2.getGames().getFirst().getPlayer().lastName()))
        .body("[1].games[0].player.email", is(event2.getGames().getFirst().getPlayer().email()))
        .body("[1].games[0].eventId", is(event2.getId().intValue()))
        .body("[1].games[0].gameDate", is(event2.getGames().getFirst().getGameDate().toString()))
        .body("[1].games[0].timeToComplete", is((float) event2.getGames().getFirst().getTimeToComplete().getSeconds()))
        .body("[1].games[1].id", is(event2.getGames().getLast().getId().intValue()))
        .body("[1].games[1].player.firstName", is(event2.getGames().getLast().getPlayer().firstName()))
        .body("[1].games[1].player.lastName", is(event2.getGames().getLast().getPlayer().lastName()))
        .body("[1].games[1].player.email", is(event2.getGames().getLast().getPlayer().email()))
        .body("[1].games[1].eventId", is(event2.getId().intValue()))
        .body("[1].games[1].gameDate", is(event2.getGames().getLast().getGameDate().toString()))
        .body("[1].games[1].timeToComplete", is((float) event2.getGames().getLast().getTimeToComplete().getSeconds()));
  }
}