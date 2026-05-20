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
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.*;
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
    public List<ArticleListResponse> getFeaturedArticles() {
        List<EventArticle> articles = eventArticleRepository.findFeaturedArticles();
        return articles.stream().map(a -> toListResponse(a, null)).collect(Collectors.toList());
    }

    @Override
    public ArticleDetailResponse getArticleDetailBySlug(String slug, Long studentId) {
        EventArticle article = eventArticleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

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
            }
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
        RegistrationCtaStatus ctaStatus = resolveRegistrationStatus(activity, LocalDateTime.now());

        response.setRegistrationStatus(ctaStatus.name());
        response.setRegistrationLink(resolveRegistrationLink(activity));

        if (activity != null) {
            ArticleDetailResponse.ArticleActivityInfo activityInfo = new ArticleDetailResponse.ArticleActivityInfo();
            activityInfo.setId(activity.getId());
            activityInfo.setName(activity.getName());
            activityInfo.setLocation(activity.getLocation());
            activityInfo.setStartDate(activity.getStartDate());
            activityInfo.setEndDate(activity.getEndDate());
            activityInfo.setRegistrationStartDate(activity.getRegistrationStartDate());
            activityInfo.setRegistrationDeadline(activity.getRegistrationDeadline());
            activityInfo.setScoreType(activity.getScoreType());
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
        } else {
            response.setWishlisted(false);
        }

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
        Long activityId = request.getActivityId();

        Activity activity = activityRepository.findByIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));

        if (eventArticleRepository.findByActivityId(activityId).isPresent()) {
            throw new BadRequestException("This activity already has an article");
        }

        String slug = normalizeSlug(request.getSlug());
        if (eventArticleRepository.existsBySlug(slug)) {
            throw new BadRequestException("Slug already exists");
        }

        EventArticle article = new EventArticle();
        article.setActivity(activity);
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

        if (request.getActivityId() != null && !request.getActivityId().equals(article.getActivity().getId())) {
            throw new BadRequestException("Cannot change activityId of an existing article");
        }

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
    public EventArticleAdminResponse getArticleByActivityId(Long activityId) {
        EventArticle article = eventArticleRepository.findByActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found for activity: " + activityId));
        return toAdminResponse(article);
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

        stats.setFeaturedArticles(eventArticleRepository.findFeaturedArticles().size());
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
            response.setRegistrationLink(resolveRegistrationLink(activity));
        }

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

        return response;
    }

    private EventArticleAdminResponse toAdminResponse(EventArticle article) {
        EventArticleAdminResponse res = new EventArticleAdminResponse();
        res.setId(article.getId());
        res.setActivityId(article.getActivity().getId());
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

    private String resolveRegistrationLink(Activity activity) {
        if (activity.getShareLink() != null && !activity.getShareLink().isBlank()) {
            return activity.getShareLink();
        }
        return "/activities/" + activity.getId();
    }

    private void validateUpsertRequest(EventArticleUpsertRequest request, boolean isCreate) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (request.getSlug() == null || request.getSlug().isBlank()) {
            throw new BadRequestException("Slug is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("Content is required");
        }
        if (isCreate && (request.getActivityId() == null)) {
            throw new BadRequestException("ActivityId is required when creating article");
        }
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
}
