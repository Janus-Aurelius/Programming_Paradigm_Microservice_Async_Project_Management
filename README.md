# Hệ Thống Quản Lý Dự Án - Project Management System - 23520020 Nguyễn Thiên An

Đây là một ứng dụng quản lý dự án phân tán được xây dựng với kiến trúc microservices, sử dụng Spring Boot cho backend và Angular cho frontend.

## 📋 Tổng Quan Dự Án

### Kiến Trúc Hệ Thống

- **Frontend**: Angular 19 với Angular Material
- **Backend**: Spring Boot 3.2.5 (Java 17)
- **Database**: MongoDB 5
- **Message Broker**: Apache Kafka 3.4.0
- **API Gateway**: Spring Cloud Gateway
- **Containerization**: Docker & Docker Compose

### Cấu Trúc Thư Mục

```
project_management/
├── frontend-angular/          # Ứng dụng Angular frontend
├── api-gateway/              # API Gateway service
├── user-service/             # Service quản lý người dùng
├── project-service/          # Service quản lý dự án
├── task-service/             # Service quản lý công việc
├── comment-service/          # Service quản lý bình luận
├── notification-service/     # Service thông báo
├── websocket-service/        # Service WebSocket real-time
├── common-contracts/         # Shared contracts
├── common-security/          # Shared security components
├── database-schema/          # MongoDB schema và scripts
└── docker-compose.yml        # Docker orchestration
```

## 🚀 Cách Chạy Ứng Dụng

### Yêu Cầu Hệ Thống

#### Phần Mềm Cần Thiết:

- **Docker Desktop** (phiên bản mới nhất)
- **Node.js** 18+ và npm (cho frontend)
- **Java 17** (nếu chạy services riêng lẻ)
- **Maven 3.6+** (nếu build từ source)

#### Kiểm Tra Yêu Cầu:

```powershell
# Kiểm tra Docker
docker --version
docker-compose --version

# Kiểm tra Node.js
node --version
npm --version

# Kiểm tra Java (optional)
java --version
```

---

## 🖥️ Chạy Frontend (Angular)

### 1. Cài Đặt Dependencies

```powershell
# Di chuyển vào thư mục frontend
cd frontend-angular

# Cài đặt các package dependencies
npm install
```

### 2. Chạy Development Server

```powershell
# Chạy Angular development server với proxy configuration
npm start

# Hoặc sử dụng ng CLI trực tiếp
ng serve --proxy-config proxy.conf.json
hoặc
ng serve
```

### 3. Truy Cập Ứng Dụng

- **URL**: http://localhost:4200
- **Proxy Configuration**: Tất cả API calls sẽ được proxy đến `http://localhost:8080`

### 4. Các Lệnh Khác cho Frontend

```powershell
# Build cho production
npm run build

# Chạy tests
npm run test

# Linting code
npm run lint

# Build và watch cho development
npm run watch
```

---

## 🔧 Chạy Backend (Microservices)

### Phương Pháp 1: Sử Dụng Docker Compose (Khuyên Dùng)

#### 1. Chạy Toàn Bộ Hệ Thống

```powershell
# Từ thư mục root của project
1/ mvn clean install -U
2/ docker-compose up --build
```

#### 2. Kiểm Tra Trạng Thái Services

```powershell
# Xem trạng thái containers
docker-compose ps

# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của service cụ thể
docker-compose logs -f api-gateway
```

#### 3. Dừng Services

```powershell
# Dừng tất cả services
docker-compose down

# Dừng và xóa volumes (reset database)
docker-compose down -v
```

### Phương Pháp 2: Chạy Từng Service Riêng Lẻ

#### 1. Khởi Động Infrastructure Services

```powershell
# Chỉ chạy MongoDB và Kafka
docker-compose up -d mongo kafka
```

#### 2. Build Tất Cả Services

```powershell
# Build toàn bộ project
mvn clean package -DskipTests

# Hoặc build với Docker profile
mvn clean package -DskipTests -f pom-docker.xml
```

#### 3. Chạy Từng Service

