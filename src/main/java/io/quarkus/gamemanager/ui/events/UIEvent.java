package io.quarkus.gamemanager.ui.events;

import com.vaadin.flow.component.UI;

public interface UIEvent<T> {
  UI source();
}
