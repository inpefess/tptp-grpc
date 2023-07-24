FROM gradle:jdk11-alpine
ENV PROJECT_ROOT=/home/gradle/project/
COPY ./build.gradle ${PROJECT_ROOT}
WORKDIR ${PROJECT_ROOT}
ENTRYPOINT ["gradle", "run"]
