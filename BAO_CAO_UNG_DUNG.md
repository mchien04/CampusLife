# 📱 BÁO CÁO SƠ LƯỢC - ỨNG DỤNG CAMPUSLIFE (ANDROID)

**Ngày cập nhật:** 15/03/2026  
**Loại dự án:** Mobile Application (Android)  
**Ngôn ngữ lập trình:** Java  
**Framework chính:** Android SDK, Retrofit 2, Firebase

---

## 📌 GIỚI THIỆU CHUNG

**CampusLife** là một ứng dụng mobile dành cho sinh viên đại học, giúp quản lý và tham gia các hoạt động ngoại khóa như:
- Các sự kiện/hoạt động của trường
- Rèn luyện/tích lũy điểm
- Vé tham dự sự kiện
- Báo cáo hoạt động
- Các trò chơi mini
- Xem thông báo từ nhà trường

---

## 🏗️ KIẾN TRÚC DỰ ÁN

Ứng dụng được tổ chức theo mô hình **MVC (Model-View-Controller)** với cấu trúc thư mục rõ ràng:

```
CampusLifeAndroid/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/campuslife/
│   │       │   ├── activity/           [Các màn hình chính]
│   │       │   ├── fragment/           [Các phần giao diện con]
│   │       │   ├── adapter/            [Hiển thị dữ liệu trong RecyclerView]
│   │       │   ├── api/                [Giao tiếp với backend server]
│   │       │   ├── entity/             [Mô hình dữ liệu]
│   │       │   ├── auth/               [Quản lý xác thực người dùng]
│   │       │   ├── fcm/                [Thông báo đẩy - Firebase Cloud Messaging]
│   │       │   ├── utils/              [Tiện ích hỗ trợ]
│   │       │   ├── data/               [Quản lý dữ liệu cục bộ]
│   │       │   └── item/               [Các item layout nhỏ]
│   │       └── res/                    [Tài nguyên (layout, drawable, string)]
│   └── build.gradle                    [Cấu hình biên dịch]
└── gradle/                             [Gradle wrapper]
```

---

## 🎯 CÁC CHỨC NĂNG CHÍNH

### 1️⃣ **XÁC THỰC & NGƯỜI DÙNG** 🔐
| Chức năng | Mô tả |
|-----------|-------|
| **Đăng nhập** | Người dùng nhập username/password |
| **Đăng ký** | Tạo tài khoản mới (nếu có) |
| **Quên mật khẩu** | Yêu cầu reset mật khẩu |
| **Token JWT** | Lưu trữ token xác thực an toàn |

**Files liên quan:**
- `LoginActivity.java` - Màn hình đăng nhập
- `ForgotActivity.java` - Màn hình quên mật khẩu
- `TokenStore.java` - Lưu token
- `AuthApi.java` - API xác thực

---

### 2️⃣ **QUẢN LÝ HOẠT ĐỘNG** 📅

#### **Xem danh sách hoạt động**
- Hiển thị tất cả sự kiện/hoạt động của trường
- Lọc theo loại (Training, Social, Business)
- Tìm kiếm hoạt động
- Xem chi tiết hoạt động

**Activities:**
- `ListActivity.java` - Danh sách hoạt động
- `EventDetailActivity.java` - Chi tiết hoạt động
- `SearchActivity.java` - Tìm kiếm

#### **Đăng ký tham gia**
- Người dùng có thể đăng ký tham gia hoạt động
- Kiểm tra thời hạn đăng ký
- Hiển thị số chỗ còn trống
- Phản hồi ngay lập tức

**APIs:**
- `RegistrationApi.java` - API đăng ký
- `ActivityApi.java` - API hoạt động

---

### 3️⃣ **HỆ THỐNG ĐIỂM & ĐIỂM RÈN LUYỆN** ⭐

#### **Xem điểm**
- Tính lũy điểm từ các hoạt động
- 3 loại điểm:
  - **REN_LUYEN**: Điểm rèn luyện
  - **CONG_TAC_XA_HOI**: Điểm công tác xã hội
  - **KHAC**: Điểm khác

