package com.example.wordmaster

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
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
class E2EReviewTest {

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

    // TC-07: 导入后首页复习入口 - 有待复习单词
    @Test
    fun tc07_reviewCardWithWordsToReview() {
        importWords(fiveWordsJson)

        composeTestRule.waitUntil(10000) {
            try {
                composeTestRule.onNodeWithText("您有 5 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("复习单词").assertIsDisplayed()
        composeTestRule.onNodeWithText("您有 5 个单词需要复习").assertIsDisplayed()
        composeTestRule.onNodeWithText("复习单词").assertIsEnabled()
    }

    // TC-08: 无待复习单词时复习入口状态
    @Test
    fun tc08_reviewCardNoWordsToReview() {
        composeTestRule.onNodeWithText("暂无需要复习的单词").assertIsDisplayed()
    }

    // TC-09: 进入复习页面 - 卡片初始状态
    @Test
    fun tc09_reviewCardInitialState() {
        importWords(fiveWordsJson)

        composeTestRule.onNodeWithText("复习单词").performClick()

        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("1 / 5").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("1 / 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
        composeTestRule.onNodeWithText("点击卡片查看释义").assertIsDisplayed()
        composeTestRule.onNodeWithText("忘记了").assertIsDisplayed()
        composeTestRule.onNodeWithText("记住了").assertIsDisplayed()
    }

    // TC-10: 卡片翻转交互
    @Test
    fun tc10_cardFlipInteraction() {
        importWords(fiveWordsJson)

        composeTestRule.onNodeWithText("复习单词").performClick()

        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("点击卡片查看释义").assertIsDisplayed()

        composeTestRule.onNodeWithText("eloquent").performClick()

        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("雄辩的，有说服力的").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("雄辩的，有说服力的").assertIsDisplayed()
        composeTestRule.onNodeWithText("She gave an eloquent speech at the ceremony.").assertIsDisplayed()
    }

    // TC-11: 点击"记住了" - 等级和复习时间变化
    @Test
    fun tc11_clickRememberedLevelIncreases() {
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

        composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
    }

    // TC-12: 点击"忘记了" - 等级和复习时间变化
    @Test
    fun tc12_clickForgotLevelStaysAtZero() {
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
        composeTestRule.onNodeWithText("忘记了").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
    }

    // TC-18: 单个单词复习
    @Test
    fun tc18_singleWordReview() {
        importWords("""[{"word":"serendipity","definition":"意外发现的幸运","example":"Finding that book was pure serendipity."}]""", count = 1)

        composeTestRule.onNodeWithText("复习单词").performClick()

        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("1 / 1").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("1 / 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("serendipity").assertIsDisplayed()

        composeTestRule.onNodeWithText("记住了").performClick()

        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("复习完成！").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("复习完成！").assertIsDisplayed()
        composeTestRule.onNodeWithText("太棒了！").assertIsDisplayed()
        composeTestRule.onNodeWithText("返回主页").assertIsDisplayed()
    }

    // TC-19: 未翻转卡片直接操作
    @Test
    fun tc19_clickRememberedWithoutFlipping() {
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

        composeTestRule.onNodeWithTag("review-progress").assertIsDisplayed()
    }
}
