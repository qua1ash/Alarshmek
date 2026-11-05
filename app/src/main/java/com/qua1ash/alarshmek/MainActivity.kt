package com.qua1ash.alarshmek

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar

enum class AlarmDay(val shortName: String) {
    MONDAY("Пн"), TUESDAY("Вт"), WEDNESDAY("Ср"), THURSDAY("Чт"),
    FRIDAY("Пт"), SATURDAY("Сб"), SUNDAY("Вс")
}

data class Alarm(
    val id: Long,
    val title: String,
    val hour: Int,
    val minute: Int,
    val days: Set<AlarmDay>,
    val modes: List<String> = emptyList(),
    val enabled: Boolean = true
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { AlarmApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmApp() {
    val alarms = remember { mutableStateListOf<Alarm>() }
    var deleteTarget by remember { mutableStateOf<Alarm?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding) {
        AddAlarmScreen(
            onDismiss = { isAdding = false },
            onSave = { alarm -> alarms.add(alarm); isAdding = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Будильник") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAdding = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить будильник")
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Будильников пока нет", style = MaterialTheme.typography.headlineSmall)
                Text("Нажмите +, чтобы создать первый", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        onEnabledChange = { enabled ->
                            val index = alarms.indexOf(alarm)
                            if (index >= 0) alarms[index] = alarm.copy(enabled = enabled)
                        },
                        onDelete = { deleteTarget = alarm }
                    )
                }
            }
        }
    }

    deleteTarget?.let { alarm ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить будильник?") },
            text = { Text("Будильник на " + formatTime(alarm) + " будет удалён.") },
            confirmButton = {
                TextButton(onClick = { alarms.remove(alarm); deleteTarget = null }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlarmScreen(onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val now = remember { Calendar.getInstance() }
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }
    val days = remember { mutableStateListOf<AlarmDay>().apply { addAll(AlarmDay.entries) } }

    BackHandler(onBack = onDismiss)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") }, placeholder = { Text("Например, Утренняя пробежка") },
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Время", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberPicker(hour, 0..23) { hour = it }
                    Text(":", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(minute, 0..59) { minute = it }
                }
            }

            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Повторять", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AlarmDay.entries.forEach { day ->
                        FilterChip(
                            selected = day in days,
                            onClick = { if (day in days) days.remove(day) else days.add(day) },
                            label = { Text(day.shortName, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Режимы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ModePlaceholder("Мелодия", "Выберите звук пробуждения")
                ModePlaceholder("Задание для отключения", "Задача, QR-код или другое действие")
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить режим")
                }
            }

            FilledTonalButton(
                onClick = {
                    onSave(Alarm(System.currentTimeMillis(), title.trim(), hour, minute, days.toSet()))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Сохранить будильник") }
        }
    }
}

@Composable
private fun ModePlaceholder(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Скоро", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NumberPicker(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    val rangeSize = range.count()
    val initialIndex = remember(range, value) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % rangeSize) + (value - range.first)
    }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(state, range) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect { index -> onValueChange(range.first + index % rangeSize) }
    }
    LazyColumn(
        state = state,
        modifier = Modifier.width(104.dp).height(168.dp),
        contentPadding = PaddingValues(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(Int.MAX_VALUE) { index ->
            val number = range.first + index % rangeSize
            TextButton(onClick = { onValueChange(number) }, modifier = Modifier.height(56.dp)) {
                Text(
                    text = "%02d".format(number),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (number == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onEnabledChange: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatTime(alarm), style = MaterialTheme.typography.displaySmall)
                Text(alarm.title.ifBlank { "Будильник" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    formatDays(alarm.days), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить будильник")
            }
        }
    }
}

private fun formatTime(alarm: Alarm) = "%02d:%02d".format(alarm.hour, alarm.minute)

private fun formatDays(days: Set<AlarmDay>) = when {
    days.isEmpty() -> "Один раз"
    days.size == AlarmDay.entries.size -> "Каждый день"
    else -> days.joinToString(" · ") { it.shortName }
}
