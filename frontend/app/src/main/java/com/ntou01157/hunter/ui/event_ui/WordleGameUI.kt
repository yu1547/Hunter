// hunter/ui/event_ui/WordleGameUI.kt

package com.ntou01157.hunter.ui.event_ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.StartGameResponse
import com.ntou01157.hunter.api.SubmitGuessRequest
import com.ntou01157.hunter.api.SubmitGuessResponse
import com.ntou01157.hunter.api.WordleFeedback
import kotlinx.coroutines.launch
// -------------------------------------------------------------
// 資料模型 (Data Models)
// -------------------------------------------------------------

// 每個字母方塊的狀態
data class LetterState(val letter: Char, val status: CharStatus)

// 字母狀態的分類
sealed class CharStatus {
    object Unknown : CharStatus()
    object NotInWord : CharStatus()
    object WrongPosition : CharStatus()
    object Correct : CharStatus()
}

// 遊戲狀態
sealed class GameState {
    object Playing : GameState()
    object Win : GameState()
    object Lose : GameState()
}

// -------------------------------------------------------------
// 主遊戲畫面 (Composable Functions)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordleGameUI(userId: String, taskId: String, onEventCompleted: (Boolean) -> Unit) {
    val apiService = RetrofitClient.apiService
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var guesses by remember {
        mutableStateOf(List(6) { List(5) { LetterState(' ', CharStatus.Unknown) } })
    }
    var currentGuess by remember { mutableStateOf("") }
    var currentRow by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf<GameState>(GameState.Playing) }
    var attemptsLeft by remember { mutableStateOf(6) }
    var secretWord by remember { mutableStateOf("") } // 謎底由後端決定，只在結束時顯示

    // 在啟動時呼叫 startGame API
    LaunchedEffect(Unit) {
        try {
            // 後端需要一個空的 body，所以傳入一個空的 map
            val response = apiService.startGame(taskId, mapOf("userId" to userId))
            if (response.success) {
                snackbarHostState.showSnackbar("遊戲開始！你有 6 次機會。")
            } else {
                snackbarHostState.showSnackbar("無法開始遊戲: ${response.message}")
                gameState = GameState.Lose // 設為失敗狀態以鎖定 UI
            }
        } catch (e: Exception) {
            Log.e("WordleGameUI", "Start game failed", e)
            snackbarHostState.showSnackbar("連線錯誤，無法開始遊戲。")
            gameState = GameState.Lose
        }
    }

    fun handleKeyClick(key: Char) {
        if (gameState == GameState.Playing && currentGuess.length < 5) {
            currentGuess += key
        }
    }

    fun handleDelete() {
        if (gameState == GameState.Playing && currentGuess.isNotEmpty()) {
            currentGuess = currentGuess.dropLast(1)
        }
    }

    fun handleEnter() {
        coroutineScope.launch {
            if (gameState != GameState.Playing) return@launch
            if (currentGuess.length < 5) {
                snackbarHostState.showSnackbar("單字長度必須是 5 個字母！")
                return@launch
            }

            try {
                val response = apiService.submitGuess(SubmitGuessRequest(userId, currentGuess))
                if (response.success) {
                    val newGuessRow =
                            response.feedback.map {
                                LetterState(it.letter, it.status.toCharStatus())
                            }
                    guesses = guesses.toMutableList().apply { this[currentRow] = newGuessRow }

                    when (response.status) {
                        "completed" -> {
                            gameState = GameState.Win
                            secretWord = currentGuess.uppercase()
                        }
                        "lose" -> {
                            gameState = GameState.Lose
                            secretWord = response.message.substringAfterLast("是 ") // 從失敗訊息中提取謎底
                        }
                        else -> {
                            currentRow++
                        }
                    }
                    attemptsLeft = response.attemptsLeft
                } else {
                    snackbarHostState.showSnackbar(response.message)
                }
                currentGuess = ""
            } catch (e: Exception) {
                Log.e("WordleGameUI", "Submit guess failed", e)
                snackbarHostState.showSnackbar("提交失敗，請檢查網路。")
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                    text = "Bug Hunt",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 16.dp)
            )

            WordleBoard(guesses = guesses, currentGuess = currentGuess, currentRow = currentRow)

            if (gameState !is GameState.Playing) {
                EndGameDialog(
                        state = gameState,
                        secretWord = secretWord,
                        onDismiss = { onEventCompleted(true) } // 無論輸贏都標記為完成並返回
                )
            }

            WordleKeyboard(
                    onKeyClick = ::handleKeyClick,
                    onEnterClick = ::handleEnter,
                    onDeleteClick = ::handleDelete,
                    enabled = gameState is GameState.Playing
            )
        }
    }
}