#### **Lịch sử điểm**
- Xem chi tiết từng hoạt động đã tham gia
- Hiển thị ngày, tên hoạt động, điểm nhận được

**Files:**
- `ScoreFragment.java` - Hiển thị điểm
- `ScoreApi.java` - API điểm

---

### 4️⃣ **VÉ & ĐẶT CHỖ** 🎫

#### **Quản lý vé**
- Xem vé đã mua/đặt
- Chi tiết vé (số hiệu, ngày, sự kiện)
- Trạng thái vé (chưa sử dụng, đã sử dụng)

#### **Quét mã QR (Check-in)**
- Sử dụng camera quét QR code
- Xác nhận tham dự sự kiện
- Hiển thị kết quả check-in

**Activities:**
- `TicketDetailActivity.java` - Chi tiết vé
- `ScanQRActivity.java` - Quét QR code
- `CheckinResultActivity.java` - Kết quả check-in

---

### 5️⃣ **BÁO CÁO HOẠT ĐỘNG** 📝

#### **Nộp báo cáo**
- Người dùng nộp báo cáo sau khi tham dự
- Điền thông tin chi tiết
- Upload ảnh/tài liệu
- Gửi feedback

**Activities:**
- `ReportActivity.java` - Danh sách báo cáo
- `SubmitReportActivity.java` - Nộp báo cáo
- `FeedbackActivity.java` - Gửi feedback

---

### 6️⃣ **TRÌNH CHIẾN MINI GAME** 🎮

#### **Trò chơi trắc nghiệm**
- Một số hoạt động có trò chơi đi kèm
- Trả lời câu hỏi trắc nghiệm
- Tích lũy điểm từ trò chơi
- Xem kết quả và bảng xếp hạng

**Activities:**
- `MiniGameActivity.java` - Trò chơi
- `MiniGameResultActivity.java` - Kết quả trò chơi

---

### 7️⃣ **QUẢN LÝ THÔNG BÁO** 🔔

#### **Nhận thông báo**
- Thông báo từ Firebase Cloud Messaging (FCM)
- Các bản cập nhật từ nhà trường
- Nhắc nhở về hoạt động sắp diễn ra

**Files:**
- `NotificationActivity.java` - Xem thông báo
- `MyFirebaseMessagingService.java` - Xử lý FCM

---

### 8️⃣ **HỒ SƠ NGƯỜI DÙNG** 👤

#### **Quản lý hồ sơ**
- Xem thông tin cá nhân
- Cập nhật ảnh đại diện
- Xem lịch sử tham gia
- Quản lý tài khoản

**Activities:**
- `ProfileFragment.java` - Trang hồ sơ
- `ProfileDetailActivity.java` - Chi tiết hồ sơ

---

### 9️⃣ **CÁC CHỨC NĂNG KHÁC**

| Chức năng | Mô tả | File |
|-----------|-------|------|
| **Lịch** | Xem sự kiện theo lịch | `CalendarActivity.java` |
| **Bài viết** | Xem bài viết từ các CLB/phòng ban | `ArticleFragment.java` |
| **Chuỗi sự kiện** | Xem các sự kiện liên tiếp | `SeriesDetailActivity.java` |
| **Ảnh sự kiện** | Xem thư viện ảnh hoạt động | `PhotoViewerActivity.java` |
| **Trang chủ** | Dashboard chính | `HomeFragment.java` |

---

## 📡 GIAO TIẾP VỚI BACKEND (API)

Ứng dụng sử dụng **Retrofit 2** để giao tiếp RESTful API với backend server.

### **Cấu hình kết nối**
```
Base URL: http://10.0.2.2:8080/ (Android Emulator)
Timeout: 20 giây
Logging: Chi tiết (Debug) hoặc Cơ bản (Release)
```

### **Danh sách các API chính**

