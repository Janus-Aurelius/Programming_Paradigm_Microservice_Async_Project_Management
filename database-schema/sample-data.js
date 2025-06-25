// Sample Data Insertion Script for Project Management System
// This script inserts sample data for testing and development purposes
// Data is distributed across microservice-specific databases

// =======================================
// INSERT SAMPLE USERS (userdb)
// =======================================
use("userdb");

const users = [
  {
    _id: "user-001",
    username: "admin",
    email: "admin@projectmanagement.com",
    hashedPassword:
      "$2a$12$nuNTK380L8NjSzV9m0gxp.GDUB09PhpJ2p9A2LaFpJS0c07PmtHl6", // password: admin123
    role: "ROLE_ADMIN",
    enabled: true,
    active: true,
    firstName: "Admin",
    lastName: "User",
    createdAt: new Date("2024-01-01T00:00:00Z"),
    updatedAt: new Date("2024-01-01T00:00:00Z"),
    lastLogin: new Date("2024-12-01T10:00:00Z"),
    emailVerified: true,
    locked: false,
    profilePictureUrl:
      "https://ui-avatars.com/api/?name=Admin+User&background=0D8ABC&color=fff",
  },
  {
    _id: "user-002",
    username: "john.doe",
    email: "john.doe@company.com",
    hashedPassword:
      "$2a$12$TlBu/tZ9lXpVvjS1CbILbOr9WeceT9DDHQPi/MieKIVmk32bXrypS", // password: password123
    role: "ROLE_PROJECT_MANAGER",
    enabled: true,
    active: true,
    firstName: "John",
    lastName: "Doe",
    createdAt: new Date("2024-01-15T00:00:00Z"),
    updatedAt: new Date("2024-01-15T00:00:00Z"),
    lastLogin: new Date("2024-12-01T09:30:00Z"),
    emailVerified: true,
    locked: false,
    profilePictureUrl:
      "https://ui-avatars.com/api/?name=John+Doe&background=28a745&color=fff",
  },
  {
    _id: "user-003",
    username: "jane.smith",
    email: "jane.smith@company.com",
    hashedPassword:
      "$2a$12$M9omtIR76XwUicyLtPhoZeNShWehUoRws8Q8OL5NFYxw5EEnM0rS.", // password: password123
    role: "ROLE_DEVELOPER",
    enabled: true,
    active: true,
    firstName: "Jane",
    lastName: "Smith",
    createdAt: new Date("2024-01-20T00:00:00Z"),
    updatedAt: new Date("2024-01-20T00:00:00Z"),
    lastLogin: new Date("2024-12-01T08:45:00Z"),
    emailVerified: true,
    locked: false,
    profilePictureUrl:
      "https://ui-avatars.com/api/?name=Jane+Smith&background=6f42c1&color=fff",
  },
  {
    _id: "user-004",
    username: "mike.wilson",
    email: "mike.wilson@company.com",
    hashedPassword:
      "$2a$12$gG18Ak7MVBnNVeohV8u1Ou9xmIJpIvzcyJI5SjZ29V7n/S2uSazC.", // password: password123
    role: "ROLE_DEVELOPER",
    enabled: true,
    active: true,
    firstName: "Mike",
    lastName: "Wilson",
    createdAt: new Date("2024-02-01T00:00:00Z"),
    updatedAt: new Date("2024-02-01T00:00:00Z"),
    lastLogin: new Date("2024-12-01T11:15:00Z"),
    emailVerified: true,
    locked: false,
    profilePictureUrl:
      "https://ui-avatars.com/api/?name=Mike+Wilson&background=fd7e14&color=fff",
  },
  {
    _id: "user-005",
    username: "sarah.brown",
    email: "sarah.brown@company.com",
    hashedPassword:
      "$2a$12$dV710Aa57CPCZ8iGxMZmYOkFyaBCNP9mrxkOrXR4NpW/1T6PHL0LG", // password: password123
    role: "ROLE_USER",
    enabled: true,
    active: true,
    firstName: "Sarah",
    lastName: "Brown",
    createdAt: new Date("2024-02-10T00:00:00Z"),
    updatedAt: new Date("2024-02-10T00:00:00Z"),
    lastLogin: new Date("2024-11-30T16:20:00Z"),
    emailVerified: true,
    locked: false,
    profilePictureUrl:
      "https://ui-avatars.com/api/?name=Sarah+Brown&background=e83e8c&color=fff",
  },
];

db.users.insertMany(users);
print("Sample users inserted successfully into userdb!");

