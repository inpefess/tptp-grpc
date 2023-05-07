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

package com.github.inpefess.tptp_grpc.tptp2proto;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.github.inpefess.tptp_grpc.tptp_proto.TPTPCNF;
import com.github.inpefess.tptp_grpc.tptp_proto.TPTPCNFParserGrpc;
import com.github.inpefess.tptp_grpc.tptp_proto.TPTPCNFString;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class TPTPCNFParserServer {
  private static final Logger logger = Logger.getLogger(TPTPCNFParserServer.class.getName());

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 50051;
    server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
        .addService(new TPTPCNFParserImpl()).build().start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          TPTPCNFParserServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final TPTPCNFParserServer server = new TPTPCNFParserServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class TPTPCNFParserImpl extends TPTPCNFParserGrpc.TPTPCNFParserImplBase {
    private TPTP2Proto tptp2ProtoParser;

    public TPTPCNFParserImpl() {
      tptp2ProtoParser = new TPTP2Proto();
    }

    @Override
    public void parseCNF(TPTPCNFString req, StreamObserver<TPTPCNF> responseObserver) {
      responseObserver.onNext(tptp2ProtoParser.tptpCNF2Proto(req.getTptpCnfString()));
      responseObserver.onCompleted();
    }
  }
}
