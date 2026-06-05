package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.ArticleType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "event_articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EventArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(length = 500)
    private String thumbnailUrl;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 255)
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    @Column(nullable = false)
    private boolean isPublished = false;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ArticleCategory category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "article_article_tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ArticleTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ArticleImage> images = new HashSet<>();

    @Column(nullable = false)
    private boolean isFeatured = false;

    @Column(nullable = false)
    private boolean isPinned = false;

    @Column(nullable = false)
    private int priority = 0;

    @Column(nullable = false)
    private Long wishlistCount = 0L;

    @Column(nullable = false)
    private Long shareCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = true)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "article_type", nullable = false, length = 30)
    private ArticleType articleType = ArticleType.ANNOUNCEMENT;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
