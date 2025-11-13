package vn.campuslife.model.mapper;

import vn.campuslife.entity.MiniGameQuizOption;
import vn.campuslife.entity.MiniGameQuizQuestion;
import vn.campuslife.model.MiniGameQuizOptionResponse;
import vn.campuslife.model.MiniGameQuizQuestionResponse;

import java.util.List;
import java.util.Set;

public class MiniGamePlayMapper {
    public static MiniGameQuizOptionResponse toOptionRespHideKey(MiniGameQuizOption op) {
        MiniGameQuizOptionResponse r = new MiniGameQuizOptionResponse();
        r.setId(op.getId());
        r.setText(op.getText());
        return r;
    }
    public static MiniGameQuizQuestionResponse toQuestionRespHideKey(MiniGameQuizQuestion q) {
        MiniGameQuizQuestionResponse r = new MiniGameQuizQuestionResponse();
        r.setId(q.getId());
        r.setQuestionText(q.getQuestionText());
        r.setOptions(q.getOptions().stream()
                .map(MiniGamePlayMapper::toOptionRespHideKey).toList());
        return r;
    }
    public static List<MiniGameQuizQuestionResponse> toQuestionsRespHideKey(Set<MiniGameQuizQuestion> qs) {
        return qs.stream().map(MiniGamePlayMapper::toQuestionRespHideKey).toList();
    }
}

