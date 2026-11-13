// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The conformance kit runner, ported from core's {@code tests/test_conformance_kit.py}.
 *
 * <p>Each case goes through {@link Pain001Client#request}; {@link #runCase} returns the failed
 * expectations, so an empty list is a pass. Two deliberate differences from the Python reference:
 * {@code equals} compares JSON values (a boolean never equals a number, as it does in Python), and
 * a {@code $file} that escapes the kit directory is refused.
 */
final class ConformanceKit {

  /** The vendored kit, relative to the project directory Maven runs tests from. */
  static final Path DIR = Path.of("conformance");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Map<String, Predicate<JsonNode>> TYPES =
      Map.of(
          "string", JsonNode::isTextual,
          "number", JsonNode::isNumber,
          "integer", JsonNode::isIntegralNumber,
          "boolean", JsonNode::isBoolean,
          "array", JsonNode::isArray,
          "object", JsonNode::isObject,
          "null", JsonNode::isNull);

  /** Numbers compare by value (1 equals 1.0); everything else by JSON equality. */
  private static final Comparator<JsonNode> JSON_VALUE =
      (a, b) -> {
        if (a.isNumber() && b.isNumber()) {
          return a.decimalValue().compareTo(b.decimalValue());
        }
        return a.equals(b) ? 0 : 1;
      };

  private ConformanceKit() {}

  /** The cases in {@code cases.json}. */
  static List<JsonNode> cases(Path dir) {
    List<JsonNode> out = new ArrayList<>();
    readJson(dir.resolve("cases.json")).path("cases").forEach(out::add);
    return out;
  }

  /** Replaces every {@code {"$file": ...}} object with that fixture's text, recursively. */
  static JsonNode resolve(JsonNode body, Path dir) {
    if (body == null || body.isMissingNode()) {
      return null;
    }
    if (body.isObject() && body.size() == 1 && body.has("$file")) {
      return TextNode.valueOf(readText(dir, body.get("$file").asText()));
    }
    if (body.isObject()) {
      ObjectNode out = MAPPER.createObjectNode();
      body.properties().forEach(e -> out.set(e.getKey(), resolve(e.getValue(), dir)));
      return out;
    }
    if (body.isArray()) {
      ArrayNode out = MAPPER.createArrayNode();
      body.forEach(item -> out.add(resolve(item, dir)));
      return out;
    }
    return body;
  }

  /** RFC 6901 JSON Pointer lookup; empty when the target is absent. */
  static Optional<JsonNode> pointer(JsonNode document, String pointer) {
    if (pointer.isEmpty()) {
      return Optional.of(document);
    }
    JsonNode value = document;
    String[] tokens = pointer.split("/", -1);
    for (int i = 1; i < tokens.length && value != null; i++) {
      value = step(value, tokens[i].replace("~1", "/").replace("~0", "~"));
    }
    return Optional.ofNullable(value);
  }

  private static JsonNode step(JsonNode value, String token) {
    if (value.isArray()) {
      return element(value, token);
    }
    return value.isObject() ? value.get(token) : null;
  }

  /** The array element a decimal token names; absent for "-", signs or overflow. */
  private static JsonNode element(JsonNode array, String token) {
    if (token.isEmpty() || !token.chars().allMatch(c -> c >= '0' && c <= '9')) {
      return null;
    }
    try {
      return array.get(Integer.parseInt(token));
    } catch (NumberFormatException overflow) {
      return null;
    }
  }

  /** Sends one case and returns the failed expectations (empty = pass). */
  static List<String> runCase(Pain001Client client, JsonNode kase, Path dir) {
    JsonNode body = resolve(kase.path("body"), dir);
    String method = kase.get("method").asText();
    ApiResponse response = client.request(method, kase.get("path").asText(), body);
    JsonNode expect = kase.get("expect");
    List<String> failures = new ArrayList<>();
    int wanted = expect.get("status").asInt();
    if (response.status() != wanted) {
      failures.add("status " + response.status() + " != " + wanted);
      return failures;
    }
    JsonNode document = response.body();
    expect.path("equals").properties().forEach(e -> checkEquals(document, e, failures));
    expect.path("types").properties().forEach(e -> checkType(document, e, failures));
    expect.path("contains").properties().forEach(e -> checkContains(document, e, failures));
    return failures;
  }

  private static void checkEquals(
      JsonNode document, Map.Entry<String, JsonNode> entry, List<String> failures) {
    Optional<JsonNode> value = pointer(document, entry.getKey());
    if (value.isEmpty()) {
      failures.add(entry.getKey() + " missing");
    } else if (!value.get().equals(JSON_VALUE, entry.getValue())) {
      failures.add(entry.getKey() + " != " + entry.getValue());
    }
  }

  private static void checkType(
      JsonNode document, Map.Entry<String, JsonNode> entry, List<String> failures) {
    Optional<JsonNode> value = pointer(document, entry.getKey());
    String kind = entry.getValue().asText();
    Predicate<JsonNode> test = TYPES.getOrDefault(kind, node -> false);
    if (value.isEmpty()) {
      failures.add(entry.getKey() + " missing");
    } else if (!test.test(value.get())) {
      failures.add(entry.getKey() + " is not " + kind);
    }
  }

  private static void checkContains(
      JsonNode document, Map.Entry<String, JsonNode> entry, List<String> failures) {
    Optional<JsonNode> value = pointer(document, entry.getKey());
    if (value.isEmpty()) {
      failures.add(entry.getKey() + " missing");
      return;
    }
    if (!value.get().isTextual()) {
      failures.add(entry.getKey() + " is not a string");
      return;
    }
    String text = value.get().asText();
    for (JsonNode needle : entry.getValue()) {
      if (!text.contains(needle.asText())) {
        failures.add(entry.getKey() + " lacks " + needle);
      }
    }
  }

  /** Reads a kit file as UTF-8 text; refuses a path outside the kit. */
  static String readText(Path dir, String relative) {
    Path root = dir.toAbsolutePath().normalize();
    Path file = root.resolve(relative).normalize();
    if (!file.startsWith(root)) {
      throw new IllegalArgumentException("$file escapes the kit: " + relative);
    }
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Parses a JSON file. */
  static JsonNode readJson(Path file) {
    try {
      return MAPPER.readTree(Files.readString(file, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