// =======================================
// INSERT SAMPLE PROJECTS (projectdb)
// =======================================
use("projectdb");
const projects = [
  {
    _id: "project-001",
    name: "E-Commerce Platform Redesign",
    description:
      "Complete redesign of the company's e-commerce platform with modern UI/UX and improved performance",
    status: "ACTIVE",
    priority: "HIGH",
    ownerId: "user-002",
    managerIds: ["user-002"],
    memberIds: ["user-003", "user-004", "user-005"],
    startDate: new Date("2024-03-01T00:00:00Z"),
    endDate: new Date("2024-06-30T00:00:00Z"),
    assignedTo: ["user-003", "user-004"],
    taskIds: ["task-001", "task-002", "task-003"],
    createdAt: new Date("2024-02-15T00:00:00Z"),
    updatedAt: new Date("2024-11-30T00:00:00Z"),
    createdBy: "user-002",
    lastModifiedBy: "user-002",
    version: 1,
  },
  {
    _id: "project-002",
    name: "Mobile App Development",
    description:
      "Development of iOS and Android mobile applications for customer engagement",
    status: "PLANNING",
    priority: "MEDIUM",
    ownerId: "user-002",
    managerIds: ["user-002"],
    memberIds: ["user-003", "user-004"],
    startDate: new Date("2024-04-01T00:00:00Z"),
    endDate: new Date("2024-08-31T00:00:00Z"),
    assignedTo: ["user-003"],
    taskIds: ["task-004", "task-005"],
    createdAt: new Date("2024-03-01T00:00:00Z"),
    updatedAt: new Date("2024-03-01T00:00:00Z"),
    createdBy: "user-002",
    lastModifiedBy: "user-002",
    version: 1,
  },
  {
    _id: "project-003",
    name: "Data Analytics Dashboard",
    description:
      "Implementation of real-time analytics dashboard for business intelligence",
    status: "COMPLETED",
    priority: "LOW",
    ownerId: "user-002",
    managerIds: ["user-002"],
    memberIds: ["user-004", "user-005"],
    startDate: new Date("2024-01-01T00:00:00Z"),
    endDate: new Date("2024-02-29T00:00:00Z"),
    assignedTo: ["user-004"],
    taskIds: ["task-006"],
    createdAt: new Date("2023-12-15T00:00:00Z"),
    updatedAt: new Date("2024-02-29T00:00:00Z"),
    createdBy: "user-002",
    lastModifiedBy: "user-004",
    version: 3,
  },
];

db.projects.insertMany(projects);
print("Sample projects inserted successfully into projectdb!");

// =======================================
// INSERT SAMPLE TASKS (taskdb)
// =======================================
use("taskdb");

const tasks = [
  {
    _id: "task-001",
    projectId: "project-001",
    name: "Design new homepage layout",
    status: "IN_PROGRESS",
    priority: "HIGH",
    description:
      "Create mockups and wireframes for the new homepage design with responsive layout",
    createdBy: "user-002",
    updatedBy: "user-003",
    createdAt: new Date("2024-03-05T00:00:00Z"),
    updatedAt: new Date("2024-11-25T00:00:00Z"),
    dueDate: new Date("2024-12-15T00:00:00Z"),
    assigneeId: "user-003",
    tags: ["design", "ui/ux", "responsive"],
    attachments: [
      {
        id: "att-001",
        filename: "homepage_mockup.figma",
        originalName: "Homepage Mockup.figma",
        contentType: "application/figma",
        size: 2048576,
        uploadedBy: "user-003",
        uploadedAt: new Date("2024-11-20T00:00:00Z"),
        url: "https://storage.company.com/attachments/homepage_mockup.figma",
      },
    ],
    version: 2,
  },
  {
    _id: "task-002",
    projectId: "project-001",
    name: "Implement user authentication",
    status: "TODO",
    priority: "CRITICAL",
    description:
      "Implement secure user authentication with JWT tokens and password hashing",
    createdBy: "user-002",
    updatedBy: "user-002",
    createdAt: new Date("2024-03-10T00:00:00Z"),
    updatedAt: new Date("2024-03-10T00:00:00Z"),
    dueDate: new Date("2024-12-20T00:00:00Z"),
    assigneeId: "user-004",
    tags: ["backend", "security", "authentication"],
    attachments: [],
    version: 1,
  },
  {
    _id: "task-003",
    projectId: "project-001",
    name: "Setup CI/CD pipeline",
    status: "DONE",
    priority: "MEDIUM",
    description:
      "Configure automated testing and deployment pipeline using GitHub Actions",
    createdBy: "user-002",
    updatedBy: "user-004",
    createdAt: new Date("2024-03-01T00:00:00Z"),
    updatedAt: new Date("2024-11-15T00:00:00Z"),
    dueDate: new Date("2024-11-30T00:00:00Z"),
    assigneeId: "user-004",
    tags: ["devops", "ci/cd", "automation"],
    attachments: [],
    version: 3,
  },
  {
    _id: "task-004",
    projectId: "project-002",
    name: "Research mobile frameworks",
    status: "REVIEW",
    priority: "HIGH",
    description:
      "Evaluate React Native vs Flutter for cross-platform mobile development",
    createdBy: "user-002",
    updatedBy: "user-003",
    createdAt: new Date("2024-03-15T00:00:00Z"),
    updatedAt: new Date("2024-11-28T00:00:00Z"),
    dueDate: new Date("2024-12-10T00:00:00Z"),
    assigneeId: "user-003",
    tags: ["research", "mobile", "framework"],
    attachments: [
      {
        id: "att-002",
        filename: "framework_comparison.pdf",
        originalName: "Mobile Framework Comparison.pdf",
        contentType: "application/pdf",
        size: 1024768,
        uploadedBy: "user-003",
        uploadedAt: new Date("2024-11-28T00:00:00Z"),
        url: "https://storage.company.com/attachments/framework_comparison.pdf",
      },
    ],
    version: 2,
  },
  {
    _id: "task-005",
    projectId: "project-002",
    name: "Create app wireframes",
    status: "BLOCKED",
    priority: "MEDIUM",
    description:
      "Design wireframes for key app screens - waiting for UX research completion",
    createdBy: "user-002",
    updatedBy: "user-003",
    createdAt: new Date("2024-03-20T00:00:00Z"),
    updatedAt: new Date("2024-11-20T00:00:00Z"),
    dueDate: new Date("2024-12-25T00:00:00Z"),
    assigneeId: "user-003",
    tags: ["design", "wireframes", "mobile"],
    attachments: [],
    version: 1,
  },
  {
    _id: "task-006",
    projectId: "project-003",
    name: "Implement dashboard charts",
    status: "DONE",
    priority: "HIGH",
    description:
      "Create interactive charts using Chart.js for the analytics dashboard",
    createdBy: "user-002",
    updatedBy: "user-004",
    createdAt: new Date("2024-01-15T00:00:00Z"),
    updatedAt: new Date("2024-02-20T00:00:00Z"),
    dueDate: new Date("2024-02-25T00:00:00Z"),
    assigneeId: "user-004",
    tags: ["frontend", "charts", "analytics"],
    attachments: [],
    version: 4,
  },
];

