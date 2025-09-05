-- Backfill NULL timestamps before constraining
UPDATE bookings SET created_at = COALESCE(created_at, NOW()), updated_at = COALESCE(updated_at, NOW());
UPDATE listings SET created_at = COALESCE(created_at, NOW()), updated_at = COALESCE(updated_at, NOW());
UPDATE vendors  SET created_at = COALESCE(created_at, NOW()), updated_at = COALESCE(updated_at, NOW());
UPDATE inventory SET created_at = COALESCE(created_at, NOW()), updated_at = COALESCE(updated_at, NOW());
UPDATE reviews  SET created_at = COALESCE(created_at, NOW()), updated_at = COALESCE(updated_at, NOW());

-- Enforce NOT NULL and sensible defaults moving forward
ALTER TABLE bookings  ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE bookings  ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE bookings  ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE bookings  ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE listings  ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE listings  ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE listings  ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE listings  ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE vendors   ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE vendors   ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE vendors   ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE vendors   ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE inventory ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE inventory ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE inventory ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE inventory ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE reviews   ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE reviews   ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE reviews   ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE reviews   ALTER COLUMN updated_at SET NOT NULL;