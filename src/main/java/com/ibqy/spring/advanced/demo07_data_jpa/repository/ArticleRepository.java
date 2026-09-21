package com.ibqy.spring.advanced.demo07_data_jpa.repository;

import com.ibqy.spring.advanced.demo07_data_jpa.entity.Article;
import com.ibqy.spring.advanced.demo07_data_jpa.projection.ArticleSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 文章 Repository —— 演示 Spring Data JPA 的高级查询特性
 *
 * <h3>继承关系</h3>
 * <pre>
 *   JpaRepository<Article, Long>
 *       ├── CrudRepository        → 基础 CRUD (save, findById, delete...)
 *       ├── PagingAndSortingRepository → 分页和排序
 *       └── ListCrudRepository    → 返回 List 的 CRUD
 *
 *   JpaSpecificationExecutor<Article>
 *       └── 提供 Specification 动态查询能力 (findAll(Spec, Pageable)...)
 * </pre>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>JpaRepository vs CrudRepository？→ JpaRepository 提供更多 JPA 特有方法如 flush、findByExample</li>
 *     <li>方法名查询的命名规则？→ findBy + 属性名 + 条件（如 findByTitleContaining、findByStatusAndAuthor）</li>
 *     <li>@Query 和 方法名查询怎么选？→ 简单查询用方法名（自动生成 SQL），复杂查询用 @Query（可控性更强）</li>
 *     <li>接口投影是什么？→ 定义一个只有 getter 的接口，Spring Data 自动生成投影实现</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see Article
 * @see ArticleSummary
 */
public interface ArticleRepository extends JpaRepository<Article, Long>,
        JpaSpecificationExecutor<Article> {

    // ====================== 方法名查询（Query Derivation） ======================

    /**
     * 按状态查询（方法名自动生成 SQL）
     * <p>生成：SELECT a FROM Article a WHERE a.status = ?1
     */
    List<Article> findByStatus(String status);

    /**
     * 按作者和状态组合查询
     * <p>生成：SELECT a FROM Article a WHERE a.author = ?1 AND a.status = ?2
     */
    List<Article> findByAuthorAndStatus(String author, String status);

    /**
     * 标题模糊查询
     * <p>生成：SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', ?1, '%'))
     */
    List<Article> findByTitleContainingIgnoreCase(String keyword);

    /**
     * 按浏览量排序，取前 N 篇
     * <p>生成：SELECT a FROM Article a ORDER BY a.viewCount DESC LIMIT ?1
     */
    List<Article> findTop5ByOrderByViewCountDesc();

    /**
     * 统计某作者的文章数量
     */
    long countByAuthor(String author);

    /**
     * 按状态删除（会触发 JPA 的 @PreRemove 回调）
     */
    void deleteByStatus(String status);

    // ====================== @Query 自定义查询 ======================

    /**
     * 使用 JPQL 自定义查询
     * <p>面试考点：JPQL 操作的是实体对象而不是表名！
     */
    @Query("SELECT a FROM Article a WHERE a.viewCount > :minViews ORDER BY a.viewCount DESC")
    List<Article> findPopularArticles(int minViews);

    /**
     * 使用原生 SQL（nativeQuery = true）
     * <p>面试考点：什么场景需要原生 SQL？→ 用到数据库特有函数（如 MySQL 的 DATE_FORMAT）时
     */
    @Query(value = "SELECT * FROM t_article WHERE author = ?1 ORDER BY created_at DESC",
            nativeQuery = true)
    List<Article> findByAuthorNative(String author);

    // ====================== @EntityGraph（解决 N+1 问题） ======================

    /**
     * 使用 @EntityGraph 解决 N+1 查询问题
     *
     * <p>N+1 问题：查询 100 篇文章 → 1 次查文章 + 100 次查标签 = 101 次 SQL
     * <p>解决方案：使用 @EntityGraph 告诉 Hibernate 在一条 SQL 中 JOIN 加载关联数据
     *
     * <p>面试考点：
     * <ul>
     *     <li>FetchType.LAZY vs EAGER？→ LAZY 延迟加载（推荐），EAGER 立即加载（可能引发性能问题）</li>
     *     <li>@EntityGraph 的 type？→ FETCH（覆盖默认加载策略）vs LOAD（只增强，不改变默认策略）</li>
     *     <li>N+1 的其它解决方案？→ @BatchSize 注解、JOIN FETCH 查询</li>
     * </ul>
     */
    @org.springframework.data.jpa.repository.EntityGraph(
            attributePaths = {"tags"},
            type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH
    )
    @Query("SELECT a FROM Article a")
    List<Article> findAllWithTags();

    /**
     * 带分页的 EntityGraph 查询
     */
    @org.springframework.data.jpa.repository.EntityGraph(
            attributePaths = {"tags"},
            type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH
    )
    Page<Article> findByStatus(String status, Pageable pageable);

    // ====================== 接口投影（Interface-based Projection） ======================

    /**
     * 接口投影：只查询部分字段
     *
     * <p>Spring Data JPA 会自动生成 {@link ArticleSummary} 接口的代理实现，
     * 只查询 title、author、viewCount 三个字段，减少数据传输。
     *
     * <p>面试考点：
     * <ul>
     *     <li>开放投影 vs 封闭投影？→ 方法签名与实体属性完全匹配 = 封闭投影（性能好），
     *     使用 SpEL 表达式 = 开放投影（灵活但性能差）</li>
     *     <li>投影和 DTO 的区别？→ 投影由 Spring Data 自动实现，DTO 需要手动映射</li>
     * </ul>
     */
    List<ArticleSummary> findByStatusOrderByViewCountDesc(String status);

    /**
     * 根据 ID 查询并返回投影
     *
     * <p>使用 @Query + 投影接口，只查询需要的字段
     */
    @Query("SELECT a.title AS title, a.author AS author, a.viewCount AS viewCount FROM Article a WHERE a.id = :id")
    ArticleSummary findSummaryById(Long id);
}
