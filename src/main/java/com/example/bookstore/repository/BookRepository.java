package com.example.bookstore.repository;

import com.example.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);

    /**
     * Векторный поиск похожих книг по косинусному сходству
     * Возвращает топ-N наиболее похожих книг
     */
    @Query(value = """
        SELECT * FROM book 
        WHERE embedding IS NOT NULL 
        AND id != :excludeId
        ORDER BY embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Book> findSimilarBooks(
            @Param("queryVector") String queryVector,
            @Param("excludeId") Long excludeId,
            @Param("limit") int limit
    );

    /**
     * Векторный поиск похожих книг без исключений
     */
    @Query(value = """
        SELECT * FROM book 
        WHERE embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Book> findSimilarBooks(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    /**
     * Векторный поиск с фильтром по жанру
     */
    @Query(value = """
        SELECT * FROM book 
        WHERE embedding IS NOT NULL
        AND LOWER(genre) = LOWER(:genre)
        ORDER BY embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Book> findSimilarBooksByGenre(
            @Param("queryVector") String queryVector,
            @Param("genre") String genre,
            @Param("limit") int limit
    );

    /**
     * Векторный поиск с фильтром по наличию на складе
     */
    @Query(value = """
        SELECT * FROM book 
        WHERE embedding IS NOT NULL
        AND stock > 0
        ORDER BY embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Book> findSimilarBooksInStock(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    /**
     * Гибридный поиск: комбинация векторного и текстового
     * Сначала текстовый фильтр, затем сортировка по векторному сходству
     */
    @Query(value = """
        SELECT * FROM book 
        WHERE embedding IS NOT NULL
        AND (
            LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(author) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        ORDER BY embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Book> hybridSearch(
            @Param("queryVector") String queryVector,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    /**
     * Получить все книги без эмбеддингов (для пакетной обработки)
     */
    @Query("SELECT b FROM Book b WHERE b.embedding IS NULL")
    List<Book> findBooksWithoutEmbeddings();

    /**
     * Подсчет книг с эмбеддингами
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.embedding IS NOT NULL")
    long countBooksWithEmbeddings();
}