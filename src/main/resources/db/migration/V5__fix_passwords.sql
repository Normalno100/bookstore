-- Миграция для исправления паролей существующих пользователей
-- Эта миграция применится автоматически при следующем запуске

-- Исправляем пароль для testuser
-- Логин: testuser, Новый пароль: password
UPDATE users
SET password = '$2a$10$a7ETJjonBOHZiFZuqOkMyOrZLPaVTS2sfJI4ATxqcRG9c6UEJ8acK'
WHERE username = 'testuser';

-- Исправляем пароль для admin (если существует)
-- Логин: admin, Новый пароль: admin
UPDATE users
SET password = '$2a$10$9enE.SdUNbSv/6b91yjXJODjVdARWEn786yUbAP2qYR2aAfNvN0hK'
WHERE username = 'admin';

-- Проверяем, что у admin есть обе роли
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users WHERE username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;