package dev.mkgg.notes.note;

import java.time.Duration;

/** Auto-expire durations offered by the frontend save dialog. */
public enum ExpiryOption {
  ONE_HOUR("1 Hour", Duration.ofHours(1)),
  ONE_DAY("1 Day", Duration.ofDays(1)),
  SEVEN_DAYS("7 Days", Duration.ofDays(7)),
  FOURTEEN_DAYS("14 Days", Duration.ofDays(14)),
  ONE_MONTH("1 Month", Duration.ofDays(30));

  private final String label;
  private final Duration duration;

  ExpiryOption(String label, Duration duration) {
    this.label = label;
    this.duration = duration;
  }

  /**
   * Resolves the option matching a frontend label such as {@code "7 Days"}.
   *
   * @throws IllegalArgumentException if the label is not a known option
   */
  public static ExpiryOption fromLabel(String label) {
    for (ExpiryOption option : values()) {
      if (option.label.equals(label)) {
        return option;
      }
    }
    throw new IllegalArgumentException("Unknown expiry option: " + label);
  }

  public Duration duration() {
    return duration;
  }
}
