package com.ibqy.spring.advanced.demo07_data_jpa;

import com.ibqy.spring.advanced.demo07_data_jpa.entity.Article;
import com.ibqy.spring.advanced.demo07_data_jpa.entity.Tag;
import com.ibqy.spring.advanced.demo07_data_jpa.projection.ArticleSummary;
import com.ibqy.spring.advanced.demo07_data_jpa.repository.ArticleRepository;
import com.ibqy.spring.advanced.demo07_data_jpa.spec.ArticleSpecifications;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Demo 07：Spring Data JPA 进阶 —— 高级查询与审计
 *
 * <h2>本 Demo 涵盖的核心知识点</h2>
 * <ol>
 *     <li><b>Specification 动态查询</b>：运行时按需组合任意查询条件，类型安全</li>
 *     <li><b>接口投影（Interface Projection）</b>：只查询需要的字段，减少数据传输</li>
 *     <li><b>@EntityGraph</b>：一条 SQL 加载关联数据，解决 N+1 查询问题</li>
 *     <li><b>JPA 审计</b>：@CreatedDate / @LastModifiedDate 自动维护时间戳</li>
 *     <li><b>自定义 Repository</b>：方法名查询 + @Query + 原生 SQL</li>
 * </ol>
 *
 * <h2>N+1 问题详解</h2>
 * <pre>
 *   场景：查询所有文章及其标签
 *
 *   没有 @EntityGraph（N+1 问题）：
 *     SQL 1: SELECT * FROM t_article              → 得到 10 篇文章
 *     SQL 2: SELECT * FROM t_article_tag WHERE article_id = 1
 *     SQL 3: SELECT * FROM t_article_tag WHERE article_id = 2
 *     ...共 11 条 SQL！
 *
 *   有 @EntityGraph（一条 SQL 搞定）：
 *     SQL 1: SELECT a.*, t.* FROM t_article a
 *            LEFT OUTER JOIN t_article_tag at ON a.id = at.article_id
 *            LEFT OUTER JOIN t_tag t ON at.tag_id = t.id
 * </pre>
 *
 * <h2>面试高频问题</h2>
 * <ul>
 *     <li>JPA 的一级缓存和二级缓存？→ 一级缓存是 Session 级别（默认启用），二级缓存需要额外配置</li>
 *     <li>@Transactional 的传播行为？→ REQUIRED 是默认值（加入当前事务或创建新的）</li>
 *     <li>JPA 的脏读问题？→ 使用乐观锁（@Version）防止并发更新冲突</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see Article
 * @see Tag
 * @see ArticleRepository
 * @see ArticleSummary
 * @see ArticleSpecifications
 */
@SpringBootApplication(scanBasePackages = "com.ibqy.spring.advanced.demo07_data_jpa")
public class JpaAdvancedDemo {

    public static void main(String[] args) {
        SpringApplication.run(JpaAdvancedDemo.class, args);
    }

    /**
     * 应用启动后自动执行数据演示
     *
     * <p>使用 {@link CommandLineRunner} 确保在所有 Bean 初始化完成后才执行业务逻辑。
     * 这里会创建一些测试数据，然后演示各种高级查询。
     */
    @Bean
    CommandLineRunner demoRunner(ArticleRepository articleRepository, EntityManager entityManager) {
        return args -> {
            System.out.println("""
                    
                    ╔══════════════════════════════════════════════════════════╗
                    ║     Demo 07: Spring Data JPA 进阶                      ║
                    ║     Specification · 投影 · EntityGraph · 审计          ║
                    ╚══════════════════════════════════════════════════════════╝
                    """);

            // ==================== 准备测试数据 ====================
            System.out.println("========== 1. 准备测试数据 ==========");
            prepareTestData(articleRepository);

            // ==================== 方法名查询演示 ====================
            System.out.println("\n========== 2. 方法名查询 ==========");
            demoMethodQueries(articleRepository);

            // ==================== Specification 动态查询演示 ====================
            System.out.println("\n========== 3. Specification 动态查询 ==========");
            demoSpecification(articleRepository);

            // ==================== 接口投影演示 ====================
            System.out.println("\n========== 4. 接口投影 ==========");
            demoProjection(articleRepository);

            // ==================== @EntityGraph 演示 ====================
            System.out.println("\n========== 5. @EntityGraph 解决 N+1 ==========");
            demoEntityGraph(articleRepository);

            // ==================== 审计功能演示 ====================
            System.out.println("\n========== 6. JPA 审计功能 ==========");
            demoAuditing(articleRepository, entityManager);

            System.out.println("""
                    
                    【面试考点速记】
                    1. Specification 动态查询 → 运行时组合条件，比方法名查询灵活
                    2. N+1 问题 → EntityGraph 用 JOIN 一次加载关联数据
                    3. 接口投影 → 只查询需要的字段，减少数据传输
                    4. JPA 审计 → @CreatedDate / @LastModifiedDate 自动维护时间
                    5. 乐观锁 → @Version 字段，防止并发更新冲突
                    """);
        };
    }

