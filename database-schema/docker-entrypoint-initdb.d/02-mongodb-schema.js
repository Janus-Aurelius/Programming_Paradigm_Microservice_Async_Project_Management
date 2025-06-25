// MongoDB Database Schema for Project Management Microservices
// This script creates collections with validation rules and indexes
// Each microservice has its own database for proper isolation

// =======================================
// USER-SERVICE DATABASE SCHEMA
// =======================================
db = db.getSiblingDB("userdb");

// USER COLLECTION SCHEMA
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["username", "email", "password", "role", "enabled", "active"],
      properties: {
        _id: { bsonType: "objectId" },
        username: {
          bsonType: "string",
          minLength: 3,
          maxLength: 50,
          pattern: "^[a-zA-Z0-9_]+$",
        },
        email: {
          bsonType: "string",
          pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        },
        password: {
          bsonType: "string",
        },
        role: {
          enum: [
            "ROLE_USER",
            "ROLE_ADMIN",
            "ROLE_PROJECT_MANAGER",
            "ROLE_DEVELOPER",
          ],
        },
        enabled: { bsonType: "bool" },
        active: { bsonType: "bool" },
        firstName: {
          bsonType: "string",
          maxLength: 100,
        },
        lastName: {
          bsonType: "string",
          maxLength: 100,
        },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" },
        lastLogin: { bsonType: "date" },
        emailVerified: { bsonType: "bool" },
        locked: { bsonType: "bool" },
        profilePictureUrl: { bsonType: "string" },
      },
    },
  },
});

// User Collection Indexes
db.users.createIndex({ username: 1 }, { unique: true });
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ role: 1 });
db.users.createIndex({ enabled: 1, active: 1 });
db.users.createIndex({ createdAt: 1 });

print("User database schema created successfully!");

// =======================================
// PROJECT-SERVICE DATABASE SCHEMA
// =======================================
db = db.getSiblingDB("projectdb");
// PROJECT COLLECTION SCHEMA
db.createCollection("projects", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name", "status", "priority", "ownerId", "createdBy"],
      properties: {
        _id: { bsonType: "objectId" },
        name: {
          bsonType: "string",
          minLength: 1,
          maxLength: 200,
        },
        description: {
          bsonType: "string",
          maxLength: 2000,
        },
        status: {
          enum: [
            "PLANNING",
            "IN_PROGRESS",
            "PENDING",
            "COMPLETED",
            "CANCELLED",
            "FAILED",
          ],
        },
        priority: {
          enum: ["LOW", "MEDIUM", "HIGH"],
        },
        ownerId: { bsonType: "string" },
        managerIds: {
          bsonType: "array",
          items: { bsonType: "string" },
        },
        memberIds: {
          bsonType: "array",
          items: { bsonType: "string" },
        },
        startDate: { bsonType: "date" },
        endDate: { bsonType: "date" },
        assignedTo: {
          bsonType: "array",
          items: { bsonType: "string" },
        },
        taskIds: {
          bsonType: "array",
          items: { bsonType: "string" },
        },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" },
        createdBy: { bsonType: "string" },
        lastModifiedBy: { bsonType: "string" },
        version: { bsonType: "long" },
      },
    },
  },
});

// Project Collection Indexes
db.projects.createIndex({ name: 1 });
db.projects.createIndex({ ownerId: 1 });
db.projects.createIndex({ managerIds: 1 });
db.projects.createIndex({ memberIds: 1 });
db.projects.createIndex({ status: 1 });
db.projects.createIndex({ priority: 1 });
db.projects.createIndex({ createdAt: 1 });
db.projects.createIndex({ startDate: 1, endDate: 1 });

print("Project database schema created successfully!");

// =======================================
// TASK-SERVICE DATABASE SCHEMA
// =======================================
db = db.getSiblingDB("taskdb");
// TASK COLLECTION SCHEMA
db.createCollection("tasks", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["projectId", "name", "status", "priority", "createdBy"],
      properties: {
        _id: { bsonType: "objectId" },
        projectId: { bsonType: "string" },
        name: {
          bsonType: "string",
          minLength: 1,
          maxLength: 200,
        },
        status: {
          enum: ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "BLOCKED"],
        },
        priority: {
          enum: ["LOW", "MEDIUM", "HIGH", "CRITICAL"],
        },
        description: {
          bsonType: "string",
          maxLength: 5000,
        },
        createdBy: { bsonType: "string" },
        updatedBy: { bsonType: "string" },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" },
        dueDate: { bsonType: "date" },
        assigneeId: { bsonType: "string" },
        tags: {
          bsonType: "array",
          items: { bsonType: "string" },
        },
        attachments: {
          bsonType: "array",
          items: {
            bsonType: "object",
            properties: {
              id: { bsonType: "string" },
              filename: { bsonType: "string" },
              originalName: { bsonType: "string" },
              contentType: { bsonType: "string" },
              size: { bsonType: "long" },
              uploadedBy: { bsonType: "string" },
              uploadedAt: { bsonType: "date" },
              url: { bsonType: "string" },
            },
          },
        },
        version: { bsonType: "long" },
      },
    },
  },
});

