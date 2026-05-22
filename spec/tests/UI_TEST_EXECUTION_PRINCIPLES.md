# WordMaster UI 测试执行原理

## 一、测试分层策略

WordMaster 项目的 UI 测试采用**两层测试策略**，分别对应 Android 项目的两种测试目录：

| 层级 | 目录 | 运行环境 | 适用场景 |
|------|------|---------|---------|
| **Instrumented UI 测试** | `app/src/androidTest/` | Android 设备/模拟器 | 验证 Compose 组件渲染、用户交互、端到端流程 |
| **Local Unit 测试** | `app/src/test/` | JVM（开发机） | 验证 ViewModel 逻辑、Repository 数据操作、ForgettingCurve 算法 |

对于 `UItests/ImportAndReviewTestCases.md` 中定义的 25 个测试用例，需要根据用例特点选择合适的执行方式：

- **纯交互验证**（如 TC-10 卡片翻转、TC-11 记住/忘记按钮点击）→ Instrumented UI 测试
- **数据状态验证**（如 TC-24 等级间隔映射、TC-25 lastReviewedAt 更新）→ Local Unit 测试 + Instrumented UI 测试配合
- **完整动线验证**（如 TC-15 混合复习流程）→ Instrumented UI 测试

---

## 二、Instrumented UI 测试执行原理

### 2.1 核心框架

项目已配置的 Compose UI 测试依赖：

```kotlin
// build.gradle.kts
androidTestImplementation(libs.androidx.compose.ui.test.junit4)   // Compose 测试规则和断言
debugImplementation(libs.androidx.compose.ui.test.manifest)       // 测试用 manifest
androidTestImplementation(libs.androidx.test.core)                // AndroidX Test 核心
androidTestImplementation(libs.androidx.test.ext.junit)           // JUnit 扩展
androidTestImplementation(libs.androidx.test.runner)              // 测试运行器
androidTestImplementation(libs.androidx.test.espresso.core)       // Espresso（用于非 Compose 组件）
```

### 2.2 测试执行流程

```
┌─────────────────────────────────────────────────────────┐
│  开发机执行 ./gradlew connectedAndroidTest               │
│                        │                                 │
│                        ▼                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Gradle 构建 APK (androidTest variant)            │   │
│  │  包含测试代码 + 应用代码 + 测试依赖               │   │
│  └──────────────────────────────────────────────────┘   │
│                        │                                 │
│                        ▼                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  adb install 安装到设备/模拟器                     │   │
│  └──────────────────────────────────────────────────┘   │
│                        │                                 │
│                        ▼                                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  AndroidJUnitRunner 启动测试                      │   │
│  │  在设备上实例化 Compose 测试规则                    │   │
│  │  渲染 Compose 组件树 → 语义树                      │   │
│  │  通过语义树查找节点 → 执行操作 → 断言结果           │   │
│  └──────────────────────────────────────────────────┘   │
│                        │                                 │
│                        ▼                                 │
│  测试结果回传到开发机，Gradle 输出报告                    │
└─────────────────────────────────────────────────────────┘
```

### 2.3 Compose 语义树（Semantic Tree）

Compose UI 测试**不直接操作视图对象**，而是通过**语义树**进行交互。Compose 框架在渲染组件时，会自动为每个组件生成语义节点，包含该组件的描述信息（文本内容、可点击状态、状态描述等）。

```
用户看到的 UI                    Compose 语义树
┌──────────────┐                ┌─ SemanticsNode (可点击)
│  eloquent    │  ──映射──▶     │   text = "eloquent"
│              │                │   onClick = onFlip
│ 点击查看释义  │                │   ├─ SemanticsNode
│              │                │   │   text = "点击卡片查看释义"
└──────────────┘                │
┌──────────────┐                ├─ SemanticsNode (Button)
│  忘记了       │  ──映射──▶     │   text = "忘记了"
└──────────────┘                │   onClick = onMarkWord(false)
┌──────────────┐                ├─ SemanticsNode (Button)
│  记住了       │  ──映射──▶     │   text = "记住了"
└──────────────┘                │   onClick = onMarkWord(true)
                                └─
```

测试代码通过语义查找器定位节点，通过语义操作器触发交互：

```kotlin
// 查找节点
composeTestRule.onNodeWithText("记住了")       // 按文本查找
composeTestRule.onNodeWithTag("reviewCard")    // 按测试标签查找

// 执行操作
.performClick()                                 // 点击
.performTextInput("hello")                      // 输入文本
.performScrollTo()                              // 滚动到可见

// 断言
.assertExists()                                 // 节点存在
.assertIsDisplayed()                            // 节点可见
.assertIsEnabled()                              // 节点可交互
.assertTextEquals("eloquent")                   // 文本匹配
```

