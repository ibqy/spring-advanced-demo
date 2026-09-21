# 08 · Spring Data JPA 进阶

> Specification 动态查询、Projection 视图映射、EntityGraph 抓取优化、Auditing 审计。

## 概述

Spring Data JPA 的 Repository 模式让简单的 CRUD 变得极其简单（只需定义接口）。但实际项目中，我们需要动态查询、选择性字段映射、关联抓取优化、审计字段自动填充等高级能力。本 Demo 一一演示。

## 核心概念

### Spring Data JPA 高级特性全景

```
Spring Data JPA
├── 基础：CrudRepository / JpaRepository
├── 进阶：
│   ├── Specification ─────── 动态条件查询（类似 MyBatis 动态 SQL）
│   ├── Projection ────────── 选择性字段映射（接口/DTO）
│   ├── EntityGraph ────────── 关联抓取策略（解决 N+1）
│   └── Auditing ───────────── 审计字段自动填充（创建人/时间等）
```

## 代码走读

### 1. Specification 动态查询

```java
// demo07/jpa/entity/User.java
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String department;
    private Boolean active;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
```

```java
// demo07/jpa/repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, Long>,
                                        JpaSpecificationExecutor<User> {
    // JpaSpecificationExecutor 提供了 Specification 支持
}
```

```java
// demo07/jpa/service/UserQueryService.java
@Service
public class UserQueryService {

    private final UserRepository userRepository;

    public List<User> search(UserSearchCriteria criteria) {
        Specification<User> spec = Specification.where(null);

        // 动态拼接条件
        if (criteria.getName() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("name"), "%" + criteria.getName() + "%"));
        }

        if (criteria.getDepartment() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("department"), criteria.getDepartment()));
        }

        if (criteria.getActive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("active"), criteria.getActive()));
        }

        if (criteria.getCreatedAfter() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAfter()));
        }

        return userRepository.findAll(spec);
    }
}
```

### 2. Projection 视图映射

```java
// 接口投影 - 只查询需要的字段
public interface UserSummary {
    String getName();
    String getEmail();
    String getDepartment();
}

// Repository 中使用
public interface UserRepository extends JpaRepository<User, Long> {
    // 返回投影类型
    List<UserSummary> findByDepartment(String department);

    // 动态投影
    <T> T findById(Long id, Class<T> type);
}
```

```java
// DTO 投影（开放投影，Spring Data JPA 3.x+）
public record UserDetail(
        Long id,
        String name,
        String email,
        String department,
        String organizationName  // 关联对象的字段
) {}

// 使用 @Query 映射
@Query("""
    SELECT new com.ibqy.spring.advanced.demo07.jpa.UserDetail(
        u.id, u.name, u.email, u.department, o.name
    )
    FROM User u JOIN u.organization o
    WHERE u.id = :id
""")
UserDetail findUserDetailById(@Param("id") Long id);
```

### 3. EntityGraph 抓取优化

```java
// 默认 LAZY 加载的关联，通过 EntityGraph 一次性抓取
@Entity
@NamedEntityGraph(
    name = "User.withOrganization",
    attributeNodes = @NamedAttributeNode("organization")
)
public class User {
    @ManyToOne(fetch = FetchType.LAZY)
    private Organization organization;
}

// Repository 中使用
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(value = "User.withOrganization")
    @Query("SELECT u FROM User u WHERE u.department = :dept")
    List<User> findAllWithOrganization(@Param("dept") String department);
}
```

::: tip 为什么需要 EntityGraph？
默认 LAZY 加载时，访问 `user.getOrganization().getName()` 会触发额外的 SQL 查询（N+1 问题）。EntityGraph 让 JPA 在第一次查询时就 JOIN 抓取关联数据。
:::

### 4. Auditing 审计

```java
// demo07/jpa/config/AuditingConfig.java
@Configuration
@EnableJpaAuditing
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // 从 SecurityContext 获取当前用户
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.isAuthenticated())
                .map(Authentication::getName);
    }
}
```

```java
// 在实体中使用审计字段
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```

## 注解与 API 参考

| 注解 / API | 说明 |
|-----------|------|
| `Specification<T>` | 动态查询条件构建器 |
| `JpaSpecificationExecutor` | 支持 Specification 的 Repository 接口 |
| `@EntityGraph` | 指定关联抓取策略 |
| `@NamedEntityGraph` | 命名实体图（可复用） |
| `@CreatedDate` | 自动填充创建时间 |
| `@LastModifiedDate` | 自动填充修改时间 |
| `@CreatedBy` | 自动填充创建人 |
| `@LastModifiedBy` | 自动填充修改人 |
| `@EnableJpaAuditing` | 启用 JPA 审计 |
| `AuditorAware<T>` | 提供当前审计用户的接口 |

## 面试考点

::: warning 高频面试题
1. **N+1 问题是什么？怎么解决？**
   - 查询 N 个实体时，每个实体的关联属性单独触发一次查询
   - 解决方案：`@EntityGraph`（JOIN FETCH）、`@BatchSize`、DTO Projection

2. **Specification 和 @Query 的区别？**
   - Specification：运行时动态构建条件，灵活但代码较多
   - @Query：编译时确定的 JPQL/SQL，性能好但条件固定
   - 复杂动态查询用 Specification，简单固定查询用 @Query

3. **LAZY vs EAGER 加载策略？**
   - LAZY：访问时才加载，适合"可能不用"的关联
   - EAGER：主实体加载时立即加载关联，适合"总要一起用"的关联
   - 推荐：默认 LAZY，需要时通过 EntityGraph 指定抓取
:::

## 常见陷阱

::: danger 陷阱 1：N+1 查询
最常见的性能问题。使用 `spring.jpa.properties.hibernate.generate_statistics=true` 开启统计，查看实际 SQL 数量。
:::

::: danger 陷阱 2：Specification 中的 null 条件
忘记检查条件参数是否为 null，导致查询条件错误。使用 `Specification.where(null)` 作为起始值。
:::

::: danger 陷阱 3：审计字段被更新
`@CreatedDate` 和 `@CreatedBy` 应该标记为 `@Column(updatable = false)`，否则每次更新都会修改创建时间。
:::

## 延伸阅读

- [Spring Data JPA 文档](https://docs.spring.io/spring-data/jpa/reference/)
- [Specification API](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html)
- [EntityGraph 详解](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)
- [JPA Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html)
