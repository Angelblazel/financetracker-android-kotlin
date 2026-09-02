package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Expense
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val userLoggedIn by viewModel.userLoggedIn.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userPhoto by viewModel.userPhoto.collectAsStateWithLifecycle()

    val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    if (!userLoggedIn) {
        GoogleLoginScreen(
            onLoginSuccess = { name, email, photoUrl, idToken ->
                viewModel.loginWithGoogle(name, email, photoUrl, idToken)
            },
            onLoginAsGuest = {
                viewModel.loginAsGuest()
            }
        )
        return
    }

    var showManualDialog by remember { mutableStateOf(false) }
    var showSimulateDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }
    var isAccessGranted by remember { mutableStateOf(viewModel.isNotificationAccessGranted(context)) }

    // Navigation state for the bottom bar simulation
    var selectedTab by remember { mutableStateOf("Inicio") }

    // Periodically refresh the permission status when the app is resumed
    LaunchedEffect(Unit) {
        while (true) {
            isAccessGranted = viewModel.isNotificationAccessGranted(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        bottomBar = {
            // Elegant navigation bar matching the HTML theme mockup
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Inicio
                    BottomNavItem(
                        label = "Inicio",
                        icon = Icons.Default.Home,
                        isSelected = selectedTab == "Inicio",
                        onClick = { selectedTab = "Inicio" }
                    )

                    // Tab 2: Análisis
                    BottomNavItem(
                        label = "Análisis",
                        icon = Icons.Default.BarChart,
                        isSelected = selectedTab == "Análisis",
                        onClick = { selectedTab = "Análisis" }
                    )

                    // Centered Simulated FAB inside the bar
                    Box(
                        modifier = Modifier
                            .offset(y = (-12).dp)
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .clickable { showSimulateDialog = true }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Simular Alerta",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Tab 3: Logs
                    BottomNavItem(
                        label = "Logs",
                        icon = Icons.Default.ChatBubbleOutline,
                        isSelected = selectedTab == "Logs",
                        onClick = { selectedTab = "Logs" }
                    )

                    // Tab 4: Ajustes
                    BottomNavItem(
                        label = "Ajustes",
                        icon = Icons.Default.Settings,
                        isSelected = selectedTab == "Ajustes",
                        onClick = { selectedTab = "Ajustes" }
                    )
                }
            }
        },
        floatingActionButton = {
            // Fast Add floating action button
            FloatingActionButton(
                onClick = { showManualDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Registrar Gasto Manual",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                "Inicio" -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Custom Top Bar header matching the sleek aesthetic
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 24.dp, end = 20.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SISTEMA INTELIGENTE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "FinanceTracker",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = { showDeleteAllConfirmation = true },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Borrar todo el historial",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isAccessGranted) Color(0xFF4CAF50) else Color(0xFFFFB74D))
                                    )
                                }
                            }
                        }

                        // LLM Local Active Banner
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(18.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val intelligenceMode by viewModel.intelligenceMode.collectAsStateWithLifecycle()
                                    val modeTitle = if (intelligenceMode == "local") {
                                        if (viewModel.isRealLlmEnabled() && viewModel.isModelFilePresent()) "LLM GEMMA LOCAL ACTIVO" else "IA LOCAL ACTIVA"
                                    } else {
                                        "GEMINI CLOUD ACTIVO"
                                    }
                                    val modeDesc = if (intelligenceMode == "local") {
                                        if (viewModel.isRealLlmEnabled() && viewModel.isModelFilePresent()) "Modelo Gemma procesando localmente en tu celular." else "Motor local interpretando alertas de forma instantánea."
                                    } else {
                                        "Procesando notificaciones usando la API de Gemini en la nube."
                                    }
                                    Text(
                                        text = modeTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = modeDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Permission Warning Banner if access is not granted
                        if (!isAccessGranted) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Advertencia",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            "Servicio Desactivado",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Text(
                                        "Otorga el permiso especial de lectura de notificaciones para automatizar el registro con IA local.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Button(
                                        onClick = { viewModel.openNotificationSettings(context) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Habilitar Lectura", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }

                        // Total Week Summary Card
                        SummaryCard(expenses = expenses)

                        // Expenses List Title
                        Text(
                            text = "Capturas Recientes",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )

                        if (expenses.isEmpty()) {
                            EmptyState(modifier = Modifier.weight(1f))
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(expenses, key = { it.id }) { expense ->
                                    ExpenseItem(
                                        expense = expense,
                                        onDelete = { viewModel.deleteExpense(expense) }
                                    )
                                }
                            }
                        }
                    }
                }
                "Análisis" -> {
                    AnalisisTabContent(expenses = expenses)
                }
                "Logs" -> {
                    LogsTabContent(expenses = expenses)
                }
                "Ajustes" -> {
                    AjustesTabContent(
                        viewModel = viewModel,
                        context = context,
                        isAccessGranted = isAccessGranted
                    )
                }
            }
        }
    }

    if (showManualDialog) {
        ManualExpenseDialog(
            onDismiss = { showManualDialog = false },
            onSave = { amount, merchant, category, currency ->
                viewModel.addManualExpense(amount, merchant, category, currency)
                showManualDialog = false
            }
        )
    }

    if (showSimulateDialog) {
        SimulateNotificationDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { title, body, appName ->
                viewModel.simulateNotification(title, body, appName) { isAdded ->
                    if (isAdded) {
                        Toast.makeText(context, "✅ Gasto registrado automáticamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "ℹ️ Notificación ignorada (Transacción rechazada, depósito o alerta)", Toast.LENGTH_LONG).show()
                    }
                }
                showSimulateDialog = false
            },
            onTriggerRealNotification = { title, body ->
                viewModel.triggerRealSystemNotification(context, title, body)
                showSimulateDialog = false
                Toast.makeText(context, "Notificación real enviada a la barra de estado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text("¿Eliminar todo el historial?") },
            text = { Text("Esta acción eliminará de forma permanente todas las alertas capturadas y el historial de gastos. No se puede deshacer. ¿Deseas continuar?") },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllExpenses()
                        showDeleteAllConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Eliminar todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AnalisisTabContent(expenses: List<Expense>) {
    val parsedExpenses = expenses.filter { !it.isPending && it.parseError == null && it.amount > 0 }
    val totalByCurrency = parsedExpenses.groupBy { it.currency }

    var selectedCurrency by remember(totalByCurrency) {
        mutableStateOf(totalByCurrency.keys.firstOrNull() ?: "USD")
    }

    val currencyExpenses = parsedExpenses.filter { it.currency == selectedCurrency }
    val totalSpend = currencyExpenses.sumOf { it.amount }

    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Utilities", "Services", "Others")
    val categorySums = categories.map { cat ->
        cat to currencyExpenses.filter { it.category == cat }.sumOf { it.amount }
    }.filter { it.second > 0.0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Análisis de Gastos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Currency Selector Row
        if (totalByCurrency.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                totalByCurrency.keys.forEach { curr ->
                    val isSelected = curr == selectedCurrency
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCurrency = curr },
                        label = { Text(curr, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Show total summary card for selected currency
        if (totalSpend > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Total Gastado ($selectedCurrency)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        String.format(Locale.getDefault(), "%,.2f", totalSpend) + " $selectedCurrency",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (parsedExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay datos de gastos procesados para graficar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 📊 1. Donut Chart (Gráfico de Pastel/Circular)
            if (totalSpend > 0 && categorySums.isNotEmpty()) {
                DonutChart(categorySums = categorySums, total = totalSpend, currency = selectedCurrency)
            }

            // 📈 2. Trend Chart (Gráfico de Tendencia de los últimos 7 días)
            if (totalSpend > 0) {
                val sdf = SimpleDateFormat("EE", Locale.getDefault())
                val last7Days = (0..6).map { i ->
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    cal
                }.reversed()

                val trendData = last7Days.map { cal ->
                    val startOfDay = cal.clone() as Calendar
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0)
                    startOfDay.set(Calendar.MINUTE, 0)
                    startOfDay.set(Calendar.SECOND, 0)
                    startOfDay.set(Calendar.MILLISECOND, 0)
                    val endOfDay = cal.clone() as Calendar
                    endOfDay.set(Calendar.HOUR_OF_DAY, 23)
                    endOfDay.set(Calendar.MINUTE, 59)
                    endOfDay.set(Calendar.SECOND, 59)
                    endOfDay.set(Calendar.MILLISECOND, 999)

                    val dayLabel = sdf.format(cal.time).uppercase(Locale.getDefault())
                    val daySum = currencyExpenses.filter { it.timestamp in startOfDay.timeInMillis..endOfDay.timeInMillis }.sumOf { it.amount }
                    dayLabel to daySum
                }

                SpendingTrendChart(trendData = trendData, currency = selectedCurrency)
            }

            // 📋 3. Breakdown with Progress Indicators
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Desglose por Categorías",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    categories.forEach { cat ->
                        val catExpenses = currencyExpenses.filter { it.category == cat }
                        val catSum = catExpenses.sumOf { it.amount }
                        
                        if (catSum > 0) {
                            val categoryEmoji = when (cat) {
                                "Food" -> "☕ Alimentación"
                                "Transport" -> "🚗 Transporte"
                                "Shopping" -> "🛒 Compras"
                                "Entertainment" -> "🎬 Entretenimiento"
                                "Utilities" -> "💡 Servicios Públicos"
                                "Services" -> "🔧 Servicios"
                                else -> "💸 Otros"
                            }
                            
                            val categoryColor = when (cat) {
                                "Food" -> Color(0xFFFFB74D)
                                "Transport" -> Color(0xFF64B5F6)
                                "Shopping" -> Color(0xFFBA68C8)
                                "Entertainment" -> Color(0xFFE57373)
                                "Utilities" -> Color(0xFFFFF176)
                                "Services" -> Color(0xFF4DB6AC)
                                else -> Color(0xFF90A4AE)
                            }

                            val pct = if (totalSpend > 0) (catSum / totalSpend).toFloat() else 0f

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = categoryEmoji,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%,.2f", catSum) + " $selectedCurrency",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = categoryColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    categorySums: List<Pair<String, Double>>,
    total: Double,
    currency: String
) {
    val categoryColors = mapOf(
        "Food" to Color(0xFFFFB74D),
        "Transport" to Color(0xFF64B5F6),
        "Shopping" to Color(0xFFBA68C8),
        "Entertainment" to Color(0xFFE57373),
        "Utilities" to Color(0xFFFFF176),
        "Services" to Color(0xFF4DB6AC),
        "Others" to Color(0xFF90A4AE)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Distribución por Categorías",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    var startAngle = -90f
                    categorySums.forEach { (category, sum) ->
                        val sweepAngle = ((sum / total) * 360f).toFloat()
                        val color = categoryColors[category] ?: Color(0xFF90A4AE)
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                        startAngle += sweepAngle
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format(Locale.getDefault(), "%,.0f", total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        currency,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Legend
            val chunkedSums = categorySums.chunked(2)
            chunkedSums.forEach { rowList ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowList.forEach { (cat, sum) ->
                        val color = categoryColors[cat] ?: Color(0xFF90A4AE)
                        val label = when (cat) {
                            "Food" -> "Comida"
                            "Transport" -> "Transporte"
                            "Shopping" -> "Compras"
                            "Entertainment" -> "Entret."
                            "Utilities" -> "Servicios"
                            "Services" -> "Manten."
                            else -> "Otros"
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = "$label (${String.format(Locale.getDefault(), "%.0f%%", (sum / total) * 100)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingTrendChart(
    trendData: List<Pair<String, Double>>,
    currency: String
) {
    val maxAmount = trendData.maxOfOrNull { it.second } ?: 1.0
    val targetMax = if (maxAmount == 0.0) 1.0 else maxAmount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Tendencia de Gastos (Últimos 7 días)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 40.dp.toPx()
                    val paddingBottom = 24.dp.toPx()
                    val chartWidth = width - paddingLeft
                    val chartHeight = height - paddingBottom

                    // Draw background grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = chartHeight - (chartHeight * (i.toFloat() / gridLines))
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Bars
                    val barSpacing = chartWidth / trendData.size
                    trendData.forEachIndexed { index, (_, sum) ->
                        val x = paddingLeft + (index * barSpacing) + (barSpacing / 4f)
                        val barWidth = barSpacing / 2f
                        val barHeight = (sum / targetMax * chartHeight).toFloat()
                        val y = chartHeight - barHeight

                        // Draw bar
                        if (sum > 0) {
                            drawRoundRect(
                                color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        } else {
                            // Draw empty dot indicating $0 spending
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x + barWidth / 2f, chartHeight)
                             )
                        }
                    }
                }
            }

            // Labels row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trendData.forEach { (label, _) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun LogsTabContent(expenses: List<Expense>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Consola de IA Local",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Registros de análisis y extracción en tiempo real",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se han registrado eventos o logs todavía.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val badgeColor = when {
                                    exp.isPending -> MaterialTheme.colorScheme.primary
                                    exp.parseError != null -> MaterialTheme.colorScheme.error
                                    exp.merchant == "Notificación Ignorada" -> MaterialTheme.colorScheme.outline
                                    else -> Color(0xFF4CAF50)
                                }
                                val badgeText = when {
                                    exp.isPending -> "PROCESANDO"
                                    exp.parseError != null -> "ERROR"
                                    exp.merchant == "Notificación Ignorada" -> "IGNORADO"
                                    else -> "ÉXITO"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = "Motor: ${exp.engineUsed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = exp.originalText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (exp.parseError != null) {
                                Text(
                                    text = "Detalle: ${exp.parseError}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    lineHeight = 12.sp
                                )
                            } else if (!exp.isPending && exp.merchant != "Notificación Ignorada") {
                                Text(
                                    text = "Extracción: ${exp.merchant} | ${exp.amount} ${exp.currency} | ${exp.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AjustesTabContent(
    viewModel: ExpenseViewModel,
    context: android.content.Context,
    isAccessGranted: Boolean
) {
    val intelligenceMode by viewModel.intelligenceMode.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userPhoto by viewModel.userPhoto.collectAsStateWithLifecycle()
    var modelPath by remember { mutableStateOf(viewModel.getModelPath()) }
    var realLlmEnabled by remember { mutableStateOf(viewModel.isRealLlmEnabled()) }
    var fileVerificationStatus by remember { mutableStateOf<Boolean?>(null) }
    var installmentMode by remember { mutableStateOf(viewModel.getInstallmentMode()) }

    // Sincronizar estado del LLM al entrar a la pantalla de ajustes
    LaunchedEffect(Unit) {
        modelPath = viewModel.getModelPath()
        realLlmEnabled = viewModel.isRealLlmEnabled()
        fileVerificationStatus = if (viewModel.isModelFilePresent()) true else null
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = viewModel.resolvePathFromUri(context, uri)
            if (resolved != null) {
                val fullPath = if (resolved.endsWith("/")) "${resolved}gemma-2b-it-cpu-int4.bin" else "$resolved/gemma-2b-it-cpu-int4.bin"
                modelPath = fullPath
                viewModel.setModelPath(fullPath)
                fileVerificationStatus = viewModel.isModelFilePresent()
                Toast.makeText(context, "Carpeta seleccionada con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No se pudo resolver la ruta física. Intenta seleccionando el archivo directamente para importarlo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importModelFromUri(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración del Sistema",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 👤 SECTION: PERFIL DE GOOGLE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Avatar placeholder / real photo
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val isGuest = userEmail.contains("invitado", ignoreCase = true)
                    Text(
                        text = if (isGuest) "Modo Invitado · Datos solo locales"
                               else "Conectado con Google · Datos en la nube",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGuest) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    // Chip indicador de almacenamiento
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = if (isGuest)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isGuest) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isGuest) MaterialTheme.colorScheme.onSecondaryContainer
                                       else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isGuest) "Almacenamiento Local" else "Sincronizado en Firebase",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isGuest) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.logout() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 🧠 SECTION 1: ENGINE SELECTOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Modo de Inteligencia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Selecciona el motor de inteligencia que procesará tus alertas bancarias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Local Engine
                    val isLocal = intelligenceMode == "local"
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setIntelligenceMode("local") },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLocal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isLocal) 2.dp else 1.dp,
                            color = if (isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "IA Local",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "100% Offline\nPrivacidad Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Cloud Engine
                    val isCloud = intelligenceMode == "cloud"
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setIntelligenceMode("cloud") },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCloud) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isCloud) 2.dp else 1.dp,
                            color = if (isCloud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (isCloud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Gemini Cloud",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Requiere API Key\nMáxima Precisión",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 🚀 SECTION 2: LOCAL LLM REAL SETUP (EXPERIMENTAL)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LLM On-Device (Gemma .bin)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = realLlmEnabled,
                        onCheckedChange = {
                            realLlmEnabled = it
                            viewModel.setRealLlmEnabled(it)
                        }
                    )
                }

                Text(
                    "Habilita el uso de un modelo de lenguaje real (como Gemma 2B o Llama) ejecutándose de manera nativa en el procesador de tu celular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (realLlmEnabled) {
                    OutlinedTextField(
                        value = modelPath,
                        onValueChange = {
                            modelPath = it
                            viewModel.setModelPath(it)
                        },
                        label = { Text("Ruta del archivo de modelo (.bin)") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seleccionar Carpeta", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*", "application/octet-stream")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar Archivo .bin", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (downloadState is DownloadState.Downloading) {
                        val state = downloadState as DownloadState.Downloading
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "Importando/Copiando archivo de modelo...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { if (state.totalMb > 0f) (state.downloadedMb / state.totalMb).toFloat() else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${state.progress}% completado",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        String.format(Locale.getDefault(), "%.1f MB / %.1f MB", state.downloadedMb, state.totalMb),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (downloadState is DownloadState.Success) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                                Column {
                                    Text(
                                        "¡Importación completada con éxito!",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        "El archivo ya está listo para usarse localmente.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        LaunchedEffect(Unit) {
                            modelPath = viewModel.getModelPath()
                            fileVerificationStatus = viewModel.isModelFilePresent()
                            viewModel.resetDownloadState()
                        }
                    } else if (downloadState is DownloadState.Error) {
                        val state = downloadState as DownloadState.Error
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            fileVerificationStatus = viewModel.isModelFilePresent()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verificar archivo en celular")
                    }

                    // Verification status message
                    fileVerificationStatus?.let { isPresent ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPresent) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isPresent) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                Text(
                                    text = if (isPresent) {
                                        "¡Modelo detectado exitosamente! El sistema procesará las notificaciones usando tu archivo Gemma."
                                    } else {
                                        "Archivo no encontrado en esa ruta. Puedes usar la guía manual de abajo o el importador automático para copiarlo directamente."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPresent) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    // ⬇️ SECCIÓN DE GUÍA DE INSTALACIÓN MANUAL DE GEMMA
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val uriHandler = LocalUriHandler.current
                            val clipboardManager = LocalClipboardManager.current
                            val context = LocalContext.current

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        "Guía de Instalación Manual de Gemma",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Sigue estos sencillos pasos para instalar el modelo LLM nativo en tu dispositivo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "1.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Descarga el modelo Gemma desde Kaggle: Toca el botón de abajo para ir a Kaggle, inicia sesión si es necesario, acepta los términos de licencia de Google Gemma y descarga el archivo 'gemma-2b-it-cpu-int4.bin' (aproximadamente 1.35 GB).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "2.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Copia la ruta interna de destino: Toca el botón 'Copiar Ruta de Destino' para guardar en el portapapeles el directorio exacto donde la app buscará el archivo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "3.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Ubica el archivo: Abre el explorador de archivos de tu celular y mueve el archivo descargado desde tu carpeta de 'Descargas' (Downloads) a la ruta interna copiada. O bien, mantén el archivo en tus Descargas y edita el campo de texto de arriba para apuntar a la ruta de tus descargas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { uriHandler.openUri("https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-cpu-int4") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar Modelo en Kaggle")
                            }

                            Button(
                                onClick = {
                                    val path = viewModel.getModelPath()
                                    clipboardManager.setText(AnnotatedString(path))
                                    Toast.makeText(context, "¡Ruta copiada al portapapeles!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copiar Ruta de Destino Interna")
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Motor Local Inteligente ACTIVO. Parsea notificaciones de forma inmediata sin consumir RAM, batería ni requerir descargas de archivos de gigabytes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 💳 SECTION: MANEJO DE CUOTAS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Manejo de Compras en Cuotas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Define cómo se registrará el monto cuando el banco notifique compras con cuotas mensuales para prevenir doble contabilización.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Individual Installment Value
                    val isIndiv = installmentMode == "individual"
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                installmentMode = "individual"
                                viewModel.setInstallmentMode("individual")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isIndiv) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isIndiv) 2.dp else 1.dp,
                            color = if (isIndiv) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isIndiv) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Cuota Individual",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isIndiv) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                "Recomendado\nRegistra solo el valor de 1 cuota (ej: $10.000). Evita duplicidad de gastos en los meses siguientes.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Option 2: Total Sum
                    val isTotal = installmentMode == "total"
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                installmentMode = "total"
                                viewModel.setInstallmentMode("total")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTotal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isTotal) 2.dp else 1.dp,
                            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Monto Total",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                "Multiplicado\nRegistra el valor total (ej: $30.000) hoy mismo para ver el impacto financiero upfront.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 🔔 SECTION 3: SYSTEM PERMISSIONS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Permisos de Notificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lectura de Notificaciones:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAccessGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAccessGranted) "CONCEDIDO" else "PENDIENTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAccessGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openNotificationSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isAccessGranted) "Reconfigurar Permisos" else "Otorgar Permiso")
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun SummaryCard(expenses: List<Expense>) {
    val parsedExpenses = expenses.filter { !it.isPending && it.parseError == null && it.amount > 0 }
    val totalByCurrency = parsedExpenses.groupBy { it.currency }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "Total Semana",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            if (totalByCurrency.isEmpty()) {
                Text(
                    "0,00 USD",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    totalByCurrency.forEach { (currency, list) ->
                        val total = list.sumOf { it.amount }
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f", total) + " $currency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val categoryColor = when (expense.category) {
        "Food" -> Color(0xFFFFB74D)
        "Transport" -> Color(0xFF64B5F6)
        "Shopping" -> Color(0xFFBA68C8)
        "Entertainment" -> Color(0xFFE57373)
        "Utilities" -> Color(0xFFFFF176)
        "Services" -> Color(0xFF4DB6AC)
        else -> Color(0xFF90A4AE)
    }

    val categoryEmoji = when (expense.category) {
        "Food" -> "☕"
        "Transport" -> "🚗"
        "Shopping" -> "🛒"
        "Entertainment" -> "🎬"
        "Utilities" -> "💡"
        "Services" -> "🔧"
        else -> "💸"
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(expense.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Emoji Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (expense.isPending) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                categoryColor.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (expense.isPending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(text = categoryEmoji, fontSize = 20.sp)
                    }
                }

                // Middle Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = expense.merchant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (expense.parseError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Detectado en: " + if (expense.originalApp == "manual") "Gasto Manual" else if (expense.originalApp == "simulated") "Simulador" else expense.originalApp.substringAfterLast("."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount
                if (!expense.isPending && expense.parseError == null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.2f", expense.amount)} ${expense.currency}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Borrar registro",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Interpretation Box matching the exact layout of HTML
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "AI:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (expense.isPending) {
                        "Procesando y analizando la alerta con IA..."
                    } else if (expense.parseError != null) {
                        "Fallo en la extracción de la alerta: ${expense.parseError}"
                    } else {
                        val translatedCategory = when (expense.category) {
                            "Food" -> "Alimentación"
                            "Transport" -> "Transporte"
                            "Shopping" -> "Compras"
                            "Entertainment" -> "Entretenimiento"
                            "Utilities" -> "Servicios Públicos"
                            "Services" -> "Servicios"
                            else -> "Otros"
                        }
                        "Gasto interpretado en la categoría de $translatedCategory en el comercio ${expense.merchant}. Registrado correctamente en base de datos local."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            // Expanded original notification inspect
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Texto Completo Recibido:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        expense.originalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Fecha de captura: $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sin Gastos Registrados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Las notificaciones de compras de tus bancos se analizarán con IA local y aparecerán aquí automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "💡 TIP: Usa el botón flotante de cerebro para simular alertas bancarias e inspectar la extracción en tiempo real.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ManualExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var category by remember { mutableStateOf("Food") }

    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Utilities", "Services", "Others")
    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto Manual") },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    placeholder = { Text("Ej. 15.50 o 12000") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio / Recipiente") },
                    placeholder = { Text("Ej. Starbucks, Walmart") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase() },
                    label = { Text("Moneda (3 letras)") },
                    placeholder = { Text("Ej. USD, COP, MXN") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { catExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Categoría: $category")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doubleAmount = amount.toDoubleOrNull() ?: 0.0
                    if (doubleAmount > 0 && merchant.isNotEmpty()) {
                        onSave(doubleAmount, merchant, category, currency)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulateNotificationDialog(
    onDismiss: () -> Unit,
    onSimulate: (String, String, String) -> Unit,
    onTriggerRealNotification: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("BBVA Alertas") }
    var body by remember { mutableStateOf("Compra autorizada en STARBUCKS por $45.50 USD.") }
    var appName by remember { mutableStateOf("com.bbva.mexico") }

    val templates = listOf(
        NotificationTemplate("BBVA (Compra USD)", "BBVA Alertas", "Compra autorizada en STARBUCKS por $45.50 USD.", "com.bbva.mexico"),
        NotificationTemplate("Pago Rechazado (Descartar)", "Bancolombia", "Transacción rechazada por $15,000 COP en Starbucks por fondos insuficientes.", "com.bancolombia.olb"),
        NotificationTemplate("Nequi (Depósito - Descartar)", "Nequi", "¡Buenas noticias! Recibiste un envío de $15,000.00 COP de Carlos Gomez.", "com.nequi.app"),
        NotificationTemplate("Mercado Pago (Pago)", "Mercado Pago", "Pagaste $350 MXN en TIENDA OXXO.", "com.mercadolibre"),
        NotificationTemplate("SMS Bancario (EUR)", "Banco Alerta", "Cargo por EUR 12.99 aprobado en NETFLIX.", "com.android.messaging"),
        NotificationTemplate("OTP Código (Descartar)", "TuBanco", "Tu código de verificación es 492043. No lo compartas.", "com.android.messaging")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulador de Notificaciones") },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Selecciona una plantilla realista o escribe una personalizada. Tienes dos opciones de prueba abajo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Template Chips / Scrollable Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    templates.forEach { temp ->
                        SuggestionChip(
                            onClick = {
                                title = temp.title
                                body = temp.body
                                appName = temp.packageName
                            },
                            label = { Text(temp.name) }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Paquete de la App") },
                    placeholder = { Text("Ej. com.bbva.app") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la Notificación") },
                    placeholder = { Text("Ej. Alerta de Tarjeta") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Mensaje de la Notificación") },
                    placeholder = { Text("Ej. Compra de $12.50 en UBER.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Button 1: Direct in-app injection
                TextButton(
                    onClick = {
                        if (body.isNotEmpty()) {
                            onSimulate(title, body, appName)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inyectar Directo", fontSize = 11.sp)
                }

                // Button 2: Real System Notification
                Button(
                    onClick = {
                        if (body.isNotEmpty()) {
                            onTriggerRealNotification(title, body)
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Notificación Real", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

data class NotificationTemplate(
    val name: String,
    val title: String,
    val body: String,
    val packageName: String
)

@Composable
fun GoogleLoginScreen(
    onLoginSuccess: (name: String, email: String, photoUrl: String?, idToken: String?) -> Unit,
    onLoginAsGuest: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDemoFallback by remember { mutableStateOf(false) }

    // Configure Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("517573726241-o1c150nej5p5amrivb203fovgtu129ai.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // Sign-In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                onLoginSuccess(
                    account.displayName ?: "Usuario de Google",
                    account.email ?: "usuario@gmail.com",
                    account.photoUrl?.toString(),
                    account.idToken
                )
                Toast.makeText(context, "¡Sesión iniciada con éxito!", Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "No se pudo obtener la cuenta de Google."
                showDemoFallback = true
            }
        } catch (e: ApiException) {
            // Error code 12501 means the user canceled the sign-in.
            // Other codes might indicate missing configuration (like SHA-1 signature mismatch or missing client ID).
            if (e.statusCode != 12501) {
                errorMessage = "Error de conexión con Google (Código: ${e.statusCode}). Esto ocurre frecuentemente si la firma SHA-1 o el Client ID no están vinculados en la Consola de Google Cloud / Firebase."
            } else {
                errorMessage = "Inicio de sesión cancelado por el usuario."
            }
            showDemoFallback = true
        }
    }

    // Google Sign-In Screen UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Decorative Fintech Header Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "FinanceTracker Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            // Application Headings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "FinanceTracker",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Automatización Inteligente de Finanzas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Rastrea gastos directamente desde tus alertas bancarias usando Inteligencia Artificial local y segura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Styled "Sign In with Google" Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        try {
                            // Reset state
                            errorMessage = null
                            showDemoFallback = false
                            // Launch Sign-In
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            errorMessage = "Error al iniciar el flujo de autenticación: ${e.localizedMessage}"
                            showDemoFallback = true
                        }
                    },
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Styled custom Google icon representation using clean colors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }

                    Text(
                        text = "Iniciar Sesión con Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Secondary Outlined "Entrar como Invitado" Button
            OutlinedButton(
                onClick = {
                    onLoginAsGuest()
                    Toast.makeText(context, "Bienvenido (Modo Invitado - datos locales)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Entrar como Invitado (Local)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Highlighting Privacy & Security
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Autenticación segura y procesamiento local",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Beautiful Resilient Fallback Dialog (If Google Sign-In fails or has missing credentials)
        if (showDemoFallback) {
            AlertDialog(
                onDismissRequest = { showDemoFallback = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Configuración de Google Play")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "El inicio de sesión de Google requiere el registro de las llaves SHA-1 de la aplicación en la consola de Firebase/Google Cloud para autorizar la conexión en producción.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (errorMessage != null) {
                            Text(
                                "Detalles del error:\n$errorMessage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Para propósitos de prueba de este prototipo en AI Studio, puedes acceder usando la cuenta Demo de Google vinculada.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDemoFallback = false
                            onLoginSuccess(
                                "Ángelo Obregón",
                                "angelobregon1998@gmail.com",
                                "https://lh3.googleusercontent.com/a/default-user=s96-c",
                                "demo_token_12345"
                            )
                            Toast.makeText(context, "Bienvenido Ángelo (Cuenta Demo)", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Usar Cuenta Demo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDemoFallback = false }) {
                        Text("Intentar de Nuevo")
                    }
                }
            )
        }
    }
}
