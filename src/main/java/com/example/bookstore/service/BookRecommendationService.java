package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookRecommendationService {

    private final ChatModel chatModel;
    private final BookService bookService;
    private final VectorSearchService vectorSearchService;

    public BookRecommendationService(ChatModel chatModel,
                                     BookService bookService,
                                     VectorSearchService vectorSearchService) {
        this.chatModel = chatModel;
        this.bookService = bookService;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Получить AI-рекомендации похожих книг
     * Теперь использует векторный поиск как основу
     */
    public List<Book> getRecommendations(Book currentBook, int limit) {
        try {
            // Шаг 1: Векторный поиск похожих книг (быстро и точно)
            List<Book> vectorResults = vectorSearchService.findSimilarBooks(currentBook, limit * 2);

            if (vectorResults.isEmpty()) {
                // Fallback на старый метод
                return getFallbackRecommendations(currentBook, limit);
            }

            // Шаг 2: Используем AI для ранжирования и фильтрации
            List<Book> refinedResults = refineRecommendationsWithAI(
                    currentBook,
                    vectorResults,
                    limit
            );

            return refinedResults;

        } catch (Exception e) {
            System.err.println("Ошибка при получении рекомендаций: " + e.getMessage());
            return getFallbackRecommendations(currentBook, limit);
        }
    }

    /**
     * Уточняет рекомендации с помощью AI
     */
    private List<Book> refineRecommendationsWithAI(Book currentBook, List<Book> candidates, int limit) {
        try {
            // Формируем промпт для AI
            String prompt = String.format("""
                Пользователь смотрит книгу:
                Название: %s
                Автор: %s
                Жанр: %s
                Описание: %s
                
                Из этих похожих книг выбери %d ЛУЧШИХ для рекомендации:
                
                %s
                
                Верни ТОЛЬКО ID лучших книг через запятую (в порядке релевантности).
                Формат: 1,3,5,7
                """,
                    currentBook.getTitle(),
                    currentBook.getAuthor(),
                    currentBook.getGenre(),
                    currentBook.getDescription().substring(0, Math.min(200, currentBook.getDescription().length())),
                    limit,
                    formatCandidates(candidates)
            );

            String response = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            // Парсим ID из ответа
            List<Long> selectedIds = parseBookIds(response);

            // Возвращаем книги в порядке, предложенном AI
            List<Book> result = new ArrayList<>();
            for (Long id : selectedIds) {
                candidates.stream()
                        .filter(b -> b.getId().equals(id))
                        .findFirst()
                        .ifPresent(result::add);
            }

            // Если AI вернул меньше книг, добавляем из кандидатов
            if (result.size() < limit) {
                candidates.stream()
                        .filter(b -> !result.contains(b))
                        .limit(limit - result.size())
                        .forEach(result::add);
            }

            return result.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Ошибка AI-уточнения: " + e.getMessage());
            // При ошибке возвращаем первые N кандидатов
            return candidates.stream().limit(limit).collect(Collectors.toList());
        }
    }

    /**
     * Форматирует кандидатов для промпта
     */
    private String formatCandidates(List<Book> candidates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Book book = candidates.get(i);
            sb.append(String.format(
                    "%d. ID: %d | %s (%s) - %s\n",
                    i + 1,
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getGenre()
            ));
        }
        return sb.toString();
    }

    /**
     * Парсит ID из ответа AI
     */
    private List<Long> parseBookIds(String response) {
        List<Long> ids = new ArrayList<>();
        String cleaned = response.replaceAll("[^0-9,]", "");
        String[] parts = cleaned.split(",");

        for (String part : parts) {
            try {
                if (!part.trim().isEmpty()) {
                    ids.add(Long.parseLong(part.trim()));
                }
            } catch (NumberFormatException e) {
                // Игнорируем неправильные значения
            }
        }

        return ids;
    }

    /**
     * Запасной вариант рекомендаций
     */
    private List<Book> getFallbackRecommendations(Book currentBook, int limit) {
        List<Book> sameGenre = bookService.findAll().stream()
                .filter(book -> !book.getId().equals(currentBook.getId()))
                .filter(book -> book.getStock() > 0)
                .filter(book -> book.getGenre() != null &&
                        book.getGenre().equalsIgnoreCase(currentBook.getGenre()))
                .limit(limit)
                .collect(Collectors.toList());

        if (sameGenre.size() < limit) {
            List<Book> additional = bookService.findAll().stream()
                    .filter(book -> !book.getId().equals(currentBook.getId()))
                    .filter(book -> book.getStock() > 0)
                    .filter(book -> !sameGenre.contains(book))
                    .limit(limit - sameGenre.size())
                    .collect(Collectors.toList());

            sameGenre.addAll(additional);
        }

        return sameGenre;
    }

    /**
     * Получить объяснение рекомендации
     */
    public String getRecommendationExplanation(Book currentBook, Book recommendedBook) {
        try {
            String prompt = String.format("""
                Пользователь смотрит книгу '%s' (%s, жанр: %s).
                Объясни в 1-2 предложениях, почему ему может понравиться '%s' (%s, жанр: %s).
                Будь кратким и конкретным.
                """,
                    currentBook.getTitle(), currentBook.getAuthor(), currentBook.getGenre(),
                    recommendedBook.getTitle(), recommendedBook.getAuthor(), recommendedBook.getGenre()
            );

            return chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (Exception e) {
            return "Похожий жанр и стиль";
        }
    }
}