-- Включаем расширение pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Добавляем колонку для хранения векторных эмбеддингов
-- Размерность 1536 - стандарт для многих моделей эмбеддингов
ALTER TABLE book ADD COLUMN embedding vector(1536);

-- Создаем индекс для быстрого векторного поиска
-- HNSW (Hierarchical Navigable Small World) - эффективный алгоритм для ANN поиска
CREATE INDEX book_embedding_idx ON book
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Альтернативный индекс IVFFlat (можно выбрать один из двух)
-- CREATE INDEX book_embedding_idx ON book
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- Комментарии для документации
COMMENT ON COLUMN book.embedding IS 'Vector embedding of book description for semantic search (1536 dimensions)';
COMMENT ON INDEX book_embedding_idx IS 'HNSW index for fast approximate nearest neighbor search';