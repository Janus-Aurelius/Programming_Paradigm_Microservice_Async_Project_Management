#!/bin/bash

# MongoDB Database Initialization Script for Project Management System
# This script sets up the database schema and inserts sample data

set -e  # Exit on any error

# Configuration
DB_NAME="project_management"
MONGO_HOST="${MONGO_HOST:-localhost}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_URI="mongodb://${MONGO_HOST}:${MONGO_PORT}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if MongoDB is running
check_mongodb() {
    print_status "Checking MongoDB connection..."
    if mongosh --host "${MONGO_HOST}" --port "${MONGO_PORT}" --eval "db.adminCommand('ping')" --quiet > /dev/null 2>&1; then
        print_success "MongoDB is running and accessible"
        return 0
    else
        print_error "Cannot connect to MongoDB at ${MONGO_URI}"
        print_error "Please ensure MongoDB is running and accessible"
        return 1
    fi
}

# Function to check if mongosh is installed
check_mongosh() {
    if command -v mongosh &> /dev/null; then
        print_success "MongoDB Shell (mongosh) is installed"
        return 0
    else
        print_error "MongoDB Shell (mongosh) is not installed"
        print_error "Please install mongosh: https://docs.mongodb.com/mongodb-shell/"
        return 1
    fi
}

# Function to backup existing database
backup_database() {
    if [ "$1" = "--backup" ] || [ "$1" = "-b" ]; then
        print_status "Creating backup of existing database..."
        BACKUP_DIR="./backups/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "${BACKUP_DIR}"
        
        if mongodump --host "${MONGO_HOST}" --port "${MONGO_PORT}" --db "${DB_NAME}" --out "${BACKUP_DIR}" --quiet; then
            print_success "Backup created at ${BACKUP_DIR}"
        else
            print_warning "Backup failed or database doesn't exist yet"
        fi
    fi
}

# Function to drop existing database
drop_database() {
    if [ "$1" = "--reset" ] || [ "$1" = "-r" ]; then
        print_warning "Dropping existing database: ${DB_NAME}"
        mongosh "${MONGO_URI}" --eval "
            use('${DB_NAME}');
            db.dropDatabase();
            print('Database ${DB_NAME} dropped successfully');
        " --quiet
        print_success "Database dropped"
    fi
}

# Function to create database schema
create_schema() {
    print_status "Creating database schema and indexes..."
    if mongosh "${MONGO_URI}" --file "./mongodb-schema.js" --quiet; then
        print_success "Database schema created successfully"
        return 0
    else
        print_error "Failed to create database schema"
        return 1
    fi
}

# Function to insert sample data
insert_sample_data() {
    if [ "$1" != "--schema-only" ] && [ "$1" != "-s" ]; then
        print_status "Inserting sample data..."
        if mongosh "${MONGO_URI}" --file "./sample-data.js" --quiet; then
            print_success "Sample data inserted successfully"
            return 0
        else
            print_error "Failed to insert sample data"
            return 1
        fi
    else
        print_warning "Skipping sample data insertion (schema-only mode)"
    fi
}

# Function to verify database setup
verify_setup() {
    print_status "Verifying database setup..."
    
    VERIFICATION_RESULT=$(mongosh "${MONGO_URI}" --eval "
        use('${DB_NAME}');
        const collections = db.getCollectionNames();
        const stats = {
            collections: collections.length,
            users: db.users.countDocuments(),
            projects: db.projects.countDocuments(),
            tasks: db.tasks.countDocuments(),
            comments: db.comments.countDocuments(),
            notifications: db.notifications.countDocuments()
        };
        print(JSON.stringify(stats));
    " --quiet)
    
    print_success "Database verification completed"
    echo "Database Statistics:"
    echo "${VERIFICATION_RESULT}" | grep -o '{.*}' | python3 -m json.tool 2>/dev/null || echo "${VERIFICATION_RESULT}"
}

# Function to display usage information
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -r, --reset         Drop existing database before setup"
    echo "  -b, --backup        Create backup before setup"
    echo "  -s, --schema-only   Create schema only, skip sample data"
    echo "  --mongo-host HOST   MongoDB host (default: localhost)"
    echo "  --mongo-port PORT   MongoDB port (default: 27017)"
    echo ""
    echo "Examples:"
    echo "  $0                              # Standard setup with sample data"
    echo "  $0 --reset                      # Reset database and setup"
    echo "  $0 --backup --reset             # Backup, reset, and setup"
    echo "  $0 --schema-only                # Create schema only"
    echo "  $0 --mongo-host 192.168.1.100  # Connect to remote MongoDB"
}

# Main execution function
main() {
    print_status "=== MongoDB Database Setup for Project Management System ==="
    
    # Parse command line arguments
    RESET_DB=false
    BACKUP_DB=false
    SCHEMA_ONLY=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -r|--reset)
                RESET_DB=true
                shift
                ;;
            -b|--backup)
                BACKUP_DB=true
                shift
                ;;
            -s|--schema-only)
                SCHEMA_ONLY=true
                shift
                ;;
            --mongo-host)
                MONGO_HOST="$2"
                MONGO_URI="mongodb://${MONGO_HOST}:${MONGO_PORT}"
                shift 2
                ;;
            --mongo-port)
                MONGO_PORT="$2"
                MONGO_URI="mongodb://${MONGO_HOST}:${MONGO_PORT}"
                shift 2
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Pre-flight checks
    if ! check_mongosh; then
        exit 1
    fi
    
    if ! check_mongodb; then
        exit 1
    fi
    
    # Backup if requested
    if [ "$BACKUP_DB" = true ]; then
        backup_database "--backup"
    fi
    
    # Reset database if requested
    if [ "$RESET_DB" = true ]; then
        drop_database "--reset"
    fi
    
    # Create schema
    if ! create_schema; then
        exit 1
    fi
    
    # Insert sample data unless schema-only mode
    if [ "$SCHEMA_ONLY" = true ]; then
        insert_sample_data "--schema-only"
    else
        if ! insert_sample_data; then
            exit 1
        fi
    fi
    
    # Verify setup
    verify_setup
    
    print_success "=== Database setup completed successfully! ==="
    print_status "MongoDB URI: ${MONGO_URI}"
    print_status "Database: ${DB_NAME}"
    
    if [ "$SCHEMA_ONLY" = false ]; then
        echo ""
        print_status "Sample user credentials:"
        echo "  Admin: admin / admin123"
        echo "  Project Manager: john.doe / password123"
        echo "  Developer: jane.smith / password123"
        echo "  Developer: mike.wilson / password123"
        echo "  User: sarah.brown / password123"
    fi
}

# Run main function with all arguments
main "$@"