db.tasks.insertMany(tasks);
print("Sample tasks inserted successfully into taskdb!");

// =======================================
// INSERT SAMPLE COMMENTS (commentdb)
// =======================================
use("commentdb");

const comments = [
  {
    _id: "comment-001",
    parentId: "task-001",
    parentType: "TASK",
    content:
      "I've completed the initial wireframes. Please review and provide feedback.",
    userId: "user-003",
    displayName: "Jane Smith",
    createdAt: new Date("2024-11-20T10:30:00Z"),
    updatedAt: new Date("2024-11-20T10:30:00Z"),
    parentCommentId: null,
    version: 1,
    deleted: false,
  },
  {
    _id: "comment-002",
    parentId: "task-001",
    parentType: "TASK",
    content:
      "Great work! The design looks modern and clean. Can you add a dark mode variant?",
    userId: "user-002",
    displayName: "John Doe",
    createdAt: new Date("2024-11-21T14:15:00Z"),
    updatedAt: new Date("2024-11-21T14:15:00Z"),
    parentCommentId: "comment-001",
    version: 1,
    deleted: false,
  },
  {
    _id: "comment-003",
    parentId: "project-001",
    parentType: "PROJECT",
    content:
      "Project is progressing well. We're on track to meet the Q2 deadline.",
    userId: "user-002",
    displayName: "John Doe",
    createdAt: new Date("2024-11-25T09:00:00Z"),
    updatedAt: new Date("2024-11-25T09:00:00Z"),
    parentCommentId: null,
    version: 1,
    deleted: false,
  },
  {
    _id: "comment-004",
    parentId: "task-004",
    parentType: "TASK",
    content:
      "After thorough research, I recommend React Native for better performance and team expertise.",
    userId: "user-003",
    displayName: "Jane Smith",
    createdAt: new Date("2024-11-28T16:45:00Z"),
    updatedAt: new Date("2024-11-28T16:45:00Z"),
    parentCommentId: null,
    version: 1,
    deleted: false,
  },
  {
    _id: "comment-005",
    parentId: "task-005",
    parentType: "TASK",
    content:
      "This task is blocked until we get the UX research results from the external agency.",
    userId: "user-003",
    displayName: "Jane Smith",
    createdAt: new Date("2024-11-20T11:20:00Z"),
    updatedAt: new Date("2024-11-20T11:20:00Z"),
    parentCommentId: null,
    version: 1,
    deleted: false,
  },
];

db.comments.insertMany(comments);
print("Sample comments inserted successfully into commentdb!");

