-- Tạo bảng mini_games
CREATE TABLE mini_games (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    title VARCHAR(255) NOT NULL COMMENT 'Tiêu đề minigame',
    description TEXT NULL COMMENT 'Mô tả',
    question_count INT NOT NULL COMMENT 'Số lượng câu hỏi',
    time_limit INT NULL COMMENT 'Thời gian giới hạn (giây)',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Đang hoạt động',
    type VARCHAR(50) NOT NULL COMMENT 'Loại minigame (QUIZ, ...)',
    activity_id BIGINT NOT NULL COMMENT 'ID activity',
    required_correct_answers INT NULL COMMENT 'Số câu đúng tối thiểu để đạt',
    reward_points DECIMAL(10,2) NULL COMMENT 'Điểm thưởng nếu đạt',
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    UNIQUE KEY uk_activity_minigame (activity_id),
    INDEX idx_type (type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Minigame';

-- Tạo bảng mini_game_quizzes
CREATE TABLE mini_game_quizzes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    mini_game_id BIGINT NOT NULL COMMENT 'ID minigame',
    FOREIGN KEY (mini_game_id) REFERENCES mini_games(id) ON DELETE CASCADE,
    UNIQUE KEY uk_minigame_quiz (mini_game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quiz của minigame';

-- Tạo bảng mini_game_quiz_questions
CREATE TABLE mini_game_quiz_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    question_text TEXT NOT NULL COMMENT 'Nội dung câu hỏi',
    mini_game_quiz_id BIGINT NOT NULL COMMENT 'ID quiz',
    display_order INT NOT NULL DEFAULT 0 COMMENT 'Thứ tự hiển thị',
    FOREIGN KEY (mini_game_quiz_id) REFERENCES mini_game_quizzes(id) ON DELETE CASCADE,
    INDEX idx_quiz_order (mini_game_quiz_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Câu hỏi quiz';

-- Tạo bảng mini_game_quiz_options
CREATE TABLE mini_game_quiz_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    text TEXT NOT NULL COMMENT 'Nội dung lựa chọn',
    is_correct TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Là đáp án đúng',
    question_id BIGINT NOT NULL COMMENT 'ID câu hỏi',
    FOREIGN KEY (question_id) REFERENCES mini_game_quiz_questions(id) ON DELETE CASCADE,
    INDEX idx_question (question_id),
    INDEX idx_is_correct (is_correct)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lựa chọn đáp án';

-- Tạo bảng mini_game_attempts
CREATE TABLE mini_game_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    mini_game_id BIGINT NOT NULL COMMENT 'ID minigame',
    student_id BIGINT NOT NULL COMMENT 'ID sinh viên',
    correct_count INT NOT NULL DEFAULT 0 COMMENT 'Số câu đúng',
    status VARCHAR(50) NOT NULL COMMENT 'Trạng thái (IN_PROGRESS, PASSED, FAILED)',
    started_at DATETIME NOT NULL COMMENT 'Thời gian bắt đầu',
    submitted_at DATETIME NULL COMMENT 'Thời gian nộp bài',
    FOREIGN KEY (mini_game_id) REFERENCES mini_games(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_student_minigame (student_id, mini_game_id),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lần làm bài của sinh viên';

-- Tạo bảng mini_game_answers
CREATE TABLE mini_game_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    attempt_id BIGINT NOT NULL COMMENT 'ID lần làm bài',
    question_id BIGINT NOT NULL COMMENT 'ID câu hỏi',
    option_id BIGINT NOT NULL COMMENT 'ID lựa chọn đã chọn',
    is_correct TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Đáp án đúng hay sai',
    FOREIGN KEY (attempt_id) REFERENCES mini_game_attempts(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES mini_game_quiz_questions(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES mini_game_quiz_options(id) ON DELETE CASCADE,
    INDEX idx_attempt_question (attempt_id, question_id),
    INDEX idx_is_correct (is_correct)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Câu trả lời của sinh viên';

