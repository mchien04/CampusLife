# 🚀 Quick Start Testing Guide

## Overview
This guide provides quick steps to test the implemented auto-score initialization and semester-based scoring system.

---

## Prerequisites
- Maven 3.6+
- Java 11+
- Running database (MySQL/PostgreSQL)
- Application running on `http://localhost:8080`
- Admin token for API calls

---

## Test 1: Verify Compilation

```bash
# Navigate to project
cd "D:\2025-2026 HKI\TLCN\campuslife"

# Clean compile
mvn clean compile

# Expected: BUILD SUCCESS
```

---

## Test 2: Check SemesterRepository Query

Open MySQL client or database tool:

```sql
-- Check if semesters exist
SELECT * FROM semesters LIMIT 5;

-- Test the query logic manually
SELECT s.* 
FROM semesters s
WHERE s.start_date <= DATE('2025-02-15')
  AND s.end_date >= DATE('2025-02-15')
ORDER BY s.is_open DESC, s.start_date DESC
LIMIT 1;
```

---

## Test 3: Auto-Initialization on Semester Creation

### Step 3.1: Create Academic Year
```bash
curl -X POST "http://localhost:8080/api/admin/academics/years" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Năm học 2024-2025",
    "startDate": "2024-09-01",
    "endDate": "2025-05-31"
  }'
```

### Step 3.2: Create Semester with isOpen=true
```bash
curl -X POST "http://localhost:8080/api/admin/academics/semesters" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "yearId": 1,
    "name": "Học kỳ 2 - 2024-2025",
    "startDate": "2025-01-15",
    "endDate": "2025-05-15",
    "open": true
  }'
```

### Step 3.3: Verify Scores Created
Check logs for: `"Auto-initialized scores for all students in new semester"`

Query database:
```sql
-- Get the semester ID from previous step (e.g., 1)
SELECT COUNT(*) as total_scores
FROM student_scores
WHERE semester_id = 1;

-- Should return: number_of_active_students * 3
-- (3 score types per student)

-- Verify score types
SELECT score_type, COUNT(*) as count
FROM student_scores
WHERE semester_id = 1
GROUP BY score_type;

-- Should show:
-- REN_LUYEN: X
-- CONG_TAC_XA_HOI: X
-- CHUYEN_DE: X
```

---

## Test 4: Manual Score Initialization API

### Scenario: Create semester with isOpen=false, then initialize manually

### Step 4.1: Create Semester with isOpen=false
```bash
curl -X POST "http://localhost:8080/api/admin/academics/semesters" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "yearId": 1,
    "name": "Học kỳ 1 - 2024-2025",
    "startDate": "2024-09-01",
    "endDate": "2024-12-31",
    "open": false
  }'
```

### Step 4.2: Call Manual Initialization API
```bash
# Replace {semesterId} with the ID from Step 4.1 (e.g., 2)
curl -X POST "http://localhost:8080/api/admin/academics/semesters/2/initialize-scores" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

### Step 4.3: Verify Response
Expected response:
```json
{
  "status": true,
  "message": "Scores initialized successfully",
  "data": {
    "semesterId": 2,
    "semesterName": "Học kỳ 1 - 2024-2025",
    "message": "Scores initialized successfully for all students"
  }
}
```

### Step 4.4: Verify Database
```sql
SELECT COUNT(*) as total_scores
FROM student_scores
WHERE semester_id = 2;
```

---

## Test 5: Semester-Based Score Attribution

### Scenario: Activity with startDate in Semester 2 should get score in Semester 2

### Step 5.1: Create Activity with startDate in future
```bash
# First get an existing activity ID or create new one
# Activity must have:
# - startDate: in Semester 2 date range (e.g., 2025-02-15)
# - scoreType: set to one of the 3 types
# - student enrollment for testing

# Example of finding activity:
# SELECT id, name, start_date FROM activities LIMIT 1;
```

### Step 5.2: Register student for activity
```bash
curl -X POST "http://localhost:8080/api/student/activities/register" \
  -H "Authorization: Bearer YOUR_STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "activityId": 1
  }'
```

### Step 5.3: Check student participation
```bash
# After participation, check which semester got the score
SELECT 
  ss.id,
  ss.student_id,
  ss.semester_id,
  s.name as semester_name,
  ss.score_type,
  ss.score
