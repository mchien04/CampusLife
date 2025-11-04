package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.MiniGameQuiz;
import vn.campuslife.entity.MiniGameQuizOption;
import vn.campuslife.entity.MiniGameQuizQuestion;
import vn.campuslife.enumeration.MiniGameType;
import vn.campuslife.model.*;
import vn.campuslife.repository.MiniGameQuizOptionRepository;
import vn.campuslife.repository.MiniGameQuizQuestionRepository;
import vn.campuslife.repository.MiniGameQuizRepository;
import vn.campuslife.repository.MiniGameRepository;
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




}