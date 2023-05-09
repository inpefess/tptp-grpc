# TPTP gRPC

This repo contains:
* a [protobuf](https://protobuf.dev/) definition of a [TPTP](https://tptp.org/) parsing server
* a [gRPC](https://grpc.io) server implementation to parse TPTP-formatted strings
* a gRPC client implementation

One can use the same protobuf to generate gRPC clients for other languages.

This project uses an exising [TPTP parser](https://github.com/marklemay/tptpParser).

# How to install

This project uses Java 11. , generate GitHub [personal access token](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages). 

For [Gradle](https://gradle.org/), add you GitHub user name as `gpr.user` and you personal access token as `grp.key` to `gradle.properties`. Maven, sbt or others should provide similar options for package registry authentication.

Then add the following to `gradle.build`:

```Groovy
repositories {
    // TPTP parser https://github.com/marklemay/tptpParser
    maven {
        url "https://raw.github.com/marklemay/tptpParser/mvn-repo/"
    }
    // TPTP gRPC https://github.com/inpefess/tptp-grpc
    maven {
        url = uri("https://maven.pkg.github.com/inpefess/tptp-grpc")
        credentials {
            username = project.findProperty("gpr.user")
            password = project.findProperty("gpr.key")
        }
    }
}

dependencies {
    implementation 'tptp-grpc:tptp_grpc:0.0.1'

    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    implementation 'com.google.protobuf:protobuf-java:3.23.0'
}
```

# How to use in your code

```Java
import com.github.inpefess.tptp_grpc.tptp2proto.TPTP2Proto;

class App {
    public static void main(String[] args) {
      System.out.println((new TPTP2Proto()).tptpCNF2Proto("cnf(test,axiom,$false).").toString());
    }
}
```

# How to run the server

```sh
git clone https://github.com/inpefess/tptp-grpc.git
cd ./tptp-grpc
./gradlew build
```

This will also run the [unit tests](https://junit.org/junit5/docs/current/user-guide/) and generate [Jacoco](https://www.jacoco.org/) coverage reports.

Start the server:

```sh
./gradlew run
```

Then from a different terminal start an example Java client:

```sh
./gradlew tptp_grpc_client:run
```
