package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ScoreRuleEngine;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivitySeriesServiceImplTest {

    @Mock
    private ActivitySeriesRepository seriesRepository;

    @Mock
    private StudentSeriesProgressRepository progressRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ScoreRuleEngine scoreRuleEngine;

    @InjectMocks
    private ActivitySeriesServiceImpl activitySeriesService;

    private Student student;
    private User studentUser;
    private Activity activity;
    private ActivitySeries series;
    private StudentSeriesProgress progress;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(33L);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);

        series = new ActivitySeries();
        series.setId(800L);
        series.setMilestonePoints("{\"3\":5,\"5\":10}");

        activity = new Activity();
        activity.setId(100L);
        activity.setSeriesId(800L);

        progress = new StudentSeriesProgress();
        progress.setId(900L);
        progress.setStudent(student);
        progress.setSeries(series);
        progress.setCompletedActivityIds("[]");
        progress.setCompletedCount(0);
        progress.setPointsEarned(BigDecimal.ZERO);
    }

    @Test
    void updateStudentProgress_NewProgressRecord_AddsActivityAndTriggersMilestone() {
        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(seriesRepository.findById(800L)).thenReturn(Optional.of(series));
        when(progressRepository.findByStudentIdAndSeriesId(10L, 800L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(progress));

        when(progressRepository.save(any(StudentSeriesProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Response response = activitySeriesService.updateStudentProgress(10L, 100L);

        assertTrue(response.isStatus());
        StudentSeriesProgress resultProgress = (StudentSeriesProgress) response.getBody();
        assertNotNull(resultProgress);
        assertEquals(1, resultProgress.getCompletedCount());
        assertTrue(resultProgress.getCompletedActivityIds().contains("100"));

        verify(progressRepository).save(any(StudentSeriesProgress.class));
        verify(scoreRuleEngine).applySeriesMilestone(any(StudentSeriesProgress.class), eq(studentUser));
    }

    @Test
    void updateStudentProgress_DuplicateActivity_NoOp() {
        progress.setCompletedActivityIds("[100]");
        progress.setCompletedCount(1);

        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(seriesRepository.findById(800L)).thenReturn(Optional.of(series));
        when(progressRepository.findByStudentIdAndSeriesId(10L, 800L)).thenReturn(Optional.of(progress));

        Response response = activitySeriesService.updateStudentProgress(10L, 100L);

        assertTrue(response.isStatus());
        verify(progressRepository, never()).save(any());
        verifyNoInteractions(scoreRuleEngine);
    }

    @Test
    void calculateMilestonePoints_ProgressNotFound_ReturnsError() {
        when(progressRepository.findByStudentIdAndSeriesId(10L, 800L)).thenReturn(Optional.empty());

        Response response = activitySeriesService.calculateMilestonePoints(10L, 800L);

        assertFalse(response.isStatus());
        assertEquals("Progress not found", response.getMessage());
    }
}