### 2.4 测试规则选择

项目中有两种 Compose 测试规则：

| 规则 | 适用场景 | 示例 |
|------|---------|------|
| `createComposeRule()` | 测试单个 Compose 组件，无需 Activity | 测试 `FlashCard`、`ReviewCard` 等独立组件 |
| `createAndroidComposeRule<ComponentActivity>()` | 需要 Activity 上下文 | 测试需要 Activity 的场景（如剪贴板操作） |

---

## 三、关键测试问题的解决原理

### 3.1 数据库隔离：内存数据库

UI 测试不能使用真实持久化数据库，否则测试间会互相影响。解决方案是使用 **Room 内存数据库**：

```kotlin
// 测试专用 Application
class TestApplication : Application() {
    val database by lazy {
        Room.inMemoryDatabaseBuilder(this, WordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
    val repository by lazy { RoomDataRepository(database.wordDao()) }
}
```

原理：
- `Room.inMemoryDatabaseBuilder` 创建的数据库仅存在于内存中，进程结束即销毁
- 每个测试用例运行前清空数据库，确保测试隔离
- `allowMainThreadQueries()` 允许在测试主线程执行数据库操作（仅测试用）

通过自定义 `TestRunner` 替换 Application：

```kotlin
class TestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, name: String, context: Context): Application {
        return Instrumentation.newApplication(TestApplication::class.java, context)
    }
}
```

### 3.2 时间控制：ForgettingCurve 的时间依赖问题

`ForgettingCurve.calculateNextReviewTime()` 内部调用 `System.currentTimeMillis()`，导致测试结果依赖真实时间，无法验证精确间隔。解决方案：

**方案 A：引入时钟抽象（推荐，需重构）**

```kotlin
// 重构 ForgettingCurve，注入时钟
object ForgettingCurve {
    var clock: () -> Long = { System.currentTimeMillis() }  // 可替换的时钟

    fun calculateNextReviewTime(currentLevel: Int, remembered: Boolean): Pair<Int, Long> {
        val now = clock()
        return if (remembered) {
            val nextLevel = (currentLevel + 1).coerceAtMost(REVIEW_INTERVALS.size)
            val interval = REVIEW_INTERVALS.getOrElse(nextLevel - 1) { REVIEW_INTERVALS.last() }
            nextLevel to now + interval
        } else {
            val nextLevel = (currentLevel - 1).coerceAtLeast(0)
            val interval = REVIEW_INTERVALS.getOrElse(nextLevel) { REVIEW_INTERVALS.first() }
            nextLevel to now + interval
        }
    }
}

// 测试中替换时钟
@Before fun setup() { ForgettingCurve.clock = { fakeTime } }
@After fun teardown() { ForgettingCurve.clock = { System.currentTimeMillis() } }
```

**方案 B：验证相对间隔（无需重构）**

不验证绝对时间戳，而是验证 `nextReviewTime - lastReviewedAt` 的差值是否等于预期间隔：

```kotlin
val interval = word.nextReviewTime - word.lastReviewedAt!!
assertEquals(ForgettingCurve.getIntervalForLevel(expectedLevel), interval)
```

**方案 C：直接测试 ForgettingCurve 算法（Local Unit 测试）**

将算法逻辑的验证放在 Local Unit 测试中，UI 测试仅验证交互行为，不验证精确时间：

```kotlin
// src/test/ 下的单元测试
class ForgettingCurveTest {
    @Test
    fun remembered_level0_to_level1() {
        val (newLevel, _) = ForgettingCurve.calculateNextReviewTime(0, true)
        assertEquals(1, newLevel)
    }
}
```

### 3.3 ViewModel 注入

当前 `WordMasterViewModel` 通过 `AndroidViewModel` 获取 Repository：

```kotlin
class WordMasterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WordMasterApplication).repository
}
```

在测试中，通过替换 Application（见 3.1），ViewModel 自动获取测试用 Repository，无需额外 Mock。

如果未来需要更灵活的依赖注入，可改为通过 ViewModelProvider.Factory 注入：

```kotlin
class WordMasterViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WordMasterViewModel(repository) as T
    }
}
```

### 3.4 导航测试

当前项目使用 Navigation3，测试导航有两种方式：

**方式 A：测试单个 Screen 组件（推荐）**

