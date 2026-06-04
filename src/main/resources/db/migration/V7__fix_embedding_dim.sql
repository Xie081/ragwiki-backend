-- V7: Change embedding column dimension from 1536 (OpenAI) to 1024 (DeepSeek pro-v1)
-- deepseek-embedding-pro-v1 returns 1024-dimensional vectors
ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(1024);
