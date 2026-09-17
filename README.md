# Hệ Thống Quản Lý Sản Phẩm & Danh Mục (Spring Boot + RESTful API + AJAX)

Dự án phát triển ứng dụng web theo mô hình **RESTful API** và giao diện **AJAX (Single Page Application UX)** không tải lại trang, sử dụng **Spring Boot 4, Spring Data JPA, Hibernate, Bootstrap 5 và jQuery AJAX**.

---

## 1. Yêu Cầu Môi Trường & Cơ Sở Dữ Liệu

- **Java Development Kit (JDK)**: Java 17 trở lên (đã kiểm thử tương thích tốt với JDK 21+ / JDK 26)
- **Cơ sở dữ liệu**: Microsoft SQL Server
  - Tên database: `mapper_vd12`
  - Cấu hình trong file [application.properties](file:///src/main/resources/application.properties):
    ```properties
    spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=mapper_vd12;encrypt=true;trustServerCertificate=true
    spring.datasource.username=sa
    spring.datasource.password=huy210906
    spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
    spring.jpa.hibernate.ddl-auto=update
    ```
- **Thư mục lưu trữ hình ảnh tải lên**:
  - Sản phẩm: `uploads/products/`
  - Danh mục: `uploads/categories/`

---

## 2. Hướng Dẫn Chạy Ứng Dụng

### Cách 1: Chạy từ Eclipse / Spring Tool Suite (STS)
1. Mở Eclipse / STS -> `File` -> `Import...` -> `Existing Maven Projects` -> Chọn thư mục project `springboot-4`.
2. Chuột phải vào project -> `Run As` -> `Spring Boot App` (hoặc mở file [Springboot3Application.java](file:///src/main/java/vn/itstar/Springboot3Application.java) -> `Run`).

### Cách 2: Chạy bằng Maven Wrapper từ dòng lệnh
```bash
./mvnw clean spring-boot:run
```

---

## 3. Danh Sách URL Giao Diện (Web URLs)

| Chức năng | Đường dẫn (URL) | Mô tả |
|---|---|---|
| **Trang chủ** | [http://localhost:8092/](http://localhost:8092/) | Tự động chuyển hướng sang trang Quản lý Sản phẩm AJAX |
| **Quản lý Sản phẩm (AJAX)** | [http://localhost:8092/products/ajax](http://localhost:8092/products/ajax) | CRUD, tìm kiếm, phân trang, chọn danh mục, tải nhiều ảnh qua AJAX |
| **Quản lý Danh mục (AJAX)** | [http://localhost:8092/categories](http://localhost:8092/categories) (hoặc `/categories/ajax`) | CRUD, tìm kiếm, phân trang, tải icon danh mục qua AJAX |
| **Quản lý Sản phẩm (Giao diện cũ)** | [http://localhost:8092/products](http://localhost:8092/products) | Giao diện SSR truyền thống với Thymeleaf form |

---

## 4. Tài Liệu RESTful API

Tất cả các API trả về JSON chuẩn theo cấu trúc:
```json
{
  "success": true,
  "message": "Thông điệp phản hồi",
  "data": { ... }
}
```
Khi có lỗi validation (HTTP 400):
```json
{
  "success": false,
  "message": "Vui lòng kiểm tra dữ liệu nhập",
  "data": null,
  "errors": {
    "name": "Tên sản phẩm không được để trống",
    "price": "Giá phải >= 0"
  }
}
```

### 4.1. API Danh Mục (Category APIs - `/api/categories`)

| Phương thức | Endpoint | Mô tả | Định dạng dữ liệu |
|---|---|---|---|
| `GET` | `/api/categories` | Lấy danh sách danh mục có phân trang và tìm kiếm theo tên (`keyword`, `page`, `size`) | Query params: `keyword`, `page` (default: 0), `size` (default: 5) |
| `GET` | `/api/categories/{id}` | Lấy thông tin chi tiết một danh mục theo ID | Path variable: `id` |
| `POST` | `/api/categories` | Thêm mới danh mục kèm upload icon | `multipart/form-data`: `categoryName` (string, required), `iconFile` (file, optional) |
| `PUT` | `/api/categories/{id}` | Cập nhật danh mục kèm thay đổi icon qua HTTP PUT | `multipart/form-data`: `categoryName` (string, required), `iconFile` (file, optional) |
| `DELETE` | `/api/categories/{id}` | Xóa danh mục (báo lỗi 409 nếu danh mục đang chứa sản phẩm) | Path variable: `id` |

### 4.2. API Sản Phẩm (Product APIs - `/api/products`)

| Phương thức | Endpoint | Mô tả | Định dạng dữ liệu |
|---|---|---|---|
| `GET` | `/api/products` | Lấy danh sách sản phẩm có phân trang và tìm kiếm theo tên (`keyword`, `page`, `size`) | Query params: `keyword`, `page` (default: 0), `size` (default: 5) |
| `GET` | `/api/products/{id}` | Lấy chi tiết một sản phẩm theo ID (kèm danh sách hình ảnh) | Path variable: `id` |
| `POST` | `/api/products` | Thêm mới sản phẩm kèm danh mục và upload nhiều hình ảnh | `multipart/form-data`: `name`, `price`, `quantity`, `description`, `categoryId`, `imageFiles` (multiple) |
| `PUT` | `/api/products/{id}` | Cập nhật sản phẩm và upload bổ sung ảnh qua HTTP PUT | `multipart/form-data`: `name`, `price`, `quantity`, `description`, `categoryId`, `imageFiles` (multiple) |
| `DELETE` | `/api/products/{id}` | Xóa sản phẩm và toàn bộ hình ảnh liên quan | Path variable: `id` |
| `DELETE` | `/api/products/images/{imageId}` | Xóa một hình ảnh cụ thể của sản phẩm (tự động đôn ảnh kế tiếp làm ảnh chính nếu xóa ảnh chính) | Path variable: `imageId` |

---

## 5. Ví Dụ Kiểm Thử API Bằng cURL

### Thêm danh mục mới:
```bash
curl -X POST http://localhost:8092/api/categories \
  -F "categoryName=Điện thoại" \
  -F "iconFile=@icon.png"
```

### Cập nhật danh mục bằng HTTP PUT:
```bash
curl -X PUT http://localhost:8092/api/categories/1 \
  -F "categoryName=Điện thoại thông minh" \
  -F "iconFile=@new_icon.png"
```

### Thêm sản phẩm kèm ảnh và danh mục:
```bash
curl -X POST http://localhost:8092/api/products \
  -F "name=iPhone 15 Pro Max" \
  -F "price=29990000" \
  -F "quantity=20" \
  -F "categoryId=1" \
  -F "description=Phiên bản 256GB Titan tự nhiên" \
  -F "imageFiles=@iphone_front.png" \
  -F "imageFiles=@iphone_back.png"
```

### Cập nhật sản phẩm bằng HTTP PUT:
```bash
curl -X PUT http://localhost:8092/api/products/1 \
  -F "name=iPhone 15 Pro Max (VN/A)" \
  -F "price=28990000" \
  -F "quantity=15" \
  -F "categoryId=1" \
  -F "description=Đã cập nhật giá khuyến mãi"
```

### Phân trang và tìm kiếm:
```bash
curl "http://localhost:8092/api/products?keyword=iPhone&page=0&size=5"
```
