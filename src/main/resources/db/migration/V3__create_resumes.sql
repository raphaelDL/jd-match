create table resumes (
    id          uuid        primary key,
    resume_hash varchar(64) not null unique,
    resume_text text        not null,
    created_at  timestamp with time zone not null
);
