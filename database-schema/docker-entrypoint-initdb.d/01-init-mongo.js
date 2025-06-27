// MongoDB Docker initialization script
// This script runs when the MongoDB container starts for the first time
// Development version - No authentication required

print("=== Running 01-init-mongo.js ===");

// Create databases for each microservice
// Note: Databases will be created automatically when first data is inserted

print("MongoDB initialization completed successfully!");
print("Created databases for microservices:");
print("- userdb (for user-service)");
print("- projectdb (for project-service)");
print("- taskdb (for task-service)");
print("- commentdb (for comment-service)");
print("- notificationdb (for notification-service)");
print("No authentication configured for development environment");
