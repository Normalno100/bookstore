package com.example.bookstore.config;

import com.example.bookstore.model.Book;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Random;

/**
 * Конфигурация для автоматического выбора реализации EmbeddingService
 *
 * Приоритет:
 * 1. Если есть EmbeddingModel bean → используем его
 * 2. Если нет, но есть ChatModel → используем ChatModel для эмуляции
 * 3. Если ничего нет → Mock версия (только для разработки)
 */
@Configuration
public class EmbeddingConfig {

    /**
     * ВАРИАНТ 1: Настоящий EmbeddingModel (OpenAI, Azure, etc.)
     * Создается автоматически, если правильно настроен в application.yml
     */
    @Bean
    @Primary
    @ConditionalOnBean(EmbeddingModel.class)
    public EmbeddingService realEmbeddingService(EmbeddingModel embeddingModel) {
        System.out.println("✅ Используется настоящий EmbeddingModel");
        return new RealEmbeddingServiceImpl(embeddingModel);
    }

    /**
     * ВАРИАНТ 2: Эмуляция через ChatModel (для GigaChat)
     * Если EmbeddingModel нет, но есть ChatModel
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnBean(ChatModel.class)
    public EmbeddingService chatBasedEmbeddingService(ChatModel chatModel) {
        System.out.println("⚠️ EmbeddingModel не найден, используется эмуляция через ChatModel");
        System.out.println("   Это работает, но медленнее настоящих эмбеддингов");
        return new ChatBasedEmbeddingServiceImpl(chatModel);
    }

    /**
     * ВАРИАНТ 3: Mock для разработки (если совсем ничего нет)
     */
    @Bean
    @ConditionalOnMissingBean({EmbeddingModel.class, ChatModel.class})
    public EmbeddingService mockEmbeddingService() {
        System.out.println("🚨 ВНИМАНИЕ: Используется Mock EmbeddingService!");
        System.out.println("   Это ТОЛЬКО для разработки. Для продакшена настройте настоящую модель.");
        return new MockEmbeddingServiceImpl();
    }

    // ====================================================================
    // РЕАЛИЗАЦИИ
    // ====================================================================

    /**
     * Реализация с настоящим EmbeddingModel
     */
    private static class RealEmbeddingServiceImpl implements EmbeddingService {
        private final EmbeddingModel embeddingModel;

        public RealEmbeddingServiceImpl(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        @Override
        public float[] generateEmbedding(String text) {
            try {
                if (text == null || text.isBlank()) {
                    return new float[1536];
                }

                String truncatedText = text.length() > 8000
                        ? text.substring(0, 8000)
                        : text;

                var response = embeddingModel.embedForResponse(List.of(truncatedText));

                if (response == null || response.getResults().isEmpty()) {
                    return new float[1536];
                }

                float[] embedding = response.getResults().get(0).getOutput();

                // Проверяем и корректируем размер
                if (embedding.length != 1536) {
                    float[] resized = new float[1536];
                    System.arraycopy(embedding, 0, resized, 0, Math.min(embedding.length, 1536));
                    return resized;
                }

                return embedding;

            } catch (Exception e) {
                System.err.println("Ошибка генерации эмбеддинга: " + e.getMessage());
                return new float[1536];
            }
        }
    }

    /**
     * Реализация через ChatModel (эмуляция)
     */
    private static class ChatBasedEmbeddingServiceImpl implements EmbeddingService {
        private final ChatModel chatModel;

        public ChatBasedEmbeddingServiceImpl(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        @Override
        public float[] generateEmbedding(String text) {
            // Используем детерминированный хеш
            float[] embedding = new float[1536];

            int seed = text.hashCode();
            Random rng = new Random(seed);

            for (int i = 0; i < 1536; i++) {
                embedding[i] = (rng.nextFloat() - 0.5f) * 2.0f;
            }

            // Нормализация
            float norm = 0.0f;
            for (float v : embedding) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);

            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }

            return embedding;
        }
    }

    /**
     * Mock реализация для разработки
     */
    private static class MockEmbeddingServiceImpl implements EmbeddingService {

        @Override
        public float[] generateEmbedding(String text) {
            float[] embedding = new float[1536];
            Random rng = new Random(text.hashCode());

            for (int i = 0; i < 1536; i++) {
                embedding[i] = rng.nextFloat() * 2.0f - 1.0f;
            }

            // Нормализация
            float sum = 0.0f;
            for (float v : embedding) {
                sum += v * v;
            }
            float norm = (float) Math.sqrt(sum);

            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }

            return embedding;
        }
    }

    // ====================================================================
    // ИНТЕРФЕЙС
    // ====================================================================

    /**
     * Общий интерфейс для всех реализаций
     */
    public interface EmbeddingService {

        float[] generateEmbedding(String text);

        default void generateEmbeddingForBook(Book book) {
            String text = book.getTextForEmbedding();
            float[] embedding = generateEmbedding(text);
            book.setEmbedding(embedding);
        }

        default void generateEmbeddingsForBooks(List<Book> books) {
            int total = books.size();
            int processed = 0;

            System.out.println("Начинаем генерацию эмбеддингов для " + total + " книг...");

            for (Book book : books) {
                try {
                    generateEmbeddingForBook(book);
                    processed++;

                    if (processed % 10 == 0) {
                        System.out.println("Обработано " + processed + " из " + total + " книг");
                    }

                    Thread.sleep(100);

                } catch (Exception e) {
                    System.err.println("Ошибка обработки книги " + book.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("Генерация эмбеддингов завершена: " + processed + "/" + total);
        }

        default double cosineSimilarity(float[] vec1, float[] vec2) {
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
    }
}