# Vehicle Rental System (Morrow) – Backend

Công nghệ phần mềm – Stage 3 (Programming) & Stage 4 (Testing), Nhóm 01.
Backend **Java 21 + Spring Boot 3.5 + Spring Data JPA + PostgreSQL + Flyway + Spring Security**.

## 1. Yêu cầu
| Công cụ | Phiên bản |
|---|---|
| JDK | 21 |
| Maven | 3.9+ (hoặc dùng Maven của IDE) |
| PostgreSQL | 14+ (đã thử với 16) |

## 2. Cấu hình PostgreSQL

1. Tạo hai database (một để chạy ứng dụng, một **riêng và trống** cho test):
   ```sql
   CREATE DATABASE vehicle_rental_system;
   CREATE DATABASE vehicle_rental_test;
   ```
2. Cấu hình bằng **biến môi trường** (không ghi mật khẩu vào code/Git):

   | Biến | Ý nghĩa | Mặc định |
   |---|---|---|
   | `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/vehicle_rental_system` |
   | `DB_USERNAME` | tài khoản | `postgres` |
   | `DB_PASSWORD` | mật khẩu | *(rỗng)* |
   | `PORT` | cổng web | `8080` |
   | `SHOW_SQL` | in câu SQL ra log | `false` |
   | `FLYWAY_BASELINE_ON_MIGRATE` | dùng khi database đã có bảng từ file backup | `false` |
   | `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` | database dùng cho test | `jdbc:postgresql://localhost:5432/vehicle_rental_test`, `postgres`, *(rỗng)* |

   macOS / Linux: `export DB_PASSWORD=matkhau` · Windows PowerShell: `$env:DB_PASSWORD="matkhau"` · IntelliJ: *Run → Edit Configurations → Environment variables*.
3. **Flyway tự tạo bảng** khi ứng dụng khởi động (`src/main/resources/db/migration`: `V1__create_schema.sql` 10 bảng, `V2__add_indexes.sql` chỉ mục). Không chạy `database/vehicle_rental_system.sql` lên database đã migrate.
   *Nếu database đã có sẵn bảng (khôi phục từ file backup)*: đặt `FLYWAY_BASELINE_ON_MIGRATE=true` một lần.
4. **Dữ liệu mẫu (tuỳ chọn, chỉ cho database chạy ứng dụng)** – sau khi chạy ứng dụng một lần để Flyway tạo bảng:
   ```bash
   psql -U postgres -d vehicle_rental_system -f database/dev_seed.sql
   ```
   Tạo 14 xe, 4 chi nhánh, 4 loại xe và 2 tài khoản:

   | Vai trò | Email | Mật khẩu |
   |---|---|---|
   | Admin (bảng `administrators`) | `admin@morrow.vn` | `Admin@123` |
   | Member (bảng `users`) | `minh.nguyen@gmail.com` | `123456` |

   Mật khẩu trong database là **BCrypt**. Không lưu mật khẩu thô.

## 3. Chạy
```bash
mvn spring-boot:run
```
Mở giao diện: http://localhost:8080/rental/index.html<br>
Kiểm tra backend/API: http://localhost:8080/api/health (chỉ hiện trạng thái chạy) và http://localhost:8080/api/vehicles

## 4. API hiện có

| Phương thức & đường dẫn | Ai gọi được | Mô tả |
|---|---|---|
| `GET /`, `GET /api/health` | Guest | Kiểm tra ứng dụng chạy |
| `GET /api/vehicles` | Guest | Danh sách xe, tham số: `categoryId`, `keyword`, `status` (Available/Rented/Maintenance), `sort` (name/price/discount/rentals), `order` (asc/desc), `page` (từ 0), `size` (≤100, mặc định 30) |
| `GET /api/vehicles/{vehicleCode}` | Guest | Chi tiết xe + giá sau giảm |
| `GET /api/categories`, `GET /api/branches` | Guest | Loại xe, chi nhánh |
| `GET /api/me` | Đã đăng nhập | Email và vai trò hiện tại |
| `GET /api/member/**` | **Member** | Khu vực thành viên (`/api/member/ping` để thử) |
| `GET /api/admin/**`, `GET /api/database` | **Admin** | Khu vực quản trị (`/api/admin/ping` để thử) |

