# Building and Running with Docker Compose

## Prerequisites
- Java 17
- Docker and Docker Compose
- Maven (or use the provided `mvnw`/`mvnw.cmd` wrappers)

## Multi-Module Maven Projects and Docker

This project uses a multi-module Maven structure. Each service (e.g., user-service, notification-service) depends on shared modules (like `common-contracts`, `common-security`) and a parent POM in the project root.

**Important:** When building Docker images for each service, the Docker build context is limited to that service's directory. This means the parent POM and shared modules are not available inside the Docker build context by default.

## Recommended Build Workflow

### 1. Build All Modules on Host
Before running Docker Compose, build and install all modules from the project root:

```
# On Windows PowerShell:
./mvnw.cmd clean install -DskipTests

# On Linux/macOS:
./mvnw clean install -DskipTests
```

This ensures all shared modules and the parent POM are available in your local Maven repository for Docker builds.

### 2. Build and Run with Docker Compose

From the project root, run:

```
docker-compose up --build
```

This will build and start all services using the Dockerfiles in each service directory.

## Advanced: Building Inside Docker (Not Recommended for Local Dev)
If you want to build everything inside Docker (e.g., for CI), you must:
- Copy the parent `pom.xml` and all shared modules (`common-contracts`, `common-security`) into the build context for each service, or
- Use a multi-stage build with a larger build context (project root).

This is more complex and not required for most local development workflows.

## Troubleshooting
- If you see errors like `Could not find artifact com.pm:project-management-parent:pom:0.0.1-SNAPSHOT` or `Could not find artifact com.pm:common-security:jar:0.0.1-SNAPSHOT`, it means the shared modules or parent POM are not available in the Maven repository. Run the root Maven build as shown above.
- If you add or change shared modules, re-run the root Maven build before building Docker images.

---

For questions or issues, check the Dockerfile and docker-compose.yml for correct build paths and port mappings.
