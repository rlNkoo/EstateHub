FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN mvn -q -DskipTests -pl user-service -am package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/user-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]