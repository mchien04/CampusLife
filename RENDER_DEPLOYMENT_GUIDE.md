# Hướng Dẫn Deploy Code Lên Render (Sử dụng MySQL từ Railway)

## 1. Lấy Thông Tin MySQL từ Railway

1. Đăng nhập vào [Railway Dashboard](https://railway.app)
2. Vào MySQL service của bạn
3. Vào tab **Variables** hoặc **Connect** để lấy connection string
4. Railway thường cung cấp format: `mysql://user:password@host:port/database`
5. Lưu lại các thông tin sau:
   - **Host**: (ví dụ: `yamabiko.proxy.rlwy.net`)
   - **Port**: (ví dụ: `39229`)
   - **Database**: (ví dụ: `railway`)
   - **Username**: (ví dụ: `root`)
   - **Password**: (password của bạn)

## 2. Tạo Web Service trên Render

1. Chọn "New" → "Web Service"
2. Kết nối repository GitHub/GitLab của bạn
3. Cấu hình:
   - **Name**: campuslife-backend (hoặc tên bạn muốn)
   - **Environment**: Docker
   - **Region**: Singapore (gần VN nhất)
   - **Branch**: main/master
   - **Root Directory**: (để trống nếu root)
   - **Dockerfile Path**: Dockerfile (hoặc để trống nếu ở root)

## 3. Environment Variables trên Render

Thêm các biến môi trường sau trong Render Dashboard (Settings → Environment):

### Database Configuration (Từ Railway MySQL)

**Cách 1: Parse từ Railway Connection String**

Nếu Railway cung cấp: `mysql://root:password@yamabiko.proxy.rlwy.net:39229/railway`

Thì set trên Render:
```
DATABASE_URL=jdbc:mysql://yamabiko.proxy.rlwy.net:39229/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_railway_password
```

**Cách 2: Từ thông tin riêng lẻ**

Nếu bạn có thông tin riêng lẻ từ Railway:
```
DATABASE_URL=jdbc:mysql://<railway_host>:<railway_port>/<railway_database>?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=<railway_username>
DATABASE_PASSWORD=<railway_password>
```

**Ví dụ thực tế:**
```
DATABASE_URL=jdbc:mysql://yamabiko.proxy.rlwy.net:39229/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=root
DATABASE_PASSWORD=nMmSWQCmhNCuqpeWWyKNxFpCpAvmOoft
```

⚠️ **Lưu ý quan trọng:**
- Railway MySQL có thể cần **Public Networking** enabled để Render có thể kết nối
- Kiểm tra Railway MySQL service → Settings → Networking → Public Networking = ON
- Đảm bảo `allowPublicKeyRetrieval=true` trong connection string

### Application URLs
```
APP_BASE_URL=https://your-app-name.onrender.com
FRONTEND_URL=https://your-frontend-url.com
CORS_ALLOWED_ORIGINS=https://your-frontend-url.com,http://localhost:3000
```

### JWT Configuration
```
JWT_SECRET=<generate-a-strong-random-secret-64-chars>
JWT_EXPIRATION=86400000
```

### Email Configuration (Optional - nếu cần)
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Logging (Optional)
```
SHOW_SQL=false
LOG_SQL=INFO
LOG_SQL_BINDER=INFO
```

## 4. Persistent Disk cho Uploads (Quan trọng!)

⚠️ **Lưu ý quan trọng**: Render không persist data trong container filesystem. Khi container restart, thư mục `uploads` sẽ bị mất!

### Giải pháp:

#### Option 1: Sử dụng Render Disk (Recommended)
1. Trong Render Dashboard, thêm **Disk**:
   - Name: `uploads-disk`
   - Mount Path: `/app/uploads`
   - Size: 1GB (hoặc tùy nhu cầu)

2. Update Dockerfile để mount disk:
```dockerfile
# Thêm vào Dockerfile sau WORKDIR /app
VOLUME ["/app/uploads"]
```

#### Option 2: Sử dụng Cloud Storage (S3, Cloudinary, etc.)
- Tích hợp S3 hoặc Cloudinary để lưu files
- Cần update code để upload lên cloud storage thay vì local filesystem

#### Option 3: Sử dụng Database để lưu files (không khuyến nghị)
- Lưu file dưới dạng BLOB trong database (không hiệu quả)

## 5. Health Check

Render sẽ tự động check health endpoint. Đảm bảo:
- Port được expose đúng (8080 hoặc PORT từ env)
- Health check endpoint hoạt động (nếu có)

## 6. Build & Deploy

1. Push code lên GitHub/GitLab
2. Render sẽ tự động build và deploy
3. Xem logs trong Render Dashboard để debug

## 7. Troubleshooting

### Lỗi Database Connection
- ✅ Kiểm tra `DATABASE_URL` format đúng JDBC: `jdbc:mysql://host:port/db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
- ✅ Kiểm tra Railway MySQL có **Public Networking** enabled (Settings → Networking)
- ✅ Kiểm tra username/password đúng từ Railway
- ✅ Đảm bảo `allowPublicKeyRetrieval=true` trong connection string
- ✅ Kiểm tra port và host đúng từ Railway connection string
- ⚠️ Railway có thể block connection từ Render nếu Public Networking chưa bật

### Lỗi Port
- Render tự động set biến `PORT`, đảm bảo `server.port=${PORT:8080}` trong properties

### Files bị mất sau restart
- Phải dùng Render Disk hoặc Cloud Storage (xem mục 4)

### CORS Error
- Kiểm tra `CORS_ALLOWED_ORIGINS` có đúng frontend URL không
- Đảm bảo không có trailing slash

## 8. Example Environment Variables (Railway MySQL)

```bash
# Database (Từ Railway)
DATABASE_URL=jdbc:mysql://yamabiko.proxy.rlwy.net:39229/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_railway_mysql_password

# URLs
APP_BASE_URL=https://campuslife-backend.onrender.com
FRONTEND_URL=https://campuslife-frontend.vercel.app
CORS_ALLOWED_ORIGINS=https://campuslife-frontend.vercel.app,http://localhost:3000

# JWT
JWT_SECRET=your-very-long-and-random-secret-key-at-least-64-characters-long-for-security
JWT_EXPIRATION=86400000

# Email (optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=projectltweb@gmail.com
MAIL_PASSWORD=your-app-password

# Logging
SHOW_SQL=false
LOG_SQL=INFO
```

## 9. Notes

- ✅ Render free tier có thể sleep sau 15 phút không có traffic (upgrade plan để tránh)
- ✅ Database đang dùng Railway MySQL (không bị ảnh hưởng bởi Render free tier)
- ✅ Nên backup database định kỳ trên Railway
- ✅ Railway MySQL cần **Public Networking** enabled để Render có thể kết nối
- ⚠️ Files trong `uploads/` sẽ bị mất khi container restart (xem mục 4 để giải quyết)

