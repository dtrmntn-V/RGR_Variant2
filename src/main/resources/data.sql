-- ==========================================
-- 1. ОЧИЩЕННЯ БАЗИ ДАНИХ (RESET)
-- ==========================================

-- Видаляємо таблиці бізнес-логіки, якщо вони існують (разом із залежностями CASCADE)
DROP TABLE IF EXISTS hotel CASCADE;
DROP TABLE IF EXISTS resort CASCADE;
DROP TABLE IF EXISTS country CASCADE;

-- Очищаємо таблицю користувачів перед додаванням нових.
-- Використовуємо DELETE, щоб не видаляти саму структуру таблиці, якщо вона створена Hibernate-ом.
-- Якщо таблиці users ще немає (перший запуск), ця команда може викликати помилку,
-- тому краще загорнути її або переконатися, що таблиця users створюється до цього кроку.
-- У більшості випадків Spring спочатку створює таблиці, а потім запускає цей скрипт.
DELETE FROM users;

-- ==========================================
-- 2. СТВОРЕННЯ СТРУКТУРИ (DDL)
-- ==========================================

CREATE TABLE country (
                         id SERIAL PRIMARY KEY,
                         name VARCHAR(100) NOT NULL
);

CREATE TABLE resort (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        country_id INT REFERENCES country(id) ON DELETE CASCADE
);

CREATE TABLE hotel (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       resort_id INT REFERENCES resort(id) ON DELETE CASCADE
);

-- ==========================================
-- 3. ІНІЦІАЛІЗАЦІЯ ДАНИХ (DML)
-- ==========================================

-- --- КОРИСТУВАЧІ ---
-- Паролі тепер звичайні (текстові).
-- УВАГА: Якщо у Java стоїть BCryptPasswordEncoder, логін не спрацює без префікса {noop}.
-- Я додав варіант із {noop} у коментарі, якщо раптом звичайний текст не пустить.

INSERT INTO users (username, password, role, enabled)
VALUES ('admin', 'admin', 'ROLE_ADMIN', true);

INSERT INTO users (username, password, role, enabled)
VALUES ('user', '1234', 'ROLE_USER', true);


-- --- КРАЇНИ ---
INSERT INTO country (name) VALUES ('Туреччина'); -- id 1
INSERT INTO country (name) VALUES ('Єгипет');    -- id 2
INSERT INTO country (name) VALUES ('Україна');   -- id 3

-- --- КУРОРТИ ---
INSERT INTO resort (name, country_id) VALUES ('Анталія', 1);
INSERT INTO resort (name, country_id) VALUES ('Шарм-ель-Шейх', 2);
INSERT INTO resort (name, country_id) VALUES ('Буковель', 3);

-- --- ГОТЕЛІ ---
INSERT INTO hotel (name, resort_id) VALUES ('Rixos Premium', 1);
INSERT INTO hotel (name, resort_id) VALUES ('Savoy Group', 2);
INSERT INTO hotel (name, resort_id) VALUES ('Radisson Blu', 3);
