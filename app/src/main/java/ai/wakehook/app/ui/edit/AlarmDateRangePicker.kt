package ai.wakehook.app.ui.edit

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import ai.wakehook.app.R
import ai.wakehook.app.domain.AlarmDateRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Adds an inclusive range with a guided start/end date selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDateRangePicker(
    onDismiss: () -> Unit,
    onAddRange: (AlarmDateRange) -> Unit,
) {
    val today = LocalDate.now()
    val earliestDay = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
        }
    }
    var choosingEnd by rememberSaveable { mutableStateOf(false) }
    var startDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val startState = rememberDatePickerState(
        yearRange = today.year..(today.year + 10), selectableDates = earliestDay,
    )
    val endState = rememberDatePickerState(
        yearRange = today.year..(today.year + 10),
        selectableDates = remember(startDate, today) {
            val minimum = startDate ?: today.plusDays(1)
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(minimum)
            }
        },
    )
    val activeDate = (if (choosingEnd) endState.selectedDateMillis else startState.selectedDateMillis)
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val validEnd = activeDate?.let { end -> startDate?.let { !end.isBefore(it) } } == true

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = if (choosingEnd) validEnd else startState.selectedDateMillis != null,
                onClick = {
                    if (!choosingEnd) {
                        startState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            choosingEnd = true
                        }
                    } else {
                        val start = startDate
                        val end = activeDate
                        if (start != null && end != null && !end.isBefore(start)) {
                            onAddRange(AlarmDateRange(start, end))
                            onDismiss()
                        }
                    }
                },
            ) { Text(stringResource(if (choosingEnd) R.string.add_date_range else R.string.next_date_step)) }
        },
        dismissButton = {
            TextButton(onClick = { if (choosingEnd) choosingEnd = false else onDismiss() }) {
                Text(stringResource(if (choosingEnd) R.string.back_to_start_date else R.string.cancel))
            }
        },
    ) {
        if (choosingEnd) DatePicker(state = endState, title = { Text(stringResource(R.string.choose_end_date)) })
        else DatePicker(state = startState, title = { Text(stringResource(R.string.choose_start_date)) })
    }
}
