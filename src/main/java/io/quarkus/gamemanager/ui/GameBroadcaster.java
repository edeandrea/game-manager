package io.quarkus.gamemanager.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.gamemanager.ui.events.UIEvent;

import com.vaadin.flow.component.DetachNotifier;
import com.vaadin.flow.shared.Registration;

@ApplicationScoped
public class GameBroadcaster {
  private final Map<Class<UIEvent<? extends UIEvent<?>>>, List<Consumer<UIEvent<? extends UIEvent<?>>>>> listeners = new ConcurrentHashMap<>();

  public <T extends UIEvent<T>> Registration register(Class<T> eventType, Consumer<T> listener, DetachNotifier detachNotifier) {
    this.listeners.compute(
        (Class<UIEvent<? extends UIEvent<?>>>) eventType,
        (_, list) -> addToExistingOrNewList(listener, list)
    );

    Registration registration = () -> getListeners(eventType).remove(listener);
    detachNotifier.addDetachListener(_ -> registration.remove());

    return registration;
  }

  private <T extends UIEvent<T>> List<Consumer<UIEvent<? extends UIEvent<?>>>> getListeners(Class<T> eventType) {
    return this.listeners.getOrDefault(eventType, new ArrayList<>());
  }

  private <T extends UIEvent<T>> List<Consumer<UIEvent<? extends UIEvent<?>>>> addToExistingOrNewList(Consumer<T> listener, List<Consumer<UIEvent<? extends UIEvent<?>>>> list) {
    List<Consumer<UIEvent<? extends UIEvent<?>>>> result = (list != null) ? list : new CopyOnWriteArrayList<>();
    result.add((Consumer<UIEvent<? extends UIEvent<?>>>) listener);

    return result;
  }

  public <T extends UIEvent<T>> void fireEvent(T event) {
    getListeners(event.getClass())
        .forEach(listener -> ((Consumer<T>) listener).accept(event));
  }
}
