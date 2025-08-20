package com.ntou01157.hunter.ui.event_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 資料模型 (Data Models)
// -------------------------------------------------------------

// 每個字母方塊的狀態
data class LetterState(
    val letter: Char,
    val status: CharStatus
)

// 字母狀態的分類
sealed class CharStatus {
    object Unknown : CharStatus()        // 尚未猜測
    object NotInWord : CharStatus()      // 字母不在單字中 (灰色)
    object WrongPosition : CharStatus()  // 字母在單字中但位置不對 (黃色)
    object Correct : CharStatus()        // 字母和位置都正確 (綠色)
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
fun WordleGameUI() {
    // 遊戲狀態變數
    var secretWord by remember { mutableStateOf("APPLE") } // 謎底單字
    var guesses by remember { mutableStateOf(List(6) { List(5) { LetterState(' ', CharStatus.Unknown) } }) }
    var currentGuess by remember { mutableStateOf("") }
    var currentRow by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf<GameState>(GameState.Playing) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 處理鍵盤輸入邏輯
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
            if (gameState != GameState.Playing) {
                // 遊戲已結束，不接受輸入
                return@launch
            }
            if (currentGuess.length < 5) {
                snackbarHostState.showSnackbar("單字長度必須是 5 個字母！")
                return@launch
            }

            // 實作單字比對邏輯
            val newGuessRow = mutableListOf<LetterState>()
            val secretWordChars = secretWord.toMutableList()
            val guessChars = currentGuess.toMutableList()

            // 第一輪比對：尋找正確位置的字母 (綠色)
            for (i in guessChars.indices) {
                if (guessChars[i].equals(secretWordChars[i], ignoreCase = true)) {
                    newGuessRow.add(LetterState(guessChars[i], CharStatus.Correct))
                    secretWordChars[i] = '_' // 標記為已使用
                    guessChars[i] = '_'
                } else {
                    newGuessRow.add(LetterState(guessChars[i], CharStatus.Unknown))
                }
            }

            // 第二輪比對：尋找位置不對的字母 (黃色)
            for (i in guessChars.indices) {
                if (guessChars[i] != '_') {
                    val indexInSecret = secretWordChars.indexOfFirst {
                        it.equals(guessChars[i], ignoreCase = true)
                    }
                    if (indexInSecret != -1) {
                        newGuessRow[i] = newGuessRow[i].copy(status = CharStatus.WrongPosition)
                        secretWordChars[indexInSecret] = '_' // 標記為已使用
                    } else {
                        newGuessRow[i] = newGuessRow[i].copy(status = CharStatus.NotInWord)
                    }
                }
            }

            // 更新遊戲狀態
            guesses = guesses.toMutableList().apply {
                this[currentRow] = newGuessRow
            }

            // 判斷遊戲是否結束
            if (currentGuess.equals(secretWord, ignoreCase = true)) {
                gameState = GameState.Win
                snackbarHostState.showSnackbar("恭喜你猜對了！謎底是 $secretWord")
            } else if (currentRow >= 5) {
                gameState = GameState.Lose
                snackbarHostState.showSnackbar("猜測次數用完囉！謎底是 $secretWord")
            } else {
                currentRow++
            }

            currentGuess = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 遊戲標題與狀態訊息
            Text(
                text = "Bug Hunt",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 遊戲板
            WordleBoard(
                guesses = guesses,
                currentGuess = currentGuess,
                currentRow = currentRow
            )

            // 虛擬鍵盤
            WordleKeyboard(
                onKeyClick = ::handleKeyClick,
                onEnterClick = ::handleEnter,
                onDeleteClick = ::handleDelete
            )
        }
    }
}

// -------------------------------------------------------------
// UI 輔助元件 (UI Helper Components)
// -------------------------------------------------------------

@Composable
fun WordleBoard(
    guesses: List<List<LetterState>>,
    currentGuess: String,
    currentRow: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        guesses.forEachIndexed { rowIndex, row ->
            val displayRow = if (rowIndex == currentRow) {
                // 如果是當前行，顯示玩家正在輸入的單字
                currentGuess.padEnd(5, ' ').mapIndexed { colIndex, char ->
                    LetterState(char, if (char != ' ') CharStatus.Unknown else CharStatus.Unknown)
                }
            } else {
                // 否則顯示已提交的猜測結果
                row
            }
            WordleRow(letters = displayRow)
        }
    }
}

@Composable
fun WordleRow(letters: List<LetterState>) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.fillMaxWidth(0.9f),
    ) {
        letters.forEach { letterState ->
            LetterTile(letterState = letterState)
        }
    }
}

@Composable
fun LetterTile(letterState: LetterState) {
    val backgroundColor = when (letterState.status) {
        CharStatus.Correct -> Color(0xFF6AAA64)
        CharStatus.WrongPosition -> Color(0xFFC9B458)
        CharStatus.NotInWord -> Color(0xFF787C7E)
        CharStatus.Unknown -> Color.Transparent
    }
    val borderColor = if (letterState.letter != ' ' && letterState.status == CharStatus.Unknown) {
        Color(0xFF878A8C)
    } else {
        Color(0xFFD3D6DA)
    }
    val textColor = if (letterState.status == CharStatus.Unknown) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(55.dp)
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
    onDeleteClick: () -> Unit
) {
    val keyboardRows = listOf(
        "QWERTYUIOP",
        "ASDFGHJKL",
        "ZXCVBNM"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keyboardRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                row.forEach { key ->
                    KeyButton(text = key.toString(), onClick = { onKeyClick(key) })
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            KeyButton(text = "Enter", onClick = onEnterClick, modifier = Modifier.weight(1.5f))
            KeyButton(text = "Backspace", onClick = onDeleteClick, modifier = Modifier.weight(1.5f))
        }
    }
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .background(Color(0xFFD3D6DA), shape = MaterialTheme.shapes.small)
            .clickable  { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWordleGameUI() {
    WordleGameUI()
}