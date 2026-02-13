package io.quarkus.gamemanager.event.mapping;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.gamemanager.event.domain.EventDto;
import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.gamemanager.game.mapping.GameMapper;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI, uses = GameMapper.class)
public interface EventMapper {
  EventDto toDto(Event event);

  @InheritInverseConfiguration
  Event toEntity(EventDto eventDto);
}
