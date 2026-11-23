-- ============================================================================
-- GHI CHÚ TÀI KHOẢN ĐĂNG NHẬP TRÊN WEB:
--   1. Admin:  admin@morrow.vn        | Mật khẩu: Admin@123
--   2. Member: minh.nguyen@gmail.com  | Mật khẩu: 123456
-- ============================================================================


-- ============================================================================
-- PHẦN 1: ĐỔI MẬT KHẨU USER POSTGRES THÀNH '123456' (KHỚP VỚI SPRING BOOT)
-- (Bôi đen dòng dưới rồi nhấn F5)
-- ============================================================================
ALTER USER postgres WITH PASSWORD '123456';


-- ============================================================================
-- PHẦN 2: TẠO TOÀN BỘ 10 BẢNG DỮ LIỆU CỦA HỆ THỐNG
-- (Bôi đen từ đây đến hết bảng payments rồi nhấn F5 - chỉ cần chạy 1 lần đầu)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.administrators (
    admin_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS public.users (
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    address TEXT,
    phone VARCHAR(20) NOT NULL,
    license_number VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.branches (
    branch_id SERIAL PRIMARY KEY,
    branch_name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    phone VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS public.categories (
    category_id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS public.vehicles (
    vehicle_code VARCHAR(40) PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES public.categories(category_id),
    branch_id BIGINT NOT NULL REFERENCES public.branches(branch_id),
    name VARCHAR(150) NOT NULL,
    license_plate VARCHAR(30) UNIQUE,
    image_url TEXT,
    short_description TEXT,
    detailed_description TEXT,
    daily_price NUMERIC(14,2) NOT NULL CHECK (daily_price >= 0),
    discount_rate NUMERIC(5,4) NOT NULL DEFAULT 0 CHECK (discount_rate >= 0 AND discount_rate <= 1),
    status VARCHAR(20) NOT NULL DEFAULT 'Available' CHECK (status IN ('Available', 'Rented', 'Maintenance')),
    view_count INT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    rental_count INT NOT NULL DEFAULT 0 CHECK (rental_count >= 0)
);

CREATE TABLE IF NOT EXISTS public.rental_carts (
    cart_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES public.users(user_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.cart_items (
    cart_item_id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES public.rental_carts(cart_id),
    vehicle_code VARCHAR(40) NOT NULL REFERENCES public.vehicles(vehicle_code),
    pickup_date DATE NOT NULL,
    return_date DATE NOT NULL CHECK (return_date > pickup_date),
    CONSTRAINT uq_cart_vehicle UNIQUE (cart_id, vehicle_code)
);

CREATE TABLE IF NOT EXISTS public.bookings (
    booking_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(user_id),
    pickup_branch_id BIGINT NOT NULL REFERENCES public.branches(branch_id),
    return_branch_id BIGINT REFERENCES public.branches(branch_id),
    pickup_location VARCHAR(255) NOT NULL,
    pickup_date DATE NOT NULL,
    return_date DATE NOT NULL CHECK (return_date > pickup_date),
    renter_name VARCHAR(150) NOT NULL,
    renter_phone VARCHAR(20) NOT NULL,
    renter_license_number VARCHAR(100) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    deposit_amount NUMERIC(14,2) NOT NULL CHECK (deposit_amount >= 0 AND deposit_amount <= total_amount),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('ONLINE', 'AT_PICKUP')),
    status VARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Confirmed', 'Picked up', 'Completed', 'Cancelled')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.booking_details (
    booking_detail_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES public.bookings(booking_id),
    vehicle_code VARCHAR(40) NOT NULL REFERENCES public.vehicles(vehicle_code),
    pickup_date DATE NOT NULL,
    return_date DATE NOT NULL CHECK (return_date > pickup_date),
    daily_price NUMERIC(14,2) NOT NULL CHECK (daily_price >= 0),
    discount_rate NUMERIC(5,4) NOT NULL DEFAULT 0 CHECK (discount_rate >= 0 AND discount_rate <= 1),
    rental_days INT NOT NULL CHECK (rental_days > 0),
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
    UNIQUE (booking_id, vehicle_code)
);

CREATE TABLE IF NOT EXISTS public.payments (
    payment_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES public.bookings(booking_id),
    amount NUMERIC(14,2) NOT NULL CHECK (amount >= 0),
    payment_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    transaction_ref VARCHAR(150) UNIQUE,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================================
-- PHẦN 3: NẠP TÀI KHOẢN ADMIN & MEMBER MẪU (ĐÃ BĂM MẬT KHẨU BCRYPT)
-- (Bôi đen 2 lệnh INSERT dưới rồi nhấn F5)
-- ============================================================================
INSERT INTO public.administrators (full_name, email, password_hash, status)
VALUES ('Morrow Admin', 'admin@morrow.vn', '$2a$10$hDMtn/t3pVuNbW5HuqJcreBpx27bAAvXdb8f4MZKl76wIQQ0NXmIO', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

INSERT INTO public.users (full_name, email, password_hash, date_of_birth, gender, address, phone, license_number, role, status)
VALUES ('Minh Nguyen', 'minh.nguyen@gmail.com', '$2a$10$7HUZgagQe045ia8O0Vo0b.EmadSnqvvKikDAxUVqmPfanWhTyDFtG',
        '1998-05-20', 'M', '12 Nguyen Hue, District 1, HCMC', '0901234567', '079123456789', 'MEMBER', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;


-- ============================================================================
-- PHẦN 4: NẠP CHI NHÁNH, LOẠI XE VÀ DANH SÁCH 14 XE MẪU (KÈM ẢNH CHUẨN)
-- (Bôi đen toàn bộ Phần 4 rồi nhấn F5)
-- ============================================================================
INSERT INTO public.branches (branch_name, address, phone)
SELECT v.branch_name, v.address, v.phone
FROM (VALUES
    ('Morrow - District 1', '1 Le Loi, District 1, HCMC', '0281111111'),
    ('Morrow - District 7', '20 Nguyen Van Linh, District 7, HCMC', '0282222222'),
    ('Morrow - Tan Son Nhat Airport', 'Truong Son, Tan Binh, HCMC', '0283333333'),
    ('Morrow - Da Nang', '5 Bach Dang, Hai Chau, Da Nang', '0236444444')
) AS v(branch_name, address, phone)
WHERE NOT EXISTS (SELECT 1 FROM public.branches b WHERE b.branch_name = v.branch_name);

INSERT INTO public.categories (category_name, description) VALUES
    ('Car', 'Cars from 4 to 7 seats'),
    ('Motorbike', 'Scooters and sport motorbikes'),
    ('Bicycle', 'City and touring bicycles'),
    ('Van', 'Vans and minibuses for groups')
ON CONFLICT (category_name) DO NOTHING;

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
JOIN public.branches   b ON b.branch_name   = v.branch
ON CONFLICT (vehicle_code) DO NOTHING;


-- ============================================================================
-- PHẦN 5: CÁC LỆNH KIỂM TRA DỮ LIỆU & XEM MẬT KHẨU BĂM (BÔI ĐEN TỪNG DÒNG + F5)
-- ============================================================================

-- 5.1. Xem tài khoản Admin và chuỗi mật khẩu đã băm ($2a$10$...):
SELECT admin_id, full_name, email, password_hash, status FROM public.administrators;

-- 5.2. Xem danh sách Khách hàng (kể cả tài khoản mới đăng ký) và mật khẩu băm:
SELECT user_id, full_name, email, phone, license_number, password_hash, role, status FROM public.users;

-- 5.3. Xem danh sách 14 xe và trạng thái hiện tại (Available / Rented / Maintenance):
SELECT vehicle_code, name, license_plate, daily_price, discount_rate, status, rental_count FROM public.vehicles;

-- 5.4. Xem danh sách các đơn đặt xe (Booking) và trạng thái trả xe:
SELECT * FROM public.bookings ORDER BY booking_id DESC;

-- 5.5. Xem chi tiết từng xe trong mỗi đơn đặt xe (ngày thuê riêng từng xe):
SELECT * FROM public.booking_details ORDER BY booking_detail_id DESC;

-- 5.6. Xem lịch sử thanh toán / đặt cọc / hoàn tiền:
SELECT * FROM public.payments ORDER BY payment_id DESC;