import re

file_path = r'd:\2025-2026 HKI\TLCN\campuslife\src\main\java\vn\campuslife\service\impl\ActivityRegistrationServiceImpl.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace block 1 (checkIn)
content = re.sub(r'if \(activity\.getSeriesId\(\) != null\)\s*\{.*?// Không fail check-out n?u update score l?i, ch? log\s*\}\s*\}',
                 '''participation.setPointsEarned(java.math.BigDecimal.ZERO);
                participationRepository.save(participation);
                try {
                    scoreRuleEngine.applyActivityCompleted(participation, registration.getStudent().getUser());
                } catch (Exception e) {
                    logger.error("Failed to apply activity rules: {}", e.getMessage(), e);
                }
                if (activity.getSeriesId() != null) {
                    try {
                        activitySeriesService.updateStudentProgress(
                                registration.getStudent().getId(),
                                activity.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to update series progress: {}", e.getMessage());
                    }
                }''', content, flags=re.DOTALL)

# Replace block 2 (checkInByQrCode) - similar block but for checkInByQrCode
content = re.sub(r'if \(activity\.getSeriesId\(\) != null\) \{.*?// Không throw d? không làm gián do?n check-in, nhung log d?y d?\s*\}\s*\}',
                 '''participation.setPointsEarned(java.math.BigDecimal.ZERO);
                    participationRepository.save(participation);
                    try {
                        scoreRuleEngine.applyActivityCompleted(participation, registration.getStudent().getUser());
                    } catch (Exception e) {
                        logger.error("Failed to apply activity rules: {}", e.getMessage(), e);
                    }
                    if (activity.getSeriesId() != null) {
                        try {
                            activitySeriesService.updateStudentProgress(
                                    registration.getStudent().getId(),
                                    activity.getId());
                        } catch (Exception e) {
                            logger.warn("Failed to update series progress: {}", e.getMessage());
                        }
                    }''', content, flags=re.DOTALL)

# Replace block 3 (gradeCompletion)
content = re.sub(r'if \(activity\.getSeriesId\(\) != null\) \{.*?updateStudentScoreFromParticipation\(participation\);',
                 '''participation.setIsCompleted(isCompleted);
            participation.setPointsEarned(java.math.BigDecimal.ZERO);
            participation.setParticipationType(vn.campuslife.enumeration.ParticipationType.COMPLETED);
            participationRepository.save(participation);
            
            try {
                scoreRuleEngine.applyActivityCompleted(participation, participation.getRegistration().getStudent().getUser());
            } catch (Exception e) {
                logger.error("Failed to apply activity rules: {}", e.getMessage(), e);
            }''', content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
