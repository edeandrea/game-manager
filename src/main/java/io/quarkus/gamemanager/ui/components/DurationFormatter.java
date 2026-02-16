package io.quarkus.gamemanager.ui.components;

import java.time.Duration;

public final class DurationFormatter {
  private DurationFormatter() {
  }

  public static String format(Duration duration) {
    var mins = duration.toMinutes();
    var minLabel = (mins == 1) ? "minute" : "minutes";
    var minsPart = (mins == 0) ? "" : "%d %s ".formatted(mins, minLabel);

    var secs = duration.toSecondsPart();
    var secLabel = (secs == 1) ? "second" : "seconds";
    var secsPart = (secs == 0) ? "" : "%d %s".formatted(secs, secLabel);

    return "%s%s".formatted(minsPart, secsPart);
  }
}
