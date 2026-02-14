package io.quarkus.gamemanager.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style.Position;

public class TitledBorder extends Div {
  public TitledBorder(String title, Component childComponent) {
    super();
    getStyle()
        .setBorder("1px solid var(--lumo-contrast-20pct)")
        .setBorderRadius("var(--lumo-border-radius)")
        .setPadding("var(--lumo-space-m)")
        .setPosition(Position.RELATIVE);

    var titleSpan = new Span(title);
    titleSpan.getStyle()
        .setPosition(Position.ABSOLUTE)
        .setTop("-0.6em")
        .setLeft("0.5em")
        .setBackground("var(--lumo-base-color)")
        .setPadding("0 0.3em");

    add(titleSpan, childComponent);
  }
}
