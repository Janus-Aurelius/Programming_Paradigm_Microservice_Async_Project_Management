# Database Setup and Migration Scripts

This directory contains MongoDB database schema definitions and sample data for the Project Management Microservices System.

## Files Overview

### 1. `mongodb-schema.js`

- **Purpose**: Defines the complete MongoDB database schema with validation rules and indexes
- **Features**:
  - Collection creation with JSON Schema validation
  - Comprehensive field validation rules
  - Performance-optimized indexes
  - Data integrity constraints

### 2. `sample-data.js`

- **Purpose**: Inserts sample data for development and testing
- **Features**:
  - Realistic sample users with different roles
  - Project data with various statuses and priorities
  - Task assignments and dependencies
  - Comments and notifications
  - Attachments and file references

## Database Collections

### Users Collection

- **Purpose**: Store user accounts and authentication data
- **Key Fields**: username, email, hashedPassword, role, enabled, active
- **Indexes**: username (unique), email (unique), role, enabled+active

### Projects Collection

- **Purpose**: Store project information and team assignments
- **Key Fields**: name, description, status, priority, ownerId, memberIds
- **Indexes**: name, ownerId, memberIds, status, priority, dates

### Tasks Collection

- **Purpose**: Store task details and assignments
- **Key Fields**: projectId, name, status, priority, assigneeId, dueDate
- **Indexes**: projectId, assigneeId, status, priority, dueDate

### Comments Collection

- **Purpose**: Store comments on projects, tasks, and other comments
- **Key Fields**: parentId, parentType, content, userId
- **Indexes**: parentId+parentType, userId, createdAt

### Notifications Collection

- **Purpose**: Store user notifications and alerts
- **Key Fields**: recipientUserId, eventType, message, entityType, entityId
- **Indexes**: recipientUserId, isRead, eventType, entityType+entityId

## Setup Instructions

### Prerequisites

- MongoDB 4.4 or higher
- MongoDB Shell (mongosh) or MongoDB Compass

### 1. Create Database Schema

```bash
# Using MongoDB Shell
mongosh mongodb://localhost:27017 --file mongodb-schema.js

# Or using MongoDB Compass
# Copy and paste the content of mongodb-schema.js into the MongoDB Compass shell
```

### 2. Insert Sample Data

```bash
# Using MongoDB Shell
mongosh mongodb://localhost:27017 --file sample-data.js

# Or using MongoDB Compass
# Copy and paste the content of sample-data.js into the MongoDB Compass shell
```

### 3. Verify Setup

```javascript
// Connect to the database
use('project_management');

// Check collections
show collections;

// Verify data
db.users.find().count();
db.projects.find().count();
db.tasks.find().count();
db.comments.find().count();
db.notifications.find().count();
```

## Data Relationships

### User Roles and Permissions

- **ROLE_ADMIN**: Full system access
- **ROLE_PROJECT_MANAGER**: Can create/manage projects and assign tasks
- **ROLE_DEVELOPER**: Can work on assigned tasks and comment
- **ROLE_USER**: Basic read access and commenting

### Entity Relationships

```
User (1) ────── (M) Project (owner)
User (M) ────── (M) Project (members/managers)
Project (1) ─── (M) Task
User (1) ─────── (M) Task (assignee)
User (1) ─────── (M) Comment (author)
Project/Task (1) ─ (M) Comment (parent)
User (1) ─────── (M) Notification (recipient)
```

## Validation Rules

### User Validation

- Username: 3-50 characters, alphanumeric + underscore
- Email: Valid email format
- Password: BCrypt hashed, minimum 60 characters
- Role: Must be one of defined enum values

### Project Validation

- Name: 1-200 characters
- Description: Maximum 2000 characters
- Status: Must be valid enum value
- Priority: Must be valid enum value

### Task Validation

- Name: 1-200 characters
- Description: Maximum 5000 characters
- Status: Must be valid enum value
- Priority: Must be valid enum value

### Comment Validation

- Content: 1-5000 characters
- ParentType: Must be PROJECT, TASK, or COMMENT
- Must reference valid parent entity

## Performance Considerations

### Indexes Created

1. **User Indexes**: username, email (unique), role, status
2. **Project Indexes**: name, owner, members, status, priority, dates
3. **Task Indexes**: project, assignee, status, priority, due date
4. **Comment Indexes**: parent entity, user, creation date
5. **Notification Indexes**: recipient, read status, event type, entity

### Query Optimization Tips

- Use compound indexes for multi-field queries
- Index fields used in WHERE clauses
- Consider text indexes for search functionality
- Monitor query performance with explain()

## Security Considerations

### Data Protection

- Passwords are BCrypt hashed with salt rounds
- Sensitive fields should be excluded from API responses
- Implement field-level security in application layer

### Access Control

- Role-based access control (RBAC) implementation
- User status checks (enabled, active, locked)
- Email verification requirements

## Backup and Maintenance

### Regular Backup

```bash
# Create backup
mongodump --db project_management --out /backup/mongodb/

# Restore backup
mongorestore --db project_management /backup/mongodb/project_management/
```

### Index Maintenance

```javascript
// Check index usage
db.users.getIndexes();
db.projects.getIndexes();

// Rebuild indexes if needed
db.users.reIndex();
db.projects.reIndex();
```

## Environment-Specific Considerations

### Development

- Use sample data for testing
- Enable detailed logging
- Consider using MongoDB replica set for testing replication

### Production

- Implement proper backup strategy
- Set up monitoring and alerting
- Configure appropriate connection limits
- Use MongoDB Atlas or dedicated MongoDB cluster

## Troubleshooting

### Common Issues

1. **Validation Errors**: Check field constraints and enum values
2. **Index Conflicts**: Drop and recreate indexes if schema changes
3. **Connection Issues**: Verify MongoDB service is running
4. **Performance Issues**: Analyze slow queries with explain()

### Useful Queries

```javascript
// Find validation errors
db.runCommand({ collMod: "users", validator: {}, validationAction: "warn" });

// Check index usage
db.users.explain("executionStats").find({ username: "admin" });

// Monitor operations
db.currentOp();
```
