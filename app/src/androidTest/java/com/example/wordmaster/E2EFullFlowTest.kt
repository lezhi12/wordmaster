package com.example.wordmaster

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wordmaster.data.WordDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2EFullFlowTest {

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

    private fun importWords(json: String, count: Int = 5) {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()
        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(json)
        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(10000) {
            try {
                composeTestRule.onNodeWithText("导入 $count 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("导入 $count 个单词").performClick()

        composeTestRule.waitUntil(10000) {
            try {
                composeTestRule.onNodeWithText("我的单词本").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private val fiveWordsJson = """[{"word":"eloquent","definition":"雄辩的，有说服力的","example":"She gave an eloquent speech at the ceremony."},{"word":"resilient","definition":"有弹性的，能迅速恢复的","example":"Children are often more resilient than adults."},{"word":"pragmatic","definition":"务实的，实用主义的","example":"We need a pragmatic approach to solve this problem."},{"word":"ambiguous","definition":"模棱两可的，含糊不清的","example":"The statement was deliberately ambiguous."},{"word":"tenacious","definition":"坚韧不拔的，顽强的","example":"She was tenacious in pursuing her goals."}]"""

    // TC-15: 完整复习流程 - 混合记住和忘记
    @Test
    fun tc15_fullReviewFlowMixedRememberAndForgot() {
        importWords(fiveWordsJson)

        composeTestRule.onNodeWithText("复习单词").performClick()

        composeTestRule.waitUntil(10000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()

        // 第1个单词 (eloquent): 记住了
        composeTestRule.onNodeWithText("记住了").performClick()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // 第2个单词 (resilient): 记住了
        composeTestRule.onNodeWithText("记住了").performClick()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // 第3个单词 (pragmatic): 忘记了
        composeTestRule.onNodeWithText("忘记了").performClick()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // 第4个单词 (ambiguous): 记住了
        composeTestRule.onNodeWithText("记住了").performClick()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // 第5个单词 (tenacious): 忘记了
        composeTestRule.onNodeWithText("忘记了").performClick()

        // 验证复习完成页面
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("复习完成！").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("复习完成！").assertIsDisplayed()
        composeTestRule.onNodeWithText("太棒了！").assertIsDisplayed()
        composeTestRule.onNodeWithText("您已经完成了所有单词的复习").assertIsDisplayed()
        composeTestRule.onNodeWithText("返回主页").assertIsDisplayed()
    }

    // TC-16: 复习完成页面 - 返回首页
    @Test
    fun tc16_reviewCompleteBackToHome() {
        importWords("""[{"word":"test","definition":"测试","example":"Test."}]""", count = 1)

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

        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("返回主页").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("暂无需要复习的单词").assertIsDisplayed()
    }

    // TC-17: 复习中途返回
    @Test
    fun tc17_reviewMidwayBack() {
        importWords(fiveWordsJson)

        composeTestRule.onNodeWithText("复习单词").performClick()

        composeTestRule.waitUntil(10000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("记住了").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("←").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("复习单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("复习单词").assertIsDisplayed()
    }
}
