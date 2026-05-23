# WordMaster 端到端 UI 测试执行原理

## 一、测试架构

WordMaster 的测试采用端到端 UI 测试方案，所有测试均在 Android 设备/模拟器上运行。

### 测试文件结构

```
app/src/androidTest/java/com/example/wordmaster/
├── E2EImportTest.kt                    # 导入相关测试 (TC-01,02,03,04,05,06,20,21,22)
├── E2EReviewTest.kt                    # 复习 UI 测试 (TC-07,08,09,10,11,12,18,19)
├── E2EFullFlowTest.kt                  # 完整流程测试 (TC-15,16,17)
└── E2EDatabaseVerificationTest.kt      # 数据库验证测试 (TC-11,12,13,14,23,24,25)
```

## 二、测试执行原理

### 2.1 核心框架

使用 Compose UI 测试框架，通过 `createAndroidComposeRule<MainActivity>()` 启动完整应用进行端到端测试。

```kotlin
@RunWith(AndroidJUnit4::class)
class E2EImportTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    private lateinit var database: WordDatabase
    
    @Before
    fun setup() {
        val app = composeTestRule.activity.applicationContext as WordMasterApplication
        database = app.database
    }
    
    @After
    fun teardown() {
        database.clearAllTables()
    }
}
```

### 2.2 Compose 语义树交互

Compose UI 测试通过语义树与组件交互，而不是直接操作视图对象：

```kotlin
// 通过文本查找并点击
composeTestRule.onNodeWithText("批量导入").performClick()

// 通过测试标签查找并输入
composeTestRule.onNodeWithTag("jsonInputField").performTextInput(jsonContent)

// 断言显示
composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
```

### 2.3 异步等待机制

由于数据加载和 UI 更新是异步的，使用 `waitUntil` 等待条件满足：

```kotlin
composeTestRule.waitUntil(5000) {
    try {
        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
        true
    } catch (e: Exception) {
        false
    }
}
```

## 三、数据隔离方案

### 3.1 测试数据清理

每个测试用例执行后，通过清空数据库表实现数据隔离：

```kotlin
@After
fun teardown() {
    database.clearAllTables()
}
```

### 3.2 直接数据库操作

为了验证数据库状态，在 DataRepository 中添加了直接操作方法：

```kotlin
// 直接添加单词（绕过 UI）
suspend fun addWordDirect(word: Word): Long

// 直接获取所有单词
suspend fun getAllWordsDirect(): List<Word>
```

## 四、测试用例与执行方式

所有 25 个测试用例均通过端到端 UI 测试实现：

### E2EImportTest.kt - 导入测试
- TC-01: 标准 JSON 解析与导入
- TC-02: 带代码块标记的 JSON 解析
- TC-03: 部分单词已存在
- TC-04: 空输入解析
- TC-05: 非法 JSON 解析
- TC-06: 手动取消选择与全选切换
- TC-20: 导入后单词立即可复习
- TC-21: Prompt 模板复制功能
- TC-22: 清空输入功能

### E2EReviewTest.kt - 复习 UI 测试
- TC-07: 导入后首页复习入口（有待复习单词）
- TC-08: 无待复习单词时复习入口状态
- TC-09: 进入复习页面 - 卡片初始状态
- TC-10: 卡片翻转交互
- TC-11: 点击"记住了" - 进度更新
- TC-12: 点击"忘记了" - 进度更新
- TC-18: 单个单词复习
- TC-19: 未翻转卡片直接操作

### E2EFullFlowTest.kt - 完整流程测试
- TC-15: 完整复习流程 - 混合记住和忘记
- TC-16: 复习完成页面 - 返回首页
- TC-17: 复习中途返回

### E2EDatabaseVerificationTest.kt - 数据库验证测试
- TC-11: 点击"记住了" - 数据库验证版
- TC-12: 点击"忘记了" - 数据库验证版
- TC-13: 高等级单词"忘记了" - 等级回退
- TC-14: 最高等级单词"记住了" - 等级不超上限
- TC-23: 连续复习多轮 - 验证等级递增
- TC-24: 等级与复习间隔完整映射验证
- TC-25: lastReviewedAt 更新验证

## 五、执行命令

```bash
# 执行所有端到端 UI 测试
./gradlew connectedAndroidTest

# 执行指定测试类
./gradlew connectedAndroidTest --tests "com.example.wordmaster.E2EImportTest"

# 执行指定测试方法
./gradlew connectedAndroidTest --tests "com.example.wordmaster.E2EImportTest.tc01_standardJsonParseAndImport"
```

## 六、测试代码规范

### 6.1 命名规范

测试方法命名格式：`tcXX_testDescription`

```kotlin
@Test
fun tc01_standardJsonParseAndImport() { ... }

@Test
fun tc07_reviewCardWithWordsToReview() { ... }
```

### 6.2 公共辅助方法

提取重复逻辑为私有方法：

```kotlin
private fun importWords(json: String, count: Int = 5) {
    composeTestRule.onNodeWithText("+").performClick()
    composeTestRule.onNodeWithText("批量导入").performClick()
    // ...
}
```

### 6.3 UI 测试标签

重要 UI 元素添加 testTag 方便定位：

```kotlin
// ImportWordScreen.kt
TextField(modifier = Modifier.testTag("jsonInputField"))
Checkbox(modifier = Modifier.testTag("checkbox_${item.word.word}"))
TextButton(modifier = Modifier.testTag("selectAllButton"))
```