| API | Mục đích | Methods |
|-----|---------|---------|
| **AuthApi** | Xác thực | Login, Logout, Refresh Token |
| **ActivityApi** | Hoạt động | List, Detail, Search |
| **RegistrationApi** | Đăng ký | Register, Unregister |
| **ScoreApi** | Điểm | Get Score, History |
| **TicketApi** | Vé | Get Tickets, Detail |
| **ReportAPI** | Báo cáo | Submit Report |
| **FeedbackApi** | Feedback | Submit Feedback |
| **ProfileAPI** | Hồ sơ | Get Profile, Update |
| **MiniGameApi** | Trò chơi | Get Questions, Submit Answers |
| **NotificationsApi** | Thông báo | Get Notifications |
| **DepartmentApi** | Phòng ban | Get Departments |
| **PhotoApi** | Ảnh | Get Photos |

---

## 🔐 QUẢN LÝ XÁC THỰC

### **Cơ chế Token JWT**
1. Người dùng đăng nhập → nhận JWT token
2. Token được lưu trong `SharedPreferences` (bảo mật)
3. Mỗi request gửi token trong header: `Authorization: Bearer {token}`
4. Khi token hết hạn → tự động refresh

**Files:**
- `AuthInterceptor.java` - Thêm token vào request
- `RefreshAuthenticator.java` - Làm mới token
- `TokenStore.java` - Lưu/lấy token

---

## 📦 CÔNG NGHỆ & THƯ VIỆN

### **Android Components**
- Fragment, Activity, RecyclerView
- Material Design Components
- Constraint Layout

### **Networking**
- Retrofit 2 - HTTP client
- OkHttp 3 - HTTP interceptor
- GSON - JSON serialization

### **Firebase**
- Firebase Cloud Messaging (FCM) - Push notifications
- Firebase Analytics

### **Image Loading**
- Glide - Load & cache ảnh

### **Other**
- Androidx (AndroidX)
- Material Components

---

## 🔄 LUỒNG HOẠT ĐỘNG CHÍNH

### **Luồng Đăng Nhập**
```
SplashActivity (1-2s)
    ↓
OnboardingActivity (nếu lần đầu)
    ↓
LoginActivity (nhập username/password)
    ↓
Verify Token → MainActivity (nếu OK)
```

### **Luồng Xem Hoạt Động & Đăng Ký**
```
HomeFragment → Xem danh sách hoạt động
    ↓
Click hoạt động → EventDetailActivity
    ↓
Hiển thị chi tiết (tên, địa điểm, thời gian, người tổ chức)
    ↓
Click "Join in" → Gọi RegistrationApi
    ↓
Thành công → Cập nhật UI (Joined)
    ↓
Nếu MINIGAME → Chuyển sang MiniGameActivity
```

### **Luồng Quét QR Check-in**
```
ListActivity (danh sách vé)
    ↓
Click vé → TicketDetailActivity
    ↓
Click "Scan QR" → ScanQRActivity (mở camera)
    ↓
Quét mã QR → Gửi CheckInQrRequest
    ↓
Server xác nhận → CheckinResultActivity (hiển thị kết quả)
```

---

## 🗂️ CÁCH TỔअ CHỨC MÃ SOURCE

### **Thư mục Activity**
Chứa các màn hình chính:
- `MainActivity.java` - Màn hình chính (Bottom Navigation + Fragments)
- `LoginActivity.java` - Đăng nhập
- `SplashActivity.java` - Khởi động (hiển thị logo)
- `EventDetailActivity.java` - Chi tiết hoạt động
- ... (23 Activities tổng cộng)

### **Thư mục Fragment**
Các phần giao diện cho MainActivity:
- `HomeFragment.java` - Trang chủ
- `ProfileFragment.java` - Hồ sơ
- `ScoreFragment.java` - Điểm
- `ArticleFragment.java` - Bài viết
- `TicketFragment.java` - Vé

### **Thư mục API**
Giao tiếp RESTful:
- `ApiClient.java` - Tạo Retrofit instances
- `ApiResponse.java` - Wrapper response chung
- `*Api.java` - Các interface API

