package app.dauphin.views.screens.other

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dauphin.data.CourseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context) }
    val studentId by repository.studentIdFlow.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library Barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                )

            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (studentId != null) {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Code39Barcode(
                            value = studentId!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            color = Color.Black
                        )

                        Text(
                            text = studentId!!,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                }

            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Student ID found", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun Code39Barcode(
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    val patterns = mapOf(
        '0' to "000110100", '1' to "100100001", '2' to "001100001", '3' to "101100000",
        '4' to "000110001", '5' to "100110000", '6' to "001110000", '7' to "000100101",
        '8' to "100100100", '9' to "001100100", 'A' to "100001001", 'B' to "001001001",
        'C' to "101001000", 'D' to "000011001", 'E' to "100011000", 'F' to "001011000",
        'G' to "000001101", 'H' to "100001100", 'I' to "001001100", 'J' to "000011100",
        'K' to "100000011", 'L' to "001000011", 'M' to "101000010", 'N' to "000010011",
        'O' to "100010010", 'P' to "001010010", 'Q' to "000000111", 'R' to "100000110",
        'S' to "001000110", 'T' to "000010110", 'U' to "110000001", 'V' to "011000001",
        'W' to "111000000", 'X' to "010010001", 'Y' to "110010000", 'Z' to "011010000",
        '*' to "010010100", '-' to "010000101", '.' to "110000100", ' ' to "011000100",
        '$' to "010101000", '/' to "010100010", '+' to "010001010", '%' to "000101010"
    )

    val fullValue = "*${value.uppercase()}*"

    Canvas(modifier = modifier) {
        val narrowWidth = 1f
        val wideWidth = 3f

        var totalUnits = 0f
        fullValue.forEachIndexed { charIndex, char ->
            val pattern = patterns[char] ?: return@forEachIndexed
            pattern.forEach { bit ->
                totalUnits += if (bit == '1') wideWidth else narrowWidth
            }
            if (charIndex < fullValue.length - 1) {
                totalUnits += narrowWidth
            }
        }

        val scale = if (totalUnits > 0) size.width / totalUnits else 0f
        var currentX = 0f

        fullValue.forEachIndexed { charIndex, char ->
            val pattern = patterns[char] ?: return@forEachIndexed
            pattern.forEachIndexed { bitIndex, bit ->
                val isBar = bitIndex % 2 == 0
                val elementWidth = (if (bit == '1') wideWidth else narrowWidth) * scale
                if (isBar) {
                    drawRect(
                        color = color,
                        topLeft = Offset(currentX, 0f),
                        size = Size(elementWidth, size.height)
                    )
                }
                currentX += elementWidth
            }
            if (charIndex < fullValue.length - 1) {
                currentX += narrowWidth * scale
            }
        }
    }
}
