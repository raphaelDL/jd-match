create table analyses (
    id              uuid         primary key,
    idempotency_key varchar(64)  not null unique,
    jd_text         text         not null,
    resume_text     text         not null,
    analysis_json   jsonb        not null,
    model           varchar(64)  not null,
    prompt_version  varchar(32)  not null,
    created_at      timestamp with time zone not null
);
