// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import java.util.Arrays;
import java.util.Optional;

/**
 * The ISO 20022 message types the server's {@code MessageType} enum lists at this SDK's version.
 *
 * <p>The client methods take the message type as a string, so a newer server's types work before
 * this enum knows them: pass {@code MessageType.PAIN_001_001_09.value()} or the literal string.
 */
public enum MessageType {
  PAIN_001_001_03("pain.001.001.03"),
  PAIN_001_001_04("pain.001.001.04"),
  PAIN_001_001_05("pain.001.001.05"),
  PAIN_001_001_06("pain.001.001.06"),
  PAIN_001_001_07("pain.001.001.07"),
  PAIN_001_001_08("pain.001.001.08"),
  PAIN_001_001_09("pain.001.001.09"),
  PAIN_001_001_10("pain.001.001.10"),
  PAIN_001_001_11("pain.001.001.11"),
  PAIN_001_001_12("pain.001.001.12"),
  PAIN_001_001_13("pain.001.001.13"),
  PAIN_008_001_02("pain.008.001.02"),
  PAIN_008_001_08("pain.008.001.08");

  private final String value;

  MessageType(String value) {
    this.value = value;
  }

  /** The wire value, for example {@code pain.001.001.09}. */
  public String value() {
    return value;
  }

  /** The constant whose wire value is {@code value}, if this SDK knows it. */
  public static Optional<MessageType> fromValue(String value) {
    return Arrays.stream(values()).filter(type -> type.value.equals(value)).findFirst();
  }

  @Override
  public String toString() {
    return value;
  }
}
