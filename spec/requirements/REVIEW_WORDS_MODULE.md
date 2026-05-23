# 复习单词模块功能梳理

## 一、什么情况下单词会进入复习单词模块？

### 核心判定条件

单词进入复习模块的**唯一判定条件**是：**单词的 `nextReviewTime` ≤ 当前系统时间**。

对应的数据库查询逻辑位于 [WordDao.kt](../app/src/main/java/com/example/wordmaster/data/WordDao.kt)：

```sql
SELECT * FROM words WHERE nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC
```

即：所有 `nextReviewTime` 已到或已过期的单词，按时间升序排列，都会出现在复习列表中。

### 单词初始状态

当单词被创建时（无论是手动添加还是批量导入），其初始状态为：

| 字段 | 初始值 | 说明 |
|------|--------|------|
| `level` | `0` | 等级从 0 开始 |
| `nextReviewTime` | `System.currentTimeMillis()` | 即当前时间，意味着**新添加的单词立即可进入复习** |
| `lastReviewedAt` | `null` | 尚未被复习过 |

相关代码位于 [Word.kt](../app/src/main/java/com/example/wordmaster/data/Word.kt)：

```kotlin
data class Word(
    val level: Int = 0,
    val nextReviewTime: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null
)
```

### 复习入口

首页 (`HomeScreen`) 通过 `ReviewCard` 组件展示待复习单词数量，用户点击后导航至 `ReviewWordsScreen`：

- 当有待复习单词时，卡片显示"您有 N 个单词需要复习"，卡片可点击
- 当没有待复习单词时，卡片显示"暂无需要复习的单词"，卡片置灰不可点击

---

## 二、复习单词时，点"记住了"和点"忘记了"分别做什么？

### 交互流程

1. 用户进入复习页面，逐个展示待复习单词
2. 卡片正面显示单词文本，提示"点击卡片查看释义"
3. 用户点击卡片翻转，查看释义和例句
4. 用户选择"记住了"或"忘记了"
5. 选择后自动切换到下一个单词，卡片重置为正面
6. 全部复习完成后，展示"复习完成"页面

### 点击"记住了"（remembered = true）

调用链：`ReviewWordsScreen.onMarkWord(word, true)` → `ViewModel.markWordAsReviewed(word, true)` → `Repository.markWordAsReviewed(word, true)`

核心逻辑位于 [DataRepository.kt](../app/src/main/java/com/example/wordmaster/data/DataRepository.kt) 和 [ForgettingCurve.kt](../app/src/main/java/com/example/wordmaster/data/ForgettingCurve.kt)：

```kotlin
// DataRepository.markWordAsReviewed
val (newLevel, nextReviewTime) = ForgettingCurve.calculateNextReviewTime(word.level, remembered)
val updatedWord = word.copy(
    level = newLevel,
    nextReviewTime = nextReviewTime,
    lastReviewedAt = System.currentTimeMillis()
)
updateWord(updatedWord)
```

**"记住了"的效果：**

| 操作 | 具体变化 |
|------|----------|
| 等级变化 | `level + 1`（上限为 9） |
| 下次复习时间 | 当前时间 + 新等级对应的复习间隔 |
| 最后复习时间 | 更新为当前时间 |

### 点击"忘记了"（remembered = false）

**"忘记了"的效果：**

| 操作 | 具体变化 |
|------|----------|
| 等级变化 | `level - 1`（下限为 0） |
| 下次复习时间 | 当前时间 + 新等级对应的复习间隔 |
| 最后复习时间 | 更新为当前时间 |

### 对比总结

| 操作 | 等级变化 | 复习间隔趋势 | 记忆强度 |
|------|----------|-------------|---------|
| 记住了 | +1（最高9） | 变长 | 增强 |
| 忘记了 | -1（最低0） | 变短 | 减弱 |

---

## 三、单词等级的定级逻辑及复习对等级的影响

### 等级定义

单词等级（`level`）是一个 0~9 的整数，代表用户对该单词的掌握程度。等级越高，表示掌握越牢固，复习间隔越长。

### 等级与复习间隔的映射

[ForgettingCurve.kt](../app/src/main/java/com/example/wordmaster/data/ForgettingCurve.kt) 中定义了 9 个复习间隔，对应等级 0~8：

| 等级 | 复习间隔 | 说明 |
|------|---------|------|
| 0 | 5 分钟 | 初始等级，最短间隔 |
| 1 | 30 分钟 | |
| 2 | 12 小时 | |
| 3 | 1 天 | |
| 4 | 2 天 | |
| 5 | 4 天 | |
| 6 | 7 天（1 周） | |
| 7 | 15 天（约 2 周） | |
| 8 | 30 天（1 个月） | 最高有效等级 |
| 9 | 30 天（1 个月） | 与等级 8 间隔相同 |

> **注意**：等级 9 是 `coerceAtMost(9)` 的结果，但 `REVIEW_INTERVALS` 数组只有 9 个元素（索引 0~8），等级 9 使用的是 `REVIEW_INTERVALS[8]`，即 30 天，与等级 8 相同。

