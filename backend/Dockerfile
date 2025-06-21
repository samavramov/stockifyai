FROM ubuntu:25.04

RUN apt update && apt upgrade -y && apt install -y openjdk-21-jdk maven

WORKDIR /app
COPY pom.xml .                   
COPY src ./src  
RUN mvn clean install -DskipTests

RUN cp target/stockdashboard-backend-*-jar-with-dependencies.jar /app/app.jar
RUN rm -rf src target pom.xml

ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]
