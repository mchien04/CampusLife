package vn.campuslife.service;

import java.util.Optional;

public interface RagService {
    Optional<String> findAnswer(String question);
}