FROM student_scores ss
JOIN semesters s ON ss.semester_id = s.id
WHERE ss.student_id = 1
  AND ss.score_type = 'REN_LUYEN'
ORDER BY s.start_date DESC
LIMIT 3;
```

---

## Test 6: Enable Debug Logging

### Edit application.properties:
```properties
logging.level.vn.campuslife.service.SemesterHelperService=DEBUG
logging.level.vn.campuslife.service.impl.AcademicServiceImpl=INFO
logging.level.vn.campuslife.service.StudentScoreInitService=INFO
```

### Expected log outputs:
```
[DEBUG] SemesterHelperService: Found semester 1 for activity 5 based on startDate 2025-02-15
[INFO] StudentScoreInitService: Initializing scores for all students in semester 1
[INFO] StudentScoreInitService: Found 150 active students to initialize scores
[INFO] StudentScoreInitService: Initialized scores for 100 students...
[INFO] StudentScoreInitService: Completed initializing scores: 150 created, 0 skipped, 150 total students
```

---

## Test 7: Error Scenarios

### Scenario 1: No semester found for activity date
```bash
# Create activity with startDate outside all semesters
# Expected: Falls back to current open semester
# Check logs for: "Could not find semester for activity X...Using current open semester as fallback"
```

### Scenario 2: Multiple overlapping open semesters
```bash
# Create Semester A: Jan 1 - Apr 30, isOpen=true
# Create Semester B: Mar 1 - Jun 30, isOpen=true
# Activity startDate: Mar 15
# Expected: Semester B selected (isOpen DESC ordering)
# Check logs for semester ID
```

### Scenario 3: Invalid semester ID for manual init
```bash
curl -X POST "http://localhost:8080/api/admin/academics/semesters/99999/initialize-scores" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# Expected response:
# { "status": false, "message": "Semester not found" }
```

---

## Test 8: Performance Check

### Initialize scores for 500+ students:
```bash
# Time the manual initialization API call
time curl -X POST "http://localhost:8080/api/admin/academics/semesters/1/initialize-scores" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# Expected: ~1-5 seconds depending on student count
# Check logs for: "Initialized scores for 100 students..." messages
```

---

## Verification Checklist

After running all tests, verify:

- [ ] Compilation successful (no errors)
- [ ] Auto-initialization triggered on semester creation with isOpen=true
- [ ] Manual API endpoint returns success response
- [ ] Correct number of scores created (students × 3)
- [ ] All 3 score types present (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
- [ ] Scores attributed to correct semester based on activity timing
- [ ] Debug logs show proper semester selection
- [ ] Error scenarios handled gracefully
- [ ] Performance acceptable for bulk operations

---

## Common Issues & Solutions

### Issue: "No semester found for score aggregation"
**Solution:**
1. Ensure at least one semester exists in database
2. Check semester `start_date` and `end_date` are valid
3. Verify activity `start_date` is within some semester range

### Issue: Scores in wrong semester
**Solution:**
1. Check activity `start_date` value
2. Check all semester date ranges
3. Review logs from SemesterHelperService
4. Manually test SemesterRepository query

### Issue: Auto-init not triggered
**Solution:**
1. Ensure `open: true` in semester creation request
2. Check application logs for exceptions
3. Verify StudentRepository returns students
4. Check database for student records (filter deleted)

### Issue: Slow initialization
**Solution:**
1. For 1000+ students, may take several minutes
2. Check application logs for progress
3. Monitor database connection pool
4. Consider running during off-peak hours

---

## Next Steps

Once all tests pass:

1. **Deploy to staging** - Test with larger dataset
2. **Load testing** - Test with production-like user volumes
3. **Integration testing** - Test with other semester-related features
4. **Documentation** - Update user guides
5. **Deployment** - Roll out to production with rollback plan ready

---

## Contact & Support

For issues or questions:
1. Check logs with DEBUG level enabled
2. Review SQL queries manually in database client
3. Consult IMPLEMENTATION_COMPLETE.md for detailed documentation
4. Review code comments in modified files

---

**Last Updated:** January 14, 2026  
**Status:** Ready for Testing ✅

