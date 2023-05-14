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
package com.github.inpefess.tptp_grpc.tptp2proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.github.inpefess.tptp_grpc.tptp_proto.Function;
import org.junit.jupiter.api.Test;

class TPTP2ProtoTest {
  @Test
  void tptpCNF2ProtoTest() throws FileNotFoundException, IOException {
    TPTP2Proto tptp2Proto = new TPTP2Proto(this.getClass().getResource("/TPTP-mock").getPath());
    InputStream testProblem =
        this.getClass().getResourceAsStream("/TPTP-mock/Problems/TST/TST001-1.p");
    assertEquals(tptp2Proto.tptp2Proto(new InputStreamReader(testProblem)),
        Function.parseFrom(this.getClass().getResourceAsStream("/test.pb")));
  }
}
