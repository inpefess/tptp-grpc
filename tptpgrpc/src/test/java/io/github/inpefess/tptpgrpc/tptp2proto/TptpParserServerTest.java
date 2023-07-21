/*
 * Copyright 2023 Boris Shminke
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.inpefess.tptpgrpc.tptp2proto;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public final class TptpParserServerTest {
  private final class ServerThread extends Thread {
    public void run() {
      try {
        TptpParserServer.main(new String[] {});
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private final class ClientThread extends Thread {
    private String[] args;

    public ClientThread(String[] args) {
      this.args = args;
    }

    public void run() {
      try {
        TptpGrpcClient.main(args);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private final void runClient(final String[] args) throws InterruptedException {
    ClientThread clientThread = new ClientThread(args);
    clientThread.start();
    clientThread.join();
  }

  @Test
  public final void mainTest() throws InterruptedException {
    ServerThread serverThread = new ServerThread();
    serverThread.start();
    runClient(new String[] {});
    runClient(new String[] {"--help"});
    runClient(new String[] {"cnf(test,axiom,$false).", "localhost:50051"});
    serverThread.join(10, 0);
  }
}
