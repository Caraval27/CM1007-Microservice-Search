FROM openjdk:17-jdk-alpine
WORKDIR /journal_app
EXPOSE 8080

COPY target/*.jar /journal_app/

ENV SPRING_DATASOURCE_PASSWORD=admin
ENV SPRING_DATASOURCE_USERNAME=admin
ENV SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/DB_Journal

CMD ["sh", "-c", "java -jar /journal_app/Backend_Search-0.0.1-SNAPSHOT.jar && java -jar /journal_app/Backend_Search-0.0.1-SNAPSHOT-jar-with-dependencies.jar"]