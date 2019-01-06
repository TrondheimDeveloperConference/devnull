FROM hseeberger/scala-sbt as SCALA_TOOL_CHAIN
COPY src /tmp/src
COPY project /tmp/project
COPY sbt /tmp/
COPY version.sbt /tmp/
COPY build.sbt /tmp/
WORKDIR /tmp/
RUN sbt appmgr:packageBin

FROM openjdk:8-jdk-alpine
COPY --from=SCALA_TOOL_CHAIN /tmp/target/appmgr/root /devnull
ADD docker/wait-for-it.sh /
ENV app.home /devnull
EXPOSE 8084
CMD /wait-for-it.sh db:5432 -- /devnull/bin/jetty
