/*
 *  Copyright 2023 Boris Shminke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

package io.github.inpefess.tptpgrpc.tptp2proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import io.github.inpefess.tptpgrpc.tptpproto.Node;
import org.junit.jupiter.api.Test;

/**
 * Test parsing a TPTP problem to a serialised protobuf.
 */
public final class Tptp2ProtoTest {
  @Test
  public final void tptpCnf2ProtoTest() throws FileNotFoundException, IOException {
    final Tptp2Proto tptp2Proto =
        new Tptp2Proto(this.getClass().getResource("/TPTP-mock").getPath());
    final InputStream testProblem =
        this.getClass().getResourceAsStream("/TPTP-mock/Problems/TST/TST001-1.p");
    assertEquals(tptp2Proto.tptp2Proto(new InputStreamReader(testProblem)),
        Node.parseFrom(this.getClass().getResourceAsStream("/test.pb")));
  }
}
