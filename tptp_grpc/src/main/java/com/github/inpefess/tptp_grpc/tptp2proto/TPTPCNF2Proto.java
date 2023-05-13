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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import com.github.inpefess.tptp_grpc.tptp_proto.SaturationProofState;

class TPTPCNF2Proto {
  public static void main(String[] args) throws IOException {
    TPTP2Proto tptp2Proto = new TPTP2Proto(args[0]);
    Scanner problemList = new Scanner(new FileInputStream(args[1]));
    int fileIndex = 0;
    while (problemList.hasNextLine()) {
      String outputFilename = Paths.get(args[2], fileIndex++ + ".pb").toString();
      SaturationProofState parsedCNF =
          tptp2Proto.tptpCNF2Proto(new FileReader(problemList.nextLine()));
      parsedCNF.writeDelimitedTo(new FileOutputStream(outputFilename));
    }
  }
}
