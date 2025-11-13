package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.MiniGameType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.MiniGameService;

import java.util.List;

@RestController
@RequestMapping("/api/minigames")
@RequiredArgsConstructor
public class MiniGameController {

    private final MiniGameService miniGameService;
    private final MiniGameRepository miniGameRepository;
    private final MiniGameQuizRepository miniGameQuizRepository;
    private final MiniGameQuizQuestionRepository questionRepository;
    private final MiniGameQuizOptionRepository optionRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    @GetMapping
    public List<MiniGameResponse> getAll() {
        return miniGameService.getAll();
    }

    @PostMapping
    public MiniGameResponse create(@RequestBody MiniGameRequest request) {
        return miniGameService.create(request);
    }
    @GetMapping("/by-activity/{activityId}")
    public ResponseEntity<MiniGameResponse> getQuizByActivity(@PathVariable Long activityId) {
        MiniGame game = miniGameRepository.findByActivityId(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mini game"));

        MiniGameQuiz quiz = miniGameQuizRepository.findByMiniGameId(game.getId())
                .orElse(null);

        MiniGameResponse response = mapToResponse(game);

        if (quiz != null && quiz.getQuestions() != null) {
            response.setQuestions(
                    quiz.getQuestions().stream().map(q -> {
                        MiniGameQuizQuestionResponse qr = new MiniGameQuizQuestionResponse();
                        qr.setId(q.getId());
                        qr.setQuestionText(q.getQuestionText());
                        qr.setOptions(q.getOptions().stream()
                                .map(o -> new MiniGameQuizOptionResponse(o.getId(), o.getText(), o.isCorrect()))
                                .toList());
                        return qr;
                    }).toList()
            );
        }

        return ResponseEntity.ok(response);
    }

    private MiniGameResponse mapToResponse(MiniGame m) {
        MiniGameResponse r = new MiniGameResponse();
        r.setId(m.getId());
        r.setTitle(m.getTitle());
        r.setDescription(m.getDescription());
        r.setType(m.getType());
        r.setRewardPoints(m.getRewardPoints());
        r.setQuestionCount(m.getQuestionCount());
        r.setTimeLimit(m.getTimeLimit());
        r.setActive(m.isActive());
        r.setRequiredCorrectAnswers(m.getRequiredCorrectAnswers());
        r.setActivityId(m.getActivity() != null ? m.getActivity().getId() : null);
        return r;
    }
    @PostMapping("/{miniGameId}/quiz")
    public ResponseEntity<?> createQuiz(
            @PathVariable Long miniGameId,
            @RequestBody MiniGameQuizRequest request) {

        MiniGame miniGame = miniGameRepository.findById(miniGameId)
                .orElseThrow(() -> new RuntimeException("MiniGame not found"));

        // Tạo quiz
        MiniGameQuiz quiz = new MiniGameQuiz();
        quiz.setMiniGame(miniGame);
        miniGameQuizRepository.save(quiz);

        // Tạo câu hỏi và đáp án
        for (MiniGameQuizQuestionRequest qReq : request.getQuestions()) {
            MiniGameQuizQuestion question = new MiniGameQuizQuestion();
            question.setMiniGameQuiz(quiz);
            question.setQuestionText(qReq.getQuestionText());
            questionRepository.save(question);

            for (MiniGameQuizOptionRequest oReq : qReq.getOptions()) {
                MiniGameQuizOption option = new MiniGameQuizOption();
                option.setQuestion(question);
                option.setText(oReq.getText());
                option.setCorrect(oReq.isCorrect());
                optionRepository.save(option);
            }
        }

        return ResponseEntity.ok("Quiz created successfully for MiniGame ID " + miniGameId);
    }
    @PutMapping("/by-activity/{activityId}")
    public ResponseEntity<?> updateMiniGameByActivity(
            @PathVariable Long activityId,
            @RequestBody MiniGameRequest request
    ) {
        miniGameService.updateMiniGameByActivity(activityId, request);
        return ResponseEntity.ok(new Response(true, "Cập nhật MiniGame thành công", null));
    }
    @PostMapping("/{activityId}/start")
    public ResponseEntity<Response> start(@PathVariable Long activityId, Authentication auth) {
        Long studentId = getStudentIdFromAuth(auth);
        var payload = miniGameService.startAttempt(activityId, studentId);
        return ResponseEntity.ok(new Response(true, "Start mini game", payload));
    }

    @PostMapping("/{activityId}/submit")
    public ResponseEntity<Response> submit(@PathVariable Long activityId,
                                           @RequestBody @Valid SubmitRequest req,
                                           Authentication auth) {
        Long studentId = getStudentIdFromAuth(auth);
        var payload = miniGameService.submitMiniGame(activityId, studentId, req);
        return ResponseEntity.ok(new Response(true, "Submit result", payload));
    }
    private Long getStudentIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String username = authentication.getName();

        return studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                .map(Student::getId)
                .orElseGet(() -> {
                    User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));


                    if (user.getRole() != Role.STUDENT) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only STUDENT can play mini game");
                    }

                    Student s = new Student();
                    s.setUser(user);
                    s.setStudentCode(username);
                    s.setFullName(username);
                    s.setDeleted(false); // boolean isDeleted -> setter là setDeleted
                    return studentRepository.save(s).getId();
                });
    }




}