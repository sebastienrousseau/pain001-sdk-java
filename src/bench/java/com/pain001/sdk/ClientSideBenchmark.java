// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * The client's own cost, no network: building each typed method's request (JSON body, its
 * serialisation and the {@link HttpRequest}) and decoding each typed method's response (parse and
 * record mapping), with the kit's fixtures and the responses pain001 0.0.87 gave for them.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ClientSideBenchmark {

  private final Pain001Client client =
      Pain001Client.builder().baseUrl("http://127.0.0.1:8000").apiKey("bench-key").build();

  private final String health = BenchFixtures.response("health");
  private final String validate = BenchFixtures.response("validate");
  private final String generate = BenchFixtures.response("generate");
  private final String check = BenchFixtures.response("check");
  private final String faces = BenchFixtures.response("faces");
  private final String projectFace = BenchFixtures.response("project-face");

  @Benchmark
  public HttpRequest buildHealth() {
    return client.buildRequest("GET", "/api/v1/health", null);
  }

  @Benchmark
  public HttpRequest buildValidate() {
    JsonNode body =
        Pain001Client.filePayload(
            BenchFixtures.CSV, BenchFixtures.FILENAME, BenchFixtures.TYPE, null);
    return client.buildRequest("POST", "/api/v1/ui/validate", body);
  }

  @Benchmark
  public HttpRequest buildGenerate() {
    JsonNode body =
        Pain001Client.filePayload(
            BenchFixtures.CSV, BenchFixtures.FILENAME, BenchFixtures.TYPE, null);
    return client.buildRequest("POST", "/api/v1/ui/generate", body);
  }

  @Benchmark
  public HttpRequest buildCheck() {
    JsonNode body = Pain001Client.checkPayload(BenchFixtures.XML, null, null);
    return client.buildRequest("POST", "/api/v1/check", body);
  }

  @Benchmark
  public HttpRequest buildFaces() {
    return client.buildRequest("GET", "/api/v1/faces", null);
  }

  @Benchmark
  public HttpRequest buildProjectFace() {
    String path = Pain001Client.facePath(BenchFixtures.FACE);
    JsonNode body = Pain001Client.facePayload(BenchFixtures.XML, null, null);
    return client.buildRequest("POST", path, body);
  }

  @Benchmark
  public HealthResponse decodeHealth() {
    return HealthResponse.from(Json.parseLenient(health));
  }

  @Benchmark
  public ValidationResponse decodeValidate() {
    return ValidationResponse.from(Json.parseLenient(validate));
  }

  @Benchmark
  public GenerateResponse decodeGenerate() {
    return GenerateResponse.from(Json.parseLenient(generate));
  }

  @Benchmark
  public CheckResponse decodeCheck() {
    return CheckResponse.from(Json.parseLenient(check));
  }

  @Benchmark
  public List<FaceInfo> decodeFaces() {
    return Json.elements(Json.parseLenient(faces)).stream().map(FaceInfo::from).toList();
  }

  @Benchmark
  public FaceResponse decodeProjectFace() {
    return FaceResponse.from(Json.parseLenient(projectFace));
  }

  @Benchmark
  public ApiResponse decodeRawRequest() {
    return new ApiResponse(200, Json.parseLenient(generate));
  }
}
