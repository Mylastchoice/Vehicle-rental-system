ALTER TABLE payments ADD COLUMN checkout_key uuid NOT NULL DEFAULT gen_random_uuid();
