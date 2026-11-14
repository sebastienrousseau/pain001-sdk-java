// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * One full call per typed method, plus the raw request, against a {@code pain001 serve} on
 * loopback at {@code PAIN001_BASE_URL}: the server's work, the HTTP round trip and the client.
 * {@code make bench} skips this class with a message when {@code PAIN001_BASE_URL} is unset.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LoopbackBenchmark {

  private Pain001Client client;

  @Setup
  public void connect() {
    String url = System.getenv("PAIN001_BASE_URL");
    if (url == null || url.isBlank()) {
      throw new IllegalStateException("set PAIN001_BASE_URL to a pain001 serve on loopback");
    }
    client = Pain001Client.builder().baseUrl(url).build();
    client.health();
  }

  @Benchmark
  public HealthResponse health() {
    return client.health();
  }

  @Benchmark
  public ValidationResponse validate() {
    return client.validate(BenchFixtures.CSV, BenchFixtures.FILENAME, BenchFixtures.TYPE);
  }

  @Benchmark
  public GenerateResponse generate() {
    return client.generate(BenchFixtures.CSV, BenchFixtures.FILENAME, BenchFixtures.TYPE);
  }

  @Benchmark
  public CheckResponse check() {
    return client.check(BenchFixtures.XML);
  }

  @Benchmark
  public List<FaceInfo> faces() {
    return client.faces();
  }

  @Benchmark
  public FaceResponse projectFace() {
    return client.projectFace(BenchFixtures.FACE, BenchFixtures.XML);
  }

  @Benchmark
  public ApiResponse rawRequest() {
    return client.request("GET", "/api/v1/health", null);
  }
}