### **Thư mục Entity**
Mô hình dữ liệu:
- `Activity.java` - Dữ liệu hoạt động
- `User.java` - Dữ liệu người dùng
- `Student.java` - Dữ liệu sinh viên
- ... (44 entity classes)

### **Thư mục Adapter**
Hiển thị dữ liệu:
- `ActivityReportAdapter.java` - Danh sách báo cáo
- `PhotoAdapter.java` - Danh sách ảnh
- ... (các adapter khác)

### **Thư mục Auth**
Quản lý xác thực:
- `TokenStore.java` - Lưu/lấy token
- `AuthInterceptor.java` - Thêm token vào request
- `RefreshAuthenticator.java` - Refresh token khi hết hạn

### **Thư mục FCM**
Thông báo đẩy:
- `MyFirebaseMessagingService.java` - Xử lý FCM message

### **Thư mục Utils**
Các hàm tiện ích:
- Format ngày tháng
- Validate dữ liệu
- Log helper
- v.v.

---

## 🔌 DEPENDENCIES CHÍNH

```gradle
// Android
androidx.appcompat:appcompat
androidx.constraintlayout:constraintlayout
com.google.android.material:material

// Networking
com.squareup.retrofit2:retrofit
com.squareup.retrofit2:converter-gson
com.squareup.okhttp3:okhttp
com.squareup.okhttp3:logging-interceptor

// Image Loading
com.github.bumptech.glide:glide

// Firebase
com.google.firebase:firebase-messaging
com.google.firebase:firebase-analytics

// Others
androidx.recyclerview:recyclerview
```

---

## 🚀 QGUI DIỄN ĐẠI LOẠI

### **Màu sắc chính:**
- Primary Color: Xanh lá/Xanh dương (theme chính)
- Secondary Color: Xanh nhạt
- Accent Color: Đỏ (cảnh báo)
- Nền: Trắng

### **Kiểu chữ:**
- Material Design Typography
- Font mặc định Android

### **Layouts:**
- ConstraintLayout (chính)
- LinearLayout (hỗ trợ)
- RecyclerView (danh sách)
- CardView (card thẻ)

---

## 📊 THỐNG KÊ DỰ ÁN

| Chỉ số | Giá trị |
|-------|--------|
| **Tổng Activities** | 23 |
| **Tổng Fragments** | 5 |
| **Tổng APIs** | 20+ |
| **Tổng Entities** | 44 |
| **Tổng Adapters** | 10+ |
| **Android Min API** | 21 |
| **Target API** | 34 |
| **Hỗ trợ Firebase** | ✅ Có |
| **Hỗ trợ RTL (Tiếng Ả Rập)** | ✅ Có |

---

## 🐛 VẤN ĐỀ & GIẢI PHÁP

### **Vấn đề kết nối hiện tại**
**Triệu chứng:** Timeout khi đăng nhập

**Nguyên nhân:** URL backend sai cho Android Emulator
- URL cũ: `http://172.21.13.137:8080/`
- URL đúng: `http://10.0.2.2:8080/`

**Giải pháp:** Chỉnh sửa `build.gradle` (xem QUICK_START.md)

---

## 📚 TÓMAN

CampusLife là một ứng dụng hoàn chỉnh cho quản lý hoạt động ngoại khóa của sinh viên với:
- ✅ Quản lý hoạt động & đăng ký
- ✅ Hệ thống điểm & điểm rèn luyện
- ✅ Quét QR code check-in
- ✅ Mini game trắc nghiệm
- ✅ Thông báo real-time (Firebase)
- ✅ Báo cáo hoạt động
- ✅ Hồ sơ & lịch sử tham gia
- ✅ Giao diện Material Design đẹp

Codebase được tổ chức rõ ràng theo từng chức năng, dễ mở rộng và bảo trì.

---

**Tài liệu này được tạo tự động từ mã nguồn. Cập nhật lần cuối: 15/03/2026**
