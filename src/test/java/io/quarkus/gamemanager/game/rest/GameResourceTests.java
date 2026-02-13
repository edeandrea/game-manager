package io.quarkus.gamemanager.game.rest;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.net.URL;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import io.quarkus.gamemanager.event.domain.jpa.EventTestHelper;
import io.quarkus.gamemanager.event.repository.EventRepository;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.http.ContentType;

import net.datafaker.Faker;

@QuarkusTest
class GameResourceTests {
  @Inject
  EventRepository eventRepository;

  @Inject
  GameRepository gameRepository;

  @TestHTTPEndpoint(GameResource.class)
  @TestHTTPResource
  URL resourceUrl;

  @AfterEach
  @Transactional
  void afterEach() {
    this.eventRepository.deleteAllWithCascade();
  }

  @Test
  void addGameToNonExistentEvent() {
    var fakeData = new Faker();
    var game = new GameDto(null,
        new PlayerDto(
            fakeData.name().firstName(),
            fakeData.name().lastName(),
            fakeData.internet().emailAddress()),
        (long) fakeData.number().positive(),
        null,
        fakeData.duration().atMostMinutes(1));

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(game)
        .when()
        .post("/games")
        .then()
        .statusCode(Status.NOT_ACCEPTABLE.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("invalidGames")
  @NullSource
  void addInvalidGame(GameDto invalidGame) {
    var game = invalidGame;

    if (invalidGame != null) {
      var event1 = EventTestHelper.createEvent();

      QuarkusTransaction.requiringNew()
          .run(() -> this.eventRepository.persistAndFlush(event1));

      if (invalidGame.eventId() != null) {
        game = new GameDto(invalidGame.id(), invalidGame.player(), event1.getId(), invalidGame.gameDate(), invalidGame.timeToComplete());
      }
    }

    var request = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON);

    if (game != null) {
      request = request.body(game);
    }

    request.when()
        .post("/games")
        .then()
        .statusCode(Status.BAD_REQUEST.getStatusCode());
  }

  static Stream<GameDto> invalidGames() {
    var fakeData = new Faker();
    return Stream.of(
        new GameDto(
            null,
            null,
            (long) fakeData.number().positive(),
            null,
            fakeData.duration().atMostMinutes(1)
        ),
        new GameDto(
            null,
            new PlayerDto(null, fakeData.name().lastName(), fakeData.internet().emailAddress()),
            (long) fakeData.number().positive(),
            null,
            fakeData.duration().atMostMinutes(1)
        ),
        new GameDto(
            null,
            new PlayerDto(fakeData.name().firstName(), null, fakeData.internet().emailAddress()),
            (long) fakeData.number().positive(),
            null,
            fakeData.duration().atMostMinutes(1)
        ),
        new GameDto(
            null,
            new PlayerDto(fakeData.name().firstName(), fakeData.name().lastName(), null),
            (long) fakeData.number().positive(),
            null,
            fakeData.duration().atMostMinutes(1)
        ),
        new GameDto(
            null,
            new PlayerDto(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress()),
            null,
            null,
            fakeData.duration().atMostMinutes(1)
        ),
        new GameDto(
            null,
            new PlayerDto(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress()),
            (long) fakeData.number().positive(),
            null,
            null
        )
    );
  }

  @Test
  void addGame() throws InterruptedException {
    findAllGamesForEvent();

    var event = QuarkusTransaction.requiringNew()
        .call(() -> {
          var e = this.eventRepository.listAll().getFirst();
          assertThat(e.getGames()).hasSize(2);
          return e;
        });

    var fakeData = new Faker();
    var game = new GameDto(null,
        new PlayerDto(
            fakeData.name().firstName(),
            fakeData.name().lastName(),
            fakeData.internet().emailAddress()),
        event.getId(),
        null,
        fakeData.duration().atMostMinutes(1));

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(game)
        .when()
        .post("/games")
        .then()
        .statusCode(Status.OK.getStatusCode())
        .body("id", greaterThanOrEqualTo(0))
        .body("player.firstName", is(game.player().firstName()))
        .body("player.lastName", is(game.player().lastName()))
        .body("player.email", is(game.player().email()))
        .body("eventId", is(event.getId().intValue()))
        .body("gameDate", not(blankOrNullString()))
        .body("timeToComplete", is((float) game.timeToComplete().getSeconds()));

    QuarkusTransaction.requiringNew()
        .run(() -> {
          var e = this.eventRepository.listAll().getFirst();
          assertThat(e.getGames()).hasSize(3);
        });
  }

  @Test
  void findAllGamesForEvent() {
    assertThat(this.eventRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent();
    var event2 = EventTestHelper.createEvent();

    QuarkusTransaction.requiringNew()
        .run(() -> {
          this.eventRepository.persist(event1, event2);
          this.eventRepository.flush();
        });

    assertThat(this.eventRepository.count()).isEqualTo(2);
    assertThat(this.gameRepository.count()).isEqualTo(4);

    get("/games/event/{eventId}", event2.getId()).then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("$.size()", is(2))
        .body("[0].id", is(event2.getGames().getFirst().getId().intValue()))
        .body("[0].player.firstName", is(event2.getGames().getFirst().getPlayer().firstName()))
        .body("[0].player.lastName", is(event2.getGames().getFirst().getPlayer().lastName()))
        .body("[0].player.email", is(event2.getGames().getFirst().getPlayer().email()))
        .body("[0].eventId", is(event2.getId().intValue()))
        .body("[0].gameDate", is(event2.getGames().getFirst().getGameDate().toString()))
        .body("[0].timeToComplete", is((float) event2.getGames().getFirst().getTimeToComplete().getSeconds()))
        .body("[1].id", is(event2.getGames().getLast().getId().intValue()))
        .body("[1].player.firstName", is(event2.getGames().getLast().getPlayer().firstName()))
        .body("[1].player.lastName", is(event2.getGames().getLast().getPlayer().lastName()))
        .body("[1].player.email", is(event2.getGames().getLast().getPlayer().email()))
        .body("[1].eventId", is(event2.getId().intValue()))
        .body("[1].gameDate", is(event2.getGames().getLast().getGameDate().toString()))
        .body("[1].timeToComplete", is((float) event2.getGames().getLast().getTimeToComplete().getSeconds()));
  }
}