```powershell
# Chạy API Gateway (Port: 8080)
cd api-gateway
mvn spring-boot:run

# Chạy User Service (Port: 8081)
cd user-service
mvn spring-boot:run

# Chạy Project Service (Port: 8082)
cd project-service
mvn spring-boot:run

# Và các services khác...
```

---

## 🗄️ Thiết Lập Database

### 1. Khởi Tạo MongoDB Schema

```powershell
# Di chuyển vào thư mục database-schema
cd database-schema

# Chạy script khởi tạo (Windows)
init-db.bat

# Hoặc với các options:
init-db.bat --reset-db --sample-data
```

### 2. Kiểm Tra Database

```powershell
# Kết nối MongoDB (nếu có MongoDB client)
mongosh mongodb://localhost:27017/project_management

# Kiểm tra collections
show collections
```

---

## 🔗 Endpoints và Ports

### Frontend

- **Angular App**: http://localhost:4200

### Backend Services

- **API Gateway**: http://localhost:8080
- **User Service**: http://localhost:8081
- **Project Service**: http://localhost:8082
- **Task Service**: http://localhost:8083
- **Notification Service**: http://localhost:8087
- **Comment Service**: http://localhost:8088
- **WebSocket Service**: http://localhost:8089

### Infrastructure

- **MongoDB**: mongodb://localhost:27017
- **Kafka**: localhost:9092

### API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

---

## 🐛 Troubleshooting

### Lỗi Thường Gặp

#### 1. Port Đã Được Sử Dụng

```powershell
# Kiểm tra port đang được sử dụng
netstat -an | findstr ":8080"

# Dừng process sử dụng port
taskkill /F /PID <process_id>
```

#### 2. Docker Build Lỗi

```powershell
# Clean Docker cache
docker system prune -a

# Rebuild without cache
docker-compose build --no-cache
```

#### 3. MongoDB Connection Issues

```powershell
# Kiểm tra MongoDB container
docker logs mongo

# Restart MongoDB
docker-compose restart mongo
```

#### 4. Frontend Proxy Issues

- Đảm bảo backend đang chạy trên port 8080
- Kiểm tra `proxy.conf.json` configuration
- Restart Angular dev server

### Logs và Debugging

```powershell
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs service cụ thể
docker-compose logs -f user-service

# Xem logs Angular
npm start # logs sẽ hiển thị trong terminal
```

---

## 🔧 Development Workflow

### 1. Workflow Hàng Ngày

```powershell
# 1. Start infrastructure
docker-compose up -d mongo kafka

# 2. Start backend services
docker-compose up -d

# 3. Start frontend
cd frontend-angular
npm start

# 4. Development...

# 5. Stop everything
docker-compose down
```

### 2. Hot Reload Development

- **Frontend**: Angular dev server tự động reload khi có thay đổi
- **Backend**: Sử dụng Spring Boot DevTools hoặc restart containers

### 3. Testing

```powershell
# Frontend tests
cd frontend-angular
npm run test

# Backend tests
mvn test

# Integration tests với Docker
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

---

## 📝 Ghi Chú Quan Trọng

### Security

- JWT authentication được enable (`JWT_ENABLED=true`)
- CORS configuration cho phép requests từ frontend
- API Gateway xử lý authentication và routing

### Performance

- Services được configure với memory limits
- Database indexes được optimize
- Kafka được setup cho async messaging

### Monitoring

- Health checks được configure cho tất cả services
- Logs được centralize qua Docker Compose
- Metrics available qua Spring Boot Actuator

---

## 🤝 Đóng Góp

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

---

## 📄 License

Dự án này được license dưới ISC License - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

---

## 📞 Liên Hệ

- **Repository**: [GitHub Repository](https://github.com/Janus-Aurelius/Programming_Paradigm_Microservice_Async_Project_Management)
- **Issues**: [GitHub Issues](https://github.com/Janus-Aurelius/Programming_Paradigm_Microservice_Async_Project_Management/issues)

---

**Happy Coding! 🚀**
