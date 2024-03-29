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

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import io.github.inpefess.tptpgrpc.tptpproto.Node;
import io.github.inpefess.tptpgrpc.tptpproto.StringMessage;
import io.github.inpefess.tptpgrpc.tptpproto.TptpParserGrpc;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

/**
 * A simple client that requests to parse a TPTP string from the {@link TptpParserServer}.
 */
public final class TptpGrpcClient {
  private static final Logger logger = Logger.getLogger(TptpGrpcClient.class.getName());

  private final TptpParserGrpc.TptpParserBlockingStub blockingStub;
  private static final String targetDefault = "localhost:50051";
  private static final String cnfStringDefault =
      "cnf(test, axiom, ~ p(f(X, g(Y, Z))) | X = Y | $false).";

  /**
   * Construct client for accessing TPTPParserServer using the existing channel.
   *
   * @param channel a Channel, not a ManagedChannel, so it is not this code's responsibility to shut
            it down.
   */
  public TptpGrpcClient(final Channel channel) {
    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = TptpParserGrpc.newBlockingStub(channel);
  }

  /**
   * Send a string to parse to server.
   *
   * @param tptpString logic formula(s) in TPTP syntax
   * @return the root node of a parsed TPTP file
   */
  public final Node parseTptp(final String tptpString) {
    final StringMessage request = StringMessage.newBuilder().setStringMessage(tptpString).build();
    final Node response = blockingStub.parseTptp(request);
    return response;
  }

  /**
   * Extract server address from the argument list.
   *
   * @param args arguments passed to the {@code main}
   * @return server address (with port)
   */
  private static final String getTarget(final String[] args) {
    if (args.length > 1) {
      return args[1];
    }
    return targetDefault;
  }

  /**
   * Extract the TPTP text to parse from the argument list.
   *
   * @param args arguments passed to the {@code main}
   * @return a TPTP text
   */
  private static final String getCnfString(final String[] args) {
    if (args.length > 0) {
      return args[0];
    }
    return cnfStringDefault;
  }

  /**
   * Parse.
   *
   * @param args two arguments (both not required): <ol>
   *     <li> the TPTP string to parse </li>
   *     <li> the target server </li>
   *     </ol>
   * @throws InterruptedException if can't shutdown the channel
   */
  public static final void main(final String[] args) throws InterruptedException {
    // Allow passing in the user and target strings as command line arguments
    final String cnfString = getCnfString(args);
    if ("--help".equals(cnfString)) {
      System.err
          .println("Usage: [cnfString [target]]\n  cnfString  The TPTP string you wish to parse."
              + " Defaults to " + cnfStringDefault
              + "\n  target     The server to connect to. Defaults to " + targetDefault);
      return;
    }
    // Access a service running on the local machine on port 50051
    final String target = getTarget(args);
    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    //
    // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
    // use TLS, use TlsChannelCredentials instead.
    final ManagedChannel channel =
        Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    try {
      logger.info("Parsing string: " + cnfString);
      final TptpGrpcClient client = new TptpGrpcClient(channel);
      logger.info("Parsing result: " + client.parseTptp(cnfString).toString());
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
