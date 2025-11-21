// MongoDB initialization script for UniFi Controller
// This script creates the unifi user with access to all unifi databases

db = db.getSiblingDB('unifi');

// Check if user already exists
var existingUser = db.getUser('unifi');

if (existingUser == null) {
  db.createUser({
    user: 'unifi',
    pwd: 'unifi',
    roles: [
      {
        role: 'dbOwner',
        db: 'unifi'
      },
      {
        role: 'dbOwner',
        db: 'unifi_stat'
      },
      {
        role: 'dbOwner',
        db: 'unifi_audit'
      }
    ]
  });
  print('UniFi user created successfully with dbOwner access to unifi, unifi_stat, and unifi_audit');
} else {
  print('UniFi user already exists, skipping creation');
}
