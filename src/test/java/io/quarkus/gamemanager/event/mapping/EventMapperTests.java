package io.quarkus.gamemanager.event.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.gamemanager.game.domain.jpa.Player;
import io.quarkus.test.junit.QuarkusTest;

import net.datafaker.Faker;

@QuarkusTest
class EventMapperTests {
  @Inject
  EventMapper mapper;

  @Test
  void mappingToDto() {
    var fakeData = new Faker();
    var player = new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var expectedPlayer = new PlayerDto(player.firstName(), player.lastName(), player.email());
    var game = new Game()
        .withId((long) fakeData.number().positive())
        .withPlayer(player)
        .withGameDate(Instant.now())
        .withTimeToComplete(fakeData.duration().atMostMinutes(1));

    var event = new Event()
        .withId((long) fakeData.number().positive())
        .withName(fakeData.lorem().characters(10, 20))
        .withDescription(fakeData.lorem().sentence())
        .withEventDate(LocalDate.now())
        .withGame(game);

    game.setEvent(event);

    var expectedGame = new GameDto(
        game.getId(),
        expectedPlayer,
        event.getId(),
        game.getGameDate(),
        game.getTimeToComplete()
    );

    var expectedEvent = new EventDto(
        event.getId(),
        event.getEventDate(),
        event.getName(),
        event.getDescription(),
        List.of(expectedGame)
    );

    assertThat(this.mapper.toDto(event))
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expectedEvent);
  }

  @Test
  void mappingToEntity() {
    var fakeData = new Faker();
    var player = new PlayerDto(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var expectedPlayer = new Player(player.firstName(), player.lastName(), player.email());
    var game = new GameDto(
        (long) fakeData.number().positive(),
        player,
        (long) fakeData.number().positive(),
        Instant.now(),
        fakeData.duration().atMostMinutes(1)
    );

    var event = new EventDto(
        game.eventId(),
        LocalDate.now(),
        fakeData.lorem().characters(10, 20),
        fakeData.lorem().sentence(),
        List.of(game)
    );

    var expectedGame = new Game()
        .withId(game.id())
        .withPlayer(expectedPlayer)
        .withTimeToComplete(game.timeToComplete())
        .withGameDate(game.gameDate());

    var expectedEvent = new Event()
        .withName(event.name())
        .withDescription(event.description())
        .withEventDate(event.eventDate())
        .withGame(expectedGame)
        .withId(event.id());

    expectedGame.setEvent(expectedEvent);

    var convertedEvent = this.mapper.toEntity(event);
    assertThat(convertedEvent)
        .isNotNull()
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*[game|hibernate].*")
        .isEqualTo(expectedEvent);

    assertThat(convertedEvent.getGames())
        .singleElement()
        .extracting(
            Game::getGameDate,
            Game::getId,
            Game::getPlayer,
            Game::getTimeToComplete,
            g -> g.getEvent().getId()
        )
        .containsExactly(
            expectedGame.getGameDate(),
            expectedGame.getId(),
            expectedPlayer,
            expectedGame.getTimeToComplete(),
            expectedGame.getEvent().getId()
        );
  }
}