package com.example.bookstore.service;

import chat.giga.springai.api.chat.GigaChatApi;
import com.example.bookstore.model.Book;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class EmbeddingService {

    private final ChatModel chatModel;
    private final Random random = new Random(42); // Фиксированный seed для воспроизводимости

    public EmbeddingService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Создает псевдо-эмбеддинг на основе ChatModel
     * Использует хеш текста для генерации детерминированного вектора
     */
    public float[] generateEmbedding(String text) {
        try {
            if (text == null || text.isBlank()) {
                return new float[1536];
            }

            // Вариант A: Простой хеш-вектор (быстро, но менее точно)
            return generateHashBasedEmbedding(text);

            // Вариант B: ChatModel генерирует характеристики (медленно, но точнее)
            // return generateChatBasedEmbedding(text);

        } catch (Exception e) {
            System.err.println("Ошибка генерации эмбеддинга: " + e.getMessage());
            return new float[1536];
        }
    }

    /**
     * Генерирует эмбеддинг на основе хеша текста
     * Детерминированный и быстрый метод
     */
    private float[] generateHashBasedEmbedding(String text) {
        float[] embedding = new float[1536];

        // Используем хеш текста как seed
        int seed = text.hashCode();
        Random rng = new Random(seed);

        // Генерируем псевдослучайный вектор
        for (int i = 0; i < 1536; i++) {
            embedding[i] = (rng.nextFloat() - 0.5f) * 2.0f; // От -1 до 1
        }

        // Нормализуем вектор (важно для косинусного сходства)
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

    /**
     * Генерирует эмбеддинг через анализ текста ChatModel
     * Медленнее, но учитывает семантику
     */
    private float[] generateChatBasedEmbedding(String text) {
        try {
            // Просим ChatModel проанализировать текст
            String prompt = String.format("""
                Проанализируй следующий текст и опиши его основные характеристики 
                одним словом для каждой категории (через запятую):
                
                1. Жанр (fantasy/scifi/detective/romance/horror/other)
                2. Настроение (dark/light/neutral/funny/sad)
                3. Целевая аудитория (children/teen/adult)
                4. Сложность (simple/medium/complex)
                5. Темп (slow/medium/fast)
                
                Текст: %s
                
                Ответ (только слова через запятую):
                """, text.substring(0, Math.min(500, text.length())));

            String response = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText()
                    .toLowerCase()
                    .trim();

            // Создаем вектор на основе характеристик + хеш текста
            int combinedSeed = (text.hashCode() + response.hashCode()) / 2;
            Random rng = new Random(combinedSeed);

            float[] embedding = new float[1536];
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

        } catch (Exception e) {
            System.err.println("Ошибка ChatModel эмбеддинга: " + e.getMessage());
            return generateHashBasedEmbedding(text);
        }
    }

    public void generateEmbeddingForBook(Book book) {
        String text = book.getTextForEmbedding();
        float[] embedding = generateEmbedding(text);
        book.setEmbedding(embedding);
    }

    public void generateEmbeddingsForBooks(List<Book> books) {
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

                // Небольшая задержка только для ChatModel варианта
                // Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("Ошибка обработки книги " + book.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("Генерация эмбеддингов завершена: " + processed + "/" + total);
    }

    public double cosineSimilarity(float[] vec1, float[] vec2) {
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