    /**
     * 准备测试数据
     */
    private void prepareTestData(ArticleRepository repo) {
        // 创建标签
        Tag java = new Tag("Java");
        Tag spring = new Tag("Spring");
        Tag jpa = new Tag("JPA");
        Tag security = new Tag("Security");

        // 创建文章
        Article a1 = new Article("Spring Boot 入门指南", "从零开始学习 Spring Boot...", "张三", "PUBLISHED");
        a1.setViewCount(1200);
        a1.getTags().add(java);
        a1.getTags().add(spring);

        Article a2 = new Article("JPA 高级查询详解", "Specification 动态查询...", "张三", "PUBLISHED");
        a2.setViewCount(800);
        a2.getTags().add(jpa);
        a2.getTags().add(java);

        Article a3 = new Article("Spring Security 实战", "JWT 认证与授权...", "李四", "PUBLISHED");
        a3.setViewCount(2500);
        a3.getTags().add(spring);
        a3.getTags().add(security);

        Article a4 = new Article("微服务架构设计", "服务拆分与治理...", "李四", "DRAFT");
        a4.setViewCount(50);
        a4.getTags().add(java);

        Article a5 = new Article("Spring Data JPA 审计功能", "@CreatedDate 自动填充...", "王五", "PUBLISHED");
        a5.setViewCount(600);
        a5.getTags().add(jpa);
        a5.getTags().add(spring);

        // 保存（级联保存关联的 Tag）
        repo.saveAll(List.of(a1, a2, a3, a4, a5));

        // 保存标签（需要单独保存，因为 Article 是 owning side）
        // 实际上通过 Article 的 tags 集合已经级联保存了，这里不再重复保存

        System.out.println("  已创建 5 篇文章和 4 个标签");
        System.out.println("  数据库中文章总数: " + repo.count());
    }

    /**
     * 演示方法名查询
     */
    private void demoMethodQueries(ArticleRepository repo) {
        // 按状态查询
        List<Article> published = repo.findByStatus("PUBLISHED");
        System.out.printf("  已发布文章: %d 篇%n", published.size());
        published.forEach(a -> System.out.printf("    - [%s] %s (浏览: %d)%n",
                a.getAuthor(), a.getTitle(), a.getViewCount()));

        // 按作者+状态查询
        List<Article> zhangSanPublished = repo.findByAuthorAndStatus("张三", "PUBLISHED");
        System.out.printf("  张三的已发布文章: %d 篇%n", zhangSanPublished.size());

        // 关键字搜索
        List<Article> searched = repo.findByTitleContainingIgnoreCase("JPA");
        System.out.printf("  标题含 'JPA' 的文章: %d 篇%n", searched.size());

        // 热门 Top5
        List<Article> top5 = repo.findTop5ByOrderByViewCountDesc();
        System.out.println("  热门 Top5:");
        top5.forEach(a -> System.out.printf("    - %s (浏览: %d)%n", a.getTitle(), a.getViewCount()));
    }

