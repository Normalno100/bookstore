package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookRecommendationService {

    private final ChatModel chatModel;
    private final BookService bookService;

    public BookRecommendationService(ChatModel chatModel, BookService bookService) {
        this.chatModel = chatModel;
        this.bookService = bookService;
    }

    /**
     * Получить объяснение почему эта книга рекомендована (опционально)
     */
    public String getRecommendationExplanation(Book currentBook, Book recommendedBook) {
        try {
            String prompt = String.format(
                    "Пользователь смотрит книгу '%s' (%s, жанр: %s). " +
                            "Объясни в 1-2 предложениях, почему ему может понравиться '%s' (%s, жанр: %s). " +
                            "Будь кратким и конкретным.",
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

    /**
     * Получить AI-рекомендации похожих книг
     */
    public List<Book> getRecommendations(Book currentBook, int limit) {
        try {
            // Получаем все доступные книги (кроме текущей)
            List<Book> availableBooks = bookService.findAll().stream()
                    .filter(book -> !book.getId().equals(currentBook.getId()))
                    .filter(book -> book.getStock() > 0) // Только книги в наличии
                    .collect(Collectors.toList());

            if (availableBooks.isEmpty()) {
                return new ArrayList<>();
            }

            // Формируем промт для LLM
            String prompt = buildRecommendationPrompt(currentBook, availableBooks, limit);

            // Получаем ответ от AI
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();

            // Парсим ID из ответа
            List<Long> recommendedIds = parseBookIds(response);

            // Получаем книги по ID
            return recommendedIds.stream()
                    .map(bookService::findById)
                    .filter(book -> book != null)
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Ошибка при получении рекомендаций: " + e.getMessage());
            // В случае ошибки возвращаем просто случайные книги того же жанра
            return getFallbackRecommendations(currentBook, limit);
        }
    }

    /**
     * Формирование промта для LLM
     */
    private String buildRecommendationPrompt(Book currentBook, List<Book> availableBooks, int limit) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Ты — опытный книжный консультант. Пользователь смотрит книгу:\n\n");
        prompt.append(formatBookInfo(currentBook));
        prompt.append("\n\nВот список доступных книг в магазине:\n\n");

        for (Book book : availableBooks) {
            prompt.append(formatBookInfo(book));
            prompt.append("\n");
        }

        prompt.append("\nНа основе интересов пользователя (жанр, автор, тематика) порекомендуй ");
        prompt.append(limit);
        prompt.append(" наиболее подходящих книги из списка.\n\n");
        prompt.append("Учитывай:\n");
        prompt.append("- Схожесть жанра\n");
        prompt.append("- Похожую тематику и стиль\n");
        prompt.append("- Уровень сложности\n");
        prompt.append("- Популярность сочетаний (например, если читает фэнтези Толкина, может понравиться Мартин)\n\n");
        prompt.append("ВАЖНО: В ответе верни ТОЛЬКО ID книг через запятую, без объяснений.\n");
        prompt.append("Формат ответа: 1,3,5");

        return prompt.toString();
    }

    /**
     * Форматирование информации о книге для промта
     */
    private String formatBookInfo(Book book) {
        return String.format("ID: %d | Название: %s | Автор: %s | Жанр: %s | Описание: %s",
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getDescription().length() > 150
                        ? book.getDescription().substring(0, 150) + "..."
                        : book.getDescription()
        );
    }

    /**
     * Парсинг ID книг из ответа AI
     */
    private List<Long> parseBookIds(String response) {
        List<Long> ids = new ArrayList<>();

        // Убираем все лишнее, оставляем только цифры и запятые
        String cleaned = response.replaceAll("[^0-9,]", "");

        // Разбиваем по запятым
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
     * Запасной вариант рекомендаций (если AI не сработал)
     */
    private List<Book> getFallbackRecommendations(Book currentBook, int limit) {
        // Сначала пытаемся найти книги того же жанра
        List<Book> sameGenre = bookService.findAll().stream()
                .filter(book -> !book.getId().equals(currentBook.getId()))
                .filter(book -> book.getStock() > 0)
                .filter(book -> book.getGenre() != null &&
                        book.getGenre().equalsIgnoreCase(currentBook.getGenre()))
                .limit(limit)
                .collect(Collectors.toList());

        // Если книг того же жанра недостаточно, добавляем любые доступные
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
}