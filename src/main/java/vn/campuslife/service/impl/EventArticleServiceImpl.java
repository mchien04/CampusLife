package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.RegistrationCtaStatus;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ArticleType;
import vn.campuslife.enumeration.ReactionType;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.ArticleCategoryRequest;
import vn.campuslife.model.ArticleCategoryResponse;
import vn.campuslife.model.ArticleDetailResponse;
import vn.campuslife.model.ArticleHistoryResponse;
import vn.campuslife.model.ArticleImageRequest;
import vn.campuslife.model.ArticleImageResponse;
import vn.campuslife.model.ArticleListResponse;
import vn.campuslife.model.ArticleStatisticsResponse;
import vn.campuslife.model.ArticleTagRequest;
import vn.campuslife.model.ArticleTagResponse;
import vn.campuslife.model.ArticleWishlistItemResponse;
import vn.campuslife.model.EventArticleAdminResponse;
import vn.campuslife.model.EventArticleUpsertRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.EventArticleService;
import vn.campuslife.service.StudentService;
import vn.campuslife.util.UrlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventArticleServiceImpl implements EventArticleService {

    private final EventArticleRepository eventArticleRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;
    private final EventArticleSlugHistoryRepository slugHistoryRepository;
    private final ActivityRegistrationService activityRegistrationService;
    private final StudentService studentService;
    private final ArticleCategoryRepository categoryRepository;
    private final ArticleTagRepository tagRepository;
    private final ArticleWishlistRepository wishlistRepository;
    private final ArticleImageRepository imageRepository;

    private final  ArticleViewHistoryRepository articleViewHistoryRepository;
    private final StudentRepository studentRepository;
    private final ArticleReactionRepository articleReactionRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final vn.campuslife.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${app.upload.public-url:http://localhost:8080}")
    private String publicUrl;

    // ============== STUDENT PUBLIC APIs ==============

    @Override
    @Transactional(readOnly = true)
    public ArticleListResponse getPublishedArticleBySlug(String slug) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
        return toListResponse(article, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleListResponse> getAllPublishedArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventArticle> articles = eventArticleRepository.findAllPublishedOrderByPinnedAndPriority(pageable);
        return articles.map(a -> toListResponse(a, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleListResponse> getAllArticlesForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventArticle> articles = eventArticleRepository.findAllOrderByPinnedAndPriority(pageable);
        return articles.map(a -> toListResponse(a, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleListResponse> getPublishedArticlesByCategory(String categorySlug, int page, int size) {
        ArticleCategory category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categorySlug));
        Pageable pageable = PageRequest.of(page, size);
        Page<EventArticle> articles = eventArticleRepository.findByCategoryId(category.getId(), pageable);
        return articles.map(a -> toListResponse(a, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleListResponse> searchPublishedArticles(String keyword, int page, int size) {
        String searchPattern = "%" + keyword.toLowerCase() + "%";
        Pageable pageable = PageRequest.of(page, size);
        Page<EventArticle> articles = eventArticleRepository.searchArticles(searchPattern, pageable);
        return articles.map(a -> toListResponse(a, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleListResponse> getFeaturedArticles(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<EventArticle> articles = eventArticleRepository.findFeaturedArticles(pageable);
        return articles.stream().map(a -> toListResponse(a, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<vn.campuslife.model.ArticleCategoryPublicResponse> getPublicCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(c -> {
                    Long count = eventArticleRepository.countPublishedArticlesByCategory(c.getId());
                    return new vn.campuslife.model.ArticleCategoryPublicResponse(c.getId(), c.getName(), c.getSlug(), count);
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<vn.campuslife.model.ArticleTagPublicResponse> getPublicTags() {
        return tagRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(t -> {
                    Long count = eventArticleRepository.countPublishedArticlesByTag(t.getId());
                    return new vn.campuslife.model.ArticleTagPublicResponse(t.getId(), t.getName(), t.getSlug(), count);
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<vn.campuslife.model.ArticleListResponse> getPublishedArticlesByTag(String tagSlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventArticle> articles = eventArticleRepository.findPublishedArticlesByTag(tagSlug, pageable);
        return articles.map(a -> toListResponse(a, null));
    }

    @Override
    public ArticleDetailResponse getArticleDetailBySlug(String slug, Long studentId) {
        Optional<EventArticle> articleOpt = eventArticleRepository.findBySlugAndIsPublishedTrue(slug);

        if (articleOpt.isEmpty()) {
            Optional<EventArticleSlugHistory> historyOpt = slugHistoryRepository.findByOldSlug(slug);
            if (historyOpt.isPresent()) {
                String newSlug = historyOpt.get().getArticle().getSlug();
                ArticleDetailResponse response = getArticleDetailBySlug(newSlug, studentId);
                response.setRedirectedFrom(slug);
                response.setCurrentSlug(newSlug);
                return response;
            }
            throw new ResourceNotFoundException("Article not found with slug: " + slug);
        }

        EventArticle article = articleOpt.get();

        if (studentId != null) {
            boolean viewedRecently = articleViewHistoryRepository
                    .existsByStudentIdAndArticleIdAndViewedAtAfter(
                            studentId,
                            article.getId(),
                            LocalDateTime.now().minusDays(1)
                    );

            if (!viewedRecently) {
                ArticleViewHistory history = new ArticleViewHistory();
                history.setArticle(article);
                history.setStudent(studentRepository.getReferenceById(studentId));
                articleViewHistoryRepository.save(history);
                
                article.setViewCount(article.getViewCount() + 1);
                eventArticleRepository.save(article);
            }
        } else {
            article.setViewCount(article.getViewCount() + 1);
            eventArticleRepository.save(article);
        }

        ArticleDetailResponse response = new ArticleDetailResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSlug(article.getSlug());
        response.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
        response.setContent(article.getContent());
        response.setSeoTitle(article.getSeoTitle());
        response.setSeoDescription(article.getSeoDescription());
        response.setPublished(article.isPublished());
        response.setPublishedAt(article.getPublishedAt());
        response.setViewCount(article.getViewCount());
        response.setWishlistCount(article.getWishlistCount());
        response.setFeatured(article.isFeatured());
        response.setPinned(article.isPinned());
        response.setPriority(article.getPriority());
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());

        Activity activity = article.getActivity();

        if (activity != null) {
            RegistrationCtaStatus ctaStatus = resolveRegistrationStatus(activity, LocalDateTime.now());
            response.setRegistrationStatus(ctaStatus.name());

            ArticleDetailResponse.ArticleActivityInfo activityInfo = new ArticleDetailResponse.ArticleActivityInfo();
            activityInfo.setId(activity.getId());
            activityInfo.setName(activity.getName());
            activityInfo.setLocation(activity.getLocation());
            activityInfo.setStartDate(activity.getStartDate());
            activityInfo.setEndDate(activity.getEndDate());
            activityInfo.setRegistrationStartDate(activity.getRegistrationStartDate());
            activityInfo.setRegistrationDeadline(activity.getRegistrationDeadline());
            
            activityInfo.setShareLink(activity.getShareLink());
            response.setActivityInfo(activityInfo);
        }

        if (article.getCategory() != null) {
            ArticleDetailResponse.ArticleCategoryInfo catInfo = new ArticleDetailResponse.ArticleCategoryInfo();
            catInfo.setId(article.getCategory().getId());
            catInfo.setName(article.getCategory().getName());
            catInfo.setSlug(article.getCategory().getSlug());
            response.setCategory(catInfo);
        }

        if (article.getTags() != null && !article.getTags().isEmpty()) {
            response.setTags(article.getTags().stream().map(ArticleTag::getName).collect(Collectors.toList()));
        }

        List<ArticleImage> images = imageRepository.findByArticleIdOrderByDisplayOrderAsc(article.getId());
        if (images != null && !images.isEmpty()) {
            List<ArticleImageResponse> imageResponses = images.stream().map(this::toImageResponse)
                    .collect(Collectors.toList());
            response.setImages(imageResponses);
            response.setCoverImages(
                    imageResponses.stream().filter(ArticleImageResponse::isCover).collect(Collectors.toList()));
        }

        if (studentId != null) {
            response.setWishlisted(wishlistRepository.existsByArticleIdAndStudentId(article.getId(), studentId));
            Optional<ArticleReaction> reactionOpt = articleReactionRepository.findByArticleIdAndStudentId(article.getId(), studentId);
            if (reactionOpt.isPresent()) {
                response.setMyReaction(reactionOpt.get().getReactionType());
            } else {
                response.setMyReaction(null);
            }
        } else {
            response.setWishlisted(false);
            response.setMyReaction(null);
        }

        response.setCommentCount(articleCommentRepository.countByArticleIdAndIsHiddenFalse(article.getId()));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleListResponse> getRelatedArticles(String slug, int limit) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Pageable pageable = PageRequest.of(0, limit);
        List<EventArticle> related;

        if (article.getCategory() != null) {
            related = eventArticleRepository.findRelatedByCategoryId(article.getId(), article.getCategory().getId(),
                    pageable);
        } else {
            related = new ArrayList<>();
        }

        if (related.size() < limit) {
            eventArticleRepository.findAll().stream()
                    .filter(a -> a.isPublished() && !a.getSlug().equals(slug))
                    .filter(a -> article.getCategory() == null || article.getCategory() != a.getCategory())
                    .limit(limit - related.size())
                    .forEach(related::add);
        }

        return related.stream().limit(limit).map(a -> toListResponse(a, null)).collect(Collectors.toList());
    }

    // ============== WISHLIST APIs ==============

    @Override
    @Transactional
    public vn.campuslife.model.Response addToWishlist(String slug, String username) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return new vn.campuslife.model.Response(false, "Student not found", null);
        }

        if (wishlistRepository.existsByArticleIdAndStudentId(article.getId(), studentId)) {
            return new vn.campuslife.model.Response(false, "Already in wishlist", null);
        }

        Student student = new Student();
        student.setId(studentId);

        ArticleWishlist wishlist = new ArticleWishlist();
        wishlist.setArticle(article);
        wishlist.setStudent(student);
        wishlistRepository.save(wishlist);

        article.setWishlistCount(article.getWishlistCount() + 1);
        eventArticleRepository.save(article);

        return new vn.campuslife.model.Response(true, "Added to wishlist", null);
    }

    @Override
    @Transactional
    public vn.campuslife.model.Response removeFromWishlist(String slug, String username) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return new vn.campuslife.model.Response(false, "Student not found", null);
        }

        Optional<ArticleWishlist> wishlist = wishlistRepository.findByArticleIdAndStudentId(article.getId(), studentId);
        if (wishlist.isEmpty()) {
            return new vn.campuslife.model.Response(false, "Not in wishlist", null);
        }

        wishlistRepository.delete(wishlist.get());

        article.setWishlistCount(Math.max(0, article.getWishlistCount() - 1));
        eventArticleRepository.save(article);

        return new vn.campuslife.model.Response(true, "Removed from wishlist", null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleWishlistItemResponse> getStudentWishlist(String username, int page, int size) {
        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleWishlist> wishlists = wishlistRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);

        return wishlists.map(wishlist -> {
            EventArticle article = wishlist.getArticle();
            ArticleWishlistItemResponse item = new ArticleWishlistItemResponse();
            item.setId(wishlist.getId());
            item.setArticleId(article.getId());
            item.setTitle(article.getTitle());
            item.setSlug(article.getSlug());
            item.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
            item.setSeoDescription(article.getSeoDescription());
            item.setPublished(article.isPublished());
            item.setPublishedAt(article.getPublishedAt());

            if (article.getActivity() != null) {
                item.setRegistrationStatus(
                        resolveRegistrationStatus(article.getActivity(), LocalDateTime.now()).name());
            }
            item.setWishlistedAt(wishlist.getCreatedAt());
            return item;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(String slug, String username) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug).orElse(null);
        if (article == null || username == null)
            return false;

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null)
            return false;

        return wishlistRepository.existsByArticleIdAndStudentId(article.getId(), studentId);
    }

    // ============== ADMIN CRUD ==============

    @Override
    @Transactional
    public EventArticleAdminResponse createArticle(EventArticleUpsertRequest request) {
        validateUpsertRequest(request, true);

        String slug = normalizeSlug(request.getSlug());
        if (eventArticleRepository.existsBySlug(slug)) {
            throw new BadRequestException("Slug already exists");
        }

        EventArticle article = new EventArticle();
        article.setTitle(request.getTitle().trim());
        article.setSlug(slug);
        article.setThumbnailUrl(trimToNull(request.getThumbnailUrl()));
        article.setContent(request.getContent());
        article.setSeoTitle(trimToNull(request.getSeoTitle()));
        article.setSeoDescription(trimToNull(request.getSeoDescription()));
        article.setPublished(false);
        article.setPublishedAt(null);
        article.setViewCount(0L);
        article.setWishlistCount(0L);
        article.setFeatured(request.isFeatured());
        article.setPinned(request.isPinned());
        article.setPriority(request.getPriority());
        article.setArticleType(request.getArticleType() != null ? request.getArticleType() : ArticleType.ANNOUNCEMENT);

        if (request.getActivityId() != null) {
            Activity activity = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + request.getActivityId()));
            article.setActivity(activity);

            boolean hasExisting = !eventArticleRepository.findByActivityId(request.getActivityId()).isEmpty();
            boolean shouldBePrimary = request.isPrimary() || !hasExisting;
            article.setPrimary(shouldBePrimary);

            if (shouldBePrimary) {
                List<EventArticle> others = eventArticleRepository.findByActivityId(request.getActivityId());
                others.forEach(a -> a.setPrimary(false));
                eventArticleRepository.saveAll(others);
            }
        } else {
            article.setActivity(null);
            article.setPrimary(false);
        }

        if (request.getCategoryId() != null) {
            ArticleCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            article.setCategory(category);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<ArticleTag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            article.setTags(tags);
        }

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public EventArticleAdminResponse updateArticle(Long articleId, EventArticleUpsertRequest request) {
        validateUpsertRequest(request, false);

        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        Long oldActivityId = article.getActivity() != null ? article.getActivity().getId() : null;
        Long newActivityId = request.getActivityId();

        boolean activityChanged = !Objects.equals(oldActivityId, newActivityId);

        if (activityChanged) {
            if (newActivityId != null) {
                Activity newActivity = activityRepository.findByIdAndIsDeletedFalse(newActivityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + newActivityId));
                article.setActivity(newActivity);

                boolean hasExisting = !eventArticleRepository.findByActivityId(newActivityId).isEmpty();
                boolean shouldBePrimary = request.isPrimary() || !hasExisting;
                article.setPrimary(shouldBePrimary);

                if (shouldBePrimary) {
                    List<EventArticle> others = eventArticleRepository.findByActivityId(newActivityId);
                    others.forEach(a -> {
                        if (!a.getId().equals(articleId)) {
                            a.setPrimary(false);
                        }
                    });
                    eventArticleRepository.saveAll(others);
                }
            } else {
                article.setActivity(null);
                article.setPrimary(false);
            }

            if (oldActivityId != null && article.isPrimary()) {
                List<EventArticle> oldArticles = eventArticleRepository.findByActivityId(oldActivityId);
                oldArticles.stream()
                        .filter(a -> !a.getId().equals(articleId))
                        .min(Comparator.comparing(EventArticle::getId))
                        .ifPresent(oldest -> {
                            oldest.setPrimary(true);
                            eventArticleRepository.save(oldest);
                        });
            }
        } else {
            if (article.getActivity() != null) {
                if (request.isPrimary()) {
                    article.setPrimary(true);
                    List<EventArticle> others = eventArticleRepository.findByActivityId(article.getActivity().getId());
                    others.forEach(a -> {
                        if (!a.getId().equals(articleId)) {
                            a.setPrimary(false);
                        }
                    });
                    eventArticleRepository.saveAll(others);
                } else {
                    if (article.isPrimary()) {
                        article.setPrimary(false);
                        List<EventArticle> others = eventArticleRepository.findByActivityId(article.getActivity().getId());
                        others.stream()
                                .filter(a -> !a.getId().equals(articleId))
                                .min(Comparator.comparing(EventArticle::getId))
                                .ifPresent(oldest -> {
                                    oldest.setPrimary(true);
                                    eventArticleRepository.save(oldest);
                                });
                    }
                }
            } else {
                article.setPrimary(false);
            }
        }

        article.setArticleType(request.getArticleType() != null ? request.getArticleType() : ArticleType.ANNOUNCEMENT);

        String oldSlug = article.getSlug();
        String newSlug = normalizeSlug(request.getSlug());
        if (!newSlug.equals(oldSlug)) {
            if (eventArticleRepository.existsBySlug(newSlug)) {
                throw new BadRequestException("New slug already exists");
            }
            vn.campuslife.entity.EventArticleSlugHistory history = new vn.campuslife.entity.EventArticleSlugHistory();
            history.setArticle(article);
            history.setOldSlug(oldSlug);
            slugHistoryRepository.save(history);
            article.setSlug(newSlug);
        }

        article.setTitle(request.getTitle().trim());
        article.setThumbnailUrl(trimToNull(request.getThumbnailUrl()));
        article.setContent(request.getContent());
        article.setSeoTitle(trimToNull(request.getSeoTitle()));
        article.setSeoDescription(trimToNull(request.getSeoDescription()));
        article.setFeatured(request.isFeatured());
        article.setPinned(request.isPinned());
        article.setPriority(request.getPriority());

        if (request.getCategoryId() != null) {
            ArticleCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            article.setCategory(category);
        } else {
            article.setCategory(null);
        }

        if (request.getTagIds() != null) {
            Set<ArticleTag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            article.setTags(tags);
        } else {
            article.setTags(new HashSet<>());
        }

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public EventArticleAdminResponse publishArticle(Long articleId) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        if (!article.isPublished()) {
            article.setPublished(true);
            article.setPublishedAt(LocalDateTime.now());

            try {
                List<ArticleWishlist> wishlists = wishlistRepository.findByArticleId(articleId);
                if (wishlists != null && !wishlists.isEmpty()) {
                    List<Long> studentUserIds = wishlists.stream()
                        .map(w -> w.getStudent().getUser().getId())
                        .collect(Collectors.toList());

                    notificationService.sendBulkNotification(
                        studentUserIds,
                        "Bài viết mới được xuất bản",
                        "Bài viết \"" + article.getTitle() + "\" trong danh sách quan tâm của bạn đã được xuất bản.",
                        vn.campuslife.enumeration.NotificationType.ARTICLE_PUBLISHED,
                        "/articles/" + article.getSlug(),
                        null
                    );
                }
            } catch (Exception e) {
                // Ignore notification failure to keep publishing transactional integrity
            }
        }

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public EventArticleAdminResponse unpublishArticle(Long articleId) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        if (article.isPublished()) {
            article.setPublished(false);
            article.setPublishedAt(null);
        }

        EventArticle saved = eventArticleRepository.save(article);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventArticleAdminResponse getArticleById(Long articleId) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));
        return toAdminResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventArticleAdminResponse> getArticlesByActivityId(Long activityId) {
        List<EventArticle> articles = eventArticleRepository.findByActivityId(activityId);
        return articles.stream().map(this::toAdminResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventArticleAdminResponse setPrimaryArticle(Long articleId) {
        EventArticle target = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        if (target.getActivity() == null) {
            throw new BadRequestException("Standalone articles cannot be primary");
        }

        Long activityId = target.getActivity().getId();

        List<EventArticle> others = eventArticleRepository.findByActivityId(activityId);
        others.forEach(a -> {
            if (!a.getId().equals(articleId)) {
                a.setPrimary(false);
            }
        });
        eventArticleRepository.saveAll(others);

        target.setPrimary(true);
        return toAdminResponse(eventArticleRepository.save(target));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleListResponse> getArticlesBySeriesId(Long seriesId) {
        List<EventArticle> articles = eventArticleRepository.findPublishedBySeriesId(seriesId);
        return articles.stream().map(a -> toListResponse(a, null)).collect(Collectors.toList());
    }

    // ============== ADMIN STATISTICS ==============

    @Override
    @Transactional(readOnly = true)
    public ArticleStatisticsResponse getArticleStatistics() {
        ArticleStatisticsResponse stats = new ArticleStatisticsResponse();

        long total = eventArticleRepository.count();
        long published = eventArticleRepository.countPublishedArticles();

        stats.setTotalArticles(total);
        stats.setPublishedArticles(published);
        stats.setDraftArticles(total - published);
        stats.setTotalViews(
                eventArticleRepository.sumTotalViews() != null ? eventArticleRepository.sumTotalViews() : 0L);

        eventArticleRepository.findAll().forEach(a -> {
            stats.setTotalWishlists(stats.getTotalWishlists() + a.getWishlistCount());
        });

        stats.setFeaturedArticles(eventArticleRepository.countFeaturedArticles());
        stats.setPinnedArticles(eventArticleRepository.findAll().stream().filter(EventArticle::isPinned).count());

        List<Map<String, Object>> topViewed = new ArrayList<>();
        eventArticleRepository.findAll().stream()
                .filter(EventArticle::isPublished)
                .sorted((a, b) -> Long.compare(b.getViewCount(), a.getViewCount()))
                .limit(5)
                .forEach(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", a.getId());
                    item.put("title", a.getTitle());
                    item.put("slug", a.getSlug());
                    item.put("viewCount", a.getViewCount());
                    topViewed.add(item);
                });
        stats.setTopViewedArticles(topViewed);

        List<Map<String, Object>> recent = new ArrayList<>();
        eventArticleRepository.findAll().stream()
                .filter(EventArticle::isPublished)
                .sorted((a, b) -> {
                    if (a.getPublishedAt() == null && b.getPublishedAt() == null)
                        return 0;
                    if (a.getPublishedAt() == null)
                        return 1;
                    if (b.getPublishedAt() == null)
                        return -1;
                    return b.getPublishedAt().compareTo(a.getPublishedAt());
                })
                .limit(5)
                .forEach(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", a.getId());
                    item.put("title", a.getTitle());
                    item.put("slug", a.getSlug());
                    item.put("publishedAt", a.getPublishedAt());
                    recent.add(item);
                });
        stats.setRecentlyPublished(recent);

        Map<String, Long> byCategory = new HashMap<>();
        eventArticleRepository.findAll().stream()
                .filter(ea -> ea.getCategory() != null)
                .forEach(ea -> {
                    String name = ea.getCategory().getName();
                    byCategory.put(name, byCategory.getOrDefault(name, 0L) + 1);
                });
        stats.setArticlesByCategory(byCategory);

        return stats;
    }

    // ============== CATEGORY MANAGEMENT ==============

    @Override
    @Transactional(readOnly = true)
    public List<ArticleCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ArticleCategoryResponse createCategory(ArticleCategoryRequest request) {
        ArticleCategory category = new ArticleCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setSlug(normalizeSlug(request.getSlug() != null ? request.getSlug() : request.getName()));
        category.setDisplayOrder(request.getDisplayOrder());
        category.setActive(request.isActive());
        ArticleCategory saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Override
    @Transactional
    public ArticleCategoryResponse updateCategory(Long categoryId, ArticleCategoryRequest request) {
        ArticleCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getSlug() != null) {
            category.setSlug(normalizeSlug(request.getSlug()));
        }
        category.setDisplayOrder(request.getDisplayOrder());
        category.setActive(request.isActive());
        ArticleCategory saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        ArticleCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        categoryRepository.delete(category);
    }

    // ============== TAG MANAGEMENT ==============

    @Override
    @Transactional(readOnly = true)
    public List<ArticleTagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ArticleTagResponse createTag(ArticleTagRequest request) {
        ArticleTag tag = new ArticleTag();
        tag.setName(request.getName());
        tag.setSlug(normalizeSlug(request.getSlug() != null ? request.getSlug() : request.getName()));
        tag.setActive(request.isActive());
        ArticleTag saved = tagRepository.save(tag);
        return toTagResponse(saved);
    }

    @Override
    @Transactional
    public ArticleTagResponse updateTag(Long tagId, ArticleTagRequest request) {
        ArticleTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
        
        tag.setName(request.getName());
        if (request.getSlug() != null) {
            tag.setSlug(normalizeSlug(request.getSlug()));
        }
        tag.setActive(request.isActive());
        ArticleTag saved = tagRepository.save(tag);
        return toTagResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        ArticleTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
        tagRepository.delete(tag);
    }

    // ============== IMAGE MANAGEMENT ==============

    @Override
    @Transactional
    public ArticleImageResponse addImageToArticle(Long articleId, ArticleImageRequest request) {
        EventArticle article = eventArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        ArticleImage image = new ArticleImage();
        image.setArticle(article);
        image.setImageUrl(request.getImageUrl());
        image.setCaption(request.getCaption());
        image.setDisplayOrder(request.getDisplayOrder());
        image.setCover(request.isCover());
        ArticleImage saved = imageRepository.save(image);
        return toImageResponse(saved);
    }

    @Override
    @Transactional
    public void removeImageFromArticle(Long articleId, Long imageId) {
        ArticleImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        if (!image.getArticle().getId().equals(articleId)) {
            throw new BadRequestException("Image does not belong to this article");
        }
        imageRepository.delete(image);
    }

    // ============== WAITLIST & CALENDAR ==============

    @Override
    @Transactional
    public vn.campuslife.model.Response registerForWaitlist(String slug, String username) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return new vn.campuslife.model.Response(false, "Student not found", null);
        }

        return activityRegistrationService.registerForWaitlist(article.getActivity().getId(), studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateIcsFile(String slug) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Activity activity = article.getActivity();
        LocalDateTime start = activity.getStartDate();
        LocalDateTime end = activity.getEndDate();

        if (start == null)
            start = LocalDateTime.now();
        if (end == null)
            end = start.plusHours(2);

        String startStr = start.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        String endStr = end.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PROID:-//CampusLife//Event//EN\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(article.getId()).append("@campuslife.vn\r\n");
        ics.append("DTSTAMP:").append(startStr).append("\r\n");
        ics.append("DTSTART:").append(startStr).append("\r\n");
        ics.append("DTEND:").append(endStr).append("\r\n");
        ics.append("SUMMARY:").append(article.getTitle()).append("\r\n");
        ics.append("DESCRIPTION:").append(article.getSeoDescription() != null ? article.getSeoDescription() : "")
                .append("\r\n");
        ics.append("LOCATION:").append(activity.getLocation() != null ? activity.getLocation() : "TBA").append("\r\n");
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        return ics.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ============== ANALYTICS ==============

    @Override
    @Transactional
    public void trackView(String slug) {
        eventArticleRepository.findBySlugAndIsPublishedTrue(slug).ifPresent(article -> {
            article.setViewCount(article.getViewCount() + 1);
            eventArticleRepository.save(article);
        });
    }

    // ============== HELPER METHODS ==============

    private ArticleListResponse toListResponse(EventArticle article, Long studentId) {
        ArticleListResponse response = new ArticleListResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSlug(article.getSlug());
        response.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
        response.setSeoDescription(article.getSeoDescription());
        response.setPublished(article.isPublished());
        response.setPublishedAt(article.getPublishedAt());
        response.setViewCount(article.getViewCount());
        response.setWishlistCount(article.getWishlistCount());
        response.setFeatured(article.isFeatured());
        response.setPinned(article.isPinned());

        Activity activity = article.getActivity();
        if (activity != null) {
            response.setRegistrationStatus(resolveRegistrationStatus(activity, LocalDateTime.now()).name());
            response.setActivityId(activity.getId());
            response.setShareLink(activity.getShareLink());
        }
        response.setArticleType(article.getArticleType());
        response.setPrimary(article.isPrimary());

        if (article.getCategory() != null) {
            response.setCategoryName(article.getCategory().getName());
        }

        if (article.getTags() != null && !article.getTags().isEmpty()) {
            response.setTags(article.getTags().stream().map(ArticleTag::getName).collect(Collectors.toList()));
        }

        List<ArticleImage> images = imageRepository.findByArticleIdOrderByDisplayOrderAsc(article.getId());
        if (images != null && !images.isEmpty()) {
            response.setImages(images.stream().map(this::toImageResponse).collect(Collectors.toList()));
        }

        response.setCommentCount(articleCommentRepository.countByArticleIdAndIsHiddenFalse(article.getId()));

        return response;
    }

    private EventArticleAdminResponse toAdminResponse(EventArticle article) {
        EventArticleAdminResponse res = new EventArticleAdminResponse();
        res.setId(article.getId());
        if (article.getActivity() != null) {
            res.setActivityId(article.getActivity().getId());
            res.setActivityName(article.getActivity().getName());
        }
        res.setArticleType(article.getArticleType());
        res.setPrimary(article.isPrimary());
        res.setTitle(article.getTitle());
        res.setSlug(article.getSlug());
        res.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
        res.setContent(article.getContent());
        res.setSeoTitle(article.getSeoTitle());
        res.setSeoDescription(article.getSeoDescription());
        res.setPublished(article.isPublished());
        res.setPublishedAt(article.getPublishedAt());
        res.setViewCount(article.getViewCount());
        res.setWishlistCount(article.getWishlistCount());
        res.setFeatured(article.isFeatured());
        res.setPinned(article.isPinned());
        res.setPriority(article.getPriority());
        res.setCreatedAt(article.getCreatedAt());
        res.setUpdatedAt(article.getUpdatedAt());

        if (article.getCategory() != null) {
            res.setCategoryName(article.getCategory().getName());
            res.setCategoryId(article.getCategory().getId());
        }

        if (article.getTags() != null && !article.getTags().isEmpty()) {
            res.setTagNames(article.getTags().stream().map(ArticleTag::getName).collect(Collectors.toList()));
        }

        return res;
    }

    private ArticleCategoryResponse toCategoryResponse(ArticleCategory category) {
        ArticleCategoryResponse res = new ArticleCategoryResponse();
        res.setId(category.getId());
        res.setName(category.getName());
        res.setDescription(category.getDescription());
        res.setSlug(category.getSlug());
        res.setDisplayOrder(category.getDisplayOrder());
        res.setActive(category.isActive());
        res.setCreatedAt(category.getCreatedAt());
        return res;
    }

    private ArticleTagResponse toTagResponse(ArticleTag tag) {
        ArticleTagResponse res = new ArticleTagResponse();
        res.setId(tag.getId());
        res.setName(tag.getName());
        res.setSlug(tag.getSlug());
        res.setActive(tag.isActive());
        res.setCreatedAt(tag.getCreatedAt());
        return res;
    }

    private ArticleImageResponse toImageResponse(ArticleImage image) {
        ArticleImageResponse res = new ArticleImageResponse();
        res.setId(image.getId());
        res.setImageUrl(UrlUtils.toFullUrl(image.getImageUrl(), publicUrl));
        res.setCaption(image.getCaption());
        res.setDisplayOrder(image.getDisplayOrder());
        res.setCover(image.isCover());
        res.setCreatedAt(image.getCreatedAt());
        return res;
    }

    private RegistrationCtaStatus resolveRegistrationStatus(Activity activity, LocalDateTime now) {
        LocalDateTime startDate = activity.getRegistrationStartDate();
        LocalDateTime deadline = activity.getRegistrationDeadline();

        if (startDate != null && now.isBefore(startDate)) {
            return RegistrationCtaStatus.UPCOMING;
        }

        if (deadline != null && now.isAfter(deadline)) {
            return RegistrationCtaStatus.CLOSED;
        }

        Long approvedCount = activityRegistrationRepository.countByActivityIdAndStatus(activity.getId(),
                RegistrationStatus.APPROVED);
        if (activity.getTicketQuantity() != null && approvedCount >= activity.getTicketQuantity()) {
            return RegistrationCtaStatus.WAITLIST;
        }

        return RegistrationCtaStatus.OPEN;
    }

    private void validateUpsertRequest(EventArticleUpsertRequest request, boolean isCreate) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (request.getSlug() == null || request.getSlug().isBlank()) {
            request.setSlug(generateSlug(request.getTitle()));
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("Content is required");
        }
    }

    private String generateSlug(String title) {
        if (title == null || title.isBlank()) return "";
        String slug = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.toLowerCase();
        slug = slug.replaceAll("đ", "d");
        slug = slug.replaceAll("[^a-z0-9\\-]", "-");
        slug = slug.replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");
        return slug;
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank())
            return "";
        return slug.toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String trimToNull(String str) {
        return (str == null || str.isBlank()) ? null : str.trim();
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.length() == 10) {
                return java.time.LocalDate.parse(dateStr).atStartOfDay();
            }
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional
    public Response addReaction(String slug, String username, vn.campuslife.enumeration.ReactionType type) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return new Response(false, "Student not found", null);
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student entity not found"));

        Optional<ArticleReaction> reactionOpt = articleReactionRepository.findByArticleIdAndStudentId(article.getId(), studentId);
        if (reactionOpt.isPresent()) {
            ArticleReaction reaction = reactionOpt.get();
            reaction.setReactionType(type);
            articleReactionRepository.save(reaction);
        } else {
            ArticleReaction reaction = new ArticleReaction();
            reaction.setArticle(article);
            reaction.setStudent(student);
            reaction.setReactionType(type);
            articleReactionRepository.save(reaction);
        }

        return Response.success("Reaction added/updated", null);
    }

    @Override
    @Transactional
    public Response removeReaction(String slug, String username) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return new Response(false, "Student not found", null);
        }

        Optional<ArticleReaction> reactionOpt = articleReactionRepository.findByArticleIdAndStudentId(article.getId(), studentId);
        if (reactionOpt.isPresent()) {
            articleReactionRepository.delete(reactionOpt.get());
            return Response.success("Reaction removed", null);
        }
        return new Response(false, "Reaction not found", null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getReactionCounts(String slug) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        Map<String, Long> counts = new HashMap<>();
        for (vn.campuslife.enumeration.ReactionType type : vn.campuslife.enumeration.ReactionType.values()) {
            counts.put(type.name(), 0L);
        }

        List<Object[]> results = articleReactionRepository.countReactionsByArticleId(article.getId());
        for (Object[] row : results) {
            vn.campuslife.enumeration.ReactionType type = (vn.campuslife.enumeration.ReactionType) row[0];
            Long count = (Long) row[1];
            counts.put(type.name(), count);
        }

        return counts;
    }

    @Override
    @Transactional
    public Response trackShare(String slug) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        article.setShareCount(article.getShareCount() + 1);
        eventArticleRepository.save(article);
        return Response.success("Share tracked", null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleHistoryResponse> getReadingHistory(String username, int page, int size) {
        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId == null) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleViewHistory> historyPage = articleViewHistoryRepository.findByStudentIdOrderByViewedAtDesc(studentId, pageable);
        return historyPage.map(history -> {
            ArticleHistoryResponse res = new ArticleHistoryResponse();
            res.setId(history.getId());
            EventArticle article = history.getArticle();
            res.setArticleId(article.getId());
            res.setTitle(article.getTitle());
            res.setSlug(article.getSlug());
            res.setThumbnailUrl(UrlUtils.toFullUrl(article.getThumbnailUrl(), publicUrl));
            res.setSeoDescription(article.getSeoDescription());
            res.setPublished(article.isPublished());
            res.setPublishedAt(article.getPublishedAt());
            res.setViewedAt(history.getViewedAt());
            if (article.getActivity() != null) {
                res.setRegistrationStatus(resolveRegistrationStatus(article.getActivity(), LocalDateTime.now()).name());
            }
            return res;
        });
    }

    @Override
    @Transactional
    public void deleteReadingHistory(String username, Long historyId) {
        Long studentId = studentService.getStudentIdByUsername(username);
        ArticleViewHistory history = articleViewHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("History entry not found: " + historyId));

        if (studentId == null || !history.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You are not authorized to delete this history entry");
        }

        articleViewHistoryRepository.delete(history);
    }

    @Override
    @Transactional
    public void clearAllReadingHistory(String username) {
        Long studentId = studentService.getStudentIdByUsername(username);
        if (studentId != null) {
            articleViewHistoryRepository.deleteByStudentId(studentId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleListResponse> getTrendingArticles(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> trendingData = articleViewHistoryRepository.findTrendingArticleIds(since, pageable);
        List<Long> articleIds = trendingData.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            return eventArticleRepository.findAllPublishedOrderByPinnedAndPriority(PageRequest.of(0, limit))
                    .map(a -> toListResponse(a, null))
                    .getContent();
        }

        List<EventArticle> articles = eventArticleRepository.findAllById(articleIds);
        articles.sort(Comparator.comparingInt(a -> articleIds.indexOf(a.getId())));
        return articles.stream().map(a -> toListResponse(a, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleListResponse> getFilteredArticlesForAdmin(
            String status, Long activityId, Long categoryId, vn.campuslife.enumeration.ArticleType articleType,
            Boolean featured, Boolean pinned, Boolean primary, String search, String dateFrom, String dateTo,
            int page, int size) {
            
        LocalDateTime from = parseDateTime(dateFrom);
        LocalDateTime to = parseDateTime(dateTo);
        
        Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.jpa.domain.Specification<EventArticle> spec = 
                vn.campuslife.repository.specification.ArticleSpecification.filterArticles(
                        status, activityId, categoryId, articleType, featured, pinned, primary, search, from, to
                );
                
        Page<EventArticle> articles = eventArticleRepository.findAll(spec, pageable);
        return articles.map(a -> toListResponse(a, null));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportArticlesToExcel(
            String status, Long activityId, Long categoryId, vn.campuslife.enumeration.ArticleType articleType,
            Boolean featured, Boolean pinned, Boolean primary, String search, String dateFrom, String dateTo) {
            
        LocalDateTime from = parseDateTime(dateFrom);
        LocalDateTime to = parseDateTime(dateTo);
        
        org.springframework.data.jpa.domain.Specification<EventArticle> spec = 
                vn.campuslife.repository.specification.ArticleSpecification.filterArticles(
                        status, activityId, categoryId, articleType, featured, pinned, primary, search, from, to
                );
                
        List<EventArticle> articles = eventArticleRepository.findAll(spec);
        
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
             
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Articles");
            
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = { "ID", "Title", "Slug", "Article Type", "Activity Name", "Category", "Published", "Published At", "View Count", "Wishlist Count", "Share Count", "Primary" };
            
            org.apache.poi.ss.usermodel.CellStyle headerCellStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);
            
            for (int col = 0; col < headers.length; col++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }
            
            int rowIdx = 1;
            for (EventArticle article : articles) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                
                row.createCell(0).setCellValue(article.getId());
                row.createCell(1).setCellValue(article.getTitle());
                row.createCell(2).setCellValue(article.getSlug());
                row.createCell(3).setCellValue(article.getArticleType() != null ? article.getArticleType().name() : "");
                row.createCell(4).setCellValue(article.getActivity() != null ? article.getActivity().getName() : "");
                row.createCell(5).setCellValue(article.getCategory() != null ? article.getCategory().getName() : "");
                row.createCell(6).setCellValue(article.isPublished() ? "Yes" : "No");
                row.createCell(7).setCellValue(article.getPublishedAt() != null ? article.getPublishedAt().toString() : "");
                row.createCell(8).setCellValue(article.getViewCount());
                row.createCell(9).setCellValue(article.getWishlistCount());
                row.createCell(10).setCellValue(article.getShareCount());
                row.createCell(11).setCellValue(article.isPrimary() ? "Yes" : "No");
            }
            
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
            }
            
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export articles to Excel", e);
        }
    }
}

