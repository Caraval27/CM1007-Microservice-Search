FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /build_journal_app
COPY Lab3_Search/pom.xml .

RUN mvn dependency:go-offline

COPY Lab3_Search/src ./src

RUN mvn clean test

RUN mvn clean package

FROM openjdk:17-jdk-alpine
WORKDIR /journal_app
EXPOSE 8083

COPY --from=build /build_journal_app/target/quarkus-app /journal_app/

CMD ["java", "-jar", "/journal_app/quarkus-run.jar"]