[![Maven Central](https://img.shields.io/maven-central/v/io.github.inpefess/tptp-grpc)](https://mvnrepository.com/artifact/io.github.inpefess/tptp-grpc)[![javadoc](https://javadoc.io/badge2/io.github.inpefess/tptp-grpc/javadoc.svg)](https://javadoc.io/doc/io.github.inpefess/tptp-grpc)

# TPTP gRPC

This repo contains:
* a [protobuf](https://protobuf.dev/) definition of a [TPTP](https://tptp.org/) parsing server
* a [gRPC](https://grpc.io) server implementation to parse TPTP-formatted strings
* a gRPC client implementation

One can use the same protobuf to generate gRPC clients for other languages.

This project uses an exising [TPTP parser](https://github.com/marklemay/tptpParser).

# How to install

This project uses Java 11. For [Gradle](https://gradle.org/), add the following to `gradle.build`:

```Groovy
repositories {
    // TPTP parser https://github.com/marklemay/tptpParser
    maven {
        url "https://raw.github.com/marklemay/tptpParser/mvn-repo/"
    }
}

dependencies {
    implementation 'io.github.inpefess:tptpgrpc:0.0.9'

    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    implementation 'com.google.protobuf:protobuf-java:3.23.0'
}
```

# How to use in your code

```Java
import io.github.inpefess.tptpgrpc.tptp2proto.Tptp2Proto;
import io.github.inpefess.tptpgrpc.tptpproto.Node;

// if you parse single statements, you can set an empty string
Tptp2Proto tptp2Proto = new Tptp2Proto("path to TPTP root folder");
// or read a file using FileReader
StringReader cnfReader = new StringReader("cnf(test,axiom,$false).");
Node parsedProto = tptp2Proto.tptp2Proto(cnfReader);
```

# How to run the server

1. Using Gradle:

```sh
git clone https://github.com/inpefess/tptp-grpc.git
cd ./tptp-grpc
./gradlew build
```

This will also run the [unit tests](https://junit.org/junit5/docs/current/user-guide/), and generate [Jacoco](https://www.jacoco.org/) and [Checkstyle](https://checkstyle.org/) reports.

Start the server:

```sh
./gradlew run
```

Then from a different terminal start an example Java client:

```sh
./gradlew run -PmainClassToRun=io.github.inpefess.tptpgrpc.tptp2proto.TptpGrpcClient
```

2. Using Docker:

```sh
docker run -p 50051:50051 inpefess/tptp-grpc
```

# To run a bulk parsing

Prepare the list of problems and the output folder, e.g.:

```sh
find $TPTP_ROOT/Problems/*/*-*.p | grep -vE "(SYN|HWV|CSR|KRS|PLA|SWV|SYO)" | xargs -I {} grep -LE "^%\ Status\ +: (Unknown|Open)" {} > problem-list.txt
mkdir output
```

Then run the parsing script:
```sh
./gradlew run -PmainClassToRun=io.github.inpefess.tptpgrpc.tptp2proto.Tptp2Proto --args="$TPTP_ROOT absolute_path_to_problem-list.txt absolute_path_to_output_folder"
```

To prepare labels for graph classification task:

```sh
cat problem-list.txt | xargs -I {} grep -E "^%\ Status\ +:\ " {} | cut -d ":" -f 2 | sed "s/ Satisfiable/0/" | sed "s/ Unsatisfiable/1/" > labels.txt
```

# Generate and use a Python client

After starting a server as indicated above use the following code to
generate ``grpc`` server and client code (we won't use former in this
example):

```sh
cd python
pip install -r requirements.txt
python -m grpc_tools.protoc -I../tptpgrpc/src/main/proto/io/github/inpefess/tptp_grpc/ --python_out=. --pyi_out=. --grpc_python_out=. ../tptpgrpc/src/main/proto/io/github/inpefess/tptp_grpc/tptp_proto/tptp_parser.proto
```

Then see ``python/test_client.py`` as an example of using the
generated Python client.
