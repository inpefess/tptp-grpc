/*
 *  Copyright 2023 Boris Shminke
 *  Copyright 2015 The gRPC Authors
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

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import io.github.inpefess.tptpgrpc.tptpproto.Node;
import io.github.inpefess.tptpgrpc.tptpproto.StringMessage;
import io.github.inpefess.tptpgrpc.tptpproto.TptpParserGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public final class TptpParserServer {
  private static final Logger logger = Logger.getLogger(TptpParserServer.class.getName());

  private Server server;

  private final void start() throws IOException {
    /* The port on which the server should run */
    final int port = 50051;
    server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
        .addService(new TptpParserImpl()).build().start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public final void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          TptpParserServer.this.stop();
        } catch (final InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private final void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private final void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Launch the server from the command line.
   *
   * @param args command line arguments (not used)
   * @throws IOException if unable to bind
   * @throws InterruptedException if server process interrupted
   */
  public static final void main(final String[] args) throws IOException, InterruptedException {
    final TptpParserServer server = new TptpParserServer();
    server.start();
    server.blockUntilShutdown();
  }

  static final class TptpParserImpl extends TptpParserGrpc.TptpParserImplBase {
    private final Tptp2Proto tptp2ProtoParser;

    public TptpParserImpl() {
      final String tptpPath = Paths.get(System.getenv("HOME"), "data", "TPTP-v8.1.2").toString();
      tptp2ProtoParser = new Tptp2Proto(tptpPath);
    }

    @Override
    public final void parseTptp(final StringMessage req,
        final StreamObserver<Node> responseObserver) {
      try {
        responseObserver
            .onNext(tptp2ProtoParser.tptp2Proto(new StringReader(req.getStringMessage())));
        responseObserver.onCompleted();
      } catch (final IOException e) {
        logger.severe(e.getMessage());
      } catch (final TptpSyntaxErrorException e) {
        responseObserver
            .onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
      }
    }
  }
}
