# TPTP gRPC

This repo contains:
* a [protobuf](https://protobuf.dev/) definition of a [TPTP](https://tptp.org/) parsing server
* a [gRPC](https://grpc.io) server implementation to parse TPTP-formatted strings
* a gRPC client implementation

One can use the same protobuf to generate gRPC clients for other languages.

This project uses an exising [TPTP parser](https://github.com/marklemay/tptpParser).

# How to install

This project uses [Gradle](https://gradle.org/):

```sh
git clone https://github.com/inpefess/tptp-grpc.git
cd ./tptp-grpc
./gradlew build
```

This will also run the [unit tests](https://junit.org/junit5/docs/current/user-guide/) and generate [Jacoco](https://www.jacoco.org/) coverage reports.

# How to use

Start the server:

```sh
./gradlew run
```

Then from a different terminal start an example Java client:

```sh
./gradlew tptp_grpc_client:run
```
