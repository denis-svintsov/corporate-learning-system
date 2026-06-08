ALTER TABLE courses.certificate
    ADD COLUMN IF NOT EXISTS object_key VARCHAR(500);

ALTER TABLE courses.certificate
    ALTER COLUMN pdf_bytes DROP NOT NULL;

UPDATE courses.certificate
SET object_key = 'legacy-db/' || id || '.pdf'
WHERE object_key IS NULL
  AND pdf_bytes IS NOT NULL;
