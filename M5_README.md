# M5 – Catalog, Search & Administration

Đây là bộ mã nguồn mẫu cho các Use Case M5:
- UC01 – Browse Vehicle
- UC02 – Search Vehicle
- UC11 – Manage Vehicle
- UC12 – Manage User

## Công nghệ
- Java 21
- Spring Boot 3.4
- Spring MVC, Thymeleaf
- Spring Data JPA
- Spring Security
- H2 dùng để chạy thử trên máy cá nhân
- JUnit 5 và Mockito

## Chạy thử
Cần cài Java 21 và Maven 3.9 trở lên.

```bash
mvn spring-boot:run
```

Mở `http://localhost:8080/vehicles`.

Tài khoản quản trị demo:
- Username: `admin`
- Password: `admin123`

Cấu hình H2 console:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:morrowdb`
- User: `sa`
- Password: để trống

Chạy kiểm thử:

```bash
mvn test
```

## Các phần đã có
- UC01: danh sách xe đang hoạt động, lọc theo danh mục, sắp xếp theo giá hoặc mới nhất, phân trang 30 xe/trang, xem chi tiết và tăng lượt xem.
- UC02: tìm theo tên hoặc mã xe, nhận ngày nhận/trả và kiểm tra định dạng cũng như thứ tự ngày.
- UC11: xem danh sách xe, thêm, sửa và chuyển xe sang trạng thái ngừng hoạt động; kiểm tra mã xe trùng và giá thuê âm.
- UC12: xem, thêm, sửa, khóa và mở khóa người dùng; mật khẩu mới được mã hóa bằng BCrypt.

## Giới hạn cần biết trước khi ghép vào project nhóm
Bộ code này được viết độc lập do chưa có source hiện tại, ERD và Class Diagram Stage 1–2 của nhóm. Vì vậy, tên bảng và thuộc tính trong `Vehicle`, `Category`, `AppUser` là giả định, cần đối chiếu trước khi ghép vào project chính.

Các nội dung chưa hoàn thiện:
1. UC02 chưa gọi `AvailabilityService` của M3 nên chưa lọc xe thực sự còn trống theo ngày.
2. UC11 chưa upload ảnh JPG/PNG và giới hạn 5 MB.
3. UC11 chưa kiểm tra booking đang hoạt động trước khi ngừng hoạt động xe vì chưa có Booking entity/repository. Hiện thao tác xóa được đổi thành ngừng hoạt động để tránh xóa cứng dữ liệu.
4. UC12 chưa phân trang và chưa có chức năng xóa user. Cần xem quy tắc xóa/soft delete và lịch sử booking trong thiết kế nhóm.
5. Tài khoản admin đang dùng cấu hình in-memory để demo; cần nối với cơ chế đăng nhập và role thật của nhóm.
6. H2 chỉ phục vụ chạy thử; khi tích hợp cần cấu hình PostgreSQL theo dự án.
7. Cấu hình entity hiện dùng tên bảng đơn giản. Không thay entity hoặc schema của nhóm trước khi đối chiếu ERD.

Không đưa bộ code độc lập này vào nhánh chính thay cho source chung nếu chưa được nhóm xác nhận.
