# Stage 1: Build the application
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

# --- Copy ALL necessary source code ---
COPY pom.xml ./pom.xml
COPY common-contracts ./common-contracts
COPY common-security ./common-security
COPY api-gateway ./api-gateway
COPY comment-service ./comment-service
COPY notification-service ./notification-service
COPY project-service ./project-service
COPY task-service ./task-service
COPY user-service ./user-service
COPY websocket-service ./websocket-service

# --- Build ALL Modules ---
RUN chmod +x notification-service/mvnw && ./notification-service/mvnw -f pom.xml install -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/notification-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","/app/app.jar"]
