-- Development sample data for the Morrow Vehicle Rental System (NOT a Flyway migration).
-- Run it ONCE on an empty, already migrated database (start the application first so that Flyway creates the tables):
--   psql -U postgres -d vehicle_rental_system -f database/dev_seed.sql
-- Do NOT run it on the test database (vehicle_rental_test): the integration tests expect an empty database.
-- Accounts (passwords are BCrypt hashes):
--   admin   admin@morrow.vn            / Admin@123   (table administrators)
--   member  minh.nguyen@gmail.com      / 123456      (table users, role MEMBER)

BEGIN;

INSERT INTO public.administrators (full_name, email, password_hash, status) VALUES
    ('Morrow Admin', 'admin@morrow.vn', '$2a$10$hDMtn/t3pVuNbW5HuqJcreBpx27bAAvXdb8f4MZKl76wIQQ0NXmIO', 'ACTIVE');

INSERT INTO public.users (full_name, email, password_hash, date_of_birth, gender, address, phone, license_number, role, status) VALUES
    ('Minh Nguyen', 'minh.nguyen@gmail.com', '$2a$10$7HUZgagQe045ia8O0Vo0b.EmadSnqvvKikDAxUVqmPfanWhTyDFtG',
     '1998-05-20', 'M', '12 Nguyen Hue, District 1, HCMC', '0901234567', '079123456789', 'MEMBER', 'ACTIVE');

INSERT INTO public.branches (branch_name, address, phone) VALUES
    ('Morrow - District 1', '1 Le Loi, District 1, HCMC', '0281111111'),
    ('Morrow - District 7', '20 Nguyen Van Linh, District 7, HCMC', '0282222222'),
    ('Morrow - Tan Son Nhat Airport', 'Truong Son, Tan Binh, HCMC', '0283333333'),
    ('Morrow - Da Nang', '5 Bach Dang, Hai Chau, Da Nang', '0236444444');

INSERT INTO public.categories (category_name, description) VALUES
    ('Car', 'Cars from 4 to 7 seats'),
    ('Motorbike', 'Scooters and sport motorbikes'),
    ('Bicycle', 'City and touring bicycles'),
    ('Van', 'Vans and minibuses for groups');

INSERT INTO public.vehicles (vehicle_code, category_id, branch_id, name, license_plate, image_url, short_description,
                             detailed_description, daily_price, discount_rate, status, view_count, rental_count)
SELECT v.code, c.category_id, b.branch_id, v.name, v.plate, v.img_url, v.short_desc,
       v.short_desc || ' Insurance included, free cancellation 24 hours before pickup.', v.price, v.disc, 'Available', 0, v.rentals
FROM (VALUES
    ('CAR-VF3',     'Car',       'Morrow - District 1',           'VinFast VF 3',       '51A-100.01', '/rental/assets/vinfast-vf3.jpg', 'Tiny electric city car.',             500000, 0.0000, 12),
    ('CAR-VF5',     'Car',       'Morrow - District 1',           'VinFast VF 5',       '51A-100.02', '/rental/assets/vinfast-vf5.jpg', 'Electric SUV for the city.',          700000, 0.1000,  9),
    ('CAR-VELOZ',   'Car',       'Morrow - District 7',           'Toyota Veloz Cross', '51A-100.03', '/rental/assets/vf-veloz.jpg',    '7-seat family MPV.',                  850000, 0.0500,  7),
    ('CAR-YARIS',   'Car',       'Morrow - District 7',           'Toyota Yaris Cross', '51A-100.04', '/rental/assets/hero-vf5.jpg',    'Compact hybrid crossover.',           800000, 0.0000,  5),
    ('CAR-MAZDA3',  'Car',       'Morrow - Tan Son Nhat Airport', 'Mazda 3',            '51A-100.05', '/rental/assets/hero-vf5.jpg',    'Stylish sedan.',                      950000, 0.1500,  6),
    ('CAR-CRETA',   'Car',       'Morrow - Da Nang',              'Hyundai Creta',      '43A-100.06', '/rental/assets/hero-vf5.jpg',    'Comfortable SUV.',                    880000, 0.0000,  4),
    ('MB-VISION',   'Motorbike', 'Morrow - District 1',           'Honda Vision',       '59X1-100.07', '/rental/assets/road-trip.jpg',   'Light automatic scooter.',           150000, 0.0000, 30),
    ('MB-AIRBLADE', 'Motorbike', 'Morrow - District 1',           'Honda Air Blade',    '59X1-100.08', '/rental/assets/road-trip.jpg',   'Sporty scooter.',                    200000, 0.1000, 22),
    ('MB-EXCITER',  'Motorbike', 'Morrow - District 7',           'Yamaha Exciter',     '59X1-100.09', '/rental/assets/road-trip.jpg',   'Underbone sport motorbike.',         250000, 0.0000, 14),
    ('MB-SHMODE',   'Motorbike', 'Morrow - Da Nang',              'Honda SH Mode',      '43X1-100.10', '/rental/assets/road-trip.jpg',   'Premium scooter.',                   300000, 0.0500, 11),
    ('BI-ATX',      'Bicycle',   'Morrow - District 1',           'Giant ATX',          NULL,         '/rental/assets/vinfast-vf3.jpg', 'Mountain bike.',                       80000, 0.0000,  8),
    ('BI-FX',       'Bicycle',   'Morrow - Da Nang',              'Trek FX 2',          NULL,         '/rental/assets/vinfast-vf3.jpg', 'Fitness hybrid bike.',                100000, 0.2000,  3),
    ('VAN-TRANSIT', 'Van',       'Morrow - Tan Son Nhat Airport', 'Ford Transit',       '51B-100.13', '/rental/assets/vf-veloz.jpg',    '16-seat minibus.',                1500000, 0.0000,  2),
    ('VAN-HIACE',   'Van',       'Morrow - District 7',           'Toyota Hiace',       '51B-100.14', '/rental/assets/vf-veloz.jpg',    '12-seat van.',                       1600000, 0.0500,  1)
) AS v(code, cat, branch, name, plate, img_url, short_desc, price, disc, rentals)
JOIN public.categories c ON c.category_name = v.cat
JOIN public.branches   b ON b.branch_name   = v.branch;

COMMIT;