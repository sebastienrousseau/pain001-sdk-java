// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * What a face projection carried and what it did not, by indexed path.
 *
 * @param face the face name
 * @param totalSourceElements leaves in the source document
 * @param mappedCount leaves the projection carried
 * @param unmappedCount leaves it did not carry
 * @param isLossless whether every leaf was carried
 * @param lossRatio the share of source leaves not carried
 * @param mappedPaths the carried paths
 * @param unmappedPaths the paths not carried
 */
public record LossReport(
    String face,
    int totalSourceElements,
    int mappedCount,
    int unmappedCount,
    boolean isLossless,
    double lossRatio,
    List<String> mappedPaths,
    List<String> unmappedPaths) {

  /** Copies the lists so the record stays immutable. */
  public LossReport {
    mappedPaths = List.copyOf(mappedPaths);
    unmappedPaths = List.copyOf(unmappedPaths);
  }

  static LossReport from(JsonNode node) {
    return new LossReport(
        Json.text(node, "face"),
        Json.integer(node, "total_source_elements"),
        Json.integer(node, "mapped_count"),
        Json.integer(node, "unmapped_count"),
        Json.bool(node, "is_lossless"),
        Json.number(node, "loss_ratio"),
        Json.texts(node, "mapped_paths"),
        Json.texts(node, "unmapped_paths"));
  }
}
