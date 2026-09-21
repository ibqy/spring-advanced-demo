package com.ibqy.spring.advanced.demo07_data_jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 标签实体 —— 与 Article 构成多对多关系
 *
 * <h3>JPA 多对多关系</h3>
 * <p>{@code @ManyToMany} 声明两个实体之间的多对多关系。
 * JPA 会自动创建一张中间表（join table）来维护关系。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>@ManyToMany 的 owning side？→ 有 @JoinTable 的一方是关系维护方</li>
 *     <li>中间表叫什么？→ 默认: 主表名_关联表名，如 article_tag</li>
 *     <li>什么时候不该用 @ManyToMany？→ 中间表有额外字段时（如排序、状态），
 *     应该拆成两个 @OneToMany 关系，创建一个独立的关联实体</li>
 *     <li>为什么 Set 比 List 好？→ Set 避免重复元素，且 Hibernate 的延迟加载用 Set 更高效</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Entity
@Table(name = "t_tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标签名称（唯一）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 反向关系：一个标签可以关联多篇文章
     * <p>{@code mappedBy} 指向 Article 中维护关系的字段名
     * <p>{@code fetch = FetchType.LAZY}：延迟加载，避免查询标签时自动加载所有关联文章
     */
    @ManyToMany(mappedBy = "tags")
    private Set<Article> articles = new HashSet<>();

    // ====================== 构造方法 ======================

    protected Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    // ====================== Getter / Setter ======================

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Article> getArticles() {
        return articles;
    }

    /**
     * 注意：不要在 Tag 侧提供 setArticles 方法，
     * 多对多关系的维护应该在 owning side（Article）
     */
    public void addArticle(Article article) {
        this.articles.add(article);
        article.getTags().add(this);
    }

    public void removeArticle(Article article) {
        this.articles.remove(article);
        article.getTags().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Tag{id=%d, name='%s'}".formatted(id, name);
    }
}
