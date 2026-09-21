package com.ibqy.spring.advanced.demo07_data_jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 文章实体 —— 演示 JPA 审计功能
 *
 * <h3>JPA 审计（Auditing）</h3>
 * <p>通过在实体类上标注 {@code @EntityListeners(AuditingEntityListener.class)}，
 * Spring Data JPA 会在 persist/update 时自动填充被 {@code @CreatedDate} / {@code @LastModifiedDate}
 * 标记的字段。开发者无需手动设置时间，保证了数据一致性。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>@CreatedDate 是如何工作的？→ AuditingEntityListener 监听 JPA 生命周期事件，在 @PrePersist 时填充</li>
 *     <li>如何自定义审计字段？→ 可以用 @CreatedBy / @LastModifiedBy + AuditorAware 接口</li>
 *     <li>审计功能在哪里启用？→ 配置类上加 @EnableJpaAuditing</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Entity
@Table(name = "t_article")
// 审计监听器：Spring Data JPA 会在实体保存/更新前自动处理审计字段
@jakarta.persistence.EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文章标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 文章内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 作者
     */
    @Column(length = 50)
    private String author;

    /**
     * 文章状态：DRAFT（草稿）、PUBLISHED（已发布）、ARCHIVED（已归档）
     */
    @Column(length = 20)
    private String status = "DRAFT";

    /**
     * 浏览次数
     */
    private int viewCount = 0;

    /**
     * 标签集合 —— 多对多关系的维护方（owning side）
     *
     * <p>@JoinTable 指定中间表的名称和关联列。
     * <p>fetch = FetchType.LAZY：延迟加载标签，避免每次查文章都加载所有标签。
     * <p>cascade = CascadeType.ALL：保存/删除文章时级联操作到标签。
     */
    @jakarta.persistence.ManyToMany(fetch = jakarta.persistence.FetchType.LAZY, cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @jakarta.persistence.JoinTable(
            name = "article_tag",
            joinColumns = @jakarta.persistence.JoinColumn(name = "article_id"),
            inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "tag_id")
    )
    private java.util.Set<Tag> tags = new java.util.HashSet<>();

    /**
     * 创建时间 —— 由 JPA 审计自动填充
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后修改时间 —— 由 JPA 审计自动填充
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ====================== 构造方法 ======================

    /**
     * JPA 要求实体类必须有一个无参构造方法（可以是 protected）
     */
    protected Article() {
    }

    public Article(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public Article(String title, String content, String author, String status) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.status = status;
    }

    // ====================== Getter / Setter ======================

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public java.util.Set<Tag> getTags() {
        return tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return Objects.equals(id, article.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Article{id=%d, title='%s', author='%s', status='%s', viewCount=%d}"
                .formatted(id, title, author, status, viewCount);
    }
}
