// =================================================================
// FULLY CORRECTED Sample Data Insertion Script
// =================================================================
// This script inserts sample data and correctly establishes all two-way relationships
// between users, projects, tasks, and comments.

print("=== Running 03-sample-data.js ===");
print("Starting sample data insertion...");

// =======================================
//  Step 1: Insert Users
// =======================================
db = db.getSiblingDB("userdb");
db.users.drop(); // Optional: Clean slate for idempotency

const users = [
  {
    _id: ObjectId("685cccd043a91a99a02f669b"),
    username: "admin",
    email: "admin@projectmanagement.com",
    password: "admin123",
    role: "ROLE_ADMIN",
    enabled: true,
    active: true,
    firstName: "Admin",
    lastName: "User",
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f669c"),
    username: "john_doe",
    email: "john.doe@company.com",
    password: "password123",
    role: "ROLE_PROJECT_MANAGER",
    enabled: true,
    active: true,
    firstName: "John",
    lastName: "Doe",
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f669d"),
    username: "jane_smith",
    email: "jane.smith@company.com",
    password: "password123",
    role: "ROLE_DEVELOPER",
    enabled: true,
    active: true,
    firstName: "Jane",
    lastName: "Smith",
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f669e"),
    username: "mike_wilson",
    email: "mike.wilson@company.com",
    password: "password123",
    role: "ROLE_DEVELOPER",
    enabled: true,
    active: true,
    firstName: "Mike",
    lastName: "Wilson",
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f669f"),
    username: "sarah_brown",
    email: "sarah.brown@company.com",
    password: "password123",
    role: "ROLE_USER",
    enabled: true,
    active: true,
    firstName: "Sarah",
    lastName: "Brown",
    version: 0,
  },
];

// We are defining _ids upfront to make referencing easier and deterministic.
// This is a good practice for seeding scripts.
db.users.insertMany(users);

// Create a map from username to ObjectId for easy lookup.
const userIdMap = {};
users.forEach((u) => {
  userIdMap[u.username] = u._id.valueOf();
});
print("✅ Step 1: Sample users inserted successfully.");

// =======================================
//  Step 2: Insert Projects
// =======================================
db = db.getSiblingDB("projectdb");
db.projects.drop(); // Optional: Clean slate

const projects = [
  {
    _id: ObjectId("685cccd043a91a99a02f66a0"),
    name: "E-Commerce Platform Redesign",
    description: "Complete redesign of the company's e-commerce platform.",
    status: "IN_PROGRESS",
    priority: "HIGH",
    ownerId: userIdMap["john_doe"],
    managerIds: [userIdMap["john_doe"]],
    memberIds: [
      userIdMap["jane_smith"],
      userIdMap["mike_wilson"],
      userIdMap["sarah_brown"],
    ],
    createdBy: userIdMap["john_doe"],
    taskIds: [], // Intentionally empty, will be updated later.
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f66a1"),
    name: "Mobile App Development",
    description: "Development of iOS and Android mobile applications.",
    status: "IN_PROGRESS",
    priority: "MEDIUM",
    ownerId: userIdMap["john_doe"],
    managerIds: [userIdMap["john_doe"]],
    memberIds: [userIdMap["jane_smith"], userIdMap["mike_wilson"]],
    createdBy: userIdMap["john_doe"],
    taskIds: [],
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f66a2"),
    name: "Data Analytics Dashboard",
    description: "Implementation of real-time analytics dashboard.",
    status: "COMPLETED",
    priority: "LOW",
    ownerId: userIdMap["john_doe"],
    managerIds: [userIdMap["john_doe"]],
    memberIds: [userIdMap["mike_wilson"], userIdMap["sarah_brown"]],
    createdBy: userIdMap["john_doe"],
    taskIds: [],
    version: 0,
  },
];
db.projects.insertMany(projects);

const projectIdMap = {};
projects.forEach((p) => {
  projectIdMap[p.name] = p._id.valueOf();
});
print("✅ Step 2: Sample projects inserted successfully.");

// =======================================
//  Step 3: Insert Tasks
// =======================================
db = db.getSiblingDB("taskdb");
db.tasks.drop(); // Optional: Clean slate

const tasks = [
  // Tasks for E-Commerce Project
  {
    _id: ObjectId("685cccd043a91a99a02f66a3"),
    projectId: projectIdMap["E-Commerce Platform Redesign"].valueOf(),
    name: "Design new homepage layout",
    status: "IN_PROGRESS",
    priority: "HIGH",
    createdBy: userIdMap["john_doe"],
    assigneeId: userIdMap["jane_smith"],
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f66a4"),
    projectId: projectIdMap["E-Commerce Platform Redesign"].valueOf(),
    name: "Implement user authentication",
    status: "TODO",
    priority: "URGENT",
    createdBy: userIdMap["john_doe"],
    assigneeId: userIdMap["mike_wilson"],
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f66a5"),
    projectId: projectIdMap["E-Commerce Platform Redesign"].valueOf(),
    name: "Setup CI/CD pipeline",
    status: "DONE",
    priority: "MEDIUM",
    createdBy: userIdMap["john_doe"],
    assigneeId: userIdMap["mike_wilson"],
    version: 0,
  },
  // Tasks for Mobile App Project
  {
    _id: ObjectId("685cccd043a91a99a02f66a6"),
    projectId: projectIdMap["Mobile App Development"].valueOf(),
    name: "Research mobile frameworks",
    status: "IN_PROGRESS",
    priority: "HIGH",
    createdBy: userIdMap["john_doe"],
    assigneeId: userIdMap["jane_smith"],
    version: 0,
  },
  {
    _id: ObjectId("685cccd043a91a99a02f66a7"),
    projectId: projectIdMap["Mobile App Development"].valueOf(),
    name: "Create app wireframes",
    status: "BLOCKED",
    priority: "MEDIUM",
    createdBy: userIdMap["john_doe"],
    assigneeId: userIdMap["jane_smith"],
    version: 0,
  },
  // Task for Analytics Project
  {
    _id: ObjectId("685cccd043a91a99a02f66a8"),
    projectId: projectIdMap["Data Analytics Dashboard"].valueOf(),
    name: "Implement dashboard charts",
    status: "DONE",
    priority: "HIGH",
    createdBy: userIdMap["john_doe"],
    assigneeId: userIdMap["mike_wilson"],
    version: 0,
  },
];
db.tasks.insertMany(tasks);

