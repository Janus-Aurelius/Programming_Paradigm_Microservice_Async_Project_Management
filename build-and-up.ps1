# This script builds the parent POM, all modules, and then runs Docker Compose with build

# Step 1: Install the parent POM
echo "Installing parent POM (pom-docker.xml)..."
mvn install -f pom-docker.xml

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to install parent POM. Aborting."
    exit 1
}

# Step 2: Build and install all modules
echo "Running mvn clean install -U..."
mvn clean install -U

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed. Aborting."
    exit 1
}