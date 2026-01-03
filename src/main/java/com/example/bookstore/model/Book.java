package com.example.bookstore.model;

import io.hypersistence.utils.hibernate.type.array.FloatArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String genre;

    @Column(length = 2000)
    private String description;

    private String isbn;
    private BigDecimal price;
    private Integer stock;

    private String imagePath;

    // Векторное представление для семантического поиска
    @Type(FloatArrayType.class)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    /**
     * Получить текст для генерации эмбеддинга
     * Комбинирует название, автора, жанр и описание
     */
    public String getTextForEmbedding() {
        StringBuilder text = new StringBuilder();

        if (title != null) {
            text.append("Название: ").append(title).append(". ");
        }
        if (author != null) {
            text.append("Автор: ").append(author).append(". ");
        }
        if (genre != null) {
            text.append("Жанр: ").append(genre).append(". ");
        }
        if (description != null) {
            text.append("Описание: ").append(description);
        }

        return text.toString();
    }
}