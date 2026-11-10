// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One problem one rung of {@code POST /api/v1/check} found.
 *
 * @param rung {@code xsd}, {@code mdr}, {@code scheme} or {@code schematron}
 * @param rule the rule identifier
 * @param severity {@code error} or {@code warning}
 * @param message what is wrong
 * @param location element path, context or transaction, or null
 */
public record CheckFinding(
    String rung, String rule, String severity, String message, String location) {

  static CheckFinding from(JsonNode node) {
    return new CheckFinding(
        Json.text(node, "rung"),
        Json.text(node, "rule"),
        Json.text(node, "severity"),
        Json.text(node, "message"),
        Json.text(node, "location"));
  }
}
