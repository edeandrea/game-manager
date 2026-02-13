package io.quarkus.gamemanager.event.rest;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.event.domain.jpa.EventTestHelper;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.gamemanager.game.domain.jpa.Player;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.http.ContentType;

import net.datafaker.Faker;

@QuarkusTest
class EventResourceTests {
  @Inject
  EventRepository eventRepository;

  @AfterEach
  @Transactional
  void afterEach() {
    this.eventRepository.deleteAllWithCascade();
  }

  @Test
  void createEvent() {
    var event = new EventDto(null, Instant.now(), "Test Event", "Test Description");

    given()
        .contentType(ContentType.JSON)
        .body(event)
        .post("/events")
        .then()
        .statusCode(Status.OK.getStatusCode())
        .body("id", greaterThanOrEqualTo(1))
        .body("eventDate", is(event.eventDate().toString()))
        .body("name", is(event.name()))
        .body("description", is(event.description()))
        .body("games", nullValue());
  }

  @Test
  void createInvalidEvent() {
    given()
        .contentType(ContentType.JSON)
        .body(new EventDto(null, null, null, null, null))
        .post("/events")
        .then()
        .statusCode(Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void deleteEvent() {
    assertThat(this.eventRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent();
    var event2 = EventTestHelper.createEvent();

    QuarkusTransaction.requiringNew()
        .run(() -> {
          this.eventRepository.persist(event1, event2);
          this.eventRepository.flush();
        });

    assertThat(this.eventRepository.count()).isEqualTo(2);

    delete("/events/{eventId}", event1.getId()).then()
        .statusCode(Status.NO_CONTENT.getStatusCode());

    QuarkusTransaction.requiringNew()
        .run(() -> {
          assertThat(this.eventRepository.count()).isOne();
          assertThat(this.eventRepository.findByIdOptional(event1.getId())).isNotPresent();
        });
  }

  @Test
  void findById() {
    assertThat(this.eventRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent();
    var event2 = EventTestHelper.createEvent();

    QuarkusTransaction.requiringNew()
        .run(() -> {
          this.eventRepository.persist(event1, event2);
          this.eventRepository.flush();
        });

    assertThat(this.eventRepository.count()).isEqualTo(2);

    get("/events/{eventId}", event1.getId()).then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("id", is(event1.getId().intValue()))
        .body("eventDate", is(event1.getEventDate().toString()))
        .body("name", is(event1.getName()))
        .body("description", is(event1.getDescription()))
        .body("games.size()", is(2))
        .body("games[0].id", is(event1.getGames().getFirst().getId().intValue()))
        .body("games[0].player.firstName", is(event1.getGames().getFirst().getPlayer().firstName()))
        .body("games[0].player.lastName", is(event1.getGames().getFirst().getPlayer().lastName()))
        .body("games[0].player.email", is(event1.getGames().getFirst().getPlayer().email()))
        .body("games[0].eventId", is(event1.getId().intValue()))
        .body("games[0].gameDate", is(event1.getGames().getFirst().getGameDate().toString()))
        .body("games[0].timeToComplete", is((float) event1.getGames().getFirst().getTimeToComplete().getSeconds()))
        .body("games[1].id", is(event1.getGames().getLast().getId().intValue()))
        .body("games[1].player.firstName", is(event1.getGames().getLast().getPlayer().firstName()))
        .body("games[1].player.lastName", is(event1.getGames().getLast().getPlayer().lastName()))
        .body("games[1].player.email", is(event1.getGames().getLast().getPlayer().email()))
        .body("games[1].eventId", is(event1.getId().intValue()))
        .body("games[1].gameDate", is(event1.getGames().getLast().getGameDate().toString()))
        .body("games[1].timeToComplete", is((float) event1.getGames().getLast().getTimeToComplete().getSeconds()));
  }

  @Test
  void findNonexistentEvent() {
    get("/events/{eventId}", 123).then()
        .statusCode(Status.NOT_FOUND.getStatusCode());
  }

  @Test
  void findByDates() {
    assertThat(this.eventRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent()
        .withName("Event 1 awesome")
        .withEventDate(Instant.now().minus(Duration.ofDays(7)));
    var event2 = EventTestHelper.createEvent()
        .withName("Event 2")
        .withEventDate(Instant.now());

    QuarkusTransaction.requiringNew()
        .run(() -> {
          this.eventRepository.persist(event1, event2);
          this.eventRepository.flush();
        });

    assertThat(this.eventRepository.count()).isEqualTo(2);

    given()
        .queryParam("start", Instant.now().minus(Duration.ofDays(10)).toString())
        .queryParam("end", Instant.now().minus(Duration.ofDays(2)).toString())
        .when()
        .get("/events")
        .then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("$.size()", is(1))
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
        .body("[0].games[1].timeToComplete", is((float) event1.getGames().getLast().getTimeToComplete().getSeconds()));
  }

  @Test
  void findByName() {
    assertThat(this.eventRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent()
        .withName("Event 1 awesome");
    var event2 = EventTestHelper.createEvent()
        .withName("Event 2");

    QuarkusTransaction.requiringNew()
        .run(() -> {
          this.eventRepository.persist(event1, event2);
          this.eventRepository.flush();
        });

    assertThat(this.eventRepository.count()).isEqualTo(2);

    given()
        .queryParam("name", "aWeSoMe")
        .when()
        .get("/events").then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("$.size()", is(1))
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
        .body("[0].games[1].timeToComplete", is((float) event1.getGames().getLast().getTimeToComplete().getSeconds()));
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

  @Test
  void getLeaderboard() {
    var event = QuarkusTransaction.requiringNew()
        .call(() -> {
          var fakeData = new Faker();
          var e = new Event()
              .withDescription(fakeData.lorem().sentence())
              .withEventDate(Instant.now())
              .withName(fakeData.lorem().word())
              .withGames(
                  List.of(
                      new Game()
                          .withPlayer(new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress()))
                          .withTimeToComplete(Duration.ofMinutes(1)),
                      new Game()
                          .withPlayer(new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress()))
                          .withTimeToComplete(Duration.ofMinutes(2)),
                      new Game()
                          .withPlayer(new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress()))
                          .withTimeToComplete(Duration.ofSeconds(30))
                  )
              );

          this.eventRepository.persistAndFlush(e);

          assertThat(this.eventRepository.count()).isOne();
          assertThat(e.getGames().size()).isEqualTo(3);

          return e;
        });

    var game1 = event.getGames().get(0);
    var game2 = event.getGames().get(1);
    var game3 = event.getGames().get(2);

    get("/events/{eventId}/leaderboard", event.getId()).then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("$.size()", is(3))
        .body("[0].id", is(game3.getId().intValue()))
        .body("[0].player.firstName", is(game3.getPlayer().firstName()))
        .body("[0].player.lastName", is(game3.getPlayer().lastName()))
        .body("[0].player.email", is(game3.getPlayer().email()))
        .body("[0].eventId", is(event.getId().intValue()))
        .body("[0].gameDate", is(game3.getGameDate().toString()))
        .body("[0].timeToComplete", is((float) game3.getTimeToComplete().getSeconds()))
        .body("[1].id", is(game1.getId().intValue()))
        .body("[1].player.firstName", is(game1.getPlayer().firstName()))
        .body("[1].player.lastName", is(game1.getPlayer().lastName()))
        .body("[1].player.email", is(game1.getPlayer().email()))
        .body("[1].eventId", is(event.getId().intValue()))
        .body("[1].gameDate", is(game1.getGameDate().toString()))
        .body("[1].timeToComplete", is((float) game1.getTimeToComplete().getSeconds()))
        .body("[2].id", is(game2.getId().intValue()))
        .body("[2].player.firstName", is(game2.getPlayer().firstName()))
        .body("[2].player.lastName", is(game2.getPlayer().lastName()))
        .body("[2].player.email", is(game2.getPlayer().email()))
        .body("[2].eventId", is(event.getId().intValue()))
        .body("[2].gameDate", is(game2.getGameDate().toString()))
        .body("[2].timeToComplete", is((float) game2.getTimeToComplete().getSeconds()));
  }
}