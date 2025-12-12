# Quick Start: Deploy lên Render (MySQL từ Railway)

## Checklist nhanh

### 1. Chuẩn bị thông tin Railway MySQL
- [ ] Lấy connection string từ Railway Dashboard
- [ ] Bật **Public Networking** trong Railway MySQL settings
- [ ] Lưu: host, port, database, username, password

### 2. Tạo Web Service trên Render
- [ ] New → Web Service
- [ ] Connect GitHub/GitLab repo
- [ ] Environment: **Docker**
- [ ] Region: Singapore (hoặc gần nhất)
- [ ] Dockerfile Path: `Dockerfile`

### 3. Set Environment Variables trên Render

**Bắt buộc:**
```bash
# Database (từ Railway)
DATABASE_URL=jdbc:mysql://<railway_host>:<railway_port>/<railway_db>?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=<railway_username>
DATABASE_PASSWORD=<railway_password>

# URLs (sau khi deploy xong, update lại)
APP_BASE_URL=https://your-app-name.onrender.com
FRONTEND_URL=https://your-frontend-url.com
CORS_ALLOWED_ORIGINS=https://your-frontend-url.com,http://localhost:3000

# JWT (tạo secret mạnh)
JWT_SECRET=<generate-strong-secret-64-chars>
```

**Tùy chọn:**
```bash
# Email (nếu cần)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Logging (production nên tắt)
SHOW_SQL=false
LOG_SQL=INFO
LOG_SQL_BINDER=INFO
```

### 4. Persistent Storage cho Uploads (Quan trọng!)

⚠️ **Files sẽ bị mất khi container restart!**

**Option 1: Render Disk (Recommended)**
- [ ] Trong Render Dashboard → Add Disk
- [ ] Mount Path: `/app/uploads`
- [ ] Size: 1GB (hoặc tùy nhu cầu)
- [ ] Uncomment `VOLUME ["/app/uploads"]` trong Dockerfile

**Option 2: Cloud Storage** (cần update code)

### 5. Deploy
- [ ] Push code lên GitHub/GitLab
- [ ] Render tự động build và deploy
- [ ] Check logs trong Render Dashboard
- [ ] Test API endpoint

## Ví dụ Environment Variables

```bash
# Railway MySQL
DATABASE_URL=jdbc:mysql://yamabiko.proxy.rlwy.net:39229/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_railway_password

# URLs
APP_BASE_URL=https://campuslife-backend.onrender.com
FRONTEND_URL=https://campuslife-frontend.vercel.app
CORS_ALLOWED_ORIGINS=https://campuslife-frontend.vercel.app,http://localhost:3000

# JWT
JWT_SECRET=your-very-long-random-secret-key-at-least-64-characters
JWT_EXPIRATION=86400000
```

## Troubleshooting

**Lỗi kết nối database:**
- ✅ Check Railway MySQL → Settings → Networking → Public Networking = ON
- ✅ Check DATABASE_URL format đúng JDBC
- ✅ Check username/password đúng

**Files bị mất:**
- ✅ Phải dùng Render Disk hoặc Cloud Storage

**CORS error:**
- ✅ Check CORS_ALLOWED_ORIGINS có đúng frontend URL không

Xem chi tiết trong `RENDER_DEPLOYMENT_GUIDE.md`

