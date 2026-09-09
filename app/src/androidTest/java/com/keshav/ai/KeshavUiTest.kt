package com.keshav.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class KeshavUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsChatUi() {
        rule.onNodeWithText("keshav", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Message keshav…", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun navigationDrawerOpensAndNewChatResponds() {
        rule.onNodeWithText("☰", useUnmergedTree = true).performClick()
        rule.onNodeWithText("＋ New Chat", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("＋ New Chat", useUnmergedTree = true).performClick()
        rule.onNodeWithText("New chat created", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun settingsSectionOpens() {
        rule.onNodeWithText("☰", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Settings", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("API key", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aboutDiagnosticsSectionOpens() {
        rule.onNodeWithText("☰", useUnmergedTree = true).performClick()
        rule.onNodeWithText("About / Diagnostics", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Keshav diagnostics", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Run connection check", useUnmergedTree = true).performClick()
    }

    @Test
    fun sendWithoutApiKeyShowsActionableMessage() {
        rule.onNodeWithText("Message keshav…", useUnmergedTree = true).performTextInput("Hello Keshav")
        rule.onNodeWithText("Send", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Open Settings and add your AgentRouter API key first", useUnmergedTree = true).assertIsDisplayed()
    }
}
