// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.FaceInfo;
import com.pain001.sdk.Pain001Client;
import java.util.List;

/**
 * {@code faces()}: lists the target standards the server can project a pain.001 onto.
 *
 * <p>Prints each face's name, the bindings it needs, and its description. Run with {@code make
 * examples}; the server URL comes from PAIN001_BASE_URL.
 */
public final class Faces {

  private Faces() {}

  public static void main(String[] args) {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();

    List<FaceInfo> faces = client.faces();
    for (FaceInfo face : faces) {
      System.out.println(face.name() + " " + face.bindings() + ": " + face.description());
    }
    if (faces.isEmpty()) {
      System.exit(1);
    }
  }
}
