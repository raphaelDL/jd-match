-- Phase 3: similarity search over past JDs.
-- Spring AI's PgVectorStore uses this table with initialize-schema=false, so Flyway
-- owns the schema. The local embedding model (all-MiniLM-L6-v2) produces 384-dim vectors.
create extension if not exists vector;
create extension if not exists hstore;
create extension if not exists "uuid-ossp";

create table if not exists vector_store (
    id        uuid default uuid_generate_v4() primary key,
    content   text,
    metadata  json,
    embedding vector(384)
);

create index if not exists vector_store_embedding_idx
    on vector_store using hnsw (embedding vector_cosine_ops);
