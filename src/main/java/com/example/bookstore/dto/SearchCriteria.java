package com.example.bookstore.dto;

import lombok.*;

import java.util.List;

/**
 * DTO для структурированных параметров поиска,
 * извлеченных AI из естественного языка
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCriteria {

    // Основные параметры
    private String genre;           // Жанр (fantasy, detective, etc.)
    private String targetAudience;  // Целевая аудитория (teen, adult, children)
    private List<String> keywords;  // Ключевые слова (magic, adventure, etc.)

    // Дополнительные фильтры
    private String author;          // Автор
    private String mood;            // Настроение (dark, funny, inspirational)
    private String theme;           // Тема (friendship, war, love)
    private Integer minYear;        // Минимальный год (для классики/новинок)
    private Integer maxYear;        // Максимальный год

    // Метаданные
    private String originalQuery;   // Исходный запрос пользователя
    private Double confidence;      // Уверенность AI в извлечении (0-1)
}