const taskIdMap = {};
tasks.forEach((t) => {
  taskIdMap[t.name] = t._id.valueOf();
});
print("✅ Step 3: Sample tasks inserted successfully.");

// =======================================
//  Step 4: Insert Comments
// =======================================
db = db.getSiblingDB("commentdb");
db.comments.drop(); // Optional: Clean slate

const comments = [
  // Comment on a Task
  {
    _id: ObjectId("685cccd043a91a99a02f66b0"),
    parentId: taskIdMap["Design new homepage layout"].valueOf(),
    parentType: "TASK",
    content: "I've completed the initial wireframes. Please review.",
    userId: userIdMap["jane_smith"],
    version: 0,
  },
  // Reply to the above comment
  {
    _id: ObjectId("685cccd043a91a99a02f66b1"),
    parentId: taskIdMap["Design new homepage layout"].valueOf(),
    parentType: "TASK",
    content: "Great work! Can you add a dark mode variant?",
    userId: userIdMap["john_doe"],
    parentCommentId: "685cccd043a91a99a02f66b0",
    version: 0,
  },
  // Comment directly on a Project
  {
    _id: ObjectId("685cccd043a91a99a02f66b2"),
    parentId: projectIdMap["E-Commerce Platform Redesign"].valueOf(),
    parentType: "PROJECT",
    content: "Project is progressing well.",
    userId: userIdMap["john_doe"],
    version: 0,
  },
];
db.comments.insertMany(comments);
print("✅ Step 4: Sample comments inserted successfully.");

// =======================================
//  Step 5: Link Tasks back to Projects
// =======================================
db = db.getSiblingDB("projectdb");

// For each project, find all tasks that reference it and update its taskIds array.
// This is a more robust method than updating one by one.
for (const projectName in projectIdMap) {
  const projId = projectIdMap[projectName];

  // Find all tasks for the current project
  const tasksForProject = db
    .getSiblingDB("taskdb")
    .tasks.find({ projectId: projId.valueOf() })
    .toArray();
  const taskIdsForProject = tasksForProject.map((task) => task._id.valueOf());

  if (taskIdsForProject.length > 0) {
    db.projects.updateOne(
      { _id: ObjectId(projId) },
      { $set: { taskIds: taskIdsForProject } }
    );
    print(
      `   -> Linked ${taskIdsForProject.length} tasks to project '${projectName}'.`
    );
  }
}
print("✅ Step 5: Project-to-Task links established.");

// =======================================
//  Step 6: Insert Notifications (Now that all entities exist)
// =======================================
db = db.getSiblingDB("notificationdb");
db.notifications.drop(); // Optional: Clean slate

const notifications = [
  {
    eventType: "TASK_ASSIGNED",
    recipientUserId: userIdMap["jane_smith"].valueOf(),
    message: "You were assigned to: Design new homepage layout",
    entityType: "TASK",
    entityId: taskIdMap["Design new homepage layout"].valueOf(),
    version: 0,
  },
  {
    eventType: "TASK_COMPLETED",
    recipientUserId: userIdMap["john_doe"].valueOf(),
    message: "Task completed: Setup CI/CD pipeline",
    entityType: "TASK",
    entityId: taskIdMap["Setup CI/CD pipeline"].valueOf(),
    version: 0,
  },
  {
    eventType: "COMMENT_ADDED",
    recipientUserId: userIdMap["jane_smith"].valueOf(),
    message: "John Doe commented on your project: E-Commerce Platform Redesign",
    entityType: "PROJECT",
    entityId: projectIdMap["E-Commerce Platform Redesign"].valueOf(),
    version: 0,
  },
];
db.notifications.insertMany(notifications);
print("✅ Step 6: Sample notifications inserted successfully.");

// =======================================
//  Final Verification
// =======================================
print("\n=== Data Insertion Summary ===");
const userCount = db.getSiblingDB("userdb").users.countDocuments({});
const projectCount = db.getSiblingDB("projectdb").projects.countDocuments({});
const taskCount = db.getSiblingDB("taskdb").tasks.countDocuments({});
const commentCount = db.getSiblingDB("commentdb").comments.countDocuments({});
const notificationCount = db
  .getSiblingDB("notificationdb")
  .notifications.countDocuments({});

print(`- Users: ${userCount}`);
print(`- Projects: ${projectCount}`);
print(`- Tasks: ${taskCount}`);
print(`- Comments: ${commentCount}`);
print(`- Notifications: ${notificationCount}`);

print("\n🎉 Sample data insertion and linking completed successfully! 🎉");
