-- Phase 3: Legacy Rule Backfill

-- Chèn rule cho các hoạt động bình thường, SUKIEN, CONG_TAC_XA_HOI, MINIGAME
INSERT INTO activity_score_rules (
    activity_id, score_type, trigger_type, calculation, 
    points, fail_points, audience, semester_policy, enabled
)
SELECT 
    id, 
    COALESCE(score_type, 'REN_LUYEN'), 
    CASE 
        WHEN type = 'MINIGAME' THEN 'MINIGAME_PASSED'
        WHEN requires_submission = 1 THEN 'SUBMISSION_GRADED'
        ELSE 'PARTICIPATION_COMPLETED'
    END, 
    'FIXED_POINTS',
    COALESCE(max_points, 0),
    COALESCE(-penalty_points_incomplete, 0),
    'ALL_PARTICIPANTS',
    'ACTIVITY_SEMESTER',
    1
FROM activities
WHERE type != 'CHUYEN_DE_DOANH_NGHIEP' AND series_id IS NULL;

-- Chuyên đề doanh nghiệp (luôn cộng 1 điểm CHUYEN_DE)
INSERT INTO activity_score_rules (
    activity_id, score_type, trigger_type, calculation, 
    points, fail_points, audience, semester_policy, enabled
)
SELECT 
    id, 
    'CHUYEN_DE', 
    CASE WHEN requires_submission = 1 THEN 'SUBMISSION_GRADED' ELSE 'PARTICIPATION_COMPLETED' END, 
    'COUNT_COMPLETION',
    1,
    0,
    'ALL_PARTICIPANTS',
    'ACTIVITY_SEMESTER',
    1
FROM activities
WHERE type = 'CHUYEN_DE_DOANH_NGHIEP';

-- Chuyên đề doanh nghiệp (cộng max_points vào REN_LUYEN nếu có)
INSERT INTO activity_score_rules (
    activity_id, score_type, trigger_type, calculation, 
    points, fail_points, audience, semester_policy, enabled
)
SELECT 
    id, 
    'REN_LUYEN', 
    CASE WHEN requires_submission = 1 THEN 'SUBMISSION_GRADED' ELSE 'PARTICIPATION_COMPLETED' END, 
    'FIXED_POINTS',
    max_points,
    COALESCE(-penalty_points_incomplete, 0),
    'ALL_PARTICIPANTS',
    'ACTIVITY_SEMESTER',
    1
FROM activities
WHERE type = 'CHUYEN_DE_DOANH_NGHIEP' AND max_points > 0;

-- Series (chuỗi hoạt động có thể có rules riêng, hoặc chính hoạt động đó nằm trong series)
-- Đối với các activity nằm trong series (có series_id != NULL)
INSERT INTO activity_score_rules (
    activity_id, score_type, trigger_type, calculation, 
    points, fail_points, audience, semester_policy, enabled
)
SELECT 
    id, 
    COALESCE(score_type, 'REN_LUYEN'), 
    'SERIES_MILESTONE_REACHED', 
    'SERIES_MILESTONE',
    COALESCE(max_points, 0),
    0,
    'ALL_PARTICIPANTS',
    'ACTIVITY_SEMESTER',
    1
FROM activities
WHERE series_id IS NOT NULL;

-- Drop các cột điểm cũ
ALTER TABLE activities DROP COLUMN score_type;
ALTER TABLE activities DROP COLUMN max_points;
ALTER TABLE activities DROP COLUMN penalty_points_incomplete;
