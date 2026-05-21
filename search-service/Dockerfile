FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN mvn -q -pl search-service -am package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/search-service/target/search-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]