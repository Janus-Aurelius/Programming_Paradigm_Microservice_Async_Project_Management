@echo off
REM MongoDB Database Initialization Script for Project Management System (Windows)
REM This script sets up the database schema and inserts sample data

setlocal enabledelayedexpansion

REM Configuration
set "DB_NAME=project_management"
if "%MONGO_HOST%"=="" set "MONGO_HOST=localhost"
if "%MONGO_PORT%"=="" set "MONGO_PORT=27017"
set "MONGO_URI=mongodb://%MONGO_HOST%:%MONGO_PORT%"

REM Parse command line arguments
set "RESET_DB=false"
set "BACKUP_DB=false"
set "SCHEMA_ONLY=false"

:parse_args
if "%~1"=="" goto :end_parse
if "%~1"=="-h" goto :show_usage
if "%~1"=="--help" goto :show_usage
if "%~1"=="-r" set "RESET_DB=true"
if "%~1"=="--reset" set "RESET_DB=true"
if "%~1"=="-b" set "BACKUP_DB=true"
if "%~1"=="--backup" set "BACKUP_DB=true"
if "%~1"=="-s" set "SCHEMA_ONLY=true"
if "%~1"=="--schema-only" set "SCHEMA_ONLY=true"
if "%~1"=="--mongo-host" (
    set "MONGO_HOST=%~2"
    set "MONGO_URI=mongodb://!MONGO_HOST!:!MONGO_PORT!"
    shift
)
if "%~1"=="--mongo-port" (
    set "MONGO_PORT=%~2"
    set "MONGO_URI=mongodb://!MONGO_HOST!:!MONGO_PORT!"
    shift
)
shift
goto :parse_args

:end_parse

echo [INFO] === MongoDB Database Setup for Project Management System ===

REM Check if mongosh is installed
echo [INFO] Checking MongoDB Shell installation...
mongosh --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] MongoDB Shell (mongosh) is not installed
    echo [ERROR] Please install mongosh: https://docs.mongodb.com/mongodb-shell/
    exit /b 1
)
echo [SUCCESS] MongoDB Shell (mongosh) is installed

REM Check MongoDB connection
echo [INFO] Checking MongoDB connection...
mongosh --host "%MONGO_HOST%" --port "%MONGO_PORT%" --eval "db.adminCommand('ping')" --quiet >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Cannot connect to MongoDB at %MONGO_URI%
    echo [ERROR] Please ensure MongoDB is running and accessible
    exit /b 1
)
echo [SUCCESS] MongoDB is running and accessible

REM Backup if requested
if "%BACKUP_DB%"=="true" (
    echo [INFO] Creating backup of existing database...
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set "timestamp=!datetime:~0,8!_!datetime:~8,6!"
    set "BACKUP_DIR=.\backups\!timestamp!"
    if not exist "!BACKUP_DIR!" mkdir "!BACKUP_DIR!"
    
    mongodump --host "%MONGO_HOST%" --port "%MONGO_PORT%" --db "%DB_NAME%" --out "!BACKUP_DIR!" --quiet
    if errorlevel 1 (
        echo [WARNING] Backup failed or database doesn't exist yet
    ) else (
        echo [SUCCESS] Backup created at !BACKUP_DIR!
    )
)

REM Reset database if requested
if "%RESET_DB%"=="true" (
    echo [WARNING] Dropping existing database: %DB_NAME%
    mongosh "%MONGO_URI%" --eval "use('%DB_NAME%'); db.dropDatabase(); print('Database %DB_NAME% dropped successfully');" --quiet
    echo [SUCCESS] Database dropped
)

REM Create database schema
echo [INFO] Creating database schema and indexes...
mongosh "%MONGO_URI%" --file "mongodb-schema.js" --quiet
if errorlevel 1 (
    echo [ERROR] Failed to create database schema
    exit /b 1
)
echo [SUCCESS] Database schema created successfully

REM Insert sample data unless schema-only mode
if "%SCHEMA_ONLY%"=="true" (
    echo [WARNING] Skipping sample data insertion (schema-only mode)
) else (
    echo [INFO] Inserting sample data...
    mongosh "%MONGO_URI%" --file "sample-data.js" --quiet
    if errorlevel 1 (
        echo [ERROR] Failed to insert sample data
        exit /b 1
    )
    echo [SUCCESS] Sample data inserted successfully
)

REM Verify database setup
echo [INFO] Verifying database setup...
mongosh "%MONGO_URI%" --eval "use('%DB_NAME%'); const collections = db.getCollectionNames(); const stats = { collections: collections.length, users: db.users.countDocuments(), projects: db.projects.countDocuments(), tasks: db.tasks.countDocuments(), comments: db.comments.countDocuments(), notifications: db.notifications.countDocuments() }; print('Collections: ' + stats.collections); print('Users: ' + stats.users); print('Projects: ' + stats.projects); print('Tasks: ' + stats.tasks); print('Comments: ' + stats.comments); print('Notifications: ' + stats.notifications);" --quiet
echo [SUCCESS] Database verification completed

echo [SUCCESS] === Database setup completed successfully! ===
echo [INFO] MongoDB URI: %MONGO_URI%
echo [INFO] Database: %DB_NAME%

if "%SCHEMA_ONLY%"=="false" (
    echo.
    echo [INFO] Sample user credentials:
    echo   Admin: admin / admin123
    echo   Project Manager: john.doe / password123
    echo   Developer: jane.smith / password123
    echo   Developer: mike.wilson / password123
    echo   User: sarah.brown / password123
)

goto :eof

:show_usage
echo Usage: %~nx0 [OPTIONS]
echo.
echo Options:
echo   -h, --help          Show this help message
echo   -r, --reset         Drop existing database before setup
echo   -b, --backup        Create backup before setup
echo   -s, --schema-only   Create schema only, skip sample data
echo   --mongo-host HOST   MongoDB host (default: localhost)
echo   --mongo-port PORT   MongoDB port (default: 27017)
echo.
echo Examples:
echo   %~nx0                              # Standard setup with sample data
echo   %~nx0 --reset                      # Reset database and setup
echo   %~nx0 --backup --reset             # Backup, reset, and setup
echo   %~nx0 --schema-only                # Create schema only
echo   %~nx0 --mongo-host 192.168.1.100  # Connect to remote MongoDB
exit /b 0
