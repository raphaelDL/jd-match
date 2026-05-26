create table jds (
    id                uuid         primary key,
    jd_hash           varchar(64)  not null unique,
    jd_text           text         not null,
    requirements_json jsonb        not null,
    model             varchar(64)  not null,
    prompt_version    varchar(32)  not null,
    created_at        timestamp with time zone not null
);
