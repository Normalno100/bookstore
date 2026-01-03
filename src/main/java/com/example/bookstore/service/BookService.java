package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {
    private final BookRepository repo;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;

    public BookService(BookRepository repo,
                       EmbeddingService embeddingService,
                       VectorSearchService vectorSearchService) {
        this.repo = repo;
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
    }

    public List<Book> findAll() {
        return repo.findAll();
    }

    public Book findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    /**
     * Умный поиск: автоматически выбирает между текстовым и семантическим
     */
    public List<Book> search(String q) {
        if (q == null || q.isBlank()) {
            return findAll();
        }

        // Пробуем семантический поиск
        List<Book> semanticResults = vectorSearchService.semanticSearch(q, 10);

        // Если найдено мало результатов, добавляем текстовый поиск
        if (semanticResults.size() < 5) {
            List<Book> textResults = repo.findByTitleContainingIgnoreCase(q);

            // Объединяем результаты, избегая дубликатов
            for (Book book : textResults) {
                if (!semanticResults.contains(book) && semanticResults.size() < 10) {
                    semanticResults.add(book);
                }
            }
        }

        return semanticResults;
    }

    /**
     * Гибридный поиск (лучший из миров)
     */
    public List<Book> hybridSearch(String query) {
        return vectorSearchService.hybridSearch(query, 20);
    }

    /**
     * Семантический поиск с фильтром
     */
    public List<Book> semanticSearchInStock(String query) {
        return vectorSearchService.semanticSearchInStock(query, 10);
    }

    @Transactional
    public Book save(Book book) {
        // Генерируем эмбеддинг при сохранении
        if (book.getEmbedding() == null || shouldRegenerateEmbedding(book)) {
            embeddingService.generateEmbeddingForBook(book);
        }
        return repo.save(book);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /**
     * Проверяет, нужно ли пересоздать эмбеддинг
     * (например, если изменилось описание)
     */
    private boolean shouldRegenerateEmbedding(Book book) {
        if (book.getId() == null) {
            return true; // Новая книга
        }

        Book existing = repo.findById(book.getId()).orElse(null);
        if (existing == null) {
            return true;
        }

        // Проверяем, изменились ли важные поля
        return !book.getTitle().equals(existing.getTitle()) ||
                !book.getAuthor().equals(existing.getAuthor()) ||
                !book.getDescription().equals(existing.getDescription()) ||
                !book.getGenre().equals(existing.getGenre());
    }
}