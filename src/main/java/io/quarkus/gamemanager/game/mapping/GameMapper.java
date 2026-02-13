package io.quarkus.gamemanager.game.mapping;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.gamemanager.game.domain.GameDto;
import io.quarkus.gamemanager.game.domain.jpa.Game;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI, uses = PlayerMapper.class)
public interface GameMapper {
  @Mapping(target = "eventId", source = "event.id")
  GameDto toDto(Game game);

  @InheritInverseConfiguration
  Game toEntity(GameDto gameDto);
}