Thử đăng nhập (HTTP Basic, tên đăng nhập = email):
```bash
curl http://localhost:8080/api/me -u minh.nguyen@gmail.com:123456
curl http://localhost:8080/api/admin/ping -u minh.nguyen@gmail.com:123456   # 403: member không phải admin
curl http://localhost:8080/api/admin/ping -u admin@morrow.vn:Admin@123      # 200
```


## 4A. M5 – Catalogue, Search & Administration

Các endpoint quản trị dưới `/api/admin/**` yêu cầu tài khoản có `ROLE_ADMIN` (HTTP Basic, email + mật khẩu). Các API quản lý đã tích hợp vào cấu trúc `com.group01`, dùng PostgreSQL và entity/repository hiện có.

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/api/admin/users` | Danh sách tài khoản |
| POST | `/api/admin/users` | Tạo tài khoản MEMBER |
| PATCH | `/api/admin/users/{id}/status` | Khóa/mở khóa tài khoản (`ACTIVE`/`INACTIVE`) |
| GET | `/api/admin/vehicles` | Danh sách xe quản trị |
| POST | `/api/admin/vehicles` | Thêm xe |
| PUT | `/api/admin/vehicles/{code}` | Cập nhật xe |
| DELETE | `/api/admin/vehicles/{code}` | Xóa xe nếu chưa được booking tham chiếu |
| GET | `/api/admin/categories` | Danh sách danh mục |
| POST | `/api/admin/categories` | Thêm danh mục |
| PUT | `/api/admin/categories/{id}` | Sửa danh mục |
| DELETE | `/api/admin/categories/{id}` | Xóa danh mục nếu chưa được xe sử dụng |

Ví dụ tạo tài khoản (gửi JSON):
```json
{"fullName":"Nguyen Van A","email":"a@example.com","password":"Password123","phone":"0900000000"}
```
Mật khẩu tối thiểu 8 ký tự, email được kiểm tra định dạng và không được trùng. Mật khẩu được mã hóa BCrypt trước khi lưu. Tài khoản chuyển sang `INACTIVE` sẽ không đăng nhập được do `DatabaseUserDetailsService` đã kiểm tra trạng thái.

Tìm xe theo khoảng ngày thuê:
`GET /api/vehicles?pickupDate=2026-12-01&returnDate=2026-12-05`

Phải truyền cả hai ngày; ngày trả phải sau ngày nhận và ngày nhận không được ở quá khứ. Xe có booking trùng khoảng ngày ở trạng thái `PENDING`, `CONFIRMED` hoặc `PICKED_UP` sẽ bị loại khỏi kết quả. Hai khoảng thuê liền kề nhau vẫn được phép. Đây là quy tắc áp dụng từ các trạng thái `BookingStatus` đang có trong project.

Mỗi lần gọi `GET /api/vehicles/{vehicleCode}`, hệ thống tăng `viewCount` thêm 1. Khi cập nhật xe, nếu không truyền `categoryId` thì danh mục hiện tại được giữ nguyên; tương tự với `branchId`.

Lưu ý: đây là các API backend. Giao diện quản trị frontend chưa được tạo trong phần tích hợp này.

## 5. Phân quyền (Spring Security) – việc 10
* **Guest** (chưa đăng nhập): xem danh mục (GET xe, loại xe, chi nhánh) và health.
* **Member**: `ROLE_MEMBER` – bảng `users` với `role = MEMBER`.
* **Admin**: `ROLE_ADMIN` – mọi dòng của bảng `administrators` và bảng `users` có `role = ADMIN`.
* Tài khoản `status = INACTIVE` không đăng nhập được.
* API hỗ trợ HTTP Basic, đồng thời M2 sử dụng đăng nhập **session** cho các trang HTML. Lưu ý: CSRF hiện đang tắt trong cấu hình gốc, phải rà soát bảo mật trước khi triển khai công khai.
* Khi thêm endpoint mới: đặt dưới `/api/member/**` hoặc `/api/admin/**` để tự có quyền đúng, hoặc thêm rule trong `SecurityConfig`. Mặc định mọi đường dẫn khác **phải đăng nhập**.

## 6. Xử lý lỗi chung – việc 11
Mọi lỗi trả về cùng một dạng JSON (`exception/GlobalExceptionHandler.java`):
```json
{ "timestamp": "2026-11-21T05:00:00Z", "status": 404, "error": "Not Found",
  "message": "Vehicle not found: ABC", "path": "/api/vehicles/ABC" }
```
| Tình huống | HTTP |
|---|---|
| Không thấy dữ liệu (`ResourceNotFoundException`) | 404 |
| Tham số sai (`BadRequestException`, sai kiểu, thiếu tham số, JSON hỏng, `@Valid` lỗi có `fieldErrors`) | 400 |
| Chưa đăng nhập / sai mật khẩu | 401 |
| Sai vai trò | 403 |
| Xung đột (`ConflictException`, vi phạm UNIQUE/CHECK/FK của database) | 409 |
| Lỗi không lường trước | 500 – chi tiết chỉ ghi log, **không** gửi cho client |

Trong code nghiệp vụ chỉ cần `throw new BadRequestException("...")`, `ResourceNotFoundException.of("Booking", id)` hoặc `new ConflictException("...")`.

## 7. Cấu trúc
```
src/main/java/com/group01
├── entity/       10 entity JPA khớp V1__create_schema.sql (+ entity/converter: enum <-> text trong DB)
├── enums/        BookingStatus ("Picked up"), VehicleStatus ("Available"), UserRole...
├── repository/   Spring Data JPA (+ DatabaseRepository dùng JdbcTemplate để kiểm tra kết nối)
├── service/      CatalogService (đọc danh mục), DatabaseService
├── controller/   REST controller
├── dto/          dữ liệu trả về cho client (không trả entity)
├── exception/    ApiError, GlobalExceptionHandler, các exception chung
└── security/     SecurityConfig, DatabaseUserDetailsService, handler 401/403 dạng JSON
```
Quy ước: controller → service → repository. Controller không dùng repository trực tiếp, không trả entity ra ngoài.

## 8. Kiểm thử – việc 13
```bash
mvn test            # chạy toàn bộ test
```
| Test | Cần PostgreSQL? | Nội dung |
|---|---|---|
| `EnumConvertersTest` | Không | Enum ↔ chuỗi trong DB đúng chính tả (`Picked up`...), giá sau giảm |
| `GlobalExceptionHandlerTest` | Không | Mỗi loại lỗi trả đúng mã HTTP + JSON, không lộ chi tiết nội bộ |
| `DatabaseMappingTest` | **Có** | Lưu/đọc 10 entity trên PostgreSQL thật, UNIQUE/CHECK, cascade giỏ hàng & booking, truy vấn trùng lịch |
| `SecurityApiTest` | **Có** | Guest/Member/Admin, tài khoản bị khoá, tìm kiếm, sắp xếp, phân trang, lỗi JSON |

Test cần PostgreSQL dùng database **`vehicle_rental_test` (trống)** qua biến `TEST_DB_*`; mỗi test tự rollback. Nếu không kết nối được PostgreSQL, các test này **bị bỏ qua** (hiện "skipped"); đặt `REQUIRE_DB=true` để bắt buộc (CI làm vậy).

## 9. Làm việc nhóm với GitHub – việc 14
* Nhánh: `main` ← `develop` ← `feature/<tên-việc>`. Không commit thẳng vào `main`/`develop`.
* Mỗi Pull Request: mô tả việc đã làm + chạy `mvn test` xanh + ít nhất 1 người review (GitHub Actions `.github/workflows/ci.yml` tự chạy test với PostgreSQL).
* **Xung đột khi merge** – các file hay trùng: `pom.xml`, `application.properties`, `SecurityConfig.java`, `GlobalExceptionHandler.java`. Cách xử lý:
  ```bash
  git checkout feature/ten-viec
  git fetch origin
  git merge origin/develop        # nếu báo CONFLICT: mở file, giữ lại CẢ HAI phần hợp lý, xoá dấu <<<<<<< ======= >>>>>>>
  mvn test                        # phải xanh
  git add <file> && git commit    # kết thúc merge, rồi git push
  ```
* Mỗi người chỉ sửa package/file của mình; cần đổi file dùng chung (entity, `SecurityConfig`, `GlobalExceptionHandler`, `pom.xml`) thì báo nhóm trước.

## M4 Booking Management (newly added, verification required)

- Member: `/bookings` (history), `/bookings/{id}` (details), `POST /bookings/{id}/cancel`; JSON: `GET /api/bookings`, `GET /api/bookings/{id}`, `POST /api/bookings/{id}/cancel`.
- Administrator: `/admin/bookings`, `/admin/bookings/{id}`, `POST /admin/bookings/{id}/status`; JSON: `GET /api/admin/bookings?status=PENDING`, `PATCH /api/admin/bookings/{id}/status` with `{"status":"CONFIRMED"}`.
- Only owners can see/cancel their own bookings. Only admins can manage booking statuses.
- No extra SQL migration; existing PostgreSQL tables are reused. See `M4_IMPLEMENTATION_NOTES.md` for important refund, date, auditing, mail and test limitations.
- This implementation was created for the new cleaned submission ZIP and **has not inherited the pass status of the old Group 01 testing spreadsheet**. Re-test it.

## Chạy demo trên máy bằng Docker

```powershell
# Run from the repository root
docker compose up --build -d
docker compose logs -f app
```

Sau khi log báo ứng dụng khởi động, nhấn Ctrl+C để thoát chế độ xem log. Mở [Morrow trên localhost](http://localhost:8080). Spring Boot và PostgreSQL chạy trong các container trên máy demo.

Nạp dữ liệu mẫu một lần trên database Docker mới, sau khi Flyway tạo bảng.

```powershell
Get-Content -Raw .\database\dev_seed.sql | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U postgres -d vehicle_rental_system
```

- Khách thuê đăng nhập bằng `minh.nguyen@gmail.com` / `123456`. Trang chủ có **My profile** và **My bookings** sau khi đăng nhập.
- Admin đăng nhập bằng `admin@morrow.vn` / `Admin@123`. Form đăng nhập chuyển admin đến **Manage bookings**.
- Để demo trả sớm, đặt xe với một khoảng bắt đầu hôm nay, rồi dùng admin xác nhận đơn và ghi nhận nhận xe. Khách vào **My bookings**, chọn ngày trả trước lịch và gửi yêu cầu. Khi nhận lại xe, admin chọn **Confirm vehicle returned**. Trang chi tiết giữ ngày hẹn ban đầu và hiển thị cả ngày đề nghị lẫn ngày trả thực tế. Số tiền thuê giữ theo đơn ban đầu.

### Thanh toán online sandbox

Đặt `MORROW_STRIPE_SECRET_KEY` thành khóa `sk_test_` của Stripe sandbox trong môi trường PowerShell hoặc file `.env` riêng của máy. Chạy `docker compose up -d app` để cập nhật biến môi trường. Chọn **Pay online** khi đặt xe, mở **Stripe sandbox checkout** trong chi tiết đơn, rồi dùng thẻ test `4242 4242 4242 4242`, ngày hết hạn tương lai và CVC test. Khi Stripe chuyển về ứng dụng, máy chủ xác minh số tiền và cập nhật tiền cọc. Admin có thể xác nhận đơn sau khi hệ thống ghi nhận tiền cọc.

Nếu cần cập nhật ngay cả khi người dùng đóng trang thanh toán, chạy Stripe CLI với `stripe listen --events checkout.session.completed --forward-to localhost:8080/api/payments/stripe/webhook`. Đặt signing secret CLI in ra vào `MORROW_STRIPE_WEBHOOK_SECRET` rồi chạy lại `docker compose up -d app`. Người dùng cũng có thể bấm **Check payment** để đối chiếu với Stripe. Xem [hướng dẫn thẻ test Stripe](https://docs.stripe.com/testing).

Dừng demo bằng `docker compose down`.

