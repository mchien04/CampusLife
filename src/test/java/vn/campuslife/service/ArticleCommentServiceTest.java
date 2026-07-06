package vn.campuslife.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.campuslife.entity.*;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.ArticleCommentRequest;
import vn.campuslife.model.ArticleCommentResponse;
import vn.campuslife.repository.ArticleCommentRepository;
import vn.campuslife.repository.EventArticleRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.impl.ArticleCommentServiceImpl;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ArticleCommentServiceTest {

    @Mock
    private ArticleCommentRepository articleCommentRepository;

    @Mock
    private EventArticleRepository eventArticleRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private vn.campuslife.util.ProfanityFilter profanityFilter;

    @InjectMocks
    private ArticleCommentServiceImpl articleCommentService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddComment_ProfanityAutoHidden() {
        // Mock data
        String slug = "test-article";
        String username = "testuser";
        Long articleId = 1L;
        Long studentId = 1L;

        EventArticle article = new EventArticle();
        article.setId(articleId);

        Student student = new Student();
        student.setId(studentId);

        ArticleCommentRequest request = new ArticleCommentRequest();
        request.setContent("This is a đm test"); // Contains profanity

        // Mock behaviors
        when(eventArticleRepository.findBySlugAndIsPublishedTrue(slug)).thenReturn(Optional.of(article));
        when(studentService.getStudentIdByUsername(username)).thenReturn(studentId);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(profanityFilter.containsProfanity(request.getContent())).thenReturn(true);
        
        when(articleCommentRepository.save(any(ArticleComment.class))).thenAnswer(invocation -> {
            ArticleComment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Execute
        ArticleCommentResponse response = articleCommentService.addComment(slug, username, request);

        // Verify
        assertTrue(response.isAutoHidden());
        assertTrue(response.isHidden());
        assertEquals("This is a đm test", response.getContent());
        verify(articleCommentRepository, times(1)).save(any(ArticleComment.class));
    }

    @Test
    public void testAddComment_CleanContent() {
        // Mock data
        String slug = "test-article";
        String username = "testuser";
        Long articleId = 1L;
        Long studentId = 1L;

        EventArticle article = new EventArticle();
        article.setId(articleId);

        Student student = new Student();
        student.setId(studentId);
        student.setStudentClass(new StudentClass());
        student.getStudentClass().setClassName("IT01");
        student.setDepartment(new Department());
        student.getDepartment().setName("Khoa CNTT");

        ArticleCommentRequest request = new ArticleCommentRequest();
        request.setContent("This is a lovely test"); // Clean

        // Mock behaviors
        when(eventArticleRepository.findBySlugAndIsPublishedTrue(slug)).thenReturn(Optional.of(article));
        when(studentService.getStudentIdByUsername(username)).thenReturn(studentId);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(profanityFilter.containsProfanity(request.getContent())).thenReturn(false);
        
        when(articleCommentRepository.save(any(ArticleComment.class))).thenAnswer(invocation -> {
            ArticleComment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        when(articleCommentRepository.countByParentCommentIdAndIsHiddenFalse(any())).thenReturn(0);

        // Execute
        ArticleCommentResponse response = articleCommentService.addComment(slug, username, request);

        // Verify
        assertFalse(response.isAutoHidden());
        assertFalse(response.isHidden());
        assertEquals("This is a lovely test", response.getContent());
        assertEquals("IT01", response.getStudent().getClassName());
        assertEquals("Khoa CNTT", response.getStudent().getDepartmentName());
        verify(articleCommentRepository, times(1)).save(any(ArticleComment.class));
    }

    @Test
    public void testEditComment_CleanContent() {
        Long commentId = 1L;
        String username = "testuser";
        Long studentId = 1L;

        Student student = new Student();
        student.setId(studentId);

        EventArticle article = new EventArticle();
        article.setId(1L);

        ArticleComment comment = new ArticleComment();
        comment.setId(commentId);
        comment.setStudent(student);
        comment.setContent("Old content");
        comment.setCreatedAt(java.time.LocalDateTime.now());
        comment.setArticle(article);

        ArticleCommentRequest request = new ArticleCommentRequest();
        request.setContent("New clean content");

        when(studentService.getStudentIdByUsername(username)).thenReturn(studentId);
        when(articleCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(profanityFilter.containsProfanity(request.getContent())).thenReturn(false);
        when(articleCommentRepository.save(any(ArticleComment.class))).thenAnswer(i -> i.getArgument(0));

        ArticleCommentResponse response = articleCommentService.editComment(commentId, username, request);

        assertTrue(response.isEdited());
        assertFalse(response.isAutoHidden());
        assertEquals("New clean content", response.getContent());
    }

    @Test
    public void testEditComment_ProfanityContent() {
        Long commentId = 1L;
        String username = "testuser";
        Long studentId = 1L;

        Student student = new Student();
        student.setId(studentId);

        EventArticle article = new EventArticle();
        article.setId(1L);

        ArticleComment comment = new ArticleComment();
        comment.setId(commentId);
        comment.setStudent(student);
        comment.setContent("Old content");
        comment.setCreatedAt(java.time.LocalDateTime.now());
        comment.setArticle(article);

        ArticleCommentRequest request = new ArticleCommentRequest();
        request.setContent("New fuck content");

        when(studentService.getStudentIdByUsername(username)).thenReturn(studentId);
        when(articleCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(profanityFilter.containsProfanity(request.getContent())).thenReturn(true);
        when(articleCommentRepository.save(any(ArticleComment.class))).thenAnswer(i -> i.getArgument(0));

        ArticleCommentResponse response = articleCommentService.editComment(commentId, username, request);

        assertTrue(response.isEdited());
        assertTrue(response.isAutoHidden());
        assertEquals("New fuck content", response.getContent());
    }
}