// =======================================
// INSERT SAMPLE NOTIFICATIONS (notificationdb)
// =======================================
use("notificationdb");
// =======================================
const notifications = [
  {
    _id: "notification-001",
    recipientUserId: "user-003",
    eventType: "TASK_ASSIGNED",
    message: "You have been assigned to task: Design new homepage layout",
    entityType: "TASK",
    entityId: "task-001",
    channel: "IN_APP",
    createdAt: new Date("2024-03-05T00:00:00Z"),
    isRead: true,
    timestamp: new Date("2024-03-05T00:00:00Z"),
    payload: {
      taskName: "Design new homepage layout",
      projectName: "E-Commerce Platform Redesign",
      assignedBy: "John Doe",
    },
    version: 1,
  },
  {
    _id: "notification-002",
    recipientUserId: "user-004",
    eventType: "TASK_ASSIGNED",
    message: "You have been assigned to task: Implement user authentication",
    entityType: "TASK",
    entityId: "task-002",
    channel: "EMAIL",
    createdAt: new Date("2024-03-10T00:00:00Z"),
    isRead: false,
    timestamp: new Date("2024-03-10T00:00:00Z"),
    payload: {
      taskName: "Implement user authentication",
      projectName: "E-Commerce Platform Redesign",
      assignedBy: "John Doe",
    },
    version: 1,
  },
  {
    _id: "notification-003",
    recipientUserId: "user-002",
    eventType: "TASK_COMPLETED",
    message: "Task completed: Setup CI/CD pipeline",
    entityType: "TASK",
    entityId: "task-003",
    channel: "IN_APP",
    createdAt: new Date("2024-11-15T00:00:00Z"),
    isRead: true,
    timestamp: new Date("2024-11-15T00:00:00Z"),
    payload: {
      taskName: "Setup CI/CD pipeline",
      projectName: "E-Commerce Platform Redesign",
      completedBy: "Mike Wilson",
    },
    version: 1,
  },
  {
    _id: "notification-004",
    recipientUserId: "user-004",
    eventType: "DEADLINE_APPROACHING",
    message:
      "Task deadline approaching: Implement user authentication (Due: Dec 20, 2024)",
    entityType: "TASK",
    entityId: "task-002",
    channel: "PUSH",
    createdAt: new Date("2024-12-01T08:00:00Z"),
    isRead: false,
    timestamp: new Date("2024-12-01T08:00:00Z"),
    payload: {
      taskName: "Implement user authentication",
      dueDate: "2024-12-20T00:00:00Z",
      daysRemaining: 19,
    },
    version: 1,
  },
  {
    _id: "notification-005",
    recipientUserId: "user-002",
    eventType: "COMMENT_ADDED",
    message: "New comment added to task: Design new homepage layout",
    entityType: "TASK",
    entityId: "task-001",
    channel: "IN_APP",
    createdAt: new Date("2024-11-20T10:30:00Z"),
    isRead: true,
    timestamp: new Date("2024-11-20T10:30:00Z"),
    payload: {
      taskName: "Design new homepage layout",
      commentBy: "Jane Smith",
      commentPreview: "I've completed the initial wireframes...",
    },
    version: 1,
  },
];

db.notifications.insertMany(notifications);
print("Sample notifications inserted successfully into notificationdb!");

// =======================================
// VERIFICATION QUERIES
// =======================================
print("\n=== Data Insertion Summary ===");

// Count documents in each database
use("userdb");
const userCount = db.users.countDocuments();

use("projectdb");
const projectCount = db.projects.countDocuments();

use("taskdb");
const taskCount = db.tasks.countDocuments();

use("commentdb");
const commentCount = db.comments.countDocuments();

use("notificationdb");
const notificationCount = db.notifications.countDocuments();

print("Users inserted (userdb): " + userCount);
print("Projects inserted (projectdb): " + projectCount);
print("Tasks inserted (taskdb): " + taskCount);
print("Comments inserted (commentdb): " + commentCount);
print("Notifications inserted (notificationdb): " + notificationCount);

print("\n=== Sample Queries ===");

use("projectdb");
print("Active projects: " + db.projects.countDocuments({ status: "ACTIVE" }));

use("taskdb");
print(
  "Tasks in progress: " + db.tasks.countDocuments({ status: "IN_PROGRESS" })
);

use("notificationdb");
print(
  "Unread notifications: " + db.notifications.countDocuments({ isRead: false })
);

use("userdb");
print("Admin users: " + db.users.countDocuments({ role: "ROLE_ADMIN" }));

print("\n=== Microservices Database Distribution Complete ===");
print("✓ userdb - Contains user data for user-service");
print("✓ projectdb - Contains project data for project-service");
print("✓ taskdb - Contains task data for task-service");
print("✓ commentdb - Contains comment data for comment-service");
print("✓ notificationdb - Contains notification data for notification-service");
print(
  "\nSample data insertion completed successfully with proper microservices isolation!"
);