不测试导航框架本身，而是直接 `setContent` 渲染目标 Screen，通过回调验证导航意图：

```kotlin
@Test
fun homeScreen_clickReview_navigatesToReview() {
    var navigated = false
    composeTestRule.setContent {
        HomeScreen(
            wordsToReview = testWords,
            allWords = testWords,
            onNavigateToReview = { navigated = true },
            onNavigateToAddWord = {},
            onNavigateToImportWord = {},
            onDeleteWord = {}
        )
    }
    composeTestRule.onNodeWithText("复习单词").performClick()
    assertTrue(navigated)
}
```

**方式 B：测试完整导航流程**

渲染 `MainNavigation`，验证页面切换：

```kotlin
@Test
fun fullNavigationFlow() {
    composeTestRule.setContent {
        MainNavigation(viewModel = testViewModel)
    }
    // 首页 → 点击复习 → 复习页面
    composeTestRule.onNodeWithText("复习单词").performClick()
    composeTestRule.onNodeWithText("忘记了").assertIsDisplayed()
}
```

### 3.5 异步数据等待

Repository 返回 `Flow<List<Word>>`，UI 通过 `collectAsState` 订阅。测试中数据更新是异步的，需要等待 Compose 完成重组：

```kotlin
// 等待数据加载完成
composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithText("eloquent").fetchSemanticsNodes().isNotEmpty()
}

// 或使用 runOnIdle 确保Compose 空闲
composeTestRule.runOnIdle {
    // 验证数据状态
}
```

---

## 四、测试用例与执行方式映射

### 4.1 Instrumented UI 测试执行的用例

| 用例 | 测试方式 | 关键验证点 |
|------|---------|-----------|
| TC-01 标准JSON导入 | 完整流程 | 预览列表、导入按钮文字、首页单词显示 |
| TC-02 代码块JSON | 完整流程 | 解析成功、无报错 |
| TC-03 部分已存在 | 预设数据+流程 | "已存在"标签、默认选中状态 |
| TC-04 空输入 | 单页面 | 错误提示文本 |
| TC-05 非法JSON | 单页面 | 错误提示文本 |
| TC-06 选择切换 | 单页面 | 按钮文字、勾选状态 |
| TC-07 有待复习 | 单组件 | 卡片颜色、文本、可点击性 |
| TC-08 无待复习 | 单组件 | 卡片颜色、文本、不可点击 |
| TC-09 卡片初始状态 | 单组件 | 进度文本、按钮文本 |
| TC-10 卡片翻转 | 单组件 | 正反面内容切换 |
| TC-11 记住了 | 组件+数据 | 等级变化、待复习数量 |
| TC-12 忘记了 | 组件+数据 | 等级不变、待复习数量 |
| TC-15 混合复习 | 完整流程 | 进度更新、完成页面、单词状态 |
| TC-16 复习完成返回 | 完整流程 | 首页状态 |
| TC-17 中途返回 | 完整流程 | 已复习数据持久化 |
| TC-18 单词复习 | 单组件 | 直接显示完成页面 |
| TC-19 未翻转操作 | 单组件 | 操作生效 |
| TC-20 导入立即可复习 | 完整流程 | 复习卡片即时更新 |
| TC-21 Prompt复制 | 单页面 | 剪贴板内容 |
| TC-22 清空输入 | 单页面 | 输入框和列表清空 |

### 4.2 Local Unit 测试执行的用例

| 用例 | 测试方式 | 关键验证点 |
|------|---------|-----------|
| TC-13 高等级忘记 | Repository 单元测试 | level 5→4, 间隔=2天 |
| TC-14 最高等级记住 | Repository 单元测试 | level 9→9, 间隔=30天 |
| TC-23 多轮等级递增 | ForgettingCurve 单元测试 | 等级递增递减、间隔映射 |
| TC-24 等级间隔完整映射 | ForgettingCurve 单元测试 | 全量等级-间隔对照 |
| TC-25 lastReviewedAt 更新 | Repository 单元测试 | null → 非null |

---

## 五、项目目录职责划分

### 5.1 两个 tests 目录的定位

项目中存在两个与测试相关的目录，职责不同，不可混淆：

