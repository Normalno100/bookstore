package com.example.bookstore.service;

import com.example.bookstore.dto.SearchCriteria;
import com.example.bookstore.model.Book;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для AI-поиска книг на естественном языке
 */
@Service
public class AISearchService {

    private final ChatModel chatModel;
    private final BookService bookService;
    private final ObjectMapper objectMapper;

    public AISearchService(ChatModel chatModel, BookService bookService) {
        this.chatModel = chatModel;
        this.bookService = bookService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Поиск книг по запросу на естественном языке
     */
    public List<Book> searchByNaturalLanguage(String query) {
        try {
            // Шаг 1: Извлекаем структурированные критерии из запроса
            SearchCriteria criteria = extractSearchCriteria(query);

            // Шаг 2: Ищем книги по критериям
            List<Book> allBooks = bookService.findAll();
            List<Book> filteredBooks = filterBooks(allBooks, criteria);

            // Шаг 3: Если найдено слишком мало или слишком много, уточняем
            if (filteredBooks.isEmpty()) {
                // Попробуем более мягкий поиск
                criteria = relaxCriteria(criteria);
                filteredBooks = filterBooks(allBooks, criteria);
            }

            return filteredBooks;

        } catch (Exception e) {
            System.err.println("Ошибка AI-поиска: " + e.getMessage());
            // Fallback на обычный поиск
            return bookService.search(query);
        }
    }

    /**
     * Извлекает структурированные критерии поиска из запроса
     */
    private SearchCriteria extractSearchCriteria(String query) {
        String prompt = buildExtractionPrompt(query);

        try {
            String response = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            // Парсим JSON из ответа
            return parseSearchCriteria(response, query);

        } catch (Exception e) {
            System.err.println("Ошибка извлечения критериев: " + e.getMessage());
            return SearchCriteria.builder()
                    .originalQuery(query)
                    .keywords(Arrays.asList(query.toLowerCase().split("\\s+")))
                    .confidence(0.3)
                    .build();
        }
    }

    /**
     * Создает промпт для извлечения критериев
     */
    private String buildExtractionPrompt(String query) {
        return String.format("""
            Ты — эксперт по книгам. Проанализируй поисковый запрос пользователя и извлеки структурированные параметры.
            
            Запрос пользователя: "%s"
            
            Извлеки следующие параметры (если применимо):
            - genre: жанр книги (fantasy, detective, romance, sci-fi, thriller, horror, non-fiction, biography, etc.)
            - targetAudience: целевая аудитория (children, teen, young-adult, adult)
            - keywords: массив ключевых слов на английском (magic, adventure, war, love, friendship, mystery, etc.)
            - author: автор (если упомянут)
            - mood: настроение (dark, funny, sad, inspirational, scary, romantic)
            - theme: основная тема (если явно выражена)
            
            ВАЖНО: Верни ТОЛЬКО валидный JSON без каких-либо пояснений.
            Используй null для параметров, которые не указаны в запросе.
            Все текстовые поля на английском, кроме author.
            
            Пример формата ответа:
            {
              "genre": "fantasy",
              "targetAudience": "teen",
              "keywords": ["magic", "school", "adventure"],
              "author": null,
              "mood": null,
              "theme": null,
              "confidence": 0.9
            }
            
            JSON:
            """, query);
    }

    /**
     * Парсит JSON с критериями из ответа LLM
     */
    private SearchCriteria parseSearchCriteria(String jsonResponse, String originalQuery) {
        try {
            // Очищаем от markdown и лишних символов
            String cleanJson = jsonResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            SearchCriteria criteria = objectMapper.readValue(cleanJson, SearchCriteria.class);
            criteria.setOriginalQuery(originalQuery);

            return criteria;

        } catch (Exception e) {
            System.err.println("Ошибка парсинга JSON: " + e.getMessage());

            // Fallback: простое извлечение ключевых слов
            return SearchCriteria.builder()
                    .originalQuery(originalQuery)
                    .keywords(Arrays.asList(originalQuery.toLowerCase().split("\\s+")))
                    .confidence(0.3)
                    .build();
        }
    }

    /**
     * Фильтрует книги по критериям
     */
    private List<Book> filterBooks(List<Book> books, SearchCriteria criteria) {
        return books.stream()
                .filter(book -> matchesCriteria(book, criteria))
                .collect(Collectors.toList());
    }

    /**
     * Проверяет соответствие книги критериям
     */
    private boolean matchesCriteria(Book book, SearchCriteria criteria) {
        int matchScore = 0;
        int totalCriteria = 0;

        // Проверка жанра (строгое соответствие)
        if (criteria.getGenre() != null) {
            totalCriteria++;
            if (book.getGenre() != null &&
                    book.getGenre().toLowerCase().contains(criteria.getGenre().toLowerCase())) {
                matchScore++;
            }
        }

        // Проверка автора (строгое соответствие)
        if (criteria.getAuthor() != null) {
            totalCriteria++;
            if (book.getAuthor() != null &&
                    book.getAuthor().toLowerCase().contains(criteria.getAuthor().toLowerCase())) {
                matchScore++;
            }
        }

        // Проверка ключевых слов (мягкое соответствие)
        if (criteria.getKeywords() != null && !criteria.getKeywords().isEmpty()) {
            totalCriteria++;
            String bookText = (book.getTitle() + " " + book.getDescription() + " " + book.getGenre())
                    .toLowerCase();

            long matchedKeywords = criteria.getKeywords().stream()
                    .filter(keyword -> bookText.contains(keyword.toLowerCase()))
                    .count();

            // Требуем хотя бы 30% совпадения ключевых слов
            if (matchedKeywords >= criteria.getKeywords().size() * 0.3) {
                matchScore++;
            }
        }

        // Если нет критериев, пропускаем книгу
        if (totalCriteria == 0) {
            return false;
        }

        // Требуем хотя бы 50% совпадения критериев
        return (double) matchScore / totalCriteria >= 0.5;
    }

    /**
     * Смягчает критерии для расширенного поиска
     */
    private SearchCriteria relaxCriteria(SearchCriteria criteria) {
        // Убираем строгие фильтры, оставляя только ключевые слова
        return SearchCriteria.builder()
                .keywords(criteria.getKeywords())
                .originalQuery(criteria.getOriginalQuery())
                .confidence(criteria.getConfidence() * 0.7)
                .build();
    }

    /**
     * Получает объяснение почему книга подошла под запрос
     */
    public String explainMatch(String query, Book book) {
        try {
            String prompt = String.format("""
                Пользователь искал: "%s"
                
                Найдена книга:
                - Название: %s
                - Автор: %s
                - Жанр: %s
                - Описание: %s
                
                Объясни в 1-2 предложениях, почему эта книга соответствует запросу пользователя.
                Будь конкретным и кратким.
                """,
                    query, book.getTitle(), book.getAuthor(), book.getGenre(),
                    book.getDescription().length() > 200
                            ? book.getDescription().substring(0, 200) + "..."
                            : book.getDescription()
            );

            return chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

        } catch (Exception e) {
            return "Книга соответствует вашему запросу по жанру и тематике.";
        }
    }
}