package com.example.bookstore.controller;

import com.example.bookstore.service.BookService;
import com.example.bookstore.model.Book;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("books", q == null || q.isBlank() ? service.findAll() : service.search(q));
        return "books";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Book book = service.findById(id);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        return "book";
    }
}