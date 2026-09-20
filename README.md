# Hướng Dẫn Sử Dụng Dự Án Spring Boot Catalog (GraphQL & AJAX)

Dự án quản lý **Sản phẩm (Product)** và **Danh mục (Category)** sử dụng kiến trúc hiện đại kết hợp **Spring Boot**, **Spring Data JPA**, **Thymeleaf**, **AJAX** và **GraphQL API** (theo chuẩn bài giảng Lập trình Web - ĐH Sư phạm Kỹ thuật TP.HCM).

---

## 📌 Các Tính Năng Chính

1. **Trang chủ (GraphQL & AJAX)**:
   - Hiển thị danh sách sản phẩm với giá được sắp xếp **từ thấp đến cao (Ascending)**.
   - Lọc nhanh sản phẩm theo từng **Danh mục** cụ thể hoặc hiển thị tất cả tức thì qua GraphQL mà không cần tải lại trang.
   - Nút điều hướng nhanh đến trang Quản trị CRUD AJAX.

2. **Quản lý Sản phẩm (CRUD AJAX)**:
   - Thêm mới, cập nhật thông tin và xóa sản phẩm không tải lại trang.
   - Tìm kiếm sản phẩm theo tên theo thời gian thực kết hợp **phân trang (Pagination)**.
   - Hỗ trợ tải lên **nhiều hình ảnh** (Multi-image upload) với kiểm tra định dạng và dung lượng an toàn.

3. **Quản lý Danh mục (CRUD AJAX)**:
   - Quản lý danh mục hàng hóa, kiểm tra ràng buộc toàn vẹn dữ liệu (không xóa danh mục khi đang có sản phẩm liên kết).

4. **GraphQL API & GraphiQL IDE**:
   - Tích hợp 1 Endpoint duy nhất `POST /graphql` xử lý toàn bộ Query và Mutation.
   - Giao diện trực quan **GraphiQL** tích hợp sẵn tại trình duyệt để test và debug API.

---

## 🛠️ Công Nghệ Sử Dụng

- **Backend**: Java, Spring Boot, Spring Data JPA, Hibernate ORM, Spring for GraphQL.
- **Database**: Microsoft SQL Server (hoặc H2 Database khi chạy kiểm thử).
- **Frontend**: Thymeleaf, Bootstrap 5, AJAX (Fetch API & jQuery), CSS3 Responsive.
- **Testing**: JUnit 5, MockMvc, AssertJ (14/14 tests kiểm thử tự động đã pass hoàn toàn).

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Dự Án

### Bước 1: Chuẩn bị Cơ sở dữ liệu (SQL Server)
Mở SQL Server Management Studio (SSMS) và chỉ cần chạy 1 dòng lệnh duy nhất để tạo database rỗng:
```sql
CREATE DATABASE mapper_vd12;
```
> **Lưu ý:** Bạn **không cần tạo bảng bằng tay**. Tính năng `spring.jpa.hibernate.ddl-auto=update` sẽ tự động đọc các class Entity (`Product`, `Category`, `ProductImage`) và tự sinh toàn bộ bảng, khóa chính, khóa ngoại khi ứng dụng khởi động.

### Bước 2: Cấu hình kết nối DB (nếu cần)
Mở file `src/main/resources/application.properties` và kiểm tra lại tài khoản SQL Server của bạn:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=mapper_vd12;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=huy210906
```

### Bước 3: Khởi chạy Ứng dụng
- **Cách 1 (Spring Tool Suite / Eclipse)**: Nhấp chuột phải vào dự án ➔ `Run As` ➔ `Spring Boot App`.
- **Cách 2 (Dòng lệnh Terminal / PowerShell)**:
  ```powershell
  mvn spring-boot:run
  ```

---

## 🌐 Danh Mục Đường Dẫn (URLs)

Khi server khởi động tại cổng **8092**, bạn có thể truy cập các trang sau:

| Tên chức năng | Đường dẫn URL | Mô tả |
| :--- | :--- | :--- |
| 🏠 **Trang chủ** | [http://localhost:8092/](http://localhost:8092/) | Giá thấp ➔ cao, lọc danh mục bằng GraphQL & AJAX |
| 📦 **CRUD Sản phẩm** | [http://localhost:8092/products/ajax](http://localhost:8092/products/ajax) | Giao diện AJAX CRUD, tìm kiếm, phân trang, upload ảnh |
| 📂 **CRUD Danh mục** | [http://localhost:8092/categories](http://localhost:8092/categories) | Giao diện quản lý danh mục |
| ⚡ **GraphiQL IDE** | [http://localhost:8092/graphiql?path=/graphql](http://localhost:8092/graphiql?path=/graphql) | Công cụ test trực quan các truy vấn GraphQL |
| 📜 **Swagger 3 UI** | [http://localhost:8092/swagger-ui/index.html](http://localhost:8092/swagger-ui/index.html) | Tài liệu & Kiểm thử trực quan toàn bộ RESTful API |

---

## 🔍 Ví Dụ Truy Vấn GraphQL (Dùng trên GraphiQL hoặc AJAX)

### 1. Lấy sản phẩm trang chủ (giá từ thấp đến cao)
```graphql
query {
  homeProducts {
    id
    name
    price
    quantity
    categoryName
  }
}
```

### 2. Lọc sản phẩm theo một danh mục (ví dụ category ID = 2)
```graphql
query {
  homeProducts(categoryId: "2") {
    id
    name
    price
    categoryName
  }
}
```

### 3. Tìm kiếm sản phẩm có phân trang
```graphql
query {
  products(keyword: "Laptop", page: 0, size: 5) {
    content {
      id
      name
      price
      categoryName
    }
    page
    size
    totalElements
    totalPages
  }
}
```

### 4. Thêm sản phẩm mới (Mutation)
```graphql
mutation {
  createProduct(input: {
    name: "MacBook Pro M3",
    price: "42000000",
    quantity: 10,
    description: "Apple M3 Chip, 16GB RAM",
    categoryId: "1"
  }) {
    id
    name
    price
  }
}
```

---

## 🧪 Chạy Kiểm Thử Tự Động (Unit & Integration Tests)
Để chạy toàn bộ bộ test kiểm thử tự động (GraphQL, Service, Upload ảnh):
```powershell
mvn test -Dtest=Springboot3ApplicationTests
```
Kết quả mong đợi: `Tests run: 14, Failures: 0, Errors: 0, BUILD SUCCESS`.
