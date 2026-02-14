package io.quarkus.gamemanager.ui.views;

import io.quarkus.gamemanager.event.service.EventService;
import io.quarkus.gamemanager.ui.MainLayout;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(layout = MainLayout.class, value = "events")
public class ManageEventsView extends VerticalLayout {
  public ManageEventsView(EventService eventService) {
  }
}
