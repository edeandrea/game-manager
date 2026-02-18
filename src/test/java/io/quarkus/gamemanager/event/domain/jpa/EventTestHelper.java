package io.quarkus.gamemanager.event.domain.jpa;

import java.time.LocalDate;
import java.util.List;

import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.gamemanager.game.domain.jpa.Player;

import net.datafaker.Faker;

public class EventTestHelper {
  public static Event createEvent() {
    var fakeData = new Faker();
    var player1 = new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var player2 = new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var game1 = new Game()
        .withPlayer(player1)
        .withTimeToComplete(fakeData.duration().atMostMinutes(1));
    var game2 = new Game()
        .withPlayer(player2)
        .withTimeToComplete(fakeData.duration().atMostMinutes(1));

    return new Event()
        .withName(fakeData.lorem().characters(10, 20))
        .withDescription(fakeData.lorem().sentence())
        .withEventDate(LocalDate.now())
        .withGames(List.of(game1, game2));
  }
}