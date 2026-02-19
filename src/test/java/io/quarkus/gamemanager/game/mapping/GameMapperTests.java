package io.quarkus.gamemanager.game.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.gamemanager.game.domain.jpa.Player;
import io.quarkus.test.junit.QuarkusTest;

import net.datafaker.Faker;

@QuarkusTest
class GameMapperTests {
  @Inject
  GameMapper mapper;

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
        .withGame(game);

    game.setEvent(event);

    var expectedGame = new GameDto(
        game.getId(),
        expectedPlayer,
        event.getId(),
        game.getGameDate(),
        game.getTimeToComplete()
    );

    assertThat(this.mapper.toDto(game))
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expectedGame);
  }

  @Test
  void mappingToEntity() {
    var fakeData = new Faker();
    var expectedPlayer = new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var player = new PlayerDto(expectedPlayer.firstName(), expectedPlayer.lastName(), expectedPlayer.email());
    var game = new GameDto(
        (long) fakeData.number().positive(),
        player,
        (long) fakeData.number().positive(),
        Instant.now(),
        fakeData.duration().atMostMinutes(1)
    );

    var expectedGame = new Game()
        .withId(game.id())
        .withPlayer(expectedPlayer)
        .withTimeToComplete(game.timeToComplete())
        .withEvent(new Event().withId(game.eventId()))
        .withGameDate(game.gameDate());

    assertThat(this.mapper.toEntity(game))
        .isNotNull()
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*hibernate.*")
        .isEqualTo(expectedGame);
  }
}