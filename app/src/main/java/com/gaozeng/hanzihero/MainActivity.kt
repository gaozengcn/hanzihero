package com.gaozeng.hanzihero

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaozeng.hanzihero.ui.theme.HanziHeroTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Data classes for JSON serialization
@Serializable
data class CharacterInfo(val char: String, val type: String)

@Serializable
data class Lesson(
    val lesson_number: Int,
    val title: String,
    val characters: List<CharacterInfo>
)

@Serializable
data class Term(
    val term: String,
    val lessons: List<Lesson>
)

@Serializable
data class Grade(
    val grade: Int,
    val terms: List<Term>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HanziHeroTheme {
                HanziApp()
            }
        }
    }
}

fun loadLessonsFromAssets(context: Context): List<Grade> {
    val jsonString = context.assets.open("lessons.json").bufferedReader().use { it.readText() }
    // Configure Json to be lenient about unknown keys if your JSON has extra fields
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString<List<Grade>>(jsonString)
}

@Composable
fun HanziApp() {
    var text by remember { mutableStateOf("") }
    var isQuizActive by remember { mutableStateOf(false) }
    var isQuizComplete by remember { mutableStateOf(false) }
    var quizCharacters by remember { mutableStateOf<List<Char>>(emptyList()) }
    var currentCharacterIndex by remember { mutableStateOf(0) }
    var showStopDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val allGrades by remember { mutableStateOf(loadLessonsFromAssets(context)) }

    if (isQuizActive) {
        QuizScreen(
            character = quizCharacters[currentCharacterIndex],
            currentIndex = currentCharacterIndex,
            totalCharacters = quizCharacters.size,
            onNextClick = {
                if (currentCharacterIndex < quizCharacters.size - 1) {
                    currentCharacterIndex++
                } else {
                    isQuizActive = false
                    isQuizComplete = true
                }
            },
            onStopClick = { showStopDialog = true }
        )

        if (showStopDialog) {
            AlertDialog(
                onDismissRequest = { showStopDialog = false },
                title = { Text("提示") },
                text = { Text("确定要终止么？") },
                confirmButton = {
                    TextButton(onClick = {
                        showStopDialog = false
                        isQuizActive = false
                        isQuizComplete = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    } else if (isQuizComplete) {
        QuizCompleteScreen(
            totalCharacters = quizCharacters.size,
            onReturn = {
                isQuizComplete = false
            },
            onRestart = {
                isQuizComplete = false
                currentCharacterIndex = 0
                isQuizActive = true
            }
        )
    } else {
        MainScreen(
            text = text,
            onTextChange = { text = it },
            onStartQuiz = {
                val filteredChars = text.filter { it in '\u4e00'..'龥' }.toList()
                if (filteredChars.isNotEmpty()) {
                    quizCharacters = filteredChars.shuffled()
                    currentCharacterIndex = 0
                    isQuizActive = true
                    isQuizComplete = false
                }
            },
            onLessonSelect = { lessonText ->
                text = lessonText
            },
            allGrades = allGrades
        )
    }
}

@Composable
fun MainScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onStartQuiz: () -> Unit,
    onLessonSelect: (String) -> Unit,
    allGrades: List<Grade>
) {
    var selectedGrade by remember { mutableStateOf<Grade?>(null) }
    var selectedTerm by remember { mutableStateOf<Term?>(null) }

    Scaffold(
        topBar = {
            Text(
                text = "识字小英雄",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "请选择课程（人教版）",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Start,
                fontSize = 14.sp,
                color = Color.Gray
            )
            // Lesson selection UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Use weight instead of fixed height
                    .border(1.dp, Color.Gray),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Grades
                LessonList(
                    items = allGrades.map { "${it.grade} 年级" },
                    selectedItem = selectedGrade?.let { "${it.grade} 年级" },
                    onItemSelected = {
                        val gradeInt = it.split(" ")[0].toInt()
                        selectedGrade = allGrades.find { g -> g.grade == gradeInt }
                        selectedTerm = null
                    },
                    modifier = Modifier.weight(1f)
                )

                // Terms
                selectedGrade?.let { grade ->
                    LessonList(
                        items = grade.terms.map { it.term },
                        selectedItem = selectedTerm?.term,
                        onItemSelected = { termString ->
                            selectedTerm = grade.terms.find { t -> t.term == termString }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Lessons
                selectedTerm?.let { term ->
                    val lessons = term.lessons
                    LessonList(
                        items = lessons.map { "${it.lesson_number} ${it.title}" },
                        onItemSelected = { lessonTitle ->
                            val selectedLesson = lessons.find { "${it.lesson_number} ${it.title}" == lessonTitle }
                            selectedLesson?.let {
                                onLessonSelect(it.characters.joinToString("") { C -> C.char })
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("在此处编辑汉字") }
            )

            Button(onClick = onStartQuiz, enabled = text.isNotBlank()) {
                Text("开始识字测验")
            }
        }
    }
}

@Composable
fun LessonList(
    items: List<String>,
    modifier: Modifier = Modifier,
    selectedItem: String? = null,
    onItemSelected: (String) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxHeight()) {
        items(items) { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemSelected(item) }
                    .background(if (item == selectedItem) Color.LightGray else Color.Transparent)
                    .padding(8.dp)
            )
        }
    }
}


@Composable
fun QuizScreen(
    character: Char,
    currentIndex: Int,
    totalCharacters: Int,
    onNextClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clickable(onClick = onNextClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "第${currentIndex + 1}个/共${totalCharacters}个",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                fontSize = 20.sp
            )
            Text(
                text = character.toString(),
                fontSize = 220.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("终止测试")
            }
        }
    }
}

@Composable
fun QuizCompleteScreen(
    totalCharacters: Int,
    onReturn: () -> Unit,
    onRestart: () -> Unit
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFE0F7FA)), // Light cyan background
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "恭喜！全部 $totalCharacters 个字已完成！",
                    fontSize = 24.sp,
                    color = Color(0xFF00695C) // Dark cyan/green text
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onReturn) {
                        Text("返回")
                    }
                    TextButton(onClick = onRestart) {
                        Text("重新开始")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HanziHeroTheme {
        MainScreen(text = "你好", onTextChange = {}, onStartQuiz = {}, onLessonSelect = {}, allGrades = emptyList())
    }
}

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview() {
    HanziHeroTheme {
        QuizScreen(character = '汉', currentIndex = 0, totalCharacters = 5, onNextClick = {}, onStopClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun QuizCompleteScreenPreview() {
    HanziHeroTheme {
        QuizCompleteScreen(totalCharacters = 14, onReturn = {}, onRestart = {})
    }
}
