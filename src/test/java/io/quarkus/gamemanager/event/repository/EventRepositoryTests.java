package io.quarkus.gamemanager.event.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.event.domain.jpa.EventTestHelper;
import io.quarkus.gamemanager.game.domain.jpa.Game;
import io.quarkus.gamemanager.game.repository.GameRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestTransaction
class EventRepositoryTests {
  @Inject
  EventRepository eventRepository;

  @Inject
  GameRepository gameRepository;

  @Test
  void deleteAllWithCascade() {
    assertThat(this.eventRepository.count()).isZero();
    assertThat(this.gameRepository.count()).isZero();

    var event1 = EventTestHelper.createEvent();
    var event2 = EventTestHelper.createEvent();

    this.eventRepository.persist(event1, event2);
    this.eventRepository.flush();

    assertThat(this.eventRepository.count()).isEqualTo(2);
    assertThat(this.gameRepository.count()).isEqualTo(4);

    verifyEvents(event1, event2);

    this.eventRepository.deleteAllWithCascade();
    assertThat(this.eventRepository.count()).isZero();
    assertThat(this.gameRepository.count()).isZero();
  }

  private void verifyEvents(Event... expectedEvents) {
    var expectedGames = Stream.of(expectedEvents)
        .flatMap(e -> e.getGames().stream())
        .toArray(Game[]::new);

    assertThat(this.eventRepository.listAll())
        .containsExactlyInAnyOrder(expectedEvents)
        .flatMap(Event::getGames)
        .containsExactlyInAnyOrder(expectedGames);
  }
}