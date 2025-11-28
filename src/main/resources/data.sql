INSERT INTO book (title, author, genre, description, isbn, price, stock) VALUES
('Clean Code', 'Robert C. Martin', 'Programming', 'A Handbook of Agile Software Craftsmanship', '9780132350884', 29.99, 10),
('The Hobbit', 'J.R.R. Tolkien', 'Fantasy', 'Bilbo Baggins adventures', '9780618968633', 15.00, 5)
ON CONFLICT DO NOTHING;