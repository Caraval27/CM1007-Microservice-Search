FROM openjdk:17-jdk-alpine
WORKDIR /journal_app
EXPOSE 8083

ARG JAR_DIR=Lab3_Search/target/quarkus-app
ARG JAR_NAME=quarkus-run.jar

COPY ${JAR_DIR}/${JAR_NAME} /journal_app/

CMD ["java", "-jar", "/journal_app/quarkus-run.jar"]
