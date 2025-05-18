# Stage 1: Build the application
FROM eclipse-temurin:17-jdk AS builder

# Set base working directory
WORKDIR /workspace

# --- Copy ALL necessary source code ---
# Copy parent pom from the context root
COPY pom.xml ./pom.xml

# Copy common modules from the context root
COPY common-contracts ./common-contracts
COPY common-security ./common-security

# Copy ALL service directories that are modules defined in the parent pom.xml
# This is needed so Maven can find and build everything.
COPY api-gateway ./api-gateway
COPY comment-service ./comment-service
COPY notification-service ./notification-service
COPY project-service ./project-service
COPY task-service ./task-service
COPY user-service ./user-service
COPY websocket-service ./websocket-service
# Add any other modules if they exist and are part of the build

# --- Build ALL Modules ---
# Run the full multi-module build from the root (/workspace)
# Use the mvnw script from one of the modules (e.g., project-service)
# The '-f pom.xml' explicitly tells Maven to use the root POM for module discovery.
# This command will build common-contracts, common-security first and install them
# into the builder container's local Maven repository before building the dependent services.
RUN chmod +x project-service/mvnw && ./project-service/mvnw -f pom.xml install -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy ONLY the specific target JAR for *this* service from the builder stage
# The path inside the builder is /workspace/project-service/target/
COPY --from=builder /workspace/project-service/target/*.jar app.jar

# Expose the correct port for this service
EXPOSE 8082

# Set the entrypoint to run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]