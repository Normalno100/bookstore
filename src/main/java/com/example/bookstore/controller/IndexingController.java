package com.example.bookstore.controller;

import com.example.bookstore.service.VectorSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления индексацией книг
 * Доступен только администраторам
 */
@Controller
@RequestMapping("/admin/indexing")
public class IndexingController {

    private final VectorSearchService vectorSearchService;

    public IndexingController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Страница управления индексацией
     */
    @GetMapping
    public String indexingPage(Model model) {
        String stats = vectorSearchService.getIndexingStats();
        model.addAttribute("stats", stats);
        return "admin/indexing";
    }

    /**
     * API: Запустить индексацию новых книг
     */
    @PostMapping("/api/index")
    @ResponseBody
    public ResponseEntity<Map<String, String>> indexBooks() {
        try {
            // Запускаем индексацию в отдельном потоке
            new Thread(() -> {
                try {
                    vectorSearchService.indexAllBooks();
                } catch (Exception e) {
                    System.err.println("Ошибка индексации: " + e.getMessage());
                }
            }).start();

            Map<String, String> response = new HashMap<>();
            response.put("status", "started");
            response.put("message", "Индексация запущена в фоновом режиме");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * API: Переиндексировать все книги
     */
    @PostMapping("/api/reindex")
    @ResponseBody
    public ResponseEntity<Map<String, String>> reindexAllBooks() {
        try {
            new Thread(() -> {
                try {
                    vectorSearchService.reindexAllBooks();
                } catch (Exception e) {
                    System.err.println("Ошибка переиндексации: " + e.getMessage());
                }
            }).start();

            Map<String, String> response = new HashMap<>();
            response.put("status", "started");
            response.put("message", "Переиндексация запущена в фоновом режиме");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * API: Получить статистику индексации
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getStats() {
        Map<String, String> stats = new HashMap<>();
        stats.put("stats", vectorSearchService.getIndexingStats());
        return ResponseEntity.ok(stats);
    }
}