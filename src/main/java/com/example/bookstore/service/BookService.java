package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository repo;

    public BookService(BookRepository repo) {
        this.repo = repo;
    }

    public List<Book> findAll() {
        return repo.findAll();
    }

    public Book findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public List<Book> search(String q) {
        return repo.findByTitleContainingIgnoreCase(q);
    }

    public Book save(Book book) {
        return repo.save(book);
    }
}
