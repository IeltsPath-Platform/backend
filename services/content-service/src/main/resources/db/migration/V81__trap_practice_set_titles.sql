-- The trap practice sets seeded earlier are no longer presented as drafts; learners see the plain title.
UPDATE content_packages
SET title = regexp_replace(title, '^Draft practice: ', '')
WHERE code LIKE 'PS-TRAP-%' AND title LIKE 'Draft practice: %';
