-- V6: Expand supported file types: add TXT, DOCX, HTML
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_file_type_check;
ALTER TABLE documents ADD CONSTRAINT documents_file_type_check
    CHECK (file_type IN ('PDF', 'MARKDOWN', 'TXT', 'DOCX', 'HTML'));
