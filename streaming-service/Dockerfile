FROM maven:3.8.4-openjdk-17-slim
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
CMD ["mvn", "spring-boot:run"]


# #Dockerfile
# FROM maven:3.8.4-openjdk-17-slim AS build
# WORKDIR /app
# COPY . .
# RUN mvn clean package -DskipTests

# FROM openjdk:17-slim
# COPY --from=build /app/target/*.jar app.jar
# EXPOSE 8080
# ENTRYPOINT ["java","-jar","/app.jar"]
