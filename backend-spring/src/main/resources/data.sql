-- Demo users for development and testing
-- Password for all users: "password" (BCrypt hashed)
-- Hash generated with BCryptPasswordEncoder.encode("password")

MERGE INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
KEY(id)
VALUES ('user-001', 'employee@fairair.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'John', 'Smith', 'EMPLOYEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
KEY(id)
VALUES ('user-002', 'jane@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Jane', 'Doe', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
KEY(id)
VALUES ('user-003', 'admin@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin', 'User', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
