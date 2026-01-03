package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.util.VectorUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * АЛЬТЕРНАТИВНАЯ ВЕРСИЯ
 * Использует JdbcTemplate для работы с векторами
 * Не требует Hypersistence Utils
 */
@Service
public class VectorSearchService {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public VectorSearchService(BookRepository bookRepository,
                               EmbeddingService embeddingService,
                               JdbcTemplate jdbcTemplate) {
        this.bookRepository = bookRepository;
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Семантический поиск книг по запросу
     */
    public List<Book> semanticSearch(String query, int limit) {
        try {
            // Генерируем эмбеддинг для запроса
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = VectorUtils.formatVectorForPostgres(queryEmbedding);

            // Выполняем нативный SQL запрос
            String sql = """
                SELECT id, title, author, genre, description, isbn, price, stock, image_path
                FROM book 
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

            return jdbcTemplate.query(
                    sql,
                    new Object[]{vectorString, limit},
                    (rs, rowNum) -> Book.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .author(rs.getString("author"))
                            .genre(rs.getString("genre"))
                            .description(rs.getString("description"))
                            .isbn(rs.getString("isbn"))
                            .price(rs.getBigDecimal("price"))
                            .stock(rs.getInt("stock"))
                            .imagePath(rs.getString("image_path"))
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Ошибка семантического поиска: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Поиск похожих книг на основе другой книги
     */
    public List<Book> findSimilarBooks(Book book, int limit) {
        try {
            // Сначала загружаем эмбеддинг книги из БД
            float[] embedding = loadEmbedding(book.getId());

            if (embedding == null) {
                // Если нет эмбеддинга, генерируем
                embedding = embeddingService.generateEmbedding(book.getTextForEmbedding());
                saveEmbedding(book.getId(), embedding);
            }

            String vectorString = VectorUtils.formatVectorForPostgres(embedding);

            String sql = """
                SELECT id, title, author, genre, description, isbn, price, stock, image_path
                FROM book 
                WHERE embedding IS NOT NULL 
                AND id != ?
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

            return jdbcTemplate.query(
                    sql,
                    new Object[]{book.getId(), vectorString, limit},
                    (rs, rowNum) -> Book.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .author(rs.getString("author"))
                            .genre(rs.getString("genre"))
                            .description(rs.getString("description"))
                            .isbn(rs.getString("isbn"))
                            .price(rs.getBigDecimal("price"))
                            .stock(rs.getInt("stock"))
                            .imagePath(rs.getString("image_path"))
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Ошибка поиска похожих книг: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Семантический поиск с фильтром по жанру
     */
    public List<Book> semanticSearchByGenre(String query, String genre, int limit) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = VectorUtils.formatVectorForPostgres(queryEmbedding);

            String sql = """
                SELECT id, title, author, genre, description, isbn, price, stock, image_path
                FROM book 
                WHERE embedding IS NOT NULL
                AND LOWER(genre) = LOWER(?)
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

            return jdbcTemplate.query(
                    sql,
                    new Object[]{genre, vectorString, limit},
                    (rs, rowNum) -> Book.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .author(rs.getString("author"))
                            .genre(rs.getString("genre"))
                            .description(rs.getString("description"))
                            .isbn(rs.getString("isbn"))
                            .price(rs.getBigDecimal("price"))
                            .stock(rs.getInt("stock"))
                            .imagePath(rs.getString("image_path"))
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Ошибка поиска по жанру: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Семантический поиск среди книг в наличии
     */
    public List<Book> semanticSearchInStock(String query, int limit) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = VectorUtils.formatVectorForPostgres(queryEmbedding);

            String sql = """
                SELECT id, title, author, genre, description, isbn, price, stock, image_path
                FROM book 
                WHERE embedding IS NOT NULL
                AND stock > 0
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

            return jdbcTemplate.query(
                    sql,
                    new Object[]{vectorString, limit},
                    (rs, rowNum) -> Book.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .author(rs.getString("author"))
                            .genre(rs.getString("genre"))
                            .description(rs.getString("description"))
                            .isbn(rs.getString("isbn"))
                            .price(rs.getBigDecimal("price"))
                            .stock(rs.getInt("stock"))
                            .imagePath(rs.getString("image_path"))
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Ошибка поиска в наличии: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Гибридный поиск
     */
    public List<Book> hybridSearch(String query, int limit) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = VectorUtils.formatVectorForPostgres(queryEmbedding);
            String keyword = extractMainKeyword(query);

            String sql = """
                SELECT id, title, author, genre, description, isbn, price, stock, image_path
                FROM book 
                WHERE embedding IS NOT NULL
                AND (
                    LOWER(title) LIKE LOWER(?)
                    OR LOWER(author) LIKE LOWER(?)
                    OR LOWER(description) LIKE LOWER(?)
                )
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

            String likePattern = "%" + keyword + "%";

            return jdbcTemplate.query(
                    sql,
                    new Object[]{likePattern, likePattern, likePattern, vectorString, limit},
                    (rs, rowNum) -> Book.builder()
                            .id(rs.getLong("id"))
                            .title(rs.getString("title"))
                            .author(rs.getString("author"))
                            .genre(rs.getString("genre"))
                            .description(rs.getString("description"))
                            .isbn(rs.getString("isbn"))
                            .price(rs.getBigDecimal("price"))
                            .stock(rs.getInt("stock"))
                            .imagePath(rs.getString("image_path"))
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Ошибка гибридного поиска: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Индексирование всех книг без эмбеддингов
     */
    @Transactional
    public void indexAllBooks() {
        List<Book> booksWithoutEmbeddings = bookRepository.findBooksWithoutEmbeddings();

        if (booksWithoutEmbeddings.isEmpty()) {
            System.out.println("Все книги уже проиндексированы!");
            return;
        }

        System.out.println("Найдено " + booksWithoutEmbeddings.size() + " книг без эмбеддингов");

        int processed = 0;
        for (Book book : booksWithoutEmbeddings) {
            try {
                float[] embedding = embeddingService.generateEmbedding(book.getTextForEmbedding());
                saveEmbedding(book.getId(), embedding);

                processed++;
                if (processed % 10 == 0) {
                    System.out.println("Обработано " + processed + " из " + booksWithoutEmbeddings.size());
                }

                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("Ошибка обработки книги " + book.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("Индексация завершена: " + processed + "/" + booksWithoutEmbeddings.size());
    }

    /**
     * Переиндексация всех книг
     */
    @Transactional
    public void reindexAllBooks() {
        List<Book> allBooks = bookRepository.findAll();
        System.out.println("Переиндексация " + allBooks.size() + " книг...");

        int processed = 0;
        for (Book book : allBooks) {
            try {
                float[] embedding = embeddingService.generateEmbedding(book.getTextForEmbedding());
                saveEmbedding(book.getId(), embedding);

                processed++;
                if (processed % 10 == 0) {
                    System.out.println("Обработано " + processed + " из " + allBooks.size());
                }

                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("Ошибка обработки книги " + book.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("Переиндексация завершена!");
    }

    /**
     * Загрузить эмбеддинг книги из БД
     */
    private float[] loadEmbedding(Long bookId) {
        try {
            String sql = "SELECT embedding FROM book WHERE id = ?";

            return jdbcTemplate.query(
                    sql,
                    new Object[]{bookId},
                    rs -> {
                        if (rs.next()) {
                            String vectorStr = rs.getString("embedding");
                            if (vectorStr != null && !vectorStr.isEmpty()) {
                                return VectorUtils.parseVectorFromPostgres(vectorStr);
                            }
                        }
                        return null;
                    }
            );
        } catch (Exception e) {
            System.err.println("Ошибка загрузки эмбеддинга: " + e.getMessage());
            return null;
        }
    }

    /**
     * Сохранить эмбеддинг книги в БД
     */
    @Transactional
    public void saveEmbedding(Long bookId, float[] embedding) {
        try {
            String vectorString = VectorUtils.formatVectorForPostgres(embedding);
            String sql = "UPDATE book SET embedding = CAST(? AS vector) WHERE id = ?";

            jdbcTemplate.update(sql, vectorString, bookId);

        } catch (Exception e) {
            System.err.println("Ошибка сохранения эмбеддинга для книги " + bookId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получить статистику индексации
     */
    public String getIndexingStats() {
        long totalBooks = bookRepository.count();
        long indexedBooks = bookRepository.countBooksWithEmbeddings();
        double percentage = totalBooks > 0 ? (indexedBooks * 100.0 / totalBooks) : 0;

        return String.format(
                "Всего книг: %d | Проиндексировано: %d (%.1f%%)",
                totalBooks, indexedBooks, percentage
        );
    }

    /**
     * Извлекает основное ключевое слово из запроса
     */
    private String extractMainKeyword(String query) {
        String[] words = query.toLowerCase()
                .replaceAll("[^a-zа-яё\\s]", "")
                .split("\\s+");

        String longest = "";
        for (String word : words) {
            if (word.length() > longest.length()) {
                longest = word;
            }
        }
        return longest;
    }
}