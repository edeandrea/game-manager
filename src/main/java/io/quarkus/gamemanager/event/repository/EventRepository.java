package io.quarkus.gamemanager.event.repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import io.quarkus.gamemanager.event.domain.EventQuery;
import io.quarkus.gamemanager.event.domain.jpa.Event;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {
  private final CriteriaBuilder criteriaBuilder;

  public EventRepository(CriteriaBuilder criteriaBuilder) {
    this.criteriaBuilder = criteriaBuilder;
  }

  @Transactional
  public void deleteAllWithCascade() {
    find("SELECT id FROM Event")
        .project(Long.class)
        .list()
        .forEach(this::deleteById);
  }

  public List<Event> getEvents(EventQuery eventQuery) {
    var query = this.criteriaBuilder.createQuery(Event.class);
    var root = query.from(Event.class);
    var predicates = new ArrayList<Predicate>(3);

    eventQuery.getStart()
        .map(start -> this.criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), start))
        .ifPresent(predicates::add);

    eventQuery.getEnd()
        .map(end -> this.criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), end))
        .ifPresent(predicates::add);

    eventQuery.getName()
        .map(name -> this.criteriaBuilder.like(this.criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"))
        .ifPresent(predicates::add);

    query.where(predicates.toArray(Predicate[]::new));

    return getEntityManager().createQuery(query).getResultList();
  }
}
