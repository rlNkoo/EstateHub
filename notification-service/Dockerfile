FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN mvn -q -pl notification-service -am package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]