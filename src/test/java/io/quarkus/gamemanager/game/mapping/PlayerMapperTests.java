package io.quarkus.gamemanager.game.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.domain.jpa.Player;
import io.quarkus.test.junit.QuarkusTest;

import net.datafaker.Faker;

@QuarkusTest
class PlayerMapperTests {
  @Inject
  PlayerMapper mapper;

  @Test
  void mappingToDto() {
    var fakeData = new Faker();
    var player = new Player(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var expected = new PlayerDto(player.firstName(), player.lastName(), player.email());

    assertThat(this.mapper.toDto(player))
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  void mappingToEntity() {
    var fakeData = new Faker();
    var player = new PlayerDto(fakeData.name().firstName(), fakeData.name().lastName(), fakeData.internet().emailAddress());
    var expected = new Player(player.firstName(), player.lastName(), player.email());

    assertThat(this.mapper.toEntity(player))
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }
}