// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** The inputs every benchmark shares: the kit's request fixtures and the recorded responses. */
final class BenchFixtures {

  static final String CSV = kit("credit-transfer.pain.001.001.09.csv");
  static final String XML = kit("credit-transfer.pain.001.001.09.xml");
  static final String FILENAME = "payments.csv";
  static final String TYPE = "pain.001.001.09";
  static final String FACE = "berlin_group";

  private BenchFixtures() {}

  /** A conformance-kit fixture, read from {@code conformance/fixtures/} in the working tree. */
  static String kit(String name) {
    try {
      return Files.readString(Path.of("conformance/fixtures", name), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("run the benchmarks from the repository root", e);
    }
  }

  /** A response body recorded by {@code scripts/capture-bench-responses.sh}. */
  static String response(String name) {
    String path = "/com/pain001/sdk/bench/" + name + ".json";
    try (InputStream in = BenchFixtures.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("missing bench resource " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
