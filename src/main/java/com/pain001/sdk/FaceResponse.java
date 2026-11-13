// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code POST /api/v1/faces/{face}}: the target payload and its loss report.
 *
 * @param face the face name
 * @param version the source pain.001 edition
 * @param payload the body in the target's shape, as raw JSON
 * @param lossReport what the projection carried and what it did not
 */
public record FaceResponse(String face, String version, JsonNode payload, LossReport lossReport) {

  static FaceResponse from(JsonNode node) {
    return new FaceResponse(
        Json.text(node, "face"),
        Json.text(node, "version"),
        node.path("payload"),
        LossReport.from(node.path("loss_report")));
  }
}
