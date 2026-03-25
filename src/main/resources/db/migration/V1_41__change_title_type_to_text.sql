-- Drop the materialized views
DROP MATERIALIZED VIEW IF EXISTS report.self_harm_summary_view;
DROP MATERIALIZED VIEW IF EXISTS report.serious_sexual_assault_summary_view;

-- Now the ALTER will work (metadata-only, fast)
ALTER TABLE report ALTER COLUMN title TYPE TEXT;

