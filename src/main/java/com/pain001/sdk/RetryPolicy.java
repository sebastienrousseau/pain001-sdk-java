// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * An opt-in retry policy for idempotent calls: {@code GET} only.
 *
 * <p>Set it with {@link Pain001Client.Builder#retryPolicy}; without one the client sends every
 * request once. With one, a {@code GET} ({@code health()}, {@code faces()}, and {@code request}
 * with {@code GET}) is sent again after a connection failure or timeout, or an HTTP 429 or 503
 * answer, up to {@link #maxAttempts()} times in all. A {@code POST} ({@code validate}, {@code
 * generate}, {@code check}, {@code projectFace}, and {@code request} with any other method) is
 * never retried.
 *
 * <p>The wait before retry {@code n} (from 1) is exponential with jitter: {@code d = min(maxBackoff,
 * initialBackoff * 2^(n-1))}, then a random point in {@code [d/2, d]}. A 429 or 503 answer with a
 * {@code Retry-After} header (seconds or an HTTP date) waits exactly that long instead; when it asks
 * for longer than {@link #maxBackoff()} the client stops retrying and returns that answer rather
 * than retry early. The last attempt's answer or failure is what the call returns or raises.
 *
 * <pre>{@code
 * Pain001Client client = Pain001Client.builder()
 *     .baseUrl("http://127.0.0.1:8000")
 *     .retryPolicy(RetryPolicy.builder().maxAttempts(4).build())
 *     .build();
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class RetryPolicy {

  /** How the policy waits; replaced in tests. */
  @FunctionalInterface
  interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
  }

  private final int maxAttempts;
  private final Duration initialBackoff;
  private final Duration maxBackoff;
  private final Sleeper sleeper;
  private final DoubleSupplier random;
  private final Clock clock;

  private RetryPolicy(Builder builder) {
    this.maxAttempts = builder.maxAttempts;
    this.initialBackoff = builder.initialBackoff;
    this.maxBackoff = builder.maxBackoff;
    this.sleeper = builder.sleeper;
    this.random = builder.random;
    this.clock = builder.clock;
  }

  /** Starts a builder with the defaults: 3 attempts, 200 ms initial and 5 s maximum backoff. */
  public static Builder builder() {
    return new Builder();
  }

  /** The number of attempts in all, including the first; at least 1. */
  public int maxAttempts() {
    return maxAttempts;
  }

  /** The wait before the first retry, before jitter. */
  public Duration initialBackoff() {
    return initialBackoff;
  }

  /** The longest wait before any retry, and the longest {@code Retry-After} honoured. */
  public Duration maxBackoff() {
    return maxBackoff;
  }

  /** Whether an answer with this status is worth sending again. */
  static boolean retriesStatus(int status) {
    return status == 429 || status == 503;
  }

  /**
   * The wait before retry {@code retry} (from 1), or empty when the server's {@code Retry-After}
   * asks for longer than {@link #maxBackoff()}.
   */
  Optional<Duration> delay(int retry, Optional<String> retryAfter) {
    Optional<Duration> asked = retryAfter.flatMap(this::parseRetryAfter);
    if (asked.isPresent()) {
      return asked.filter(d -> d.compareTo(maxBackoff) <= 0);
    }
    return Optional.of(backoff(retry));
  }

  private Duration backoff(int retry) {
    int doublings = Math.min(retry - 1, 30);
    long cap = maxBackoff.toNanos();
    long base = Math.min(cap, saturatedShift(initialBackoff.toNanos(), doublings));
    long half = base / 2;
    return Duration.ofNanos(half + (long) ((base - half) * random.getAsDouble()));
  }

  private static long saturatedShift(long value, int bits) {
    return value > (Long.MAX_VALUE >> bits) ? Long.MAX_VALUE : value << bits;
  }

  /** {@code Retry-After} as delta-seconds or an HTTP date; empty when it is neither. */
  Optional<Duration> parseRetryAfter(String value) {
    String text = value.strip();
    if (text.matches("\\d{1,9}")) {
      return Optional.of(Duration.ofSeconds(Long.parseLong(text)));
    }
    try {
      Instant at = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
      Duration wait = Duration.between(clock.instant(), at);
      return Optional.of(wait.isNegative() ? Duration.ZERO : wait);
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  void sleep(Duration duration) throws InterruptedException {
    sleeper.sleep(duration);
  }

  /** Builds a {@link RetryPolicy}. */
  public static final class Builder {

    private int maxAttempts = 3;
    private Duration initialBackoff = Duration.ofMillis(200);
    private Duration maxBackoff = Duration.ofSeconds(5);
    private Sleeper sleeper = d -> Thread.sleep(d.toMillis(), d.toNanosPart() % 1_000_000);
    private DoubleSupplier random = () -> ThreadLocalRandom.current().nextDouble();
    private Clock clock = Clock.systemUTC();

    private Builder() {}

    /** The number of attempts in all, including the first (default 3); 1 disables retries. */
    public Builder maxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      return this;
    }

    /** The wait before the first retry, before jitter (default 200 ms). */
    public Builder initialBackoff(Duration initialBackoff) {
      this.initialBackoff = initialBackoff;
      return this;
    }

    /** The longest wait before any retry (default 5 s), and the longest Retry-After honoured. */
    public Builder maxBackoff(Duration maxBackoff) {
      this.maxBackoff = maxBackoff;
      return this;
    }

    Builder sleeper(Sleeper sleeper) {
      this.sleeper = sleeper;
      return this;
    }

    Builder random(DoubleSupplier random) {
      this.random = random;
      return this;
    }

    Builder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Builds the policy.
     *
     * @throws IllegalArgumentException when {@code maxAttempts} is below 1, a backoff is missing or
     *     not positive, or {@code maxBackoff} is shorter than {@code initialBackoff}
     */
    public RetryPolicy build() {
      if (maxAttempts < 1) {
        throw new IllegalArgumentException("maxAttempts must be at least 1");
      }
      if (!positive(initialBackoff) || !positive(maxBackoff)) {
        throw new IllegalArgumentException("backoffs must be positive");
      }
      if (maxBackoff.compareTo(initialBackoff) < 0) {
        throw new IllegalArgumentException("maxBackoff must not be shorter than initialBackoff");
      }
      return new RetryPolicy(this);
    }

    private static boolean positive(Duration value) {
      return value != null && !value.isZero() && !value.isNegative();
    }
  }
}
