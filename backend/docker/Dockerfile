FROM ubuntu:25.04

RUN apt update && apt upgrade -y && apt install -y openjdk-21-jdk maven

COPY target/stockdashboard-backend-*-jar-with-dependencies.jar /app/

ENTRYPOINT [ "java", "-jar", "/app/stockdashboard-backend-1.0-SNAPSHOT-jar-with-dependencies.jar" ]