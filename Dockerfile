FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /build_journal_app
COPY Backend_Search/pom.xml .
COPY Backend_Search/src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-alpine
WORKDIR /journal_app
EXPOSE 8080

COPY --from=build /build_journal_app/target/*.jar /journal_app/

CMD ["sh", "-c", "java -jar /journal_app/Backend_Search-0.0.1-SNAPSHOT-jar-with-dependencies.jar"]
