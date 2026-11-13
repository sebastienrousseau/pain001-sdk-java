// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.List;

/** The one place the SDK touches Jackson: parsing, writing and lenient field access. */
final class Json {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Json() {}

  /** A new, empty JSON object. */
  static ObjectNode object() {
    return MAPPER.createObjectNode();
  }

  /** Converts a plain Java value (map, list, string, number) to a JSON tree. */
  static JsonNode toNode(Object value) {
    return MAPPER.valueToTree(value);
  }

  /** Serialises a JSON tree to compact text. */
  static String write(JsonNode node) {
    try {
      return MAPPER.writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("body cannot be written as JSON", e);
    }
  }

  /**
   * Parses a response body. An empty body becomes JSON null; a body that is not JSON (a proxy's
   * HTML error page, say) is kept as a JSON string so the caller still sees it.
   */
  static JsonNode parseLenient(String text) {
    if (text == null || text.isBlank()) {
      return NullNode.getInstance();
    }
    try {
      return MAPPER.readTree(text);
    } catch (JsonProcessingException e) {
      return TextNode.valueOf(text);
    }
  }

  /** The text of {@code field}, or null when it is absent or JSON null. */
  static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isValueNode() && !value.isNull() ? value.asText() : null;
  }

  /** The integer value of {@code field}, or 0 when it is absent. */
  static int integer(JsonNode node, String field) {
    return node.path(field).asInt(0);
  }

  /** The number value of {@code field}, or 0 when it is absent. */
  static double number(JsonNode node, String field) {
    return node.path(field).asDouble(0);
  }

  /** The boolean value of {@code field}, or false when it is absent. */
  static boolean bool(JsonNode node, String field) {
    return node.path(field).asBoolean(false);
  }

  /** The elements of an array node; an empty list for anything else. */
  static List<JsonNode> elements(JsonNode array) {
    List<JsonNode> out = new ArrayList<>();
    if (array.isArray()) {
      array.forEach(out::add);
    }
    return List.copyOf(out);
  }

  /** The elements of the array {@code field}, or an empty list when it is absent. */
  static List<JsonNode> nodes(JsonNode node, String field) {
    return elements(node.path(field));
  }

  /** The elements of the array {@code field} as text, or an empty list when it is absent. */
  static List<String> texts(JsonNode node, String field) {
    return nodes(node, field).stream().map(JsonNode::asText).toList();
  }
}
