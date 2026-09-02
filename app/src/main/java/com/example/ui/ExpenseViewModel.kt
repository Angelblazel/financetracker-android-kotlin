package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Expense
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: Int, val downloadedMb: Double, val totalMb: Double) : DownloadState
    object Success : DownloadState
    data class Error(val message: String) : DownloadState
}

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _intelligenceMode = MutableStateFlow(repository.getIntelligenceMode())
    val intelligenceMode: StateFlow<String> = _intelligenceMode.asStateFlow()

    // ── Autenticación ──────────────────────────────────────────────────────────
    private val _userLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val userLoggedIn: StateFlow<Boolean> = _userLoggedIn.asStateFlow()

    private val _isGuestMode = MutableStateFlow(repository.isGuestMode())
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _userName = MutableStateFlow(repository.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(repository.getUserEmail())
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPhoto = MutableStateFlow(repository.getUserPhoto())
    val userPhoto: StateFlow<String?> = _userPhoto.asStateFlow()

    // Estado de sincronización con Firestore
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun loginWithGoogle(name: String, email: String, photo: String?, token: String?) {
        repository.saveUser(name, email, photo, token, isGuest = false)
        _userLoggedIn.value = true
        _isGuestMode.value = false
        _userName.value = name
        _userEmail.value = email
        _userPhoto.value = photo
        // Al iniciar sesión con Google, sincronizar datos locales a la nube
        syncLocalDataToCloud()
    }

    fun loginAsGuest() {
        repository.saveUser("Invitado", "invitado@local.com", null, null, isGuest = true)
        _userLoggedIn.value = true
        _isGuestMode.value = true
        _userName.value = "Invitado"
        _userEmail.value = "invitado@local.com"
        _userPhoto.value = null
    }

    fun logout() {
        repository.logoutUser()
        _userLoggedIn.value = false
        _isGuestMode.value = false
        _userName.value = ""
        _userEmail.value = ""
        _userPhoto.value = null
    }

    /** Sincroniza todos los gastos locales pendientes hacia Firestore */
    private fun syncLocalDataToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = SyncStatus.Syncing
            try {
                repository.syncExpensesToCloud()
                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("Error al sincronizar: ${e.localizedMessage}")
            }
        }
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    // ── Descarga / Importación del modelo Gemma ────────────────────────────────
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun downloadModelFile() {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadState.value = DownloadState.Downloading(0, 0.0, 1380.0)
            try {
                val urlString = "https://huggingface.co/raun/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                connection.instanceFollowRedirects = true
                connection.connect()

                var activeConnection = connection
                var responseCode = connection.responseCode
                var redirectCount = 0
                while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) && redirectCount < 5) {
                    val newUrl = activeConnection.getHeaderField("Location")
                    activeConnection.disconnect()
                    val redirectedUrl = URL(newUrl)
                    activeConnection = redirectedUrl.openConnection() as HttpURLConnection
                    activeConnection.instanceFollowRedirects = true
                    activeConnection.connect()
                    responseCode = activeConnection.responseCode
                    redirectCount++
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    _downloadState.value = DownloadState.Error("Error HTTP: $responseCode")
                    activeConnection.disconnect()
                    return@launch
                }

                val totalLength = activeConnection.contentLengthLong
                val file = File(getModelPath())
                file.parentFile?.mkdirs()
                if (file.exists()) file.delete()

                val input = BufferedInputStream(activeConnection.inputStream)
                val output = FileOutputStream(file)
                val data = ByteArray(1024 * 32)
                var total: Long = 0
                var count: Int
                var lastProgressUpdate = 0L

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    val progress = if (totalLength > 0) ((total * 100) / totalLength).toInt() else 0
                    val currentMb = total.toDouble() / (1024 * 1024)
                    val totalMb = if (totalLength > 0) totalLength.toDouble() / (1024 * 1024) else 1380.0
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 250 || progress == 100) {
                        _downloadState.value = DownloadState.Downloading(progress, currentMb, totalMb)
                        lastProgressUpdate = now
                    }
                }

                output.flush(); output.close(); input.close(); activeConnection.disconnect()
                _downloadState.value = DownloadState.Success
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Error downloading model file", e)
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    fun importModelFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadState.value = DownloadState.Downloading(0, 0.0, 1380.0)
            try {
                val contentResolver = context.contentResolver
                var totalBytes = 0L
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            totalBytes = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (_: Exception) { }

                val totalMb = if (totalBytes > 0) totalBytes.toDouble() / (1024.0 * 1024.0) else 1380.0
                val targetFile = File(getModelPath())
                targetFile.parentFile?.mkdirs()

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            val progress = if (totalBytes > 0) ((totalBytesRead * 100) / totalBytes).toInt() else 0
                            val currentMb = totalBytesRead.toDouble() / (1024.0 * 1024.0)
                            _downloadState.value = DownloadState.Downloading(progress, currentMb, totalMb)
                        }
                    }
                }
                _downloadState.value = DownloadState.Success
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error("Error al importar: ${e.localizedMessage}")
            }
        }
    }

    // ── Modo de inteligencia ───────────────────────────────────────────────────
    fun setIntelligenceMode(mode: String) {
        repository.setIntelligenceMode(mode)
        _intelligenceMode.value = mode
    }

    fun getInstallmentMode(): String = repository.getInstallmentMode()
    fun setInstallmentMode(mode: String) = repository.setInstallmentMode(mode)

    // ── LLM On-Device ─────────────────────────────────────────────────────────
    fun isModelFilePresent(): Boolean = repository.localLlmManager.isModelFilePresent()
    fun isRealLlmEnabled(): Boolean = repository.localLlmManager.isUseRealLlmEnabled()
    fun setRealLlmEnabled(enabled: Boolean) = repository.localLlmManager.setUseRealLlmEnabled(enabled)
    fun getModelPath(): String = repository.localLlmManager.getModelPath()
    fun setModelPath(path: String) = repository.localLlmManager.setModelPath(path)

    fun resolvePathFromUri(context: Context, uri: Uri): String? {
        try {
            if ("com.android.externalstorage.documents" == uri.authority) {
                val pathSegments = uri.pathSegments
                val docId = when {
                    pathSegments.contains("tree") -> pathSegments.getOrNull(pathSegments.indexOf("tree") + 1)
                    pathSegments.contains("document") -> pathSegments.getOrNull(pathSegments.indexOf("document") + 1)
                    else -> null
                }
                if (docId != null) {
                    val split = docId.split(":")
                    if ("primary".equals(split[0], ignoreCase = true)) {
                        val relativePath = split.getOrElse(1) { "" }
                        return File(Environment.getExternalStorageDirectory(), relativePath).absolutePath
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    // ── Lista de gastos ────────────────────────────────────────────────────────
    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.delete(expense) }
    }

    fun deleteAllExpenses() {
        viewModelScope.launch { repository.deleteAll() }
    }

    fun addManualExpense(amount: Double, merchant: String, category: String, currency: String) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                merchant = merchant,
                category = category,
                currency = currency,
                originalText = "Registrado manualmente",
                originalApp = "manual",
                timestamp = System.currentTimeMillis(),
                isPending = false
            )
            repository.insert(expense)
            // Si el usuario tiene sesión Google, también guarda en la nube
            if (!_isGuestMode.value) {
                repository.saveExpenseToCloud(expense)
            }
        }
    }

    fun simulateNotification(title: String, body: String, appName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val isSuccess = repository.processNotification(title = title, body = body, appName = appName, timestamp = System.currentTimeMillis())
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onResult(isSuccess)
            }
        }
    }

    fun triggerRealSystemNotification(context: Context, title: String, body: String) {
        val channelId = "expense_test_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Prueba de Notificaciones", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val testTitle = if (title.startsWith("[Test]")) title else "[Test] $title"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(testTitle)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            flat.split(":").forEach { name ->
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && pkgName == cn.packageName) return true
            }
        }
        return false
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    object Success : SyncStatus
    data class Error(val message: String) : SyncStatus
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
