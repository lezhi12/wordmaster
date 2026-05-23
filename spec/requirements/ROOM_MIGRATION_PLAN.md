# WordMaster Room 数据库改造方案

## 设计决策

| # | 决策 | 选择 | 理由 |
|---|------|------|------|
| 1 | 数据库方案 | Room | Android 官方推荐 ORM，编译期 SQL 检查，原生支持 Flow |
| 2 | 注解处理器 | KSP | 比 KAPT 快 2 倍以上，KAPT 已进入维护模式 |
| 3 | Repository 接口返回类型 | Flow | Room 原生返回 Flow，ViewModel 层用 stateIn 转换 |
| 4 | 依赖注入 | 保持现有模式 | 项目规模小，Hilt/Koin 过度工程 |
| 5 | 迁移策略 | fallbackToDestructiveMigration | 开发阶段省事，发布前补 Migration |
| 6 | 主键策略 | AutoGenerate | 数据库自动递增，无需手动管理 |
| 7 | 索引 | nextReviewTime 加索引 | 高频查询字段，查询频率远高于写入 |

## 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `gradle/libs.versions.toml` | 修改 | 添加 Room 和 KSP 版本声明 |
| `build.gradle.kts` (根项目) | 修改 | 添加 KSP 插件 |
| `app/build.gradle.kts` | 修改 | 添加 Room 和 KSP 依赖 |
| `data/Word.kt` | 修改 | 添加 @Entity、@PrimaryKey、@Index 注解 |
| `data/WordDao.kt` | 新建 | Room DAO 接口 |
| `data/WordDatabase.kt` | 新建 | Room Database 抽象类 |
| `data/DataRepository.kt` | 修改 | 接口改返回 Flow，实现改用 Room |
| `WordMasterApplication.kt` | 修改 | 创建 Database 实例 |
| `WordMasterViewModel.kt` | 修改 | 适配 Flow，用 stateIn 转换 |

## 技术栈版本

- Room: 2.6.1
- KSP: 与 Kotlin 版本匹配
- Kotlin: 项目现有版本

## 数据库表结构

```sql
CREATE TABLE words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL,
    definition TEXT NOT NULL,
    example TEXT NOT NULL DEFAULT '',
    level INTEGER NOT NULL DEFAULT 0,
    nextReviewTime INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    lastReviewedAt INTEGER
);

CREATE INDEX index_words_nextReviewTime ON words(nextReviewTime);
```

## DAO 查询

```kotlin
@Query("SELECT * FROM words ORDER BY createdAt DESC")
fun getAllWords(): Flow<List<Word>>

@Query("SELECT * FROM words WHERE nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC")
fun getWordsToReview(currentTime: Long): Flow<List<Word>>

@Insert
suspend fun insert(word: Word): Long

@Update
suspend fun update(word: Word)

@Delete
suspend fun delete(word: Word)
```
