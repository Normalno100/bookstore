package com.example.bookstore.controller;

import com.example.bookstore.service.BookService;
import com.example.bookstore.service.BookRecommendationService;
import com.example.bookstore.model.Book;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService service;
    private final BookRecommendationService recommendationService;

    public BookController(BookService service, BookRecommendationService recommendationService) {
        this.service = service;
        this.recommendationService = recommendationService;
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

        // Добавляем AI-рекомендации
        try {
            List<Book> recommendations = recommendationService.getRecommendations(book, 4);
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("hasRecommendations", !recommendations.isEmpty());
        } catch (Exception e) {
            System.err.println("Ошибка получения рекомендаций: " + e.getMessage());
            model.addAttribute("hasRecommendations", false);
        }

        return "book";
    }
}