FROM openjdk:17-jdk-alpine
WORKDIR /journal_app
EXPOSE 8083

COPY Lab3_Search/target/quarkus-app /journal_app/quarkus-app
WORKDIR /journal_app/quarkus-app

CMD ["java", "-jar", "/journal_app/quarkus-run.jar"]