    /**
     * 演示 Specification 动态查询
     */
    private void demoSpecification(ArticleRepository repo) {
        // 场景1：按状态查询
        Specification<Article> publishedSpec = ArticleSpecifications.hasStatus("PUBLISHED");
        List<Article> published = repo.findAll(publishedSpec);
        System.out.printf("  [动态查询] 已发布: %d 篇%n", published.size());

        // 场景2：组合查询 → 张三的已发布文章
        Specification<Article> combined = ArticleSpecifications.hasStatus("PUBLISHED")
                .and(ArticleSpecifications.hasAuthor("张三"));
        List<Article> results = repo.findAll(combined);
        System.out.printf("  [动态查询] 张三的已发布文章: %d 篇%n", results.size());

        // 场景3：全文搜索 + 分页
        Specification<Article> searchSpec = ArticleSpecifications.fullTextSearch("JPA");
        Page<Article> page = repo.findAll(searchSpec, PageRequest.of(0, 10));
        System.out.printf("  [全文搜索 'JPA'] 共 %d 篇，第 1 页显示 %d 篇%n",
                page.getTotalElements(), page.getContent().size());

        // 场景4：复杂组合 → 已发布 + 热门 + 分页
        Specification<Article> complexSpec = ArticleSpecifications.publishedAndPopular("张三", 100);
        Page<Article> complexPage = repo.findAll(complexSpec,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount")));
        System.out.printf("  [复杂组合] 张三的热门文章: %d 篇%n", complexPage.getTotalElements());

        // 场景5：动态条件（模拟前端传入的搜索参数）
        System.out.println("  [动态条件模拟] 前端传入: keyword=Spring, status=PUBLISHED");
        Specification<Article> dynamicSpec = Specification
                .where(ArticleSpecifications.fullTextSearch("Spring"))
                .and(ArticleSpecifications.hasStatus("PUBLISHED"));
        List<Article> dynamicResults = repo.findAll(dynamicSpec);
        System.out.printf("    匹配结果: %d 篇%n", dynamicResults.size());
        dynamicResults.forEach(a -> System.out.printf("      - %s [%s]%n", a.getTitle(), a.getStatus()));
    }

    /**
     * 演示接口投影
     */
    private void demoProjection(ArticleRepository repo) {
        // 查询已发布文章的摘要（只返回 title, author, viewCount）
        List<ArticleSummary> summaries = repo.findByStatusOrderByViewCountDesc("PUBLISHED");
        System.out.println("  已发布文章摘要（接口投影）：");
        summaries.forEach(s -> System.out.printf(
                "    - %s | 作者: %s | 浏览: %d | 预览: %s%n",
                s.getTitle(), s.getAuthor(), s.getViewCount(), s.getTitlePreview()));
        System.out.println("  【注意】投影只查询了 title, author, viewCount 字段，减少了数据传输");
    }

    /**
     * 演示 @EntityGraph 解决 N+1 问题
     */
    private void demoEntityGraph(ArticleRepository repo) {
        // 使用 @EntityGraph 一次性加载文章和标签
        List<Article> articles = repo.findAllWithTags();
        System.out.println("  使用 @EntityGraph 加载文章及标签：");
        for (Article article : articles) {
            System.out.printf("    - %s → 标签: [%s]%n",
                    article.getTitle(),
                    article.getTags().stream()
                            .map(Tag::getName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("无标签"));
        }
        System.out.println("  【注意】开启 JPA show-sql 可以看到只执行了 1 条 SQL（含 JOIN）");
    }

    /**
     * 演示 JPA 审计功能
     */
    private void demoAuditing(ArticleRepository repo, EntityManager em) {
        // 查询一篇已保存的文章，观察审计字段
        List<Article> published = repo.findByStatus("PUBLISHED");
        if (!published.isEmpty()) {
            Article article = published.getFirst();
            System.out.printf("  文章 '%s':%n", article.getTitle());
            System.out.printf("    创建时间 (@CreatedDate): %s%n", article.getCreatedAt());
            System.out.printf("    修改时间 (@LastModifiedDate): %s%n", article.getUpdatedAt());

            // 修改文章并保存，观察 @LastModifiedDate 更新
            String originalTitle = article.getTitle();
            article.setTitle(originalTitle + " [已更新]");
            article.setViewCount(article.getViewCount() + 1);
            // flush 让变更立即同步到数据库
            em.flush();
            // clear 清除一级缓存，下次查询会重新从数据库加载
            em.clear();

            Article updated = repo.findById(article.getId()).orElseThrow();
            System.out.printf("  更新后:%n");
            System.out.printf("    标题: %s%n", updated.getTitle());
            System.out.printf("    创建时间 (不变): %s%n", updated.getCreatedAt());
            System.out.printf("    修改时间 (已更新): %s%n", updated.getUpdatedAt());

            // 恢复标题
            updated.setTitle(originalTitle);
            repo.save(updated);
        }

        System.out.println("  【审计要点】@CreatedDate 只在首次保存时填充，之后不会改变");
        System.out.println("  【审计要点】@LastModifiedDate 每次保存都会更新");
    }
}
