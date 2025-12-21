-- Добавление колонки для хранения пути к изображению
ALTER TABLE book ADD COLUMN image_path VARCHAR(500);

-- Добавление тестового администратора (пароль: admin)
INSERT INTO users (username, password, email, enabled)
VALUES ('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@bookstore.com', true);

INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE username = 'admin'), 'ADMIN');

INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE username = 'admin'), 'USER');