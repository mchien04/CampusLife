# Hướng Dẫn Frontend - QR Code Check-in

## Tổng Quan

Hệ thống hỗ trợ chức năng quét QR code để điểm danh sự kiện. Khi sinh viên quét QR code từ ban tổ chức, hệ thống sẽ tự động set trạng thái thành `ATTENDED` (bỏ qua bước check-in 2 lần như bình thường).

## 1. Model Updates

### 1.1. ActivityResponse

Thêm field `checkInCode` vào model `Activity`:

```typescript
interface ActivityResponse {
  id: number;
  name: string;
  // ... các field khác
  contactInfo?: string;
  checkInCode?: string; // ← Mã QR code unique của activity
  requiresApproval: boolean;
  // ...
}
```

**Lưu ý:**
- `checkInCode` có format: `ACT-{activityId}-{8 ký tự random}` (ví dụ: `ACT-000123-A7B9C2D1`)
- `checkInCode` được tự động generate khi tạo activity mới
- Các activity cũ chưa có `checkInCode` sẽ có giá trị `null` hoặc `undefined`

## 2. API Endpoints

### 2.1. Check-in bằng QR Code

**Endpoint:** `POST /api/registrations/checkin/qr`

**Authentication:** Required (STUDENT, ADMIN, MANAGER)

**Request Body:**
```json
{
  "checkInCode": "ACT-000123-A7B9C2D1"
}
```

**Response Success (200):**
```json
{
  "status": true,
  "message": "Điểm danh thành công bằng QR code",
  "data": {
    "id": 123,
    "registrationId": 456,
    "activityId": 1,
    "activityName": "Sự kiện ABC",
    "studentId": 789,
    "studentName": "Nguyễn Văn A",
    "participationType": "ATTENDED",
    "isCompleted": true,
    "pointsEarned": 10.0,
    "checkInTime": "2025-12-05T10:30:00",
    "checkOutTime": "2025-12-05T10:30:00",
    "date": "2025-12-05T10:30:00"
  }
}
```

**Response Error (400):**
```json
{
  "status": false,
  "message": "Không tìm thấy activity với mã QR này",
  "data": null
}
```

**Các lỗi có thể xảy ra:**
- `"checkInCode is required"` - Thiếu checkInCode trong request
- `"Student not found"` - Không tìm thấy thông tin sinh viên từ authentication
- `"Không tìm thấy activity với mã QR này"` - CheckInCode không tồn tại
- `"Activity chưa được công bố"` - Activity đang ở trạng thái draft
- `"Bạn chưa đăng ký hoặc chưa được duyệt tham gia activity này"` - Sinh viên chưa đăng ký hoặc registration chưa được APPROVED
- `"Bạn đã điểm danh activity này rồi"` - Đã check-in trước đó

### 2.2. Backfill CheckInCodes (Admin/Manager only)

**Endpoint:** `POST /api/activities/backfill-checkin-codes`

**Authentication:** Required (ADMIN, MANAGER only)

**Request Body:** Không cần

**Response Success (200):**
```json
{
  "status": true,
  "message": "Đã tạo checkInCode cho 15 activity",
  "data": {
    "updatedCount": 15,
    "totalActivities": 15
  }
}
```

**Response khi tất cả đã có code:**
```json
{
  "status": true,
  "message": "Tất cả activities đã có checkInCode",
  "data": null
}
```

**Lưu ý:** Endpoint này dùng để tạo checkInCode cho các activity đã tồn tại nhưng chưa có code. Chỉ cần gọi 1 lần sau khi deploy tính năng mới.

## 3. UI Components

### 3.1. QR Code Scanner Component

Tạo component để quét QR code:

```typescript
import { useState } from 'react';
import { useQRCodeScanner } from './hooks/useQRCodeScanner';

const QRCodeCheckIn = () => {
  const [checkInCode, setCheckInCode] = useState<string>('');
  const { scanQRCode, isScanning, error } = useQRCodeScanner();

  const handleScan = async (code: string) => {
    try {
      const response = await checkInByQrCode(code);
      // Show success message
      alert('Điểm danh thành công!');
    } catch (err) {
      // Show error message
      alert(err.message);
    }
  };

  return (
    <div>
      <QRCodeScanner onScan={handleScan} />
      {/* Hoặc input thủ công */}
      <input
        type="text"
        value={checkInCode}
        onChange={(e) => setCheckInCode(e.target.value)}
        placeholder="Nhập mã QR code"
      />
      <button onClick={() => handleScan(checkInCode)}>
        Điểm danh
      </button>
    </div>
  );
};
```

### 3.2. Activity Detail - Hiển thị CheckInCode

Trong trang chi tiết activity (Admin/Manager), hiển thị checkInCode để in QR code:

```typescript
const ActivityDetail = ({ activity }: { activity: ActivityResponse }) => {
  const generateQRCode = () => {
    if (!activity.checkInCode) {
      alert('Activity chưa có checkInCode. Vui lòng liên hệ admin.');
      return;
    }
    // Generate QR code image từ checkInCode
    // Có thể dùng thư viện như qrcode.react hoặc qrcode.js
  };

  return (
    <div>
      <h1>{activity.name}</h1>
      {/* ... các thông tin khác */}
      
      {activity.checkInCode && (
        <div>
          <p>Mã QR Code: <strong>{activity.checkInCode}</strong></p>
          <button onClick={generateQRCode}>Tạo QR Code để in</button>
          <QRCode value={activity.checkInCode} size={200} />
        </div>
      )}
      
      {!activity.checkInCode && (
        <div className="warning">
          Activity này chưa có checkInCode. 
          Vui lòng gọi API backfill để tạo code.
        </div>
      )}
    </div>
  );
};
```

## 4. API Service Functions

### 4.1. Check-in bằng QR Code

