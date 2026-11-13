// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The vendored kit is byte for byte the one {@code conformance/SHA256SUMS} pins. */
class KitChecksumTest {

  @Test
  void everyKitFileMatchesItsPinnedHash() throws IOException, NoSuchAlgorithmException {
    Path dir = ConformanceKit.DIR;
    List<String> pinned = Files.readAllLines(dir.resolve("SHA256SUMS"), StandardCharsets.UTF_8);
    List<String> actual;
    try (Stream<Path> files = Files.walk(dir)) {
      List<Path> sorted =
          files
              .filter(Files::isRegularFile)
              .filter(file -> !file.getFileName().toString().equals("SHA256SUMS"))
              .sorted((a, b) -> relative(dir, a).compareTo(relative(dir, b)))
              .toList();
      actual = new ArrayList<>();
      for (Path file : sorted) {
        actual.add(sha256(file) + "  " + relative(dir, file));
      }
    }
    assertEquals(pinned, actual);
  }

  private static String relative(Path dir, Path file) {
    return dir.relativize(file).toString().replace('\\', '/');
  }

  private static String sha256(Path file) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
  }
}