### 等级变化算法详解

```kotlin
fun calculateNextReviewTime(currentLevel: Int, remembered: Boolean): Pair<Int, Long> {
    return if (remembered) {
        val nextLevel = (currentLevel + 1).coerceAtMost(REVIEW_INTERVALS.size) // max = 9
        val interval = REVIEW_INTERVALS.getOrElse(nextLevel - 1) { REVIEW_INTERVALS.last() }
        nextLevel to System.currentTimeMillis() + interval
    } else {
        val nextLevel = (currentLevel - 1).coerceAtLeast(0) // min = 0
        val interval = REVIEW_INTERVALS.getOrElse(nextLevel) { REVIEW_INTERVALS.first() }
        nextLevel to System.currentTimeMillis() + interval
    }
}
```

#### "记住了"的等级计算

- `nextLevel = min(currentLevel + 1, 9)`
- `interval = REVIEW_INTERVALS[nextLevel - 1]`
- 即：先升级，再按新等级取间隔

**示例**：
- 当前等级 0，记住 → 等级变为 1，间隔取 `REVIEW_INTERVALS[0]` = 5 分钟
- 当前等级 3，记住 → 等级变为 4，间隔取 `REVIEW_INTERVALS[3]` = 1 天
- 当前等级 8，记住 → 等级变为 9，间隔取 `REVIEW_INTERVALS[8]` = 30 天
- 当前等级 9，记住 → 等级仍为 9，间隔取 `REVIEW_INTERVALS[8]` = 30 天

#### "忘记了"的等级计算

- `nextLevel = max(currentLevel - 1, 0)`
- `interval = REVIEW_INTERVALS[nextLevel]`
- 即：先降级，再按新等级取间隔

**示例**：
- 当前等级 0，忘记 → 等级仍为 0，间隔取 `REVIEW_INTERVALS[0]` = 5 分钟
- 当前等级 5，忘记 → 等级变为 4，间隔取 `REVIEW_INTERVALS[4]` = 2 天
- 当前等级 9，忘记 → 等级变为 8，间隔取 `REVIEW_INTERVALS[8]` = 30 天

### 等级变化的完整生命周期示例

以一个新添加的单词为例，展示其在不同复习结果下的等级变化：

```
添加单词 → level=0, nextReviewTime=now (立即可复习)

第1次复习：记住了 → level=1, 30分钟后复习
第2次复习：记住了 → level=2, 12小时后复习
第3次复习：忘记了 → level=1, 30分钟后复习
第4次复习：记住了 → level=2, 12小时后复习
第5次复习：记住了 → level=3, 1天后复习
第6次复习：记住了 → level=4, 2天后复习
第7次复习：忘记了 → level=3, 1天后复习
第8次复习：记住了 → level=4, 2天后复习
第9次复习：记住了 → level=5, 4天后复习
第10次复习：记住了 → level=6, 7天后复习
第11次复习：记住了 → level=7, 15天后复习
第12次复习：记住了 → level=8, 30天后复习
第13次复习：记住了 → level=9, 30天后复习 (已达最高等级)
```

### 设计理念

该复习模块基于**艾宾浩斯遗忘曲线**理论：

1. **间隔重复**：随着记忆巩固，复习间隔逐渐增大
2. **遗忘回退**：一旦忘记，等级下降，间隔缩短，重新加强记忆
3. **渐进式掌握**：从 5 分钟到 30 天，经过 9 个阶段逐步建立长期记忆

---

## 四、关键代码文件索引

| 文件 | 职责 |
|------|------|
| [Word.kt](../app/src/main/java/com/example/wordmaster/data/Word.kt) | 单词数据模型，定义 level、nextReviewTime 等字段 |
| [ForgettingCurve.kt](../app/src/main/java/com/example/wordmaster/data/ForgettingCurve.kt) | 遗忘曲线算法，定义复习间隔和等级计算逻辑 |
| [WordDao.kt](../app/src/main/java/com/example/wordmaster/data/WordDao.kt) | 数据库查询，包含获取待复习单词的 SQL |
| [DataRepository.kt](../app/src/main/java/com/example/wordmaster/data/DataRepository.kt) | 数据仓库，实现 markWordAsReviewed 逻辑 |
| [WordMasterViewModel.kt](../app/src/main/java/com/example/wordmaster/WordMasterViewModel.kt) | ViewModel，连接 UI 与数据层 |
| [ReviewWordsScreen.kt](../app/src/main/java/com/example/wordmaster/ui/ReviewWordsScreen.kt) | 复习页面 UI，卡片翻转和记住/忘记按钮 |
| [HomeScreen.kt](../app/src/main/java/com/example/wordmaster/ui/HomeScreen.kt) | 首页，展示复习入口和待复习数量 |
| [Navigation.kt](../app/src/main/java/com/example/wordmaster/Navigation.kt) | 导航逻辑，连接各页面 |
