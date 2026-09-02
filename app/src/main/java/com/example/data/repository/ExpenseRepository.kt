package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ExpenseDao
import com.example.data.local.LocalLlmManager
import com.example.data.model.Expense
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import com.example.data.remote.Schema
import com.example.data.remote.ExtractedExpense
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val context: Context
) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    
    val localLlmManager = LocalLlmManager(context)

    fun getIntelligenceMode(): String {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getString("pref_intelligence_mode", "local") ?: "local"
    }

    fun setIntelligenceMode(mode: String) {
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .edit().putString("pref_intelligence_mode", mode).apply()
    }

    fun getInstallmentMode(): String {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getString("pref_installment_mode", "individual") ?: "individual"
    }

    fun setInstallmentMode(mode: String) {
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .edit().putString("pref_installment_mode", mode).apply()
    }

    // Google User Session Management
    fun isUserLoggedIn(): Boolean {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getBoolean("pref_user_logged_in", false)
    }

    fun getUserName(): String {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getString("pref_user_name", "") ?: ""
    }

    fun getUserEmail(): String {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getString("pref_user_email", "") ?: ""
    }

    fun getUserPhoto(): String? {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getString("pref_user_photo", null)
    }

    fun saveUser(name: String, email: String, photo: String?, token: String?, isGuest: Boolean = false) {
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("pref_user_logged_in", true)
            .putBoolean("pref_is_guest", isGuest)
            .putString("pref_user_name", name)
            .putString("pref_user_email", email)
            .putString("pref_user_photo", photo)
            .putString("pref_user_token", token)
            .apply()
    }

    fun logoutUser() {
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("pref_user_logged_in", false)
            .putBoolean("pref_is_guest", false)
            .remove("pref_user_name")
            .remove("pref_user_email")
            .remove("pref_user_photo")
            .remove("pref_user_token")
            .apply()
    }

    fun isGuestMode(): Boolean {
        return context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
            .getBoolean("pref_is_guest", false)
    }

    // ── Cloud sync (Firestore) ─────────────────────────────────────────────────

    /**
     * Guarda un gasto en Firestore bajo la colección del usuario autenticado.
     * Solo se llama cuando hay sesión Google (no invitado).
     */
    suspend fun saveExpenseToCloud(expense: Expense) {
        val email = getUserEmail()
        if (email.isBlank() || email == "invitado@local.com") return
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id"           to expense.id,
                "amount"       to expense.amount,
                "merchant"     to expense.merchant,
                "category"     to expense.category,
                "currency"     to expense.currency,
                "originalText" to expense.originalText,
                "originalApp"  to expense.originalApp,
                "timestamp"    to expense.timestamp,
                "isPending"    to expense.isPending,
                "parseError"   to expense.parseError,
                "engineUsed"   to expense.engineUsed
            )
            db.collection("users")
                .document(email)
                .collection("expenses")
                .document(expense.id.toString())
                .set(data)
                .await()
            Log.d("ExpenseRepository", "Gasto ${expense.id} guardado en Firestore")
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error al guardar en Firestore", e)
        }
    }

    /**
     * Sube todos los gastos locales a Firestore (se llama una vez al hacer login con Google).
     */
    suspend fun syncExpensesToCloud() {
        val email = getUserEmail()
        if (email.isBlank() || email == "invitado@local.com") return
        try {
            val localExpenses = allExpenses.first()
            localExpenses.forEach { saveExpenseToCloud(it) }
            Log.d("ExpenseRepository", "Sincronización completada: ${localExpenses.size} gastos subidos")
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error en syncExpensesToCloud", e)
            throw e
        }
    }

    suspend fun insert(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun update(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteAll() {
        expenseDao.deleteAllExpenses()
    }

    suspend fun processNotification(
        title: String,
        body: String,
        appName: String,
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val originalText = if (title.isNotEmpty()) "[$title] $body" else body
        
        // Check if we are running in local mode
        val mode = getIntelligenceMode()
        if (mode == "local") {
            try {
                val localResult = localLlmManager.extractExpense(title, body)
                if (localResult.isExpense) {
                    val adjustedAmount = adjustAmountForInstallments(originalText, localResult.amount)
                    val completedExpense = Expense(
                        amount = adjustedAmount,
                        merchant = localResult.merchant,
                        category = localResult.category,
                        currency = localResult.currency,
                        originalText = originalText,
                        originalApp = appName,
                        timestamp = timestamp,
                        isPending = false,
                        parseError = if (localResult.engineUsed.contains("Error")) localResult.rawResponse else null,
                        engineUsed = localResult.engineUsed
                    )
                    expenseDao.insertExpense(completedExpense)
                    // Sincronizar a nube si el usuario tiene sesión Google
                    if (!isGuestMode()) saveExpenseToCloud(completedExpense)
                    return@withContext true
                } else {
                    Log.d("ExpenseRepository", "Local notification ignored (not an expense): $originalText")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Error in local parser", e)
                val errorExpense = Expense(
                    amount = 0.0,
                    merchant = "Error Motor Local",
                    category = "Otros",
                    currency = "USD",
                    originalText = originalText,
                    originalApp = appName,
                    timestamp = timestamp,
                    isPending = false,
                    parseError = e.localizedMessage ?: "Error desconocido en el motor local",
                    engineUsed = "Error Local"
                )
                expenseDao.insertExpense(errorExpense)
                return@withContext false
            }
        }

        // 1. Create a pending expense entry for Cloud mode
        val pendingExpense = Expense(
            amount = 0.0,
            merchant = "Procesando...",
            category = "Otros",
            currency = "USD",
            originalText = originalText,
            originalApp = appName,
            timestamp = timestamp,
            isPending = true,
            engineUsed = "Pendiente"
        )
        val id = expenseDao.insertExpense(pendingExpense)

        // 2. Query Gemini API (Cloud mode)
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val errorExpense = pendingExpense.copy(
                id = id,
                merchant = "Error de Configuración",
                isPending = false,
                parseError = "Configura tu GEMINI_API_KEY en el panel de Secrets de AI Studio",
                engineUsed = "Gemini Nube"
            )
            expenseDao.updateExpense(errorExpense)
            return@withContext false
        }

        val prompt = """
            Extract transaction details from this mobile notification. If it is NOT an expense (such as a code, OTP, general alert, deposit, or advertising), set 'isExpense' to false.
            
            Notification text:
            $originalText
        """.trimIndent()

        val systemInstruction = """
            You are a precise payment notification extractor. Analyze mobile notification texts (often in Spanish or English) from banks, credit cards, digital wallets, or payment apps. Identify if they describe an actual purchase, expense, or payment. Extract the amount, the merchant/vendor name, the category of spending, the 3-letter currency code, and set isExpense appropriately.
        """.trimIndent()

        val schema = Schema(
            type = "OBJECT",
            description = "Expense details extracted from notification text",
            properties = mapOf(
                "amount" to Schema(
                    type = "NUMBER",
                    description = "The amount of the transaction. Number only (e.g. 15.5 or 12000.0). Return 0.0 if not found."
                ),
                "merchant" to Schema(
                    type = "STRING",
                    description = "The vendor/merchant name (e.g. Starbucks, Uber, Netflix, Walmart). Default to 'Desconocido' if not found."
                ),
                "category" to Schema(
                    type = "STRING",
                    description = "The category. Must be exactly one of: Food, Transport, Shopping, Entertainment, Utilities, Services, Others."
                ),
                "currency" to Schema(
                    type = "STRING",
                    description = "The 3-letter currency code (e.g. USD, COP, MXN, EUR). Guess based on context or currency symbol. Default to USD."
                ),
                "isExpense" to Schema(
                    type = "BOOLEAN",
                    description = "Set to true only if this notification is an expense, payment, purchase, or debit. Set to false if it is a deposit, income, OTP/code, advertisement, or unrelated notification."
                )
            ),
            required = listOf("amount", "merchant", "category", "currency", "isExpense")
        )

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.1f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (responseText != null) {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(ExtractedExpense::class.java)
                val extracted = adapter.fromJson(responseText)
                
                if (extracted != null) {
                    if (extracted.isExpense) {
                        val adjustedAmount = adjustAmountForInstallments(originalText, extracted.amount)
                        val updatedExpense = pendingExpense.copy(
                            id = id,
                            amount = adjustedAmount,
                            merchant = extracted.merchant,
                            category = extracted.category,
                            currency = extracted.currency,
                            isPending = false,
                            parseError = null,
                            engineUsed = "Gemini Nube"
                        )
                        expenseDao.updateExpense(updatedExpense)
                        return@withContext true
                    } else {
                        // Not an expense - delete it so it's not shown in logs/list
                        val toDelete = pendingExpense.copy(id = id)
                        expenseDao.deleteExpense(toDelete)
                        Log.d("ExpenseRepository", "Deleted pending notification: not an expense in Cloud mode")
                        return@withContext false
                    }
                } else {
                    val updatedExpense = pendingExpense.copy(
                        id = id,
                        merchant = "Error de Procesamiento",
                        isPending = false,
                        parseError = "Error al parsear el JSON de respuesta del LLM",
                        engineUsed = "Gemini Nube"
                    )
                    expenseDao.updateExpense(updatedExpense)
                    return@withContext false
                }
            } else {
                val updatedExpense = pendingExpense.copy(
                    id = id,
                    merchant = "Error de Respuesta",
                    isPending = false,
                    parseError = "La respuesta del LLM estaba vacía",
                    engineUsed = "Gemini Nube"
                )
                expenseDao.updateExpense(updatedExpense)
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error calling Gemini", e)
            val updatedExpense = pendingExpense.copy(
                id = id,
                merchant = "Error de Red/API",
                isPending = false,
                parseError = e.localizedMessage ?: "Error desconocido en la llamada a Gemini",
                engineUsed = "Gemini Nube"
            )
            expenseDao.updateExpense(updatedExpense)
            return@withContext false
        }
    }

    private fun adjustAmountForInstallments(text: String, parsedAmount: Double): Double {
        val regex = Regex("(?i)(\\d+)\\s*(?:cuotas|meses|mensualidades|pagos)(?:\\s+sin\\s+inter(?:é|e)s)?\\s*(?:de|por)\\s*(?:USD|COP|MXN|CLP|PEN|ARS|UYU|[\\$€])?[ \\t]*([0-9.,]+)")
        val match = regex.find(text)
        if (match != null) {
            val numCuotasStr = match.groupValues[1]
            val valorCuotaStr = match.groupValues[2]
            val numCuotas = numCuotasStr.toIntOrNull() ?: 1
            var clean = valorCuotaStr.replace("[^0-9.,]".toRegex(), "").trim()
            if (clean.isNotEmpty()) {
                var amountVal = 0.0
                try {
                    if (clean.contains(",") && clean.contains(".")) {
                        if (clean.indexOf(",") < clean.indexOf(".")) {
                            clean = clean.replace(",", "")
                        } else {
                            clean = clean.replace(".", "").replace(",", ".")
                        }
                    } else if (clean.contains(",")) {
                        val lastComma = clean.lastIndexOf(",")
                        val charsAfter = clean.length - 1 - lastComma
                        clean = if (charsAfter == 2) {
                            clean.replace(".", "").replace(",", ".")
                        } else {
                            clean.replace(",", "")
                        }
                    }
                    amountVal = clean.toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    // Ignore
                }
                
                if (numCuotas > 1 && amountVal > 0.0) {
                    val mode = getInstallmentMode()
                    val result = if (mode == "total") {
                        numCuotas * amountVal
                    } else {
                        amountVal
                    }
                    Log.d("ExpenseRepository", "Standardized installment ($mode): $numCuotas cuotas de $amountVal = $result")
                    return result
                }
            }
        }
        return parsedAmount
    }
}
