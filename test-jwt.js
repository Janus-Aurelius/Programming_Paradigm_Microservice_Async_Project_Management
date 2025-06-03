const jwt = require('jsonwebtoken');

// Simulate the JWT token creation that your backend does
const createJwtToken = (userId, email, role) => {
  const payload = {
    sub: userId,
    email: email,
    role: role,  // Single role as string
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60) // 24 hours
  };
  
  return jwt.sign(payload, 'your-secret-key');
};

// Create a sample token
const token = createJwtToken('123', 'admin@example.com', 'ROLE_ADMIN');
console.log('Generated Token:', token);

// Decode the token to see what's inside
const decoded = jwt.decode(token);
console.log('\nDecoded Token Payload:');
console.log(JSON.stringify(decoded, null, 2));

// Test what your frontend expects vs what backend provides
console.log('\n=== FRONTEND vs BACKEND ANALYSIS ===');
console.log('Frontend expects:');
console.log('- decoded.roles (array)');
console.log('- decoded.permissions (array)');
console.log('- response.user.permissions (array)');

console.log('\nBackend provides:');
console.log('- decoded.role (string):', decoded.role);
console.log('- decoded.roles (array):', decoded.roles || 'UNDEFINED');
console.log('- decoded.permissions (array):', decoded.permissions || 'UNDEFINED');

// Simulate what your UPDATED frontend's extractPermissionsFromToken will do
console.log('\n=== UPDATED FRONTEND TOKEN EXTRACTION SIMULATION ===');
try {
  const payload = token.split('.')[1];
  const decodedForFrontend = JSON.parse(Buffer.from(payload, 'base64').toString());
  
  console.log('Frontend decoded payload:');
  console.log(JSON.stringify(decodedForFrontend, null, 2));
  
  // This is what your UPDATED frontend does
  const tokenPermissions = decodedForFrontend.permissions || [];
  
  // Handle both single role (backend format) and roles array (if upgraded later)
  let roles = [];
  if (decodedForFrontend.roles && Array.isArray(decodedForFrontend.roles)) {
    // New format: roles array
    roles = decodedForFrontend.roles;
  } else if (decodedForFrontend.role && typeof decodedForFrontend.role === 'string') {
    // Current backend format: single role string
    roles = [decodedForFrontend.role];
  }
  
  const rolePermissions = extractPermissionsFromRoles(roles);
  
  console.log('\nUpdated frontend extraction results:');
  console.log('- tokenPermissions:', tokenPermissions);
  console.log('- detected roles:', roles);
  console.log('- rolePermissions:', rolePermissions);
  console.log('- Final permissions:', [...new Set([...tokenPermissions, ...rolePermissions])]);
  
} catch (error) {
  console.error('Frontend extraction error:', error.message);
}

// Simulate the role-based permission extraction from your frontend
function extractPermissionsFromRoles(roles) {
  const rolePermissions = {
    'ROLE_ADMIN': [
      'USER_READ', 'USER_UPDATE', 'USER_DELETE', 'USER_CREATE',
      'PRJ_READ', 'PRJ_UPDATE', 'PRJ_DELETE', 'PRJ_CREATE', 'PRJ_MANAGE_MEMBERS',
      'TASK_READ', 'TASK_UPDATE', 'TASK_DELETE', 'TASK_CREATE', 'TASK_ASSIGN',
      'CMT_READ', 'CMT_UPDATE', 'CMT_DELETE', 'CMT_CREATE',
      'NOTI_READ', 'NOTI_UPDATE', 'NOTI_DELETE', 'NOTI_CREATE'
    ],
    'ROLE_PROJECT_MANAGER': [
      'USER_READ', 'PRJ_READ', 'PRJ_UPDATE', 'PRJ_CREATE', 'PRJ_MANAGE_MEMBERS',
      'TASK_READ', 'TASK_UPDATE', 'TASK_CREATE', 'TASK_ASSIGN',
      'CMT_READ', 'CMT_UPDATE', 'CMT_CREATE',
      'NOTI_READ', 'NOTI_CREATE'
    ],
    'ROLE_DEVELOPER': [
      'USER_READ', 'PRJ_READ', 'TASK_READ', 'TASK_UPDATE', 'TASK_CREATE',
      'CMT_READ', 'CMT_UPDATE', 'CMT_CREATE', 'NOTI_READ'
    ],
    'ROLE_USER': [
      'USER_READ', 'PRJ_READ', 'TASK_READ', 'CMT_READ', 'NOTI_READ'
    ]
  };

  const allPermissions = new Set();
  roles.forEach(role => {
    if (rolePermissions[role]) {
      rolePermissions[role].forEach(permission => allPermissions.add(permission));
    }
  });
  
  return Array.from(allPermissions);
}
