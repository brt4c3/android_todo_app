package com.example.todo_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Expected duration input with Hours/Minutes fields and +/- steppers.
 * - No KeyboardOptions/KeyboardType dependency.
 * - Hours: 0..99, Minutes: 0..59.
 * - Calls onDurationChanged whenever either field changes.
 */
@Composable
fun ExpectedDurationInput(
    modifier: Modifier = Modifier,
    initialHours: Int = 0,
    initialMinutes: Int = 0,
    onDurationChanged: (Int, Int) -> Unit
) {
    val hoursState = rememberDurationNumberFieldState(
        initial = initialHours.coerceIn(0, 99),
        range = 0..99,
        label = "Hours"
    )
    val minutesState = rememberDurationNumberFieldState(
        initial = initialMinutes.coerceIn(0, 59),
        range = 0..59,
        label = "Minutes"
    )

    // Notify parent when values change (after clamping/validation)
    LaunchedEffect(hoursState.text, minutesState.text) {
        onDurationChanged(hoursState.valueInt ?: 0, minutesState.valueInt ?: 0)
    }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Expected duration (manual)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Hours with steppers
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = hoursState.text,
                        onValueChange = { hoursState.updateValue(it) },
                        label = { Text("Hours") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = hoursState.hasError,
                        supportingText = {
                            if (hoursState.hasError) {
                                Text(hoursState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    IconButton(
                        onClick = { hoursState.step(-1) },
                        enabled = (hoursState.valueInt ?: 0) > hoursState.range.first
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Dec hours") }
                    IconButton(
                        onClick = { hoursState.step(+1) },
                        enabled = (hoursState.valueInt ?: 0) < hoursState.range.last
                    ) { Icon(Icons.Filled.Add, contentDescription = "Inc hours") }
                }

                // Minutes with steppers
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = minutesState.text,
                        onValueChange = { minutesState.updateValue(it) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = minutesState.hasError,
                        supportingText = {
                            if (minutesState.hasError) {
                                Text(minutesState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    IconButton(
                        onClick = { minutesState.step(-1) },
                        enabled = (minutesState.valueInt ?: 0) > minutesState.range.first
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Dec minutes") }
                    IconButton(
                        onClick = { minutesState.step(+1) },
                        enabled = (minutesState.valueInt ?: 0) < minutesState.range.last
                    ) { Icon(Icons.Filled.Add, contentDescription = "Inc minutes") }
                }
            }

            Text(
                text = "Total: ${(hoursState.valueInt ?: 0) * 60 + (minutesState.valueInt ?: 0)} minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

/** Numeric text field state with range clamp, validation and stepper helpers. */
class DurationNumberFieldState internal constructor(
    initialText: String,
    val range: IntRange,
    private val label: String,
    private val setter: (String) -> Unit,
    private val getter: () -> String
) {
    val text: String get() = getter()

    val valueInt: Int?
        get() = text.toIntOrNull()

    val hasError: Boolean
        get() {
            val v = valueInt ?: return text.isNotEmpty() // non-empty but not a number
            return v !in range
        }

    val errorMessage: String?
        get() = if (hasError) "$label must be between ${range.first} and ${range.last}" else null

    /** Accept digits only; limit length; clamp if numeric. */
    fun updateValue(newText: String) {
        val digits = newText.filter { it.isDigit() }.take(3)
        val clamped = digits.toIntOrNull()?.coerceIn(range)?.toString() ?: digits
        setter(clamped)
    }

    /** Step +/- 1 within range. */
    fun step(delta: Int) {
        val current = valueInt ?: 0
        val next = (current + delta).coerceIn(range)
        setter(next.toString())
    }
}

@Composable
private fun rememberDurationNumberFieldState(
    initial: Int,
    range: IntRange,
    label: String
): DurationNumberFieldState {
    val state = rememberSaveable(label) { mutableStateOf(initial.coerceIn(range).toString()) }
    return remember(range, label) {
        DurationNumberFieldState(
            initialText = state.value,
            range = range,
            label = label,
            setter = { state.value = it },
            getter = { state.value }
        )
    }
}
