-- The JD and resume text now live in their own tables (jds, resumes); analyses
-- reference them instead of duplicating the text.
alter table analyses
    drop column jd_text,
    drop column resume_text,
    add column jd_id     uuid references jds(id),
    add column resume_id uuid references resumes(id);
