package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.Activity;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.score.ActivityScoreRuleResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.RagService;
import vn.campuslife.service.StudentService;
import vn.campuslife.service.ai.ChatbotNluService;
import vn.campuslife.service.ai.GeminiApiClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceImplTest {

    @Mock
    private ChatbotConversationRepository conversationRepository;

    @Mock
    private ChatbotMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private ChatbotNluService chatbotNluService;

    @Mock
    private EventArticleRepository eventArticleRepository;

    @Mock
    private GeminiApiClient geminiApiClient;

    @Mock
    private RagService ragService;

    @Mock
    private ActivityScoreRuleService activityScoreRuleService;

    @InjectMocks
    private ChatbotServiceImpl chatbotService;

    private Activity activity;

    @BeforeEach
    void setUp() {
        activity = new Activity();
        activity.setId(100L);
        activity.setName("Test Activity");
        activity.setRequiresSubmission(false);
    }

    @Test
    void isAskingPoints_ShouldMatchVariousKeywords() {
        // Từng từ khóa có dấu / không dấu
        assertTrue(ChatbotServiceImpl.isAskingPoints("diem cua su kien nay"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("quy tắc cộng điểm"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("tối đa bao nhiêu điểm"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("có bị phạt không"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("nếu no-show thì sao"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("bị phạt vắng mặt"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("quá hạn nộp bài phạt bao nhiêu"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("mốc chuỗi sự kiện"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("tiến trình đạt milestone"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("tính điểm preset"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("chuỗi sự kiện này cộng gì"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("su kien chuoi"));
        assertTrue(ChatbotServiceImpl.isAskingPoints("phat vang mat"));
        
        // Không chứa từ khóa liên quan điểm
        assertFalse(ChatbotServiceImpl.isAskingPoints("sự kiện diễn ra ở đâu"));
        assertFalse(ChatbotServiceImpl.isAskingPoints("liên hệ ban tổ chức bằng cách nào"));
    }

    @Test
    void formatPointsAnswer_WhenActivityBelongsToSeries_ShouldReturnSeriesMessage() {
        activity.setSeriesId(200L);

        String answer = chatbotService.formatPointsAnswer(activity);

        assertTrue(answer.contains("thuộc chuỗi sự kiện"));
        assertTrue(answer.contains("không cộng điểm riêng lẻ"));
        verifyNoInteractions(activityScoreRuleService);
    }

    @Test
    void formatPointsAnswer_WhenNoEnabledRules_ShouldReturnNoRulesMessage() {
        when(activityScoreRuleService.getRuleResponses(activity.getId()))
                .thenReturn(Collections.emptyList());

        String answer = chatbotService.formatPointsAnswer(activity);

        assertTrue(answer.contains("chưa có quy tắc tính điểm nào được bật"));
    }

    @Test
    void formatPointsAnswer_WhenHasRules_ShouldListRulesAndSubmissionNotes() {
        activity.setPresetCode(ActivityPresetCode.EVENT_BASIC);
        activity.setRequiresSubmission(true);

        List<ActivityScoreRuleResponse> rules = new ArrayList<>();
        
        // 1. PARTICIPATION_COMPLETED
        ActivityScoreRuleResponse rule1 = new ActivityScoreRuleResponse();
        rule1.setTriggerType(ScoreRuleTrigger.PARTICIPATION_COMPLETED);
        rule1.setScoreType(ScoreType.REN_LUYEN);
        rule1.setPoints(BigDecimal.valueOf(5.0));
        rule1.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        rules.add(rule1);

        // 2. NO_SHOW (with failPoints)
        ActivityScoreRuleResponse rule2 = new ActivityScoreRuleResponse();
        rule2.setTriggerType(ScoreRuleTrigger.NO_SHOW);
        rule2.setScoreType(ScoreType.REN_LUYEN);
        rule2.setFailScoreType(ScoreType.REN_LUYEN);
        rule2.setPoints(BigDecimal.valueOf(0.0));
        rule2.setFailPoints(BigDecimal.valueOf(2.0));
        rule2.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        rules.add(rule2);

        // 3. SUBMISSION_GRADED (with points & failPoints)
        ActivityScoreRuleResponse rule3 = new ActivityScoreRuleResponse();
        rule3.setTriggerType(ScoreRuleTrigger.SUBMISSION_GRADED);
        rule3.setScoreType(ScoreType.CONG_TAC_XA_HOI);
        rule3.setFailScoreType(ScoreType.REN_LUYEN);
        rule3.setPoints(BigDecimal.valueOf(3.0));
        rule3.setFailPoints(BigDecimal.valueOf(1.0));
        rule3.setAudience(ScoreRuleAudience.DEPARTMENT_ONLY);
        rule3.setTargetDepartmentIds(List.of(1L, 2L));
        rules.add(rule3);

        // 4. TASK_OVERDUE (with failPoints)
        ActivityScoreRuleResponse rule4 = new ActivityScoreRuleResponse();
        rule4.setTriggerType(ScoreRuleTrigger.TASK_OVERDUE);
        rule4.setScoreType(ScoreType.CHUYEN_DE);
        rule4.setPoints(BigDecimal.valueOf(0.0));
        rule4.setFailPoints(BigDecimal.valueOf(4.0));
        rule4.setAudience(ScoreRuleAudience.OUTSIDE_DEPARTMENTS_ONLY);
        rules.add(rule4);

        // 5. MINIGAME_PASSED
        ActivityScoreRuleResponse rule5 = new ActivityScoreRuleResponse();
        rule5.setTriggerType(ScoreRuleTrigger.MINIGAME_PASSED);
        rule5.setScoreType(ScoreType.REN_LUYEN);
        rule5.setPoints(BigDecimal.valueOf(1.0));
        rule5.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        rules.add(rule5);

        // 6. MINIGAME_EXHAUSTED_ATTEMPTS
        ActivityScoreRuleResponse rule6 = new ActivityScoreRuleResponse();
        rule6.setTriggerType(ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS);
        rule6.setScoreType(ScoreType.REN_LUYEN);
        rule6.setPoints(BigDecimal.valueOf(0.0));
        rule6.setFailPoints(BigDecimal.valueOf(1.5));
        rule6.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        rules.add(rule6);

        // 7. SERIES_MILESTONE_REACHED
        ActivityScoreRuleResponse rule7 = new ActivityScoreRuleResponse();
        rule7.setTriggerType(ScoreRuleTrigger.SERIES_MILESTONE_REACHED);
        rule7.setScoreType(ScoreType.REN_LUYEN);
        rule7.setPoints(BigDecimal.valueOf(10.0));
        rule7.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);
        rules.add(rule7);

        when(activityScoreRuleService.getRuleResponses(activity.getId())).thenReturn(rules);

        String answer = chatbotService.formatPointsAnswer(activity);

        // Verify title & preset code
        assertTrue(answer.contains("Quy tắc tính điểm của sự kiện \"Test Activity\" (preset: EVENT_BASIC):"));

        // Verify PARTICIPATION_COMPLETED
        assertTrue(answer.contains("Khi điểm danh thành công: +5 điểm rèn luyện (áp dụng: tất cả sinh viên)."));

        // Verify NO_SHOW penalty (must use failPoints = 2.0)
        assertTrue(answer.contains("Nếu đăng ký nhưng không tham gia (no-show): -2 điểm rèn luyện (áp dụng: tất cả sinh viên)."));

        // Verify SUBMISSION_GRADED (pass +3, fail -1)
        assertTrue(answer.contains("Khi nộp bài và được chấm đạt: +3 điểm công tác xã hội; trượt/quá hạn: -1 điểm rèn luyện"));
        assertTrue(answer.contains("chỉ áp dụng sinh viên thuộc khoa đã chọn"));

        // Verify TASK_OVERDUE (must use failPoints = 4.0)
        assertTrue(answer.contains("Nếu quá hạn nộp bài: -4 điểm chuyên đề (chỉ áp dụng sinh viên ngoài các khoa đã chọn)."));

        // Verify MINIGAME_PASSED
        assertTrue(answer.contains("Khi vượt qua minigame: +1 điểm rèn luyện (áp dụng: tất cả sinh viên)."));

        // Verify MINIGAME_EXHAUSTED_ATTEMPTS
        assertTrue(answer.contains("Nếu hết lượt chơi mà không vượt qua: -1.5 điểm rèn luyện (áp dụng: tất cả sinh viên)."));

        // Verify SERIES_MILESTONE_REACHED
        assertTrue(answer.contains("Khi đạt mốc chuỗi sự kiện: +10 điểm rèn luyện (áp dụng: tất cả sinh viên)."));

        // Verify requiresSubmission notes
        assertTrue(answer.contains("Lưu ý: Sự kiện yêu cầu nộp minh chứng. Điểm chỉ được cộng đầy đủ khi sinh viên **đã điểm danh (ATTENDED)** và **đã được chấm bài (GRADED)**."));
    }

    @Test
    void formatPointsAnswer_WhenNoRequiresSubmission_ShouldShowSimpleNote() {
        activity.setRequiresSubmission(false);
        ActivityScoreRuleResponse rule = new ActivityScoreRuleResponse();
        rule.setTriggerType(ScoreRuleTrigger.PARTICIPATION_COMPLETED);
        rule.setScoreType(ScoreType.REN_LUYEN);
        rule.setPoints(BigDecimal.valueOf(2.0));
        rule.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);

        when(activityScoreRuleService.getRuleResponses(activity.getId())).thenReturn(List.of(rule));

        String answer = chatbotService.formatPointsAnswer(activity);

        assertTrue(answer.contains("Lưu ý: Sự kiện không bắt buộc nộp minh chứng, điểm được cộng khi hoàn thành điểm danh."));
    }
}