```typescript
// services/activityRegistrationService.ts

export const checkInByQrCode = async (checkInCode: string): Promise<ActivityParticipationResponse> => {
  const response = await fetch('/api/registrations/checkin/qr', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getAuthToken()}`,
    },
    body: JSON.stringify({ checkInCode }),
  });

  const data = await response.json();
  
  if (!data.status) {
    throw new Error(data.message || 'Failed to check-in by QR code');
  }

  return data.data;
};
```

### 4.2. Backfill CheckInCodes (Admin/Manager)

```typescript
// services/activityService.ts

export const backfillCheckInCodes = async (): Promise<{ updatedCount: number; totalActivities: number }> => {
  const response = await fetch('/api/activities/backfill-checkin-codes', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });

  const data = await response.json();
  
  if (!data.status) {
    throw new Error(data.message || 'Failed to backfill checkInCodes');
  }

  return data.data;
};
```

## 5. Flow Hoàn Chỉnh

### 5.1. Flow Admin/Manager tạo Activity

1. Admin/Manager tạo activity mới
2. Hệ thống tự động generate `checkInCode`
3. Response trả về activity với `checkInCode`
4. Admin/Manager có thể:
   - Xem `checkInCode` trong trang chi tiết activity
   - Generate QR code từ `checkInCode`
   - In QR code để đặt tại sự kiện

### 5.2. Flow Sinh viên quét QR Code

1. Sinh viên mở app và chọn chức năng "Quét QR Code"
2. Quét QR code từ ban tổ chức (hoặc nhập thủ công)
3. App gửi request `POST /api/registrations/checkin/qr` với `checkInCode`
4. Hệ thống:
   - Tìm activity theo `checkInCode`
   - Tìm registration của sinh viên (từ authentication)
   - Set trực tiếp thành `ATTENDED` (bỏ qua CHECKED_IN và CHECKED_OUT)
   - Xử lý điểm tự động (giống check-out bình thường)
5. Trả về response thành công với thông tin participation

### 5.3. Flow Backfill (Một lần duy nhất)

1. Sau khi deploy tính năng mới, Admin/Manager gọi API `POST /api/activities/backfill-checkin-codes`
2. Hệ thống tìm tất cả activities chưa có `checkInCode`
3. Generate `checkInCode` cho từng activity
4. Trả về số lượng activity đã được cập nhật

## 6. Khác Biệt với Check-in Bình Thường

### Check-in Bình Thường (2 lần):
1. **Lần 1:** REGISTERED → CHECKED_IN (check-in)
2. **Lần 2:** CHECKED_IN → CHECKED_OUT → ATTENDED (check-out)

### QR Code Check-in (1 lần):
1. **Một lần:** REGISTERED → ATTENDED (tự động, bỏ qua CHECKED_IN và CHECKED_OUT)

**Lưu ý:** Cả hai cách đều xử lý điểm giống nhau (tự động tính điểm nếu activity không yêu cầu submission).

## 7. Error Handling

### 7.1. CheckInCode không tồn tại
```typescript
try {
  await checkInByQrCode('INVALID-CODE');
} catch (error) {
  if (error.message.includes('Không tìm thấy activity')) {
    // Hiển thị thông báo: "Mã QR code không hợp lệ"
  }
}
```

### 7.2. Sinh viên chưa đăng ký
```typescript
try {
  await checkInByQrCode('ACT-000123-A7B9C2D1');
} catch (error) {
  if (error.message.includes('chưa đăng ký')) {
    // Hiển thị thông báo: "Bạn chưa đăng ký sự kiện này"
    // Có thể redirect đến trang đăng ký
  }
}
```

### 7.3. Đã check-in rồi
```typescript
try {
  await checkInByQrCode('ACT-000123-A7B9C2D1');
} catch (error) {
  if (error.message.includes('đã điểm danh')) {
    // Hiển thị thông báo: "Bạn đã điểm danh sự kiện này rồi"
  }
}
```

## 8. Testing

### 8.1. Test với cURL

**Check-in bằng QR code:**
```bash
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000123-A7B9C2D1"
}'
```

**Backfill checkInCodes:**
```bash
curl --location 'http://localhost:8080/api/activities/backfill-checkin-codes' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--request POST
```

## 9. Best Practices

1. **Validate checkInCode format:** Kiểm tra format `ACT-{id}-{random}` trước khi gửi request
2. **Show loading state:** Hiển thị loading khi đang xử lý quét QR code
3. **Handle offline:** Lưu checkInCode vào local storage nếu mất kết nối, sync lại sau
4. **QR Code size:** Đảm bảo QR code đủ lớn để dễ quét (tối thiểu 200x200px)
5. **Error messages:** Hiển thị thông báo lỗi rõ ràng, dễ hiểu cho người dùng

## 10. Dependencies (Optional)

Nếu cần generate QR code image từ checkInCode, có thể dùng các thư viện:

- **React:** `qrcode.react` hoặc `react-qr-code`
- **Vue:** `vue-qr` hoặc `qrcode.vue`
- **Angular:** `angularx-qrcode`
- **Vanilla JS:** `qrcode.js`

**Ví dụ với React:**
```bash
npm install qrcode.react
```

```typescript
import QRCode from 'qrcode.react';

<QRCode value={activity.checkInCode} size={200} />
```

---

## Tóm Tắt

- ✅ Activity có field `checkInCode` (tự động generate khi tạo mới)
- ✅ Endpoint `POST /api/registrations/checkin/qr` để check-in bằng QR code
- ✅ Endpoint `POST /api/activities/backfill-checkin-codes` để tạo code cho activity cũ
- ✅ QR code check-in tự động set thành ATTENDED (1 lần thay vì 2 lần)
- ✅ Xử lý điểm tự động giống check-out bình thường

