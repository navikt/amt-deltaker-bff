alter table deltaker add column tilgjengelige_data jsonb not null default '[]'::jsonb;