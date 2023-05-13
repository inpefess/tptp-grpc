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

package com.github.inpefess.tptp_grpc.tptp_grpc_client;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.github.inpefess.tptp_grpc.tptp2proto.TPTPCNFParserServer;
import com.github.inpefess.tptp_grpc.tptp_proto.SaturationProofState;
import com.github.inpefess.tptp_grpc.tptp_proto.StringMessage;
import com.github.inpefess.tptp_grpc.tptp_proto.TPTPCNFParserGrpc;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

/**
 * A simple client that requests to parse a TPTP CNF string from the {@link TPTPCNFParserServer}.
 */
public class TPTPCNFgRPCClient {
  private static final Logger logger = Logger.getLogger(TPTPCNFgRPCClient.class.getName());

  private final TPTPCNFParserGrpc.TPTPCNFParserBlockingStub blockingStub;

  /** Construct client for accessing TPTPCNFParserServer using the existing channel. */
  public TPTPCNFgRPCClient(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = TPTPCNFParserGrpc.newBlockingStub(channel);
  }

  /** Send a string to parse to server. */
  public SaturationProofState parseCNF(String name) {
    StringMessage request = StringMessage.newBuilder().setStringMessage(name).build();
    SaturationProofState response;
    response = blockingStub.parseCNF(request);
    return response;
  }

  /**
   * Parse CNF. If provided, the first element of {@code args} is the TPTP CNF string to parse.
   * The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    String cnfString = "cnf(test, axiom, ~ p(f(X, g(Y, Z))) | X = Y | $false).";
    // Access a service running on the local machine on port 50051
    String target = "localhost:50051";
    // Allow passing in the user and target strings as command line arguments
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.println("Usage: [cnfString [target]]");
        System.err.println("");
        System.err.println(
            "  cnfString    The TPTP CNF string you wish to parse. Defaults to " + cnfString);
        System.err.println("  target  The server to connect to. Defaults to " + target);
        System.exit(1);
      }
      cnfString = args[0];
    }
    if (args.length > 1) {
      target = args[1];
    }

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    //
    // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
    // use TLS, use TlsChannelCredentials instead.
    ManagedChannel channel =
        Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    try {
      logger.info("Parsing string: " + cnfString);
      TPTPCNFgRPCClient client = new TPTPCNFgRPCClient(channel);
      logger.info("Parsing result: " + client.parseCNF(cnfString).toString());
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