// 將後端回傳的字串狀態轉為前端的 sealed class
private fun String.toCharStatus(): CharStatus {
    return when (this) {
        "correct" -> CharStatus.Correct
        "wrong_position" -> CharStatus.WrongPosition
        "not_in_word" -> CharStatus.NotInWord
        else -> CharStatus.Unknown
    }
}

// 遊戲結束對話框
@Composable
fun EndGameDialog(state: GameState, secretWord: String, onDismiss: () -> Unit) {
    val title = if (state is GameState.Win) "恭喜勝利！" else "挑戰失敗！"
    val message = if (state is GameState.Win) "你成功抓到了 Bug！" else "謎底是: $secretWord"

    AlertDialog(
            onDismissRequest = { /* 禁止點擊外部關閉 */},
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = { Button(onClick = onDismiss) { Text("返回地圖") } }
    )
}

// -------------------------------------------------------------
// UI 輔助元件 (UI Helper Components)
// -------------------------------------------------------------

@Composable
fun WordleBoard(guesses: List<List<LetterState>>, currentGuess: String, currentRow: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        guesses.forEachIndexed { rowIndex, row ->
            val displayRow =
                    if (rowIndex == currentRow) {
                        currentGuess.padEnd(5, ' ').map { char ->
                            LetterState(char, CharStatus.Unknown)
                        }
                    } else {
                        row
                    }
            WordleRow(letters = displayRow)
        }
    }
}

@Composable
fun WordleRow(letters: List<LetterState>) {
    Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
    ) { letters.forEach { letterState -> LetterTile(letterState = letterState) } }
}

@Composable
fun LetterTile(letterState: LetterState) {
    val backgroundColor =
            when (letterState.status) {
                CharStatus.Correct -> Color(0xFF6AAA64)
                CharStatus.WrongPosition -> Color(0xFFC9B458)
                CharStatus.NotInWord -> Color(0xFF787C7E)
                CharStatus.Unknown -> Color.Transparent
            }
    val borderColor = if (letterState.letter != ' ') Color.Black else Color.LightGray
    val textColor = if (letterState.status == CharStatus.Unknown) Color.Black else Color.White

    Box(
            modifier =
                    Modifier.size(55.dp)
                            .background(backgroundColor)
                            .border(2.dp, borderColor)
                            .padding(4.dp),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = letterState.letter.uppercaseChar().toString(),
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WordleKeyboard(
        onKeyClick: (Char) -> Unit,
        onEnterClick: () -> Unit,
        onDeleteClick: () -> Unit,
        enabled: Boolean
) {
    val keyboardRows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        keyboardRows.forEach { row ->
            Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
            ) {
                row.forEach { key ->
                    KeyButton(
                            text = key.toString(),
                            onClick = { onKeyClick(key) },
                            enabled = enabled
                    )
                }
            }
        }
        Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 10.dp)
        ) {
            KeyButton(
                    text = "Enter",
                    onClick = onEnterClick,
                    modifier = Modifier.weight(1.5f),
                    enabled = enabled
            )
            KeyButton(
                    text = "Backspace",
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1.5f),
                    enabled = enabled
            )
        }
    }
}

@Composable
fun KeyButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
) {
    Box(
            modifier =
                    modifier.height(50.dp)
                            .background(Color(0xFFD3D6DA), shape = MaterialTheme.shapes.small)
                            .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
    ) { Text(text = text.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

// 預覽函式需要一個假的 onEventCompleted Lambda
@Preview(showBackground = true)
@Composable
fun PreviewWordleGameUI() {
    WordleGameUI(userId = "previewUser", taskId = "previewTask", onEventCompleted = {})
}
