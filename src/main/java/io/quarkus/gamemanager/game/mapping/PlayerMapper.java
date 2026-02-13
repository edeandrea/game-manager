package io.quarkus.gamemanager.game.mapping;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.gamemanager.game.domain.PlayerDto;
import io.quarkus.gamemanager.game.domain.jpa.Player;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface PlayerMapper {
  PlayerDto toDto(Player player);

  @InheritInverseConfiguration
  Player toEntity(PlayerDto playerDto);
}
