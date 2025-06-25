// Sample Data Insertion Script for Project Management System (ObjectId Reference Version)
// This script inserts sample data for testing and development purposes
// All references use real MongoDB ObjectIds for schema compliance

// =======================================
// INSERT SAMPLE USERS (userdb)
// =======================================
db = db.getSiblingDB("userdb");

const users = [
  {
    username: "admin",
    email: "admin@projectmanagement.com",
    password: "admin123",
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
    username: "john_doe",
    email: "john.doe@company.com",
    password: "password123",
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
    username: "jane_smith",
    email: "jane.smith@company.com",
    password: "password123",
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
    username: "mike_wilson",
    email: "mike.wilson@company.com",
    password: "password123",
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
    username: "sarah_brown",
    email: "sarah.brown@company.com",
    password: "password123",
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

const userInsertResult = db.users.insertMany(users);
const userIdMap = {};
users.forEach((u, i) => {
  userIdMap[u.username] = userInsertResult.insertedIds[i];
});
print("Sample users inserted successfully into userdb!");

// =======================================
// INSERT SAMPLE PROJECTS (projectdb)
// =======================================
db = db.getSiblingDB("projectdb");
const projects = [
  {
    name: "E-Commerce Platform Redesign",
    description:
      "Complete redesign of the company's e-commerce platform with modern UI/UX and improved performance",
    status: "IN_PROGRESS", // changed from "ACTIVE"
    priority: "HIGH",
    ownerId: String(userIdMap["john_doe"]),
    managerIds: [String(userIdMap["john_doe"])],
    memberIds: [
      String(userIdMap["jane_smith"]),
      String(userIdMap["mike_wilson"]),
      String(userIdMap["sarah_brown"]),
    ],
    startDate: new Date("2024-03-01T00:00:00Z"),
    endDate: new Date("2024-06-30T00:00:00Z"),
    assignedTo: [
      String(userIdMap["jane_smith"]),
      String(userIdMap["mike_wilson"]),
    ],
    taskIds: [], // Will be filled after tasks are inserted
    createdAt: new Date("2024-02-15T00:00:00Z"),
    updatedAt: new Date("2024-11-30T00:00:00Z"),
    createdBy: String(userIdMap["john_doe"]),
    lastModifiedBy: String(userIdMap["john_doe"]),
    version: NumberLong(1),
  },
  {
    name: "Mobile App Development",
    description:
      "Development of iOS and Android mobile applications for customer engagement",
    status: "IN_PROGRESS",
    priority: "MEDIUM",
    ownerId: String(userIdMap["john_doe"]),
    managerIds: [String(userIdMap["john_doe"])],
    memberIds: [
      String(userIdMap["jane_smith"]),
      String(userIdMap["mike_wilson"]),
    ],
    startDate: new Date("2024-04-01T00:00:00Z"),
    endDate: new Date("2024-08-31T00:00:00Z"),
    assignedTo: [String(userIdMap["jane_smith"])],
    taskIds: [],
    createdAt: new Date("2024-03-01T00:00:00Z"),
    updatedAt: new Date("2024-03-01T00:00:00Z"),
    createdBy: String(userIdMap["john_doe"]),
    lastModifiedBy: String(userIdMap["john_doe"]),
    version: NumberLong(1),
  },
  {
    name: "Data Analytics Dashboard",
    description:
      "Implementation of real-time analytics dashboard for business intelligence",
    status: "COMPLETED",
    priority: "LOW",
    ownerId: String(userIdMap["john_doe"]),
    managerIds: [String(userIdMap["john_doe"])],
    memberIds: [
      String(userIdMap["mike_wilson"]),
      String(userIdMap["sarah_brown"]),
    ],
    startDate: new Date("2024-01-01T00:00:00Z"),
    endDate: new Date("2024-02-29T00:00:00Z"),
    assignedTo: [String(userIdMap["mike_wilson"])],
    taskIds: [],
    createdAt: new Date("2023-12-15T00:00:00Z"),
    updatedAt: new Date("2024-02-29T00:00:00Z"),
    createdBy: String(userIdMap["john_doe"]),
    lastModifiedBy: String(userIdMap["mike_wilson"]),
    version: NumberLong(3),
  },
];
const projectInsertResult = db.projects.insertMany(projects);
const projectIdMap = {};
projects.forEach((p, i) => {
  projectIdMap[p.name] = projectInsertResult.insertedIds[i];
});
print("Sample projects inserted successfully into projectdb!");

// =======================================
// INSERT SAMPLE TASKS (taskdb)
// =======================================
db = db.getSiblingDB("taskdb");
const tasks = [
  {
    projectId: String(projectIdMap["E-Commerce Platform Redesign"]),
    name: "Design new homepage layout",
    status: "IN_PROGRESS",
    priority: "HIGH",
    description:
      "Create mockups and wireframes for the new homepage design with responsive layout",
    createdBy: String(userIdMap["john_doe"]),
    updatedBy: String(userIdMap["jane_smith"]),
    createdAt: new Date("2024-03-05T00:00:00Z"),
    updatedAt: new Date("2024-11-25T00:00:00Z"),
    dueDate: new Date("2024-12-15T00:00:00Z"),
    assigneeId: String(userIdMap["jane_smith"]),
    tags: ["design", "ui/ux", "responsive"],
    attachments: [
      {
        id: "att-001",
        filename: "homepage_mockup.figma",
        originalName: "Homepage Mockup.figma",
        contentType: "application/figma",
        size: NumberLong(2048576),
        uploadedBy: String(userIdMap["jane_smith"]),
        uploadedAt: new Date("2024-11-20T00:00:00Z"),
        url: "https://storage.company.com/attachments/homepage_mockup.figma",
      },
    ],
    version: NumberLong(2),
  },
  {
    projectId: String(projectIdMap["E-Commerce Platform Redesign"]),
    name: "Implement user authentication",
    status: "TODO",
    priority: "CRITICAL",
    description:
      "Implement secure user authentication with JWT tokens and password hashing",
    createdBy: String(userIdMap["john_doe"]),
    updatedBy: String(userIdMap["john_doe"]),
    createdAt: new Date("2024-03-10T00:00:00Z"),
    updatedAt: new Date("2024-03-10T00:00:00Z"),
    dueDate: new Date("2024-12-20T00:00:00Z"),
    assigneeId: String(userIdMap["mike_wilson"]),
    tags: ["backend", "security", "authentication"],
    attachments: [],
    version: NumberLong(1),
  },
  {
    projectId: String(projectIdMap["E-Commerce Platform Redesign"]),
    name: "Setup CI/CD pipeline",
    status: "DONE",
    priority: "MEDIUM",
    description:
      "Configure automated testing and deployment pipeline using GitHub Actions",
    createdBy: String(userIdMap["john_doe"]),
    updatedBy: String(userIdMap["mike_wilson"]),
    createdAt: new Date("2024-03-01T00:00:00Z"),
    updatedAt: new Date("2024-11-15T00:00:00Z"),
    dueDate: new Date("2024-11-30T00:00:00Z"),
    assigneeId: String(userIdMap["mike_wilson"]),
    tags: ["devops", "ci/cd", "automation"],
    attachments: [],
    version: NumberLong(3),
  },
  {
    projectId: String(projectIdMap["Mobile App Development"]),
    name: "Research mobile frameworks",
    status: "REVIEW",
    priority: "HIGH",
    description:
      "Evaluate React Native vs Flutter for cross-platform mobile development",
    createdBy: String(userIdMap["john_doe"]),
    updatedBy: String(userIdMap["jane_smith"]),
    createdAt: new Date("2024-03-15T00:00:00Z"),
    updatedAt: new Date("2024-11-28T00:00:00Z"),
    dueDate: new Date("2024-12-10T00:00:00Z"),
    assigneeId: String(userIdMap["jane_smith"]),
    tags: ["research", "mobile", "framework"],
    attachments: [
      {
        id: "att-002",
        filename: "framework_comparison.pdf",
        originalName: "Mobile Framework Comparison.pdf",
        contentType: "application/pdf",
        size: NumberLong(1024768),
        uploadedBy: String(userIdMap["jane_smith"]),
        uploadedAt: new Date("2024-11-28T00:00:00Z"),
        url: "https://storage.company.com/attachments/framework_comparison.pdf",
      },
    ],
    version: NumberLong(2),
  },
  {
    projectId: String(projectIdMap["Mobile App Development"]),
    name: "Create app wireframes",
    status: "BLOCKED",
    priority: "MEDIUM",
    description:
      "Design wireframes for key app screens - waiting for UX research completion",
    createdBy: String(userIdMap["john_doe"]),
    updatedBy: String(userIdMap["jane_smith"]),
    createdAt: new Date("2024-03-20T00:00:00Z"),
    updatedAt: new Date("2024-11-20T00:00:00Z"),
    dueDate: new Date("2024-12-25T00:00:00Z"),
    assigneeId: String(userIdMap["jane_smith"]),
    tags: ["design", "wireframes", "mobile"],
    attachments: [],
    version: NumberLong(1),
  },
  {
    projectId: String(projectIdMap["Data Analytics Dashboard"]),
    name: "Implement dashboard charts",
    status: "DONE",
    priority: "HIGH",
    description:
      "Create interactive charts using Chart.js for the analytics dashboard",
    createdBy: String(userIdMap["john_doe"]),
    updatedBy: String(userIdMap["mike_wilson"]),
    createdAt: new Date("2024-01-15T00:00:00Z"),
    updatedAt: new Date("2024-02-20T00:00:00Z"),
    dueDate: new Date("2024-02-25T00:00:00Z"),
    assigneeId: String(userIdMap["mike_wilson"]),
    tags: ["frontend", "charts", "analytics"],
    attachments: [],
    version: NumberLong(4),
  },
];
const taskInsertResult = db.tasks.insertMany(tasks);
const taskIdMap = {};
tasks.forEach((t, i) => {
  taskIdMap[t.name] = taskInsertResult.insertedIds[i];
});
print("Sample tasks inserted successfully into taskdb!");

// Update project taskIds
// (This step is necessary because task ObjectIds are only known after insertion)
db = db.getSiblingDB("projectdb");
projects[0].taskIds = [
  String(taskIdMap["Design new homepage layout"]),
  String(taskIdMap["Implement user authentication"]),
  String(taskIdMap["Setup CI/CD pipeline"]),
];
projects[1].taskIds = [
  String(taskIdMap["Research mobile frameworks"]),
  String(taskIdMap["Create app wireframes"]),
];
projects[2].taskIds = [String(taskIdMap["Implement dashboard charts"])];

// Update in DB
db.projects.updateOne(
  { _id: projectIdMap["E-Commerce Platform Redesign"] },
  { $set: { taskIds: projects[0].taskIds } }
);
db.projects.updateOne(
  { _id: projectIdMap["Mobile App Development"] },
  { $set: { taskIds: projects[1].taskIds } }
);
db.projects.updateOne(
  { _id: projectIdMap["Data Analytics Dashboard"] },
  { $set: { taskIds: projects[2].taskIds } }
);

// =======================================
// INSERT SAMPLE COMMENTS (commentdb)
// =======================================
db = db.getSiblingDB("commentdb");
const comments = [
  {
    parentId: String(taskIdMap["Design new homepage layout"]),
    parentType: "TASK",
    content:
      "I've completed the initial wireframes. Please review and provide feedback.",
    userId: String(userIdMap["jane_smith"]),
    displayName: "Jane Smith",
    createdAt: new Date("2024-11-20T10:30:00Z"),
    updatedAt: new Date("2024-11-20T10:30:00Z"),
    version: NumberLong(1),
    deleted: false,
  },
  {
    parentId: String(taskIdMap["Design new homepage layout"]),
    parentType: "TASK",
    content:
      "Great work! The design looks modern and clean. Can you add a dark mode variant?",
    userId: String(userIdMap["john_doe"]),
    displayName: "John Doe",
    createdAt: new Date("2024-11-21T14:15:00Z"),
    updatedAt: new Date("2024-11-21T14:15:00Z"),
    // parentCommentId will be set after insert
    version: NumberLong(1),
    deleted: false,
  },
  {
    parentId: String(projectIdMap["E-Commerce Platform Redesign"]),
    parentType: "PROJECT",
    content:
      "Project is progressing well. We're on track to meet the Q2 deadline.",
    userId: String(userIdMap["john_doe"]),
    displayName: "John Doe",
    createdAt: new Date("2024-11-25T09:00:00Z"),
    updatedAt: new Date("2024-11-25T09:00:00Z"),
    version: NumberLong(1),
    deleted: false,
  },
  {
    parentId: String(taskIdMap["Research mobile frameworks"]),
    parentType: "TASK",
    content:
      "After thorough research, I recommend React Native for better performance and team expertise.",
    userId: String(userIdMap["jane_smith"]),
    displayName: "Jane Smith",
    createdAt: new Date("2024-11-28T16:45:00Z"),
    updatedAt: new Date("2024-11-28T16:45:00Z"),
    version: NumberLong(1),
    deleted: false,
  },
  {
    parentId: String(taskIdMap["Create app wireframes"]),
    parentType: "TASK",
    content:
      "This task is blocked until we get the UX research results from the external agency.",
    userId: String(userIdMap["jane_smith"]),
    displayName: "Jane Smith",
    createdAt: new Date("2024-11-20T11:20:00Z"),
    updatedAt: new Date("2024-11-20T11:20:00Z"),
    version: NumberLong(1),
    deleted: false,
  },
];
const commentInsertResult = db.comments.insertMany(comments);
const commentIdMap = {};
comments.forEach((c, i) => {
  commentIdMap[c.content] = commentInsertResult.insertedIds[i];
});
// Update parentCommentId for reply comment
const replyCommentContent =
  "Great work! The design looks modern and clean. Can you add a dark mode variant?";
db.comments.updateOne(
  { _id: commentIdMap[replyCommentContent] },
  {
    $set: {
      parentCommentId: String(
        commentIdMap[
          "I've completed the initial wireframes. Please review and provide feedback."
        ]
      ),
    },
  }
);
print("Sample comments inserted successfully into commentdb!");

// =======================================
// INSERT SAMPLE NOTIFICATIONS (notificationdb)
// =======================================
db = db.getSiblingDB("notificationdb");
const notifications = [
  {
    recipientUserId: String(userIdMap["jane_smith"]),
    eventType: "TASK_ASSIGNED",
    message: "You have been assigned to task: Design new homepage layout",
    entityType: "TASK",
    entityId: String(taskIdMap["Design new homepage layout"]),
    channel: "IN_APP",
    createdAt: new Date("2024-03-05T00:00:00Z"),
    isRead: true,
    timestamp: new Date("2024-03-05T00:00:00Z"),
    payload: {
      taskName: "Design new homepage layout",
      projectName: "E-Commerce Platform Redesign",
      assignedBy: "John Doe",
    },
    version: NumberLong(1),
  },
  {
    recipientUserId: String(userIdMap["mike_wilson"]),
    eventType: "TASK_ASSIGNED",
    message: "You have been assigned to task: Implement user authentication",
    entityType: "TASK",
    entityId: String(taskIdMap["Implement user authentication"]),
    channel: "EMAIL",
    createdAt: new Date("2024-03-10T00:00:00Z"),
    isRead: false,
    timestamp: new Date("2024-03-10T00:00:00Z"),
    payload: {
      taskName: "Implement user authentication",
      projectName: "E-Commerce Platform Redesign",
      assignedBy: "John Doe",
    },
    version: NumberLong(1),
  },
  {
    recipientUserId: String(userIdMap["john_doe"]),
    eventType: "TASK_COMPLETED",
    message: "Task completed: Setup CI/CD pipeline",
    entityType: "TASK",
    entityId: String(taskIdMap["Setup CI/CD pipeline"]),
    channel: "IN_APP",
    createdAt: new Date("2024-11-15T00:00:00Z"),
    isRead: true,
    timestamp: new Date("2024-11-15T00:00:00Z"),
    payload: {
      taskName: "Setup CI/CD pipeline",
      projectName: "E-Commerce Platform Redesign",
      completedBy: "Mike Wilson",
    },
    version: NumberLong(1),
  },
  {
    recipientUserId: String(userIdMap["mike_wilson"]),
    eventType: "DEADLINE_APPROACHING",
    message:
      "Task deadline approaching: Implement user authentication (Due: Dec 20, 2024)",
    entityType: "TASK",
    entityId: String(taskIdMap["Implement user authentication"]),
    channel: "PUSH",
    createdAt: new Date("2024-12-01T08:00:00Z"),
    isRead: false,
    timestamp: new Date("2024-12-01T08:00:00Z"),
    payload: {
      taskName: "Implement user authentication",
      dueDate: "2024-12-20T00:00:00Z",
      daysRemaining: 19,
    },
    version: NumberLong(1),
  },
  {
    recipientUserId: String(userIdMap["john_doe"]),
    eventType: "COMMENT_ADDED",
    message: "New comment added to task: Design new homepage layout",
    entityType: "TASK",
    entityId: String(taskIdMap["Design new homepage layout"]),
    channel: "IN_APP",
    createdAt: new Date("2024-11-20T10:30:00Z"),
    isRead: true,
    timestamp: new Date("2024-11-20T10:30:00Z"),
    payload: {
      taskName: "Design new homepage layout",
      commentBy: "Jane Smith",
      commentPreview: "I've completed the initial wireframes...",
    },
    version: NumberLong(1),
  },
];
db.notifications.insertMany(notifications);
print("Sample notifications inserted successfully into notificationdb!");

print("\n=== Data Insertion Summary ===");
// Count documents in each database
db = db.getSiblingDB("userdb");
const userCount = db.users.count();
db = db.getSiblingDB("projectdb");
const projectCount = db.projects.count();
db = db.getSiblingDB("taskdb");
const taskCount = db.tasks.count();
db = db.getSiblingDB("commentdb");
const commentCount = db.comments.count();
db = db.getSiblingDB("notificationdb");
const notificationCount = db.notifications.count();
print("Users inserted (userdb): " + userCount);
print("Projects inserted (projectdb): " + projectCount);
print("Tasks inserted (taskdb): " + taskCount);
print("Comments inserted (commentdb): " + commentCount);
print("Notifications inserted (notificationdb): " + notificationCount);

print("\n=== Microservices Database Distribution Complete ===");
print("✓ userdb - Contains user data for user-service");
print("✓ projectdb - Contains project data for project-service");
print("✓ taskdb - Contains task data for task-service");
print("✓ commentdb - Contains comment data for comment-service");
print("✓ notificationdb - Contains notification data for notification-service");
print(
  "\nSample data insertion completed successfully with proper microservices isolation!"
);
