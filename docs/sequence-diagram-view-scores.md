# Sequence Diagram - Chức năng Xem Điểm

## Mô tả
Sequence diagram mô tả luồng xử lý xem điểm sinh viên trong hệ thống CampusLife. Bao gồm các chức năng xem điểm chi tiết, tổng điểm và bảng xếp hạng.

## Sequence Diagrams

### 1. Xem điểm chi tiết (View Scores)

```mermaid
sequenceDiagram
    participant User as Student/Admin/Manager
    participant Client as Client/Frontend
    participant ScoreController as ScoreController
    participant ScoreService as ScoreServiceImpl
    participant Repository as Repository
    participant Database as Database

    User->>Client: Xem điểm theo học kỳ
    Client->>ScoreController: GET /api/scores/student/{studentId}/semester/{semesterId}
    
    ScoreController->>ScoreService: viewScores(studentId, semesterId)
    
    ScoreService->>Repository: findByStudentAndSemester(studentId, semesterId)
    Repository->>Database: SELECT * FROM student_scores<br/>WHERE student_id = ? AND semester_id = ?
    Database-->>Repository: List<StudentScore>
    Repository-->>ScoreService: List<StudentScore>
    
    Note over ScoreService: Nhóm điểm theo ScoreType<br/>(REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
    ScoreService->>ScoreService: groupBy(ScoreType)
    
    Note over ScoreService: Tính tổng điểm cho mỗi loại<br/>và parse activityIds JSON
    ScoreService->>ScoreService: Tính total cho mỗi ScoreType<br/>Parse activityIds từ JSON<br/>Tạo ScoreViewResponse
    
    ScoreService-->>ScoreController: Response(success, ScoreViewResponse)
    ScoreController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị điểm theo từng loại<br/>và danh sách activity đã nhận điểm
```

### 2. Xem tổng điểm (Get Total Score)

```mermaid
sequenceDiagram
    participant User as Student/Admin/Manager
    participant Client as Client/Frontend
    participant ScoreController as ScoreController
    participant ScoreService as ScoreServiceImpl
    participant Repository as Repository
    participant Database as Database

    User->>Client: Xem tổng điểm
    Client->>ScoreController: GET /api/scores/student/{studentId}/semester/{semesterId}/total
    
    ScoreController->>ScoreService: getTotalScore(studentId, semesterId)
    
    ScoreService->>Repository: findByStudentAndSemester(studentId, semesterId)
    Repository->>Database: SELECT * FROM student_scores<br/>WHERE student_id = ? AND semester_id = ?
    Database-->>Repository: List<StudentScore>
    Repository-->>ScoreService: List<StudentScore>
    
    Note over ScoreService: Tính tổng điểm theo từng loại<br/>và tổng điểm chung
    ScoreService->>ScoreService: groupBy(ScoreType) và tính total<br/>Tính grandTotal = tổng tất cả
    
    ScoreService-->>ScoreController: Response(success, {grandTotal, totalsByType, scoreCount})
    ScoreController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị tổng điểm chung<br/>và tổng điểm theo từng loại
```

### 3. Xem bảng xếp hạng (Get Ranking)

```mermaid
sequenceDiagram
    participant User as Student/Admin/Manager
    participant Client as Client/Frontend
    participant ScoreController as ScoreController
    participant ScoreService as ScoreServiceImpl
    participant Repository as Repository
    participant Database as Database

    User->>Client: Xem bảng xếp hạng<br/>(semesterId, scoreType, departmentId, classId, sortOrder)
    Client->>ScoreController: GET /api/scores/ranking<br/>?semesterId=1&scoreType=REN_LUYEN&sortOrder=DESC
    
    ScoreController->>ScoreService: getStudentRanking(semesterId, scoreType, departmentId, classId, sortOrder)
    
    Note over ScoreService: Kiểm tra semester tồn tại
    ScoreService->>Repository: findById(semesterId)
    Repository->>Database: SELECT * FROM semesters<br/>WHERE id = ?
    Database-->>Repository: Semester
    Repository-->>ScoreService: Semester
    
    alt scoreType được chỉ định
        Note over ScoreService: Xếp hạng theo một loại điểm cụ thể<br/>(có thể filter theo department/class)
        ScoreService->>Repository: findBySemesterIdAndScoreType...OrderByScoreDesc(...)
        Repository->>Database: SELECT * FROM student_scores<br/>WHERE semester_id = ? AND score_type = ?<br/>(+ filter department/class nếu có)<br/>ORDER BY score DESC
        Database-->>Repository: List<StudentScore>
        Repository-->>ScoreService: List<StudentScore>
    else scoreType = null (tổng điểm)
        Note over ScoreService: Xếp hạng theo tổng điểm tất cả loại
        ScoreService->>Repository: findBySemesterIdOrderByScoreDesc(semesterId)
        Repository->>Database: SELECT * FROM student_scores<br/>WHERE semester_id = ?<br/>ORDER BY score DESC
        Database-->>Repository: List<StudentScore>
        Repository-->>ScoreService: List<StudentScore>
        
        Note over ScoreService: Group by student, tính tổng điểm<br/>Filter theo department/class nếu có
        ScoreService->>ScoreService: groupBy(studentId) và sum(score)
    end
    
    Note over ScoreService: Gán rank (điểm bằng nhau cùng rank)<br/>Lấy thông tin student và tạo response
    ScoreService->>Repository: findByIdAndIsDeletedFalse(studentId) cho mỗi student
    Repository->>Database: SELECT * FROM students<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Student
    Repository-->>ScoreService: Student
    
    ScoreService-->>ScoreController: Response(success, {rankings, semesterName, totalStudents, ...})
    ScoreController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị bảng xếp hạng<br/>với rank, thông tin sinh viên và điểm
```

## Ghi chú

1. **Quyền truy cập**: 
   - Tất cả các chức năng xem điểm: Student, Admin và Manager đều có thể truy cập
   - Student chỉ có thể xem điểm của chính mình (thông qua studentId từ authentication)

2. **Loại điểm (ScoreType)**:
   - `REN_LUYEN`: Điểm rèn luyện
   - `CONG_TAC_XA_HOI`: Điểm công tác xã hội
   - `CHUYEN_DE`: Điểm chuyên đề doanh nghiệp
   - `null`: Tổng điểm tất cả loại

3. **Xem điểm chi tiết**:
   - Hiển thị điểm theo từng loại (ScoreType)
   - Mỗi loại có tổng điểm và danh sách activity đã nhận điểm (activityIds)
   - ActivityIds được lưu dưới dạng JSON string trong database

4. **Tổng điểm**:
   - Tính tổng điểm cho từng loại (totalsByType)
   - Tính tổng điểm chung (grandTotal)
   - Trả về số lượng bản ghi điểm (scoreCount)

5. **Bảng xếp hạng**:
   - Có thể xếp hạng theo một loại điểm cụ thể hoặc tổng điểm
   - Có thể lọc theo department hoặc class
   - Sắp xếp tăng dần (ASC) hoặc giảm dần (DESC)
   - Xử lý rank: sinh viên có điểm bằng nhau sẽ có cùng rank

6. **Semester**: 
   - Tất cả các chức năng đều yêu cầu semesterId
   - Điểm được lưu và hiển thị theo từng học kỳ

