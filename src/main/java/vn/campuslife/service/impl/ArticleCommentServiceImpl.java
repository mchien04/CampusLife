package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.ArticleComment;
import vn.campuslife.entity.EventArticle;
import vn.campuslife.entity.Student;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.ArticleCommentRequest;
import vn.campuslife.model.ArticleCommentResponse;
import vn.campuslife.repository.ArticleCommentRepository;
import vn.campuslife.repository.EventArticleRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.ArticleCommentService;
import vn.campuslife.service.StudentService;
import vn.campuslife.util.ProfanityFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleCommentServiceImpl implements ArticleCommentService {

    private final ArticleCommentRepository commentRepository;
    private final EventArticleRepository articleRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final ProfanityFilter profanityFilter;

    @Override
    @Transactional
    public ArticleCommentResponse addComment(String slug, String username, ArticleCommentRequest request) {
        EventArticle article = articleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            throw new BadRequestException("Student not found for user: " + username);
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student entity not found for id: " + studentId));

        ArticleComment comment = new ArticleComment();
        comment.setArticle(article);
        comment.setStudent(student);
        comment.setContent(request.getContent().trim());

        // Kiểm tra từ ngữ thô tục
        boolean hasProfanity = profanityFilter.containsProfanity(comment.getContent());
        if (hasProfanity) {
            comment.setFlagged(true);
            comment.setFlagReason("PROFANITY");
            comment.setHidden(true);
        }

        if (request.getParentCommentId() != null) {
            ArticleComment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found: " + request.getParentCommentId()));
            if (!parent.getArticle().getId().equals(article.getId())) {
                throw new BadRequestException("Parent comment does not belong to the same article");
            }
            comment.setParentComment(parent);
        }

        ArticleComment saved = commentRepository.save(comment);
        return toResponse(saved, false);
    }

    @Override
    @Transactional
    public ArticleCommentResponse editComment(Long commentId, String username, ArticleCommentRequest request) {
        ArticleComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null || !comment.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You are not authorized to edit this comment");
        }

        if (comment.getCreatedAt().plusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot edit comment after 15 minutes");
        }

        comment.setContent(request.getContent().trim());

        // Kiểm tra lại từ ngữ thô tục
        boolean hasProfanity = profanityFilter.containsProfanity(comment.getContent());
        if (hasProfanity) {
            comment.setFlagged(true);
            comment.setFlagReason("PROFANITY");
            comment.setHidden(true);
        } else {
            // Nếu sửa từ bậy thành không bậy, tự động gỡ flag (hoặc tùy policy)
            comment.setFlagged(false);
            comment.setFlagReason(null);
            comment.setHidden(false);
        }

        comment.setEdited(true);
        comment.setEditedAt(LocalDateTime.now());

        ArticleComment saved = commentRepository.save(comment);
        return toResponse(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleCommentResponse> getArticleComments(String slug, boolean isAdmin, int page, int size) {
        EventArticle article = articleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleComment> commentPage;

        if (isAdmin) {
            commentPage = commentRepository.findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(article.getId(), pageable);
        } else {
            commentPage = commentRepository.findByArticleIdAndParentCommentIsNullAndIsHiddenFalseOrderByCreatedAtDesc(article.getId(), pageable);
        }

        return commentPage.map(comment -> toResponse(comment, isAdmin));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String username, boolean isAdmin) {
        ArticleComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        if (!isAdmin) {
            Long studentId = studentService.getStudentIdByUsername(username);
            if (studentId == null || !comment.getStudent().getId().equals(studentId)) {
                throw new BadRequestException("You are not authorized to delete this comment");
            }
        }

        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public ArticleCommentResponse hideComment(Long commentId, boolean hide) {
        ArticleComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        comment.setHidden(hide);
        ArticleComment saved = commentRepository.save(comment);
        return toResponse(saved, true);
    }

    private ArticleCommentResponse toResponse(ArticleComment comment, boolean isAdmin) {
        ArticleCommentResponse res = new ArticleCommentResponse();
        res.setId(comment.getId());
        res.setArticleId(comment.getArticle().getId());
        res.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);
        res.setContent(comment.getContent());
        res.setFlagged(comment.isFlagged());
        res.setFlagReason(comment.getFlagReason());
        res.setHidden(comment.isHidden());
        res.setAutoHidden(comment.isFlagged() && "PROFANITY".equals(comment.getFlagReason()) && comment.isHidden());
        res.setEdited(comment.isEdited());
        res.setEditedAt(comment.getEditedAt());
        res.setCreatedAt(comment.getCreatedAt());
        res.setUpdatedAt(comment.getUpdatedAt());

        if (comment.getStudent() != null) {
            ArticleCommentResponse.StudentBasicInfo studentInfo = new ArticleCommentResponse.StudentBasicInfo();
            studentInfo.setId(comment.getStudent().getId());
            studentInfo.setFullName(comment.getStudent().getFullName());
            studentInfo.setStudentCode(comment.getStudent().getStudentCode());
            
            String avatar = comment.getStudent().getAvatarUrl();
            studentInfo.setAvatarUrl(avatar != null ? avatar : "https://cdn.campuslife.vn/avatars/default.png");
            
            studentInfo.setDepartmentName(comment.getStudent().getDepartment() != null ? comment.getStudent().getDepartment().getName() : null);
            studentInfo.setClassName(comment.getStudent().getStudentClass() != null ? comment.getStudent().getStudentClass().getClassName() : null);
            
            res.setStudent(studentInfo);
        }

        if (comment.getReplies() != null) {
            List<ArticleCommentResponse> replies = comment.getReplies().stream()
                    .filter(r -> isAdmin || !r.isHidden())
                    .map(r -> toResponse(r, isAdmin))
                    .collect(Collectors.toList());
            res.setReplies(replies);
        }

        return res;
    }
}
