# Sequence Diagram - Chức năng Đăng xuất

## Mô tả
Sequence diagram mô tả luồng xử lý đăng xuất trong hệ thống CampusLife sử dụng JWT stateless authentication.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Client/Frontend
    participant LocalStorage as Local Storage
    participant Database as Database

    Note over Client: User clicks logout button
    
    Note over Client: Xóa token và user info
    Client->>LocalStorage: removeItem("token")
    LocalStorage-->>Client: Token removed
    
    Client->>Client: Clear user state/context
    
    Note over Client: Redirect về trang login
    Client->>Client: Navigate to /login
    
    Note over Client,Database: Logout chỉ xử lý ở client<br/>Không có tương tác với database
    Note over Client: Token vẫn còn hiệu lực trên server<br/>nhưng client không gửi nữa
```

## Các thành phần tham gia

1. **Client/Frontend**: Giao diện người dùng thực hiện logout
2. **Local Storage**: Nơi lưu trữ token ở phía client (browser)
3. **Database**: Cơ sở dữ liệu (không có tương tác trong logout stateless)

## Các bước xử lý

1. User click logout button
2. Xóa token khỏi local storage
3. Xóa user info khỏi application state
4. Redirect về trang login

## Đặc điểm

- **Stateless**: Logout chỉ xử lý ở client, không cần tương tác với server
- **Token vẫn hiệu lực**: Token vẫn còn hiệu lực trên server cho đến khi hết hạn, nhưng client không gửi token nữa

