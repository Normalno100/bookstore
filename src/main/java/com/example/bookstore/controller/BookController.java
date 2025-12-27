package com.example.bookstore.controller;

import com.example.bookstore.service.BookService;
import com.example.bookstore.service.BookRecommendationService;
import com.example.bookstore.service.AISearchService;
import com.example.bookstore.model.Book;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService service;
    private final BookRecommendationService recommendationService;
    private final AISearchService aiSearchService;

    public BookController(BookService service,
                          BookRecommendationService recommendationService,
                          AISearchService aiSearchService) {
        this.service = service;
        this.recommendationService = recommendationService;
        this.aiSearchService = aiSearchService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mode,
            Model model
    ) {
        List<Book> books;
        boolean isAISearch = "ai".equals(mode) && q != null && !q.isBlank();

        if (isAISearch) {
            // AI-поиск
            books = aiSearchService.searchByNaturalLanguage(q);
            model.addAttribute("searchMode", "ai");
            model.addAttribute("searchQuery", q);

            // Добавляем объяснения для каждой найденной книги
            if (!books.isEmpty()) {
                Map<Long, String> explanations = new HashMap<>();
                for (Book book : books) {
                    String explanation = aiSearchService.explainMatch(q, book);
                    explanations.put(book.getId(), explanation);
                }
                model.addAttribute("explanations", explanations);
            }
        } else if (q != null && !q.isBlank()) {
            // Обычный поиск
            books = service.search(q);
            model.addAttribute("searchMode", "standard");
            model.addAttribute("searchQuery", q);
        } else {
            // Показываем весь каталог
            books = service.findAll();
            model.addAttribute("searchMode", "none");
        }

        model.addAttribute("books", books);
        model.addAttribute("resultCount", books.size());

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

    /**
     * API endpoint для проверки AI-поиска (опционально)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public Map<String, Object> apiSearch(@RequestParam String q) {
        List<Book> books = aiSearchService.searchByNaturalLanguage(q);

        Map<String, Object> response = new HashMap<>();
        response.put("query", q);
        response.put("count", books.size());
        response.put("books", books);

        return response;
    }
}