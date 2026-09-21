package com.ibqy.spring.advanced.demo07_data_jpa.spec;

import com.ibqy.spring.advanced.demo07_data_jpa.entity.Article;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * 动态查询规格工厂 —— 演示 {@link Specification} 的用法
 *
 * <h3>什么是 Specification？</h3>
 * <p>{@code Specification<T>} 是 JPA Criteria API 的封装，允许在运行时动态组合查询条件。
 * 它解决了以下痛点：
 * <ul>
 *     <li>方法名查询：条件组合爆炸（findByAAndBAndC...），不够灵活</li>
 *     <li>@Query 查询：条件固定，无法动态拼接</li>
 *     <li>Specification：在运行时按需组合任意条件，类型安全</li>
 * </ul>
 *
 * <h3>核心方法</h3>
 * <pre>{@code
 *   // 创建 Specification：
 *   Specification<Article> spec = (root, query, cb) -> {
 *       // root: 实体根对象，用于访问属性
 *       // query: JPA CriteriaQuery 对象
 *       // cb: CriteriaBuilder，用于构建条件表达式
 *       return cb.equal(root.get("status"), "PUBLISHED");
 *   };
 *
 *   // 组合多个 Specification：
 *   Specification<Article> combined = spec1.and(spec2).or(spec3);
 *
 *   // 传给 Repository 执行：
 *   List<Article> results = articleRepository.findAll(combined);
 * }</pre>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>Specification vs QueryDSL？→ Specification 是 JPA 标准 API（无需额外依赖），
 *     QueryDSL 更强大但需要引入第三方库和注解处理器</li>
 *     <li>Specification 的线程安全性？→ Specification 本身是无状态的函数式接口，线程安全</li>
 *     <li>如何避免 SQL 注入？→ Specification 使用参数绑定，Hibernate 会自动处理</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see com.ibqy.spring.advanced.demo07_data_jpa.repository.ArticleRepository
 */
@Component
public class ArticleSpecifications {

    /**
     * 按状态筛选
     *
     * @param status 文章状态（DRAFT / PUBLISHED / ARCHIVED）
     */
    public static Specification<Article> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction(); // 空条件 = true，不影响其它条件
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * 按作者筛选
     *
     * @param author 作者名
     */
    public static Specification<Article> hasAuthor(String author) {
        return (root, query, cb) -> {
            if (author == null || author.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("author"), author);
        };
    }

    /**
     * 标题关键字搜索（不区分大小写）
     *
     * @param keyword 搜索关键词
     */
    public static Specification<Article> titleContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(
                    cb.lower(root.get("title")),
                    "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    /**
     * 内容关键字搜索
     *
     * @param keyword 搜索关键词
     */
    public static Specification<Article> contentContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(
                    cb.lower(root.get("content")),
                    "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    /**
     * 最低浏览量筛选
     *
     * @param minViews 最低浏览次数
     */
    public static Specification<Article> minViewCount(int minViews) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("viewCount"), minViews);
    }

    /**
     * 创建时间范围查询
     *
     * @param from 起始时间（含）
     * @param to   截止时间（含）
     */
    public static Specification<Article> createdBetween(
            java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    /**
     * 全文搜索：标题或内容包含关键字
     *
     * @param keyword 搜索关键词
     */
    public static Specification<Article> fullTextSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.where((Specification<Article>) null);
        }
        return titleContains(keyword).or(contentContains(keyword));
    }

    /**
     * 组合查询示例：构建一个"已发布 + 某作者 + 热门"的组合规格
     *
     * <p>使用 {@link Specification#where(Specification)} 作为起始点，
     * 然后用 {@code .and()} / {@code .or()} 链式组合条件。
     *
     * @param author   作者
     * @param minViews 最低浏览量
     * @return 组合后的 Specification
     */
    public static Specification<Article> publishedAndPopular(String author, int minViews) {
        return Specification.where(hasStatus("PUBLISHED"))
                .and(hasAuthor(author))
                .and(minViewCount(minViews));
    }
}
