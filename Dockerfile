FROM openjdk:17-jdk-alpine
WORKDIR /journal_app

COPY Lab3_Search/target/quarkus-app /journal_app/

EXPOSE 8083

CMD ["java", "-jar", "quarkus-run.jar"]
