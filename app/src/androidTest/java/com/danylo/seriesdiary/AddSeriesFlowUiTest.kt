package com.danylo.seriesdiary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ЛР №13, Завдання 2 — UI-тестування користувацького сценарію.
 *
 * Сценарій (заповнення форми додавання) охоплює 3+ екрани/стани:
 *   1) Екран списку (стартовий) — натискається FAB «Додати серіал».
 *   2) Екран AddSeriesScreen у стані Idle — заповнюються поля форми.
 *   3) Екран AddSeriesScreen у стані помилок валідації (порожні поля).
 *   4) Повернення на екран списку після збереження (стан списку оновлено).
 *
 * Тест запускається на емуляторі або підключеному пристрої. Передбачає, що
 * onboarding вже пройдено (saved name присутній у DataStore) — інакше тест
 * спершу пройде онбординг.
 */
@RunWith(AndroidJUnit4::class)
class AddSeriesFlowUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addSeriesFlow_threeScreens_persistsNewSeries() {
        // --- Екран 1 (онбординг або список) ---
        // Якщо застосунок відкрив екран онбордингу — пройдемо його.
        // Кнопка має текст "Розпочати", а після вводу імені — "Привіт, X! Розпочати",
        // тому шукаємо за substring.
        val nodes = composeTestRule.onAllNodesWithText("Розпочати", substring = true)
        if (nodes.fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule
                .onNodeWithText("Ваше ім'я")
                .performTextInput("UITestUser")
            composeTestRule.onNodeWithText("Розпочати", substring = true).performClick()
        }

        // Чекаємо, поки з'явиться FAB «Додати серіал».
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Додати серіал")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithContentDescription("Додати серіал")
            .assertIsDisplayed()
            .performClick()

        // --- Екран 2 (форма у стані Idle) ---
        composeTestRule.onNodeWithText("Новий серіал").assertIsDisplayed()

        // --- Стан валідації: при порожній формі кнопка «Зберегти» заблокована
        // (enabled = isFormValid && !isSaving). Це окремий стан інтерфейсу. ---
        composeTestRule.onNodeWithText("Зберегти").performScrollTo().assertIsNotEnabled()

        // --- Заповнюємо поля форми ---
        composeTestRule.onNodeWithText("Назва серіалу *").performScrollTo().performTextInput("UI Test Series")
        composeTestRule.onNodeWithText("Рік випуску *").performScrollTo().performTextInput("2024")
        composeTestRule.onNodeWithText("Статус *").performScrollTo().performClick()
        // CONTINUING.description == "Виходить" (див. SeriesStatus.kt)
        composeTestRule.onNodeWithText("Виходить").performClick()
        composeTestRule
            .onNodeWithText("Кількість сезонів *")
            .performScrollTo()
            .performTextInput("1")

        // --- Збереження ---
        composeTestRule.onNodeWithText("Зберегти").performScrollTo().performClick()

        // --- Екран 3 (повернення на список, новий серіал у списку) ---
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("UI Test Series")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("UI Test Series").assertExists()
    }
}
