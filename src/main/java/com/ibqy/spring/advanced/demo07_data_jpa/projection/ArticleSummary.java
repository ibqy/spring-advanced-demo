package com.ibqy.spring.advanced.demo07_data_jpa.projection;

/**
 * 文章摘要投影接口 —— 演示接口投影（Interface-based Projection）
 *
 * <h3>什么是接口投影？</h3>
 * <p>在 Repository 方法的返回类型中使用接口，Spring Data JPA 会自动生成该接口的代理实现。
 * 代理只查询接口中定义的 getter 方法对应的字段，从而实现"按需查询"。
 *
 * <h3>投影类型</h3>
 * <ul>
 *     <li><b>封闭投影（Closed Projection）</b>：getter 方法名与实体属性完全匹配 → 性能最优，Spring 可优化 SQL</li>
 *     <li><b>开放投影（Open Projection）</b>：使用 @Value + SpEL 表达式 → 灵活，但会加载完整实体再计算</li>
 * </ul>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>为什么接口投影比返回整个实体好？→ 减少 SQL 查询的字段数、减少网络传输、减少内存占用</li>
 *     <li>投影接口的方法命名规则？→ get + 属性名首字母大写（如 getTitle 对应 title 字段）</li>
 *     <li>投影可以用于嵌套关系吗？→ 可以，getter 返回另一个投影接口即可</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 *   // Repository 中声明方法
 *   List<ArticleSummary> findByStatus(String status);
 *
 *   // 使用时只获取 title、author、viewCount
 *   List<ArticleSummary> summaries = articleRepository.findByStatus("PUBLISHED");
 *   for (ArticleSummary summary : summaries) {
 *       System.out.println(summary.getTitle() + " by " + summary.getAuthor());
 *   }
 * }</pre>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see com.ibqy.spring.advanced.demo07_data_jpa.repository.ArticleRepository
 */
public interface ArticleSummary {

    /**
     * 文章标题 —— 对应 Article.title
     */
    String getTitle();

    /**
     * 作者 —— 对应 Article.author
     */
    String getAuthor();

    /**
     * 浏览次数 —— 对应 Article.viewCount
     */
    int getViewCount();

    /**
     * 开放投影示例：使用 SpEL 表达式动态计算
     * <p>这里演示一个"标题预览"，取前 30 个字符
     *
     * <p>面试考点：开放投影会加载完整实体再计算，性能不如封闭投影。
     * 适合字段少、计算简单的场景。
     */
    @org.springframework.beans.factory.annotation.Value(
            "#{target.title.length() > 30 ? target.title.substring(0, 30) + '...' : target.title}"
    )
    String getTitlePreview();
}