```
WordMaster/
├── spec/                               ← 规格文档目录
│   ├── tests/                          ← 测试文档仓库（不可执行）
│   │   ├── UI_TEST_EXECUTION_PRINCIPLES.md   测试执行原理文档
│   │   └── UItests/
│   │       └── ImportAndReviewTestCases.md   测试用例设计文档
│   ├── IMPORT_FEATURE_PLAN.md
│   ├── REVIEW_WORDS_MODULE.md
│   └── ROOM_MIGRATION_PLAN.md
│
└── app/src/
    ├── test/                           ← JVM 本地测试（可执行，Gradle 识别）
    │   └── java/com/example/wordmaster/
    │       ├── data/
    │       │   ├── ForgettingCurveTest.kt
    │       │   └── RoomDataRepositoryTest.kt
    │       └── WordMasterViewModelTest.kt
    │
    └── androidTest/                    ← 设备端 UI 测试（可执行，Gradle 识别）
        └── java/com/example/wordmaster/
            ├── TestRunner.kt
            ├── TestApplication.kt
            ├── ui/
            │   ├── HomeScreenTest.kt
            │   ├── ImportWordScreenTest.kt
            │   ├── ReviewWordsScreenTest.kt
            │   └── FullFlowTest.kt
            └── util/
                └── TestDataProvider.kt
```

| 目录 | 职责 | 内容类型 | Gradle 是否识别 | 能否执行 |
|------|------|---------|----------------|---------|
| `spec/tests/` | 测试文档仓库 | Markdown 文档、测试用例设计 | ❌ 不识别 | ❌ 不可执行 |
| `app/src/test/` | JVM 本地测试 | Kotlin 测试代码 | ✅ 识别 | ✅ `./gradlew test` |
| `app/src/androidTest/` | 设备端 UI 测试 | Kotlin 测试代码 | ✅ 识别 | ✅ `./gradlew connectedAndroidTest` |

### 5.2 为什么测试代码必须放在 src/ 中

1. **Gradle 构建系统限制**：`./gradlew test` 和 `./gradlew connectedAndroidTest` 只扫描 `app/src/test/` 和 `app/src/androidTest/` 下的源码，放在其他位置的测试代码不会被编译和执行
2. **Android 项目约定**：`src/test/` 和 `src/androidTest/` 是 Android 项目的标准测试目录，IDE（Android Studio）也只在这两个目录下提供测试相关的代码补全和运行入口
3. **依赖解析**：`testImplementation` 和 `androidTestImplementation` 声明的依赖只对对应 `src/` 子目录生效

### 5.3 为什么 spec/tests/ 适合存放文档

1. **与代码分离**：测试用例文档是设计产物，不是可执行代码，放在 `spec/` 目录与源码目录解耦
2. **版本控制友好**：文档变更与代码变更独立，PR 审查更清晰
3. **团队协作**：非开发人员（QA、产品）可以直接阅读 `spec/tests/` 下的文档，无需翻阅源码目录
4. **不污染构建**：文档不会被 Gradle 扫描，不影响构建速度

### 5.4 测试代码文件与用例文档的对应关系

| 测试代码文件（src/ 中） | 对应的用例文档（spec/tests/ 中） |
|------------------------|--------------------------|
| `ForgettingCurveTest.kt` | TC-13, TC-14, TC-23, TC-24 |
| `RoomDataRepositoryTest.kt` | TC-25 |
| `HomeScreenTest.kt` | TC-07, TC-08 |
| `ImportWordScreenTest.kt` | TC-01~06, TC-20~22 |
| `ReviewWordsScreenTest.kt` | TC-09~12, TC-18, TC-19 |
| `FullFlowTest.kt` | TC-15~17 |

---

## 六、典型测试代码示例

### 6.1 单组件测试：复习卡片翻转（TC-10）

```kotlin
class ReviewWordsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testWord = Word(id = 1, word = "eloquent", definition = "雄辩的", example = "An eloquent speech.")

    @Test
    fun flipCard_showsDefinition() {
        composeTestRule.setContent {
            ReviewWordsScreen(
                words = listOf(testWord),
                onMarkWord = { _, _ -> },
                onBack = {}
            )
        }

        // 正面：显示单词
        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
        composeTestRule.onNodeWithText("点击卡片查看释义").assertIsDisplayed()

        // 点击翻转
        composeTestRule.onNodeWithText("eloquent").performClick()

        // 背面：显示释义和例句
        composeTestRule.onNodeWithText("雄辩的").assertIsDisplayed()
        composeTestRule.onNodeWithText("An eloquent speech.").assertIsDisplayed()
    }
}
```

### 6.2 数据验证测试：记住了等级变化（TC-11）

