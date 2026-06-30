
Khi viết bất kỳ chức năng nào, phải tuân theo cấu trúc sau:

### 1. Mô tả nghiệp vụ

\[Brief description of the business operation]

### 2. API Endpoint

- **Method:** GET/POST/PUT/DELETE/PATCH
- **Path:** `/api/{module}/{resource}` hoặc `/api/{module}/{resource}/{id}`
- **Nested Path (nếu có):** `/api/{module}/{resource}/{id}/{sub-resource}`
- **Action Path (nếu có):** `/api/{module}/{resource}/{id}/{action}`
- **Versioning:** Backend hiện tại **không** mặc định dùng `/api/v1/...`; chỉ ghi version nếu endpoint thực sự có version trong code
- **Authentication:** \[Required/Not required] + \[Role requirements]

### 3. Request

- **Path Parameters:** \[list with type and description]
- **Query Parameters:** \[list with type and description]
- **Request Body:**
  ```json
  {
    "field1": "type - description",
    "field2": "type - description"
  }
  ```

### 4. Response

- **Success (200/201):**

  ```json
  {
    "status": true,
    "message": "success",
    "body": { ... }
  }
  ```
- **Error (JSON wrapper - đa số endpoint):**

  ```json
  {
    "status": false,
    "message": "error message",
    "body": null
  }
  ```
- **Non-standard Responses (phải ghi đúng theo controller thực tế):**
    - Raw DTO/List/Page:
      ```json
      {
        "field1": "value",
        "field2": "value"
      }
      ```
    - File download: mô tả `Content-Type`, `Content-Disposition`, kiểu dữ liệu trả về (`byte[]`/binary)
    - Empty body: dùng cho một số response như `204 No Content`, hoặc một số `401/404` trả về không có JSON body
- **Error Responses:** \[liệt kê các status code phổ biến như 400, 401, 403, 404, 409, 500 và body thực tế nếu có]

### 5. Documentation Notes

- Ưu tiên mô tả **đúng contract hiện tại của backend**, không ép tất cả endpoint về cùng một mẫu nếu controller trả kiểu khác
- Với các endpoint dùng wrapper `Response`, luôn dùng tên field thực tế là `status`, `message`, `body`
- Với các endpoint không dùng wrapper `Response`, phải ghi rõ response schema riêng của endpoint đó

