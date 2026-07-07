package vn.campuslife.service;

import org.springframework.data.domain.Page;
import vn.campuslife.model.ArticleCommentRequest;
import vn.campuslife.model.ArticleCommentResponse;

public interface ArticleCommentService {
    
    ArticleCommentResponse addComment(String slug, String username, ArticleCommentRequest request);

    ArticleCommentResponse editComment(Long commentId, String username, ArticleCommentRequest request);

    Page<ArticleCommentResponse> getArticleComments(String slug, boolean isAdmin, int page, int size);

    void deleteComment(Long commentId, String username, boolean isAdmin);

    ArticleCommentResponse hideComment(Long commentId, boolean hide);
}
