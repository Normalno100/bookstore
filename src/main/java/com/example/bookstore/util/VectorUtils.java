package com.example.bookstore.util;

import java.util.Arrays;

/**
 * Утилиты для работы с векторными эмбеддингами
 */
public class VectorUtils {

    /**
     * Форматирует вектор для PostgreSQL pgvector
     * Пример: [0.1,0.2,0.3,...]
     */
    public static String formatVectorForPostgres(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Парсит вектор из формата PostgreSQL
     * Пример: "[0.1,0.2,0.3]" → float[]
     */
    public static float[] parseVectorFromPostgres(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return new float[0];
        }

        // Убираем квадратные скобки
        String cleaned = vectorStr.trim();
        if (cleaned.startsWith("[")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        // Разбиваем по запятым
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                result[i] = 0.0f;
            }
        }

        return result;
    }

    /**
     * Вычисляет косинусное сходство между двумя векторами
     * Возвращает значение от -1 до 1 (чем ближе к 1, тем более похожи)
     */
    public static double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Вычисляет евклидово расстояние между векторами
     * Чем меньше, тем более похожи
     */
    public static double euclideanDistance(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return Double.MAX_VALUE;
        }

        double sum = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            double diff = vec1[i] - vec2[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * Нормализует вектор (длина = 1)
     */
    public static float[] normalize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return vector;
        }

        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm == 0.0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }

        return normalized;
    }

    /**
     * Проверяет валидность вектора
     */
    public static boolean isValidVector(float[] vector, int expectedDimension) {
        if (vector == null || vector.length != expectedDimension) {
            return false;
        }

        // Проверяем на NaN и Infinity
        for (float v : vector) {
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Создает пустой вектор заданной размерности
     */
    public static float[] createEmptyVector(int dimension) {
        return new float[dimension];
    }

    /**
     * Создает случайный нормализованный вектор (для тестирования)
     */
    public static float[] createRandomVector(int dimension, long seed) {
        java.util.Random random = new java.util.Random(seed);
        float[] vector = new float[dimension];

        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat() * 2.0f - 1.0f;
        }

        return normalize(vector);
    }

    /**
     * Вычисляет среднее значение элементов вектора
     */
    public static double mean(float[] vector) {
        if (vector == null || vector.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (float v : vector) {
            sum += v;
        }

        return sum / vector.length;
    }

    /**
     * Находит минимальное и максимальное значение в векторе
     */
    public static float[] minMax(float[] vector) {
        if (vector == null || vector.length == 0) {
            return new float[]{0.0f, 0.0f};
        }

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (float v : vector) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        return new float[]{min, max};
    }

    /**
     * Сравнивает два вектора на равенство с допустимой погрешностью
     */
    public static boolean equals(float[] vec1, float[] vec2, float epsilon) {
        if (vec1 == vec2) return true;
        if (vec1 == null || vec2 == null) return false;
        if (vec1.length != vec2.length) return false;

        for (int i = 0; i < vec1.length; i++) {
            if (Math.abs(vec1[i] - vec2[i]) > epsilon) {
                return false;
            }
        }

        return true;
    }

    /**
     * Копирует вектор
     */
    public static float[] copy(float[] vector) {
        if (vector == null) return null;
        return Arrays.copyOf(vector, vector.length);
    }

    /**
     * Изменяет размер вектора (дополняет нулями или обрезает)
     */
    public static float[] resize(float[] vector, int newSize) {
        if (vector == null) {
            return new float[newSize];
        }

        float[] resized = new float[newSize];
        System.arraycopy(vector, 0, resized, 0, Math.min(vector.length, newSize));
        return resized;
    }

    /**
     * Форматирует вектор для отладки (показывает первые N элементов)
     */
    public static String debugString(float[] vector, int maxElements) {
        if (vector == null) return "null";
        if (vector.length == 0) return "[]";

        StringBuilder sb = new StringBuilder("[");
        int limit = Math.min(maxElements, vector.length);

        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.4f", vector[i]));
        }

        if (vector.length > maxElements) {
            sb.append(", ... (").append(vector.length - maxElements).append(" more)");
        }

        sb.append("]");
        return sb.toString();
    }
}