package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.MiniGameType;
import vn.campuslife.model.*;
import vn.campuslife.repository.MiniGameQuizOptionRepository;
import vn.campuslife.repository.MiniGameQuizQuestionRepository;
import vn.campuslife.repository.MiniGameQuizRepository;
import vn.campuslife.repository.MiniGameRepository;
import vn.campuslife.service.MiniGameService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MiniGameServiceImpl implements MiniGameService {

    private final MiniGameRepository miniGameRepository;
    private final MiniGameQuizQuestionRepository questionRepository;
    private final MiniGameQuizOptionRepository optionRepository;
    private final MiniGameQuizRepository miniGameQuizRepository;

    //Tạo minigame
    @Override
    public MiniGameResponse create(MiniGameRequest request) {
        MiniGame game = new MiniGame();
        game.setTitle(request.getTitle());
        game.setDescription(request.getDescription());
        game.setType(request.getType());
        game.setQuestionCount(request.getQuestionCount());
        game.setTimeLimit(request.getTimeLimit());
        game.setRewardPoints(request.getRewardPoints());
        game.setRequiredCorrectAnswers(request.getRequiredCorrectAnswers());
        game.setActive(true);
        miniGameRepository.save(game);

        if (request.getQuestions() != null) {
            for (MiniGameQuizQuestionRequest qReq : request.getQuestions()) {
                MiniGameQuizQuestion question = new MiniGameQuizQuestion();
                question.setQuestionText(qReq.getQuestionText());
                question.setMiniGameQuiz(null);
                questionRepository.save(question);

                if (qReq.getOptions() != null) {
                    for (MiniGameQuizOptionRequest oReq : qReq.getOptions()) {
                        MiniGameQuizOption option = new MiniGameQuizOption();
                        option.setQuestion(question);
                        option.setText(oReq.getText());
                        option.setCorrect(oReq.isCorrect());
                        optionRepository.save(option);
                    }
                }
            }
        }

        return mapToResponse(game);
    }

    @Override
    public List<MiniGameResponse> getAll() {
        return miniGameRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MiniGameResponse mapToResponse(MiniGame m) {
        MiniGameResponse r = new MiniGameResponse();
        r.setId(m.getId());
        r.setTitle(m.getTitle());
        r.setDescription(m.getDescription());
        r.setType(m.getType());
        r.setQuestionCount(m.getQuestionCount());
        r.setTimeLimit(m.getTimeLimit());
        r.setRewardPoints(m.getRewardPoints());
        r.setActive(m.isActive());
        r.setActivityId(m.getActivity() != null ? m.getActivity().getId() : null);
        return r;
    }
    //Tạo Minigame Quiz
    @Override
    @Transactional
    public MiniGame createMiniGameForActivity(Activity activity, MiniGameConfig config) {

        MiniGame game = new MiniGame();
        game.setActivity(activity);
        game.setTitle(config.getTitle() != null ? config.getTitle() : activity.getName());
        game.setDescription(config.getDescription());
        game.setType(config.getType());
        game.setQuestionCount(config.getQuestionCount());
        game.setTimeLimit(config.getTimeLimit());
        game.setRewardPoints(config.getRewardPoints());
        game.setActive(true);
        miniGameRepository.save(game);

        if (config.getType() == MiniGameType.QUIZ && config.getQuestions() != null) {

            MiniGameQuiz quiz = new MiniGameQuiz();
            quiz.setMiniGame(game);
            miniGameQuizRepository.save(quiz);

            for (MiniGameQuizQuestionRequest qReq : config.getQuestions()) {
                MiniGameQuizQuestion question = new MiniGameQuizQuestion();
                question.setQuestionText(qReq.getQuestionText());
                question.setMiniGameQuiz(quiz);
                miniGameQuizRepository.flush();
                questionRepository.save(question);

                // Nếu có options
                if (qReq.getOptions() != null) {
                    for (MiniGameQuizOptionRequest oReq : qReq.getOptions()) {
                        MiniGameQuizOption option = new MiniGameQuizOption();
                        option.setQuestion(question);
                        option.setText(oReq.getText());
                        option.setCorrect(oReq.isCorrect());
                        optionRepository.save(option);
                    }
                }
            }
        }
        return game;
    }

    @Override
    @Transactional
    public void updateMiniGameByActivity(Long activityId, MiniGameRequest request) {
        MiniGame miniGame = miniGameRepository.findByActivityId(activityId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy MiniGame của Activity ID " + activityId));

        // Cập nhật thông tin chung
        miniGame.setTitle(request.getTitle());
        miniGame.setDescription(request.getDescription());
        miniGame.setQuestionCount(request.getQuestionCount());
        miniGame.setRewardPoints(request.getRewardPoints());
        miniGame.setTimeLimit(request.getTimeLimit());
        miniGame.setRequiredCorrectAnswers(request.getRequiredCorrectAnswers());
        miniGameRepository.save(miniGame);

        // --- Lấy quiz gốc
        MiniGameQuiz quiz = miniGameQuizRepository.findByMiniGame(miniGame)
                .orElseGet(() -> {
                    MiniGameQuiz qz = new MiniGameQuiz();
                    qz.setMiniGame(miniGame);
                    return miniGameQuizRepository.save(qz);
                });

        // --- Câu hỏi hiện có trong DB
        List<MiniGameQuizQuestion> existingQuestions = questionRepository.findByMiniGameQuiz(quiz);

        // --- Xóa các câu hỏi không còn trong request
        List<Long> requestQuestionIds = request.getQuestions().stream()
                .filter(q -> q.getId() != null)
                .map(MiniGameQuizQuestionRequest::getId)
                .toList();

        for (MiniGameQuizQuestion oldQ : existingQuestions) {
            if (!requestQuestionIds.contains(oldQ.getId())) {
                questionRepository.delete(oldQ);
            }
        }

        // --- Thêm / cập nhật câu hỏi
        for (MiniGameQuizQuestionRequest qReq : request.getQuestions()) {
            MiniGameQuizQuestion question;
            if (qReq.getId() != null) {
                // Cập nhật câu hỏi có sẵn
                question = questionRepository.findById(qReq.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi ID " + qReq.getId()));
                question.setQuestionText(qReq.getQuestionText());
            } else {
                // Thêm mới câu hỏi
                question = new MiniGameQuizQuestion();
                question.setMiniGameQuiz(quiz);
                question.setQuestionText(qReq.getQuestionText());
            }
            questionRepository.save(question);

            // --- Xử lý đáp án (options)
            List<MiniGameQuizOption> existingOptions = optionRepository.findByQuestion(question);
            List<Long> reqOptIds = qReq.getOptions().stream()
                    .filter(o -> o.getId() != null)
                    .map(MiniGameQuizOptionRequest::getId)
                    .toList();

            // Xóa các option không còn trong request
            for (MiniGameQuizOption oldOpt : existingOptions) {
                if (!reqOptIds.contains(oldOpt.getId())) {
                    optionRepository.delete(oldOpt);
                }
            }

            // Thêm hoặc cập nhật đáp án
            for (MiniGameQuizOptionRequest oReq : qReq.getOptions()) {
                MiniGameQuizOption opt;
                if (oReq.getId() != null) {
                    opt = optionRepository.findById(oReq.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy option ID " + oReq.getId()));
                    opt.setText(oReq.getText());
                    opt.setCorrect(oReq.isCorrect());
                } else {
                    opt = new MiniGameQuizOption();
                    opt.setQuestion(question);
                    opt.setText(oReq.getText());
                    opt.setCorrect(oReq.isCorrect());
                }
                optionRepository.save(opt);
            }
        }
    }



}