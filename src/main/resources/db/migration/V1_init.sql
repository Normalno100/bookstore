CREATE TABLE book (
id bigserial PRIMARY KEY,
title varchar(255),
author varchar(255),
genre varchar(255),
description text,
isbn varchar(50),
price numeric(10,2),
stock integer
);


INSERT INTO book (title, author, genre, description, isbn, price, stock) VALUES
('Clean Code', 'Robert C. Martin', 'Programming', 'A Handbook of Agile Software Craftsmanship', '9780132350884', 29.99, 10),
('The Hobbit', 'J.R.R. Tolkien', 'Fantasy', 'Bilbo Baggins adventures', '9780618968633', 15.00, 5);