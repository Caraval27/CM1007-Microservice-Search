FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /build_journal_app
COPY Backend_Search/pom.xml .
COPY Backend_Search/src ./src
RUN mvn clean package

FROM openjdk:17-jdk-alpine
WORKDIR /journal_app
EXPOSE 8083

COPY --from=build /build_journal_app/target/quarkus-app /journal_app/

CMD ["java", "-jar", "/journal_app/quarkus-run.jar"]
