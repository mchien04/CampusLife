## Rule: API-First Documentation

Khi viết bất kỳ chức năng nào, phải tuân theo cấu trúc sau:

### 1. Mô tả nghiệp vụ

\[Brief description of the business operation]

### 2. API Endpoint

- **Method:** GET/POST/PUT/DELETE/PATCH
- **Path:** /api/{version}/{resource}/{action}
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

### **4. Response**

- **Success (200/201):**

  json
  ```
  {
    "code": 200,
    "message": "success",
    "data": { ... }
  }
  ```
- **Error Responses:** \[list common errors with status codes]
- <br />

  <br />

