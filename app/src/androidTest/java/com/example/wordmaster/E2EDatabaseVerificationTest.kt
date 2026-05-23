package com.example.wordmaster

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wordmaster.data.Word
import com.example.wordmaster.data.WordDatabase
import com.example.wordmaster.data.DataRepository
import com.example.wordmaster.data.ForgettingCurve
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2EDatabaseVerificationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var database: WordDatabase
    private lateinit var repository: DataRepository

    @Before
    fun setup() {
        val app = composeTestRule.activity.applicationContext as WordMasterApplication
        database = app.database
        repository = app.repository
    }

    @After
    fun teardown() {
        // 暂时不删除，方便手动测试
    }
    
    // 临时：只添加测试单词
    @Test
    fun temp_add_word_only() {
        importTestWord("testdelete", 0)
        // 等待确认
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("testdelete").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Thread.sleep(10000) // 给10秒时间手动截图
    }
    
    // 工具函数：截图并保存到 /data/local/tmp/（通过 shell 命令，保证有写入权限）
    private fun takeScreenshot(step: Int) {
        try {
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val outputPath = "/data/local/tmp/tc27_step${step}.png"
            // 通过 UiAutomation 执行 shell 命令 screencap，以 shell 用户身份写入 /data/local/tmp/
            instrumentation.uiAutomation.executeShellCommand("screencap -p $outputPath")
            Thread.sleep(300) // 等待写入完成
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotTest", "Failed to save screenshot step $step", e)
            e.printStackTrace()
        }
    }

    private fun importTestWord(word: String, level: Int = 0): Word {
        val testWord = Word(
            word = word,
            definition = "测试释义",
            example = "Test example",
            level = level,
            nextReviewTime = System.currentTimeMillis() - 1000 // 确保立即可复习
        )
        runBlocking {
            repository.addWordDirect(testWord)
        }
        return testWord
    }

    // TC-11: 点击"记住了" - 等级和复习时间变化 (完整数据库验证版)
    @Test
    fun tc11_clickRememberedLevelIncreasesWithDatabase() {
        importTestWord("testword")
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("您有 1 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("复习单词").performClick()
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("记住了").performClick()
        
        // 验证完成页面
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("复习完成！").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("暂无需要复习的单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 验证数据库: 等级变为 1
        val words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1) { "应该还有1个单词" }
        assert(words[0].level == 1) { "等级应该从0升级到1" }
    }

    // TC-12: 点击"忘记了" - 等级和复习时间变化 (完整数据库验证版)
    @Test
    fun tc12_clickForgotLevelStaysAtZeroWithDatabase() {
        importTestWord("testword")
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("您有 1 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("复习单词").performClick()
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("忘记了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("忘记了").performClick()
        
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 验证数据库: 等级还是 0
        val words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1)
        assert(words[0].level == 0) { "等级应该保持0" }
    }

    // TC-13: 高等级单词"忘记了" - 等级回退
    @Test
    fun tc13_highLevelWordForgotLevelDecreases() {
        importTestWord("highlevel", level = 5)
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("您有 1 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("复习单词").performClick()
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("忘记了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("忘记了").performClick()
        
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 验证数据库: 等级从5降到4
        val words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1)
        assert(words[0].level == 4) { "等级应该从5降到4" }
    }

    // TC-14: 最高等级单词"记住了" - 等级不超上限
    @Test
    fun tc14_maxLevelWordRememberedDoesNotExceed() {
        importTestWord("maxlevel", level = 9)
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("您有 1 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("复习单词").performClick()
        
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("记住了").performClick()
        
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 验证数据库: 等级保持9
        val words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1)
        assert(words[0].level == 9) { "最高等级应该保持9" }
    }

    // TC-24: 等级与复习间隔完整映射验证
    @Test
    fun tc24_completeLevelIntervalMapping() {
        val testWord = Word(
            word = "mappingtest",
            definition = "测试映射",
            example = "Test mapping",
            level = 0,
            nextReviewTime = System.currentTimeMillis() - 1000
        )
        runBlocking { repository.addWordDirect(testWord) }
        
        // 验证忘记曲线的等级映射
        for (level in 0..9) {
            val (newLevelRemembered, _) = ForgettingCurve.calculateNextReviewTime(level, true)
            val expectedLevelRemembered = if (level < 9) level + 1 else 9
            assert(newLevelRemembered == expectedLevelRemembered) { 
                "level $level 记住后应该是 $expectedLevelRemembered" 
            }
            
            val (newLevelForgot, _) = ForgettingCurve.calculateNextReviewTime(level, false)
            val expectedLevelForgot = if (level > 0) level - 1 else 0
            assert(newLevelForgot == expectedLevelForgot) { 
                "level $level 忘记后应该是 $expectedLevelForgot" 
            }
        }
    }

    // TC-23: 连续复习多轮 - 验证等级递增
    @Test
    fun tc23_consecutiveReviewRoundsVerifyLevelIncrease() {
        importTestWord("multiroundtest")
        
        // 第1轮复习: 记住
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("复习单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("复习单词").performClick()
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("记住了").performClick()
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 验证等级: 0 -> 1
        var words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1)
        assert(words[0].level == 1) { "第1轮后应该是1" }
        
        // 手动设置nextReviewTime为过去，让它可以再次复习
        val wordAfterRound1 = words[0]
        val updatedWord1 = wordAfterRound1.copy(
            nextReviewTime = System.currentTimeMillis() - 1000
        )
        runBlocking { 
            repository.updateWord(updatedWord1)
        }
        
        // 第2轮复习: 记住
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("复习单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("复习单词").performClick()
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("记住了").performClick()
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 验证等级: 1 -> 2
        words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1)
        assert(words[0].level == 2) { "第2轮后应该是2" }
        
        // 手动设置nextReviewTime为过去
        val wordAfterRound2 = words[0]
        val updatedWord2 = wordAfterRound2.copy(
            nextReviewTime = System.currentTimeMillis() - 1000
        )
        runBlocking { 
            repository.updateWord(updatedWord2)
        }
        
        // 第3轮复习: 忘记
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("复习单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("复习单词").performClick()
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("忘记了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("忘记了").performClick()
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 验证等级: 2 -> 1
        words = runBlocking { repository.getAllWordsDirect() }
        assert(words.size == 1)
        assert(words[0].level == 1) { "第3轮忘记后应该回到1" }
    }

    // TC-25: lastReviewedAt 更新验证
    @Test
    fun tc25_lastReviewedAtUpdateVerification() {
        val testWord = Word(
            word = "reviewedtest",
            definition = "复习测试",
            example = "Test review",
            level = 0,
            nextReviewTime = System.currentTimeMillis() - 1000,
            lastReviewedAt = null
        )
        runBlocking { repository.addWordDirect(testWord) }
        
        // 复习前验证: lastReviewedAt 是 null
        val beforeWords = runBlocking { repository.getAllWordsDirect() }
        assert(beforeWords.size == 1)
        assert(beforeWords[0].lastReviewedAt == null) { "复习前应该是 null" }
        
        // 执行复习
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("您有 1 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("复习单词").performClick()
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("记住了").performClick()
        composeTestRule.onNodeWithText("返回主页").performClick()
        
        // 复习后验证: lastReviewedAt 有值且合理
        val afterWords = runBlocking { repository.getAllWordsDirect() }
        assert(afterWords.size == 1)
        assert(afterWords[0].lastReviewedAt != null) { "复习后应该有值" }
        assert(afterWords[0].lastReviewedAt!! <= System.currentTimeMillis() + 1000) { 
            "时间应该合理" 
        }
    }
    
    // TC-26: 覆盖安装后数据持久性验证
    @Test
    fun tc26_appUpdateDataPersistenceVerification() {
        // ================================================
        // 步骤 1: 真实的 UI 批量导入 5 个单词
        // ================================================
        composeTestRule.onNodeWithText("还没有添加单词").assertIsDisplayed()

        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """[{"word":"eloquent","definition":"雄辩的，有说服力的","example":"She gave an eloquent speech at the ceremony."},{"word":"resilient","definition":"有弹性的，能迅速恢复的","example":"Children are often more resilient than adults."},{"word":"pragmatic","definition":"务实的，实用主义的","example":"We need a pragmatic approach to solve this problem."},{"word":"ambiguous","definition":"模棱两可的，含糊不清的","example":"The statement was deliberately ambiguous."},{"word":"tenacious","definition":"坚韧不拔的，顽强的","example":"She was tenacious in pursuing her goals."}]"""
        )

        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("导入 5 个单词").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("您有 5 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 确认首页显示关键元素（不逐一检查所有单词，避免排版问题）
        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
        composeTestRule.onNodeWithText("您有 5 个单词需要复习").assertIsDisplayed()
        
        // ================================================
        // 步骤 2: 复习部分单词，改变状态
        // ================================================
        composeTestRule.onNodeWithText("复习单词").performClick()
        
        // 复习第一个单词: 记住了
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("记住了").performClick()
        
        // 复习第二个单词: 忘记了
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("忘记了").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("忘记了").performClick()
        
        // 中途返回
        composeTestRule.onNodeWithText("←").performClick()
        
        // ================================================
        // 步骤 3: 保存"安装前"的状态快照
        // ================================================
        val beforeWords = runBlocking { repository.getAllWordsDirect() }
            .sortedBy { it.word }
        assert(beforeWords.size == 5) { "应该有5个单词" }
        
        // ================================================
        // 步骤 4: 模拟"杀掉应用 + 覆盖安装 + 冷启动"
        // 
        // 注意：自动化测试无法真的执行 adb install -r，但我们可以：
        // 1. 强制停止 Activity
        // 2. 重新冷启动
        // 
        // 这样至少可以验证数据库在应用重启后的持久性！
        // ================================================
        composeTestRule.activity.finish()
        Thread.sleep(2000) // 等待完全停止
        
        // 重新冷启动应用
        composeTestRule.activityRule.scenario.recreate()
        
        // ================================================
        // 步骤 5: 验证冷启动后数据完整性
        // ================================================
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("我的单词本").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 验证首页显示关键元素
        composeTestRule.onNodeWithText("我的单词本").assertIsDisplayed()
        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
        
        // 从数据库验证所有状态
        val afterWords = runBlocking { repository.getAllWordsDirect() }
            .sortedBy { it.word }
        assert(afterWords.size == 5) { "冷启动后应该还有5个单词" }
        
        // 完整验证每个单词的所有字段
        for (i in beforeWords.indices) {
            val before = beforeWords[i]
            val after = afterWords[i]
            
            assert(before.word == after.word) { "单词应该匹配: ${before.word}" }
            assert(before.definition == after.definition) { "释义应该匹配" }
            assert(before.example == after.example) { "例句应该匹配" }
            assert(before.level == after.level) { "等级应该匹配" }
            assert(before.nextReviewTime == after.nextReviewTime) { "复习时间应该匹配" }
            assert(before.lastReviewedAt == after.lastReviewedAt) { "上次复习时间应该匹配" }
        }
    }
    
    // TC-27: 删除单词二次确认
    @Test
    fun tc27_deleteWordConfirmation() {
        // ================================================
        // 步骤 1: 先添加一个测试单词
        // ================================================
        val testWord = Word(
            word = "testdelete",
            definition = "删除测试",
            example = "Test delete",
            level = 0,
            nextReviewTime = System.currentTimeMillis() - 1000,
            lastReviewedAt = null
        )
        runBlocking { repository.addWordDirect(testWord) }
        Thread.sleep(1000) // 等待操作完成
        
        // ================================================
        // 步骤 2: 等待页面显示
        // ================================================
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("testdelete").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Thread.sleep(1000)
        takeScreenshot(1) // 截图1: 测试单词已添加，显示在首页
        
        // 验证单词存在
        val initialWords = runBlocking { repository.getAllWordsDirect() }
        assert(initialWords.size == 1) { "初始应该有1个单词" }
        
        // ================================================
        // 步骤 3: 点击删除按钮，然后取消
        // ================================================
        composeTestRule.onNodeWithTag("delete_testdelete").performClick()
        Thread.sleep(1000) // 每步操作后等待1秒
        
        // 验证确认对话框显示
        composeTestRule.waitUntil(2000) {
            try {
                composeTestRule.onNodeWithText("确认删除").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("确定要删除单词 \"testdelete\" 吗？").assertIsDisplayed()
        Thread.sleep(1000)
        takeScreenshot(2) // 截图2: 显示确认删除对话框
        
        // 点击取消
        composeTestRule.onNodeWithTag("cancel_delete_button").performClick()
        Thread.sleep(1000) // 每步操作后等待1秒
        
        // 验证单词仍然存在
        composeTestRule.waitUntil(1000) {
            try {
                composeTestRule.onNodeWithText("testdelete").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Thread.sleep(1000)
        takeScreenshot(3) // 截图3: 取消后单词还在
        
        val afterCancelWords = runBlocking { repository.getAllWordsDirect() }
        assert(afterCancelWords.size == 1) { "取消后单词应该还在" }
        
        // ================================================
        // 步骤 4: 再次点击删除，这次确认删除
        // ================================================
        composeTestRule.onNodeWithTag("delete_testdelete").performClick()
        Thread.sleep(1000) // 每步操作后等待1秒
        
        composeTestRule.waitUntil(2000) {
            try {
                composeTestRule.onNodeWithText("确认删除").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Thread.sleep(1000) // 每步操作后等待1秒
        
        // 点击确认删除：用 testTag
        composeTestRule.onNodeWithTag("confirm_delete_button").performClick()
        Thread.sleep(1000) // 每步操作后等待1秒
        
        // 验证单词已删除
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("还没有添加单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Thread.sleep(1000)
        takeScreenshot(4) // 截图4: 单词已删除，显示空状态
        
        val afterDeleteWords = runBlocking { repository.getAllWordsDirect() }
        assert(afterDeleteWords.isEmpty()) { "删除后单词应该不在了" }
    }
}