```kotlin
class RoomDataRepositoryTest {
    private lateinit var database: WordDatabase
    private lateinit var repository: DataRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, WordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomDataRepository(database.wordDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun markWordAsReviewed_remembered_levelIncreases() = runTest {
        val wordId = repository.addWord("eloquent", "雄辩的")
        val word = repository.getWordById(wordId)!!

        repository.markWordAsReviewed(word, remembered = true)

        val updated = repository.getWordById(wordId)!!
        assertEquals(1, updated.level)
        assertNotNull(updated.lastReviewedAt)
    }
}
```

### 6.3 算法验证测试：等级间隔映射（TC-24）

```kotlin
class ForgettingCurveTest {
    private val expectedIntervals = listOf(
        5L * 60L * 1000L,       // level 0
        30L * 60L * 1000L,      // level 1
        12L * 60L * 60L * 1000L, // level 2
        1L * 24L * 60L * 60L * 1000L,  // level 3
        2L * 24L * 60L * 60L * 1000L,  // level 4
        4L * 24L * 60L * 60L * 1000L,  // level 5
        7L * 24L * 60L * 60L * 1000L,  // level 6
        15L * 24L * 60L * 60L * 1000L, // level 7
        30L * 24L * 60L * 60L * 1000L  // level 8
    )

    @Test
    fun remembered_eachLevel_increasesAndReturnsCorrectInterval() {
        for (level in 0..8) {
            val (newLevel, nextReviewTime) = ForgettingCurve.calculateNextReviewTime(level, true)
            val expectedLevel = level + 1
            assertEquals(expectedLevel, newLevel)
            val interval = nextReviewTime - System.currentTimeMillis()
            val expectedInterval = expectedIntervals.getOrElse(expectedLevel - 1) { expectedIntervals.last() }
            assertTrue(Math.abs(interval - expectedInterval) < 1000) // 允许1秒误差
        }
    }

    @Test
    fun remembered_level9_staysAtMax() {
        val (newLevel, _) = ForgettingCurve.calculateNextReviewTime(9, true)
        assertEquals(9, newLevel)
    }

    @Test
    fun forgot_level0_staysAtMin() {
        val (newLevel, _) = ForgettingCurve.calculateNextReviewTime(0, false)
        assertEquals(0, newLevel)
    }
}
```

---

## 七、执行命令

```bash
# 执行所有 Instrumented UI 测试（需连接设备或模拟器）
./gradlew connectedAndroidTest

# 执行所有 Local Unit 测试（无需设备）
./gradlew test

# 执行指定测试类
./gradlew connectedAndroidTest --tests "com.example.wordmaster.ui.ReviewWordsScreenTest"

# 执行指定测试方法
./gradlew test --tests "com.example.wordmaster.data.ForgettingCurveTest.remembered_eachLevel_increasesAndReturnsCorrectInterval"
```

---

## 八、测试执行原理总结

```
┌──────────────────────────────────────────────────────────────────┐
│                     UI 测试执行原理全景                           │
│                                                                  │
│  测试用例文档 (spec/tests/)                                       │
│       │                                                          │
│       ▼  用例文档指导测试代码编写                                  │
│  ┌─────────────────┐     ┌─────────────────┐                    │
│  │ Instrumented 测试 │     │ Local Unit 测试  │                    │
│  │ (app/src/        │     │ (app/src/        │                    │
│  │  androidTest/)   │     │  test/)          │                    │
│  └────────┬────────┘     └────────┬────────┘                    │
│           │                       │                              │
│           ▼                       ▼                              │
│  ┌─────────────────┐     ┌─────────────────┐                    │
│  │ Compose 语义树    │     │ JVM 直接执行     │                    │
│  │ 查找→操作→断言    │     │ 纯逻辑验证       │                    │
│  └────────┬────────┘     └────────┬────────┘                    │
│           │                       │                              │
│           ▼                       ▼                              │
│  ┌─────────────────────────────────────────┐                    │
│  │         测试数据隔离                      │                    │
│  │  • 内存数据库 (Room inMemory)            │                    │
│  │  • 测试用 Application 替换               │                    │
│  │  • 时钟抽象 / 相对间隔验证               │                    │
│  └─────────────────────────────────────────┘                    │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────────────────────────────┐                    │
│  │         测试结果                          │                    │
│  │  • Gradle 控制台输出                      │                    │
│  │  • HTML 报告 (app/build/reports/)        │                    │
│  │  • XML 报告 (CI 集成)                    │                    │
│  └─────────────────────────────────────────┘                    │
└──────────────────────────────────────────────────────────────────┘
```

核心原理：**Compose UI 测试不操作真实视图，而是通过语义树与组件交互；数据层通过内存数据库和依赖注入实现隔离；时间依赖通过时钟抽象或相对间隔验证解决。**
