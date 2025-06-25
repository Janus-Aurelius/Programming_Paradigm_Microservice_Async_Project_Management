# MongoDB Setup for Project Management Microservices

## Overview

This setup provides MongoDB database initialization for a microservices architecture where each service has its own isolated database.

## Database Structure

- **userdb** - User service data (users collection)
- **projectdb** - Project service data (projects collection)
- **taskdb** - Task service data (tasks collection)
- **commentdb** - Comment service data (comments collection)
- **notificationdb** - Notification service data (notifications collection)

## Files Structure

```
database-schema/
├── docker-entrypoint-initdb.d/          # Docker MongoDB initialization scripts
│   ├── 01-init-mongo.js                 # Basic initialization (no auth)
│   ├── 02-mongodb-schema.js             # Database schemas and indexes
│   └── 03-sample-data.js                # Sample data for testing
├── init-mongo.js                        # Original init script (legacy)
├── mongodb-schema.js                    # Original schema script (legacy)
└── sample-data.js                       # Original sample data script (legacy)
```

## Auto-Reset Options

### Option A: Persistent Data (Default)

Database data persists between container restarts.

```yaml
volumes:
  - mongo_data:/data/db
```

### Option B: Auto-Reset on Restart

Database data is cleared when containers are restarted.

```yaml
volumes:
  # Comment out the persistent volume line and uncomment this:
  # - /tmp/mongo_data:/data/db
```

## Quick Start

### 1. Start Services

```bash
docker-compose up -d
```

### 2. Reset Database (Development)

```bash
# Windows PowerShell
.\reset-mongodb.ps1

# Manual reset
docker-compose down -v
docker-compose up -d
```

### 3. View MongoDB Logs

```bash
docker-compose logs mongo
```

## Database Access

### Connection String (No Authentication)

```
mongodb://localhost:27017
```

### Access via MongoDB Compass

- Host: localhost
- Port: 27017
- No authentication required

### Access via Container

```bash
docker exec -it mongo mongosh
```

## Sample Data

The initialization automatically creates:

- 5 sample users (admin, project manager, developers)
- 3 sample projects
- 6 sample tasks
- 5 sample comments
- 5 sample notifications

## Microservices Configuration

Each service's `application.yml` should point to its specific database:

```yaml
# user-service
spring.data.mongodb.uri: mongodb://mongo:27017/userdb

# project-service
spring.data.mongodb.uri: mongodb://mongo:27017/projectdb

# task-service
spring.data.mongodb.uri: mongodb://mongo:27017/taskdb

# comment-service
spring.data.mongodb.uri: mongodb://mongo:27017/commentdb

# notification-service
spring.data.mongodb.uri: mongodb://mongo:27017/notificationdb
```

## Development vs Production

- **Development**: No authentication, auto-reset available
- **Production**: Add authentication in `init-mongo.js`, use persistent volumes

## Troubleshooting

### Initialization Scripts Not Running

- Ensure folder path is correct: `./database-schema/docker-entrypoint-initdb.d`
- Scripts only run on first container creation
- Remove volumes if you need to re-run: `docker-compose down -v`

### Cannot Connect to Database

- Check if MongoDB container is running: `docker-compose ps`
- View logs: `docker-compose logs mongo`
- Ensure port 27017 is not used by another process

### Data Not Persisting

- Check volume configuration in docker-compose.yml
- Ensure `mongo_data` volume exists: `docker volume ls`

## Manual Database Management

### Connect to MongoDB Shell

```bash
docker exec -it mongo mongosh
```

### Switch Databases

```javascript
use("userdb"); // Switch to user database
use("projectdb"); // Switch to project database
use("taskdb"); // Switch to task database
use("commentdb"); // Switch to comment database
use("notificationdb"); // Switch to notification database
```

### View Collections

```javascript
show collections
```

### Count Documents

```javascript
db.users.countDocuments();
db.projects.countDocuments();
db.tasks.countDocuments();
db.comments.countDocuments();
db.notifications.countDocuments();
```

## Performance Notes

- Each database has proper indexes for optimal performance
- Collections include validation schemas
- Resource limits are set in docker-compose.yml for development
