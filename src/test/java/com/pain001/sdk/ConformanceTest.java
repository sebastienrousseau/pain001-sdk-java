// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Every case in {@code conformance/cases.json}, through {@link Pain001Client#request}. */
class ConformanceTest {

  @TestFactory
  Stream<DynamicTest> everyCaseHoldsForTheServer() {
    Pain001Client client = LiveServer.client();
    return ConformanceKit.cases(ConformanceKit.DIR).stream()
        .map(
            kase ->
                dynamicTest(
                    kase.get("id").asText(),
                    () ->
                        assertEquals(
                            List.of(), ConformanceKit.runCase(client, kase, ConformanceKit.DIR))));
  }
}
