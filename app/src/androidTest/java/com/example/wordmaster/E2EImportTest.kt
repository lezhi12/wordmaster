package com.example.wordmaster

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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

    // TC-01: 批量导入 - 标准JSON解析与导入
    @Test
    fun tc01_standardJsonParseAndImport() {
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

        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
        composeTestRule.onNodeWithText("resilient").assertIsDisplayed()
        composeTestRule.onNodeWithText("导入 5 个单词").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("您有 5 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("您有 5 个单词需要复习").assertIsDisplayed()
        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
    }

    // TC-02: 批量导入 - 带代码块标记的JSON解析
    @Test
    fun tc02_codeBlockJsonParse() {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """```json
[{"word":"eloquent","definition":"雄辩的","example":"An eloquent speech."}]
```"""
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

        composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
    }

    // TC-04: 批量导入 - 空输入解析
    @Test
    fun tc04_emptyInputParse() {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("请先粘贴 JSON 内容").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("请先粘贴 JSON 内容").assertIsDisplayed()
    }

    // TC-05: 批量导入 - 非法JSON解析
    @Test
    fun tc05_invalidJsonParse() {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithTag("jsonInputField").performTextInput("hello world")

        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("JSON 解析失败", substring = true).assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("JSON 解析失败", substring = true).assertIsDisplayed()
    }

    // TC-20: 导入后单词立即可复习
    @Test
    fun tc20_wordsImmediatelyAvailableForReview() {
        composeTestRule.onNodeWithText("暂无需要复习的单词").assertIsDisplayed()

        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """[{"word":"test1","definition":"测试1","example":"Test 1."},{"word":"test2","definition":"测试2","example":"Test 2."},{"word":"test3","definition":"测试3","example":"Test 3."}]"""
        )

        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("导入 3 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("导入 3 个单词").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("您有 3 个单词需要复习").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("您有 3 个单词需要复习").assertIsDisplayed()
    }

    // TC-21: Prompt模板复制功能 - 展开验证
    @Test
    fun tc21_promptTemplateExpandAndCopy() {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithText("📋 Prompt 模板").performClick()

        composeTestRule.onNodeWithText("复制 Prompt").assertIsDisplayed()
        composeTestRule.onNodeWithText("复制 Prompt").performClick()
    }

    // TC-03: 批量导入 - 部分单词已存在
    @Test
    fun tc03_partialWordsAlreadyExist() {
        // 第一步: 先导入 eloquent 和 resilient
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()
        
        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """[{"word":"eloquent","definition":"雄辩的，有说服力的","example":"She gave an eloquent speech at the ceremony."},{"word":"resilient","definition":"有弹性的，能迅速恢复的","example":"Children are often more resilient than adults."}]"""
        )
        
        composeTestRule.onNodeWithText("解析").performClick()
        
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("导入 2 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("导入 2 个单词").performClick()
        
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("我的单词本").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 第二步: 再次导入包含这两个词的完整JSON
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()
        
        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """[{"word":"eloquent","definition":"雄辩的，有说服力的","example":"She gave an eloquent speech at the ceremony."},{"word":"resilient","definition":"有弹性的，能迅速恢复的","example":"Children are often more resilient than adults."},{"word":"pragmatic","definition":"务实的，实用主义的","example":"We need a pragmatic approach to solve this problem."},{"word":"ambiguous","definition":"模棱两可的，含糊不清的","example":"The statement was deliberately ambiguous."},{"word":"tenacious","definition":"坚韧不拔的，顽强的","example":"She was tenacious in pursuing her goals."}]"""
        )
        
        composeTestRule.onNodeWithText("解析").performClick()
        
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("eloquent").assertIsDisplayed()
                composeTestRule.onNodeWithText("resilient").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 验证: eloquent 和 resilient 显示"已存在"且未选中，导入按钮显示3个
        composeTestRule.onNodeWithTag("existingLabel_eloquent").assertIsDisplayed()
        composeTestRule.onNodeWithTag("existingLabel_resilient").assertIsDisplayed()
        composeTestRule.onNodeWithText("导入 3 个单词").assertIsDisplayed()
    }

    // TC-06: 批量导入 - 手动取消选择与全选切换
    @Test
    fun tc06_manualDeselectAndSelectAllToggle() {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """[{"word":"eloquent","definition":"雄辩的，有说服力的","example":"She gave an eloquent speech at the ceremony."},{"word":"resilient","definition":"有弹性的，能迅速恢复的","example":"Children are often more resilient than adults."},{"word":"pragmatic","definition":"务实的，实用主义的","example":"We need a pragmatic approach to solve this problem."}]"""
        )

        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("导入 3 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 验证初始状态: 3个全部选中
        composeTestRule.onNodeWithText("导入 3 个单词").assertIsDisplayed()
        composeTestRule.onNodeWithText("全选").assertDoesNotExist()
        composeTestRule.onNodeWithText("取消全选").assertIsDisplayed()
        
        // 手动取消第2个单词 (resilient)
        composeTestRule.onNodeWithTag("checkbox_resilient").performClick()
        
        // 验证: 现在显示导入2个
        composeTestRule.waitUntil(2000) {
            try {
                composeTestRule.onNodeWithText("导入 2 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("导入 2 个单词").assertIsDisplayed()
        
        // 点击"全选"
        composeTestRule.onNodeWithTag("selectAllButton").performClick()
        
        // 验证: 回到3个
        composeTestRule.waitUntil(2000) {
            try {
                composeTestRule.onNodeWithText("导入 3 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // 点击"取消全选"
        composeTestRule.onNodeWithTag("selectAllButton").performClick()
        
        // 验证: 按钮变灰，显示导入0个
        composeTestRule.waitUntil(2000) {
            try {
                composeTestRule.onNodeWithText("导入 0 个单词").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // TC-22: 清空输入功能
    @Test
    fun tc22_clearInput() {
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("批量导入").performClick()

        composeTestRule.onNodeWithTag("jsonInputField").performTextInput(
            """[{"word":"test","definition":"测试","example":"Test."}]"""
        )

        composeTestRule.onNodeWithText("解析").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("test").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("清空").performClick()

        // 验证: 输入框和预览列表都已清空
        composeTestRule.waitUntil(3000) {
            try {
                composeTestRule.onNodeWithText("test").assertDoesNotExist()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
