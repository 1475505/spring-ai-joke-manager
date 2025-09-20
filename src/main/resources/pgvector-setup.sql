-- pgvector extension installation script
-- Execute this script in PostgreSQL database

-- 1. Create pgvector extension (requires superuser privileges)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Verify extension installation
SELECT extname, extversion 
FROM pg_extension 
WHERE extname = 'vector';

-- 3. Create vector storage table (Spring AI will auto-create, this is just example)
/*
CREATE TABLE IF NOT EXISTS vector_store (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding vector(1536)  -- Dimension matches configuration in application.properties
);

-- Create vector index (optional, improves similarity search performance)
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding 
ON vector_store 
USING ivfflat (embedding vector_cosine_ops);
*/

-- 4. Check current database supported vector operations
SELECT oprname, oprleft::regtype, oprright::regtype 
FROM pg_operator 
WHERE oprname LIKE '<->' 
   OR oprname LIKE '<=>' 
   OR oprname LIKE '<~>';