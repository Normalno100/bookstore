CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    enabled BOOLEAN DEFAULT true
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Создание тестового пользователя (пароль: password)
INSERT INTO users (username, password, email, enabled)
VALUES ('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMye9nC5P3F/LnT.y8TfF/gJyMKVLrwJL0S', 'test@example.com', true);

INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE username = 'testuser'), 'USER');