// Task Collection Indexes
db.tasks.createIndex({ projectId: 1 });
db.tasks.createIndex({ assigneeId: 1 });
db.tasks.createIndex({ status: 1 });
db.tasks.createIndex({ priority: 1 });
db.tasks.createIndex({ createdBy: 1 });
db.tasks.createIndex({ dueDate: 1 });
db.tasks.createIndex({ tags: 1 });
db.tasks.createIndex({ createdAt: 1 });

print("Task database schema created successfully!");

// =======================================
// COMMENT-SERVICE DATABASE SCHEMA
// =======================================
db = db.getSiblingDB("commentdb");
// COMMENT COLLECTION SCHEMA
db.createCollection("comments", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["parentId", "parentType", "content", "userId"],
      properties: {
        _id: { bsonType: "objectId" },
        parentId: { bsonType: "string" },
        parentType: {
          enum: ["PROJECT", "TASK", "COMMENT"],
        },
        content: {
          bsonType: "string",
          minLength: 1,
          maxLength: 5000,
        },
        userId: { bsonType: "string" },
        displayName: { bsonType: "string" },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" },
        parentCommentId: { bsonType: "string" },
        version: { bsonType: "long" },
        deleted: { bsonType: "bool" },
      },
    },
  },
});

// Comment Collection Indexes
db.comments.createIndex({ parentId: 1, parentType: 1 });
db.comments.createIndex({ userId: 1 });
db.comments.createIndex({ parentCommentId: 1 });
db.comments.createIndex({ createdAt: 1 });
db.comments.createIndex({ deleted: 1 });

print("Comment database schema created successfully!");

// =======================================
// NOTIFICATION-SERVICE DATABASE SCHEMA
// =======================================
db = db.getSiblingDB("notificationdb");
// NOTIFICATION COLLECTION SCHEMA
db.createCollection("notifications", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: [
        "recipientUserId",
        "eventType",
        "message",
        "entityType",
        "entityId",
        "channel",
      ],
      properties: {
        _id: { bsonType: "objectId" },
        recipientUserId: { bsonType: "string" },
        eventType: {
          enum: [
            "TASK_ASSIGNED",
            "TASK_COMPLETED",
            "PROJECT_CREATED",
            "COMMENT_ADDED",
            "DEADLINE_APPROACHING",
            "USER_MENTIONED",
          ],
        },
        message: {
          bsonType: "string",
          maxLength: 1000,
        },
        entityType: {
          enum: ["USER", "PROJECT", "TASK", "COMMENT"],
        },
        entityId: { bsonType: "string" },
        channel: {
          enum: ["EMAIL", "IN_APP", "PUSH", "SMS"],
        },
        createdAt: { bsonType: "date" },
        isRead: { bsonType: "bool" },
        timestamp: { bsonType: "date" },
        payload: { bsonType: "object" },
        version: { bsonType: "long" },
      },
    },
  },
});

// Notification Collection Indexes
db.notifications.createIndex({ recipientUserId: 1 });
db.notifications.createIndex({ isRead: 1 });
db.notifications.createIndex({ eventType: 1 });
db.notifications.createIndex({ entityType: 1, entityId: 1 });
db.notifications.createIndex({ createdAt: 1 });
db.notifications.createIndex({ timestamp: 1 });

print("Notification database schema created successfully!");

print("\n=== MongoDB Schema Creation Summary ===");
print("✓ userdb - users collection with indexes");
print("✓ projectdb - projects collection with indexes");
print("✓ taskdb - tasks collection with indexes");
print("✓ commentdb - comments collection with indexes");
print("✓ notificationdb - notifications collection with indexes");
print("\nAll databases are properly isolated for microservices architecture!");
