package com.jaibtap.app.data

import android.content.Context
import android.content.SharedPreferences
import com.jaibtap.app.data.model.FirqatakContribution
import com.jaibtap.app.data.model.Merchant
import com.jaibtap.app.data.model.NfcTagInfo
import com.jaibtap.app.data.model.Transaction
import com.jaibtap.app.data.model.TransactionStatus
import com.jaibtap.app.data.model.TransactionType
import com.jaibtap.app.data.model.User
import org.json.JSONArray
import org.json.JSONObject

/**
 * مدير البيانات والمحاكاة المالية مع الحفظ المحلي (Persistence)
 */
class DataManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("jaib_tap_prefs", Context.MODE_PRIVATE)

    private val users = mutableListOf<User>()
    private val merchants = mutableListOf<Merchant>()
    private val transactions = mutableListOf<Transaction>()
    private val firqatakContributions = mutableListOf<FirqatakContribution>()
    private val tagMapping = mutableMapOf<String, String>() // TagUid -> WalletNumber

    var currentUser: User? = null
        private set

    init {
        loadData()
    }

    companion object {
        @Volatile
        private var INSTANCE: DataManager? = null

        fun init(context: Context): DataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataManager(context).also { INSTANCE = it }
            }
        }

        fun get(): DataManager {
            return INSTANCE ?: throw IllegalStateException("DataManager is not initialized!")
        }
    }

    // ================== الحفظ والاسترجاع المحلي ==================

    @Synchronized
    private fun loadData() {
        val usersJson = prefs.getString("users_data", null)
        if (usersJson != null) {
            parseUsers(usersJson)
        } else {
            users.clear()
            users.addAll(MockCentralDatabase.getInitialUsers())
            saveUsers()
        }

        val merchantsJson = prefs.getString("merchants_data", null)
        if (merchantsJson != null) {
            parseMerchants(merchantsJson)
        } else {
            merchants.clear()
            merchants.addAll(MockCentralDatabase.getInitialMerchants())
            saveMerchants()
        }

        val transJson = prefs.getString("transactions_data", null)
        if (transJson != null) {
            parseTransactions(transJson)
        } else {
            // بيانات أولية لسجل العمليات
            transactions.clear()
            transactions.add(
                Transaction(
                    id = "TX-INIT-01",
                    type = TransactionType.TRANSFER_SUBSCRIBER,
                    amount = 250.0,
                    senderWallet = "771000001",
                    receiverWallet = "771000002",
                    receiverName = "محمد",
                    timestamp = System.currentTimeMillis() - 86400000,
                    status = TransactionStatus.SUCCESS,
                    notes = "تحويل تجريبي سابق",
                    nfcUsed = false
                )
            )
            saveTransactions()
        }

        val firqatakJson = prefs.getString("firqatak_data", null)
        if (firqatakJson != null) {
            parseFirqatak(firqatakJson)
        }

        // استرجاع خريطة الميداليات
        val tagsJson = prefs.getString("tags_mapping", null)
        if (tagsJson != null) {
            val json = JSONObject(tagsJson)
            json.keys().forEach { tag ->
                tagMapping[tag] = json.getString(tag)
            }
        } else {
            // ربط مبدئي للميداليات الافتراضية
            users.forEach { u ->
                u.nfcTagUid?.let { tagMapping[it] = u.walletNumber }
            }
            saveTagsMapping()
        }

        // استرجاع الجلسة
        val lastLoggedInWallet = prefs.getString("logged_in_wallet", null)
        if (lastLoggedInWallet != null) {
            currentUser = users.find { it.walletNumber == lastLoggedInWallet }
        }
        if (currentUser == null && users.isNotEmpty()) {
            currentUser = users[0] // الافتراضي أحمد
        }
    }

    private fun saveUsers() {
        val array = JSONArray()
        users.forEach { u ->
            val obj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("phone", u.phone)
                put("walletNumber", u.walletNumber)
                put("balance", u.balance)
                put("firqatakCode", u.firqatakCode ?: "")
                put("pin", u.pin)
                put("password", u.password)
                put("withdrawalLimit", u.withdrawalLimit)
                put("authCode", u.authCode)
                put("nfcTagUid", u.nfcTagUid ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString("users_data", array.toString()).apply()
    }

    private fun saveMerchants() {
        val array = JSONArray()
        merchants.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("posNumber", m.posNumber)
                put("balance", m.balance)
            }
            array.put(obj)
        }
        prefs.edit().putString("merchants_data", array.toString()).apply()
    }

    private fun saveTransactions() {
        val array = JSONArray()
        transactions.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("type", t.type.name)
                put("amount", t.amount)
                put("senderWallet", t.senderWallet)
                put("receiverWallet", t.receiverWallet)
                put("receiverName", t.receiverName)
                put("timestamp", t.timestamp)
                put("status", t.status.name)
                put("notes", t.notes ?: "")
                put("failureReason", t.failureReason ?: "")
                put("nfcUsed", t.nfcUsed)
            }
            array.put(obj)
        }
        prefs.edit().putString("transactions_data", array.toString()).apply()
    }

    private fun saveFirqatak() {
        val array = JSONArray()
        firqatakContributions.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("firqatakCode", c.firqatakCode)
                put("participantName", c.participantName)
                put("participantWallet", c.participantWallet)
                put("amount", c.amount)
                put("timestamp", c.timestamp)
                put("status", c.status)
                put("nfcTagUid", c.nfcTagUid ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString("firqatak_data", array.toString()).apply()
    }

    private fun saveTagsMapping() {
        val obj = JSONObject()
        tagMapping.forEach { (tag, wallet) ->
            obj.put(tag, wallet)
        }
        prefs.edit().putString("tags_mapping", obj.toString()).apply()
    }

    private fun parseUsers(jsonStr: String) {
        users.clear()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            users.add(
                User(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    phone = obj.getString("phone"),
                    walletNumber = obj.getString("walletNumber"),
                    balance = obj.getDouble("balance"),
                    firqatakCode = obj.optString("firqatakCode").ifEmpty { null },
                    pin = obj.getString("pin"),
                    password = obj.getString("password"),
                    withdrawalLimit = obj.optDouble("withdrawalLimit", 5000.0),
                    authCode = obj.optString("authCode", "9988"),
                    nfcTagUid = obj.optString("nfcTagUid").ifEmpty { null }
                )
            )
        }
    }

    private fun parseMerchants(jsonStr: String) {
        merchants.clear()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            merchants.add(
                Merchant(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    posNumber = obj.getString("posNumber"),
                    balance = obj.getDouble("balance")
                )
            )
        }
    }

    private fun parseTransactions(jsonStr: String) {
        transactions.clear()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            transactions.add(
                Transaction(
                    id = obj.getString("id"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    amount = obj.getDouble("amount"),
                    senderWallet = obj.getString("senderWallet"),
                    receiverWallet = obj.getString("receiverWallet"),
                    receiverName = obj.getString("receiverName"),
                    timestamp = obj.getLong("timestamp"),
                    status = TransactionStatus.valueOf(obj.getString("status")),
                    notes = obj.optString("notes").ifEmpty { null },
                    failureReason = obj.optString("failureReason").ifEmpty { null },
                    nfcUsed = obj.optBoolean("nfcUsed", false)
                )
            )
        }
    }

    private fun parseFirqatak(jsonStr: String) {
        firqatakContributions.clear()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            firqatakContributions.add(
                FirqatakContribution(
                    id = obj.getString("id"),
                    firqatakCode = obj.getString("firqatakCode"),
                    participantName = obj.getString("participantName"),
                    participantWallet = obj.getString("participantWallet"),
                    amount = obj.getDouble("amount"),
                    timestamp = obj.getLong("timestamp"),
                    status = obj.getString("status"),
                    nfcTagUid = obj.optString("nfcTagUid").ifEmpty { null }
                )
            )
        }
    }

    // ================== إدارة الحسابات والجلسة ==================

    fun authenticate(phoneOrWallet: String, secret: String, isPin: Boolean): User? {
        val user = users.find { it.phone == phoneOrWallet || it.walletNumber == phoneOrWallet }
        if (user != null) {
            val match = if (isPin) user.pin == secret else user.password == secret
            if (match) {
                currentUser = user
                prefs.edit().putString("logged_in_wallet", user.walletNumber).apply()
                return user
            }
        }
        return null
    }

    fun logout() {
        currentUser = null
        prefs.edit().remove("logged_in_wallet").apply()
    }

    fun getUser(walletNumber: String): User? = users.find { it.walletNumber == walletNumber }

    fun getAllUsers(): List<User> = users.toList()

    fun getMerchant(posNumber: String): Merchant? = merchants.find { it.posNumber == posNumber }

    fun getAllMerchants(): List<Merchant> = merchants.toList()

    fun getTransactions(): List<Transaction> = transactions.sortedByDescending { it.timestamp }

    fun getFirqatakContributions(firqatakCode: String): List<FirqatakContribution> =
        firqatakContributions.filter { it.firqatakCode == firqatakCode }.sortedByDescending { it.timestamp }

    // ================== خدمات NFC والتعرف على الميداليات ==================

    fun resolveNfcTag(uid: String): NfcTagInfo {
        val cleanUid = uid.trim().uppercase()
        val wallet = tagMapping[cleanUid]
        val user = if (wallet != null) getUser(wallet) else null

        // نوع الشريحة المكتشفة افتراضياً NTAG213 مع مرونة البرمجة
        val tagType = if (cleanUid.startsWith("04:DNA") || cleanUid.contains("DNA")) "NTAG 424 DNA" else "NTAG213"

        return NfcTagInfo(
            tagUid = cleanUid,
            tagType = tagType,
            isKnown = user != null,
            ownerUser = user
        )
    }

    fun linkTagToUser(tagUid: String, walletNumber: String) {
        val cleanUid = tagUid.trim().uppercase()
        tagMapping[cleanUid] = walletNumber
        saveTagsMapping()
    }

    // ================== محاكاة العمليات المالية ==================

    /**
     * محاكاة: تحويل لمشترك
     */
    @Synchronized
    fun simulateTransferToSubscriber(
        senderWallet: String,
        receiverWallet: String,
        amount: Double,
        notes: String?
    ): Result<Transaction> {
        val sender = getUser(senderWallet)
            ?: return Result.failure(Exception("حساب المرسل غير موجود"))
        val receiver = getUser(receiverWallet)
            ?: return Result.failure(Exception("رقم محفظة المستفيد غير موجود في قاعدة البيانات التجريبية"))

        if (senderWallet == receiverWallet) {
            return Result.failure(Exception("لا يمكن التحويل لنفس المحفظة"))
        }

        if (amount <= 0) {
            return Result.failure(Exception("يرجى إدخال مبلغ صحيح"))
        }

        if (sender.balance < amount) {
            val failedTx = Transaction(
                id = "TX-${System.currentTimeMillis()}",
                type = TransactionType.TRANSFER_SUBSCRIBER,
                amount = amount,
                senderWallet = senderWallet,
                receiverWallet = receiverWallet,
                receiverName = receiver.name,
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.FAILED,
                notes = notes,
                failureReason = "رصيد غير كافٍ"
            )
            transactions.add(0, failedTx)
            saveTransactions()
            return Result.failure(Exception("الرصيد غير كافٍ لإتمام العملية (الرصيد الحالي: ${sender.balance} ريال)"))
        }

        // خصم وإيداع وهمي
        sender.balance -= amount
        receiver.balance += amount
        saveUsers()

        val tx = Transaction(
            id = "TX-${System.currentTimeMillis()}",
            type = TransactionType.TRANSFER_SUBSCRIBER,
            amount = amount,
            senderWallet = senderWallet,
            receiverWallet = receiverWallet,
            receiverName = receiver.name,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            notes = notes,
            nfcUsed = false
        )
        transactions.add(0, tx)
        saveTransactions()

        return Result.success(tx)
    }

    /**
     * محاكاة: دفع المشتريات لنقطة بيع
     */
    @Synchronized
    fun simulatePosPayment(
        senderWallet: String,
        posNumber: String,
        amount: Double,
        description: String?
    ): Result<Transaction> {
        val sender = getUser(senderWallet)
            ?: return Result.failure(Exception("حساب المرسل غير موجود"))
        val merchant = getMerchant(posNumber)
            ?: return Result.failure(Exception("نقطة البيع غير موجودة أو غير معتمدة"))

        if (amount <= 0) {
            return Result.failure(Exception("يرجى إدخال مبلغ صحيح"))
        }

        if (sender.balance < amount) {
            val failedTx = Transaction(
                id = "TX-${System.currentTimeMillis()}",
                type = TransactionType.POS_PAYMENT,
                amount = amount,
                senderWallet = senderWallet,
                receiverWallet = posNumber,
                receiverName = merchant.name,
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.FAILED,
                notes = description,
                failureReason = "رصيد غير كافٍ"
            )
            transactions.add(0, failedTx)
            saveTransactions()
            return Result.failure(Exception("رصيد غير كافٍ لإتمام الدفع"))
        }

        sender.balance -= amount
        merchant.balance += amount
        saveUsers()
        saveMerchants()

        val tx = Transaction(
            id = "TX-${System.currentTimeMillis()}",
            type = TransactionType.POS_PAYMENT,
            amount = amount,
            senderWallet = senderWallet,
            receiverWallet = posNumber,
            receiverName = merchant.name,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            notes = description,
            nfcUsed = false
        )
        transactions.add(0, tx)
        saveTransactions()

        return Result.success(tx)
    }

    /**
     * محاكاة: السحب النقدي عبر ميدالية NFC
     */
    @Synchronized
    fun simulateNfcWithdrawal(
        tagOwnerWallet: String,
        amount: Double,
        authCodeInput: String
    ): Result<Transaction> {
        val tagOwner = getUser(tagOwnerWallet)
            ?: return Result.failure(Exception("صاحب الميدالية غير مسجل"))

        if (amount <= 0) {
            return Result.failure(Exception("يرجى إدخال مبلغ صحيح"))
        }

        // التحقق من حد التفويض التجريبي
        if (amount > tagOwner.withdrawalLimit) {
            return Result.failure(Exception("المبلغ يتجاوز حد التفويض المسموح (${tagOwner.withdrawalLimit} ريال)"))
        }

        // التحقق من كود التفويض
        if (authCodeInput.trim() != tagOwner.authCode) {
            return Result.failure(Exception("رمز التفويض غير صحيح! يرجى التأكد من الرمز"))
        }

        // التحقق من الرصيد
        if (tagOwner.balance < amount) {
            return Result.failure(Exception("رصيد صاحب الميدالية غير كافٍ لإتمام عملية السحب"))
        }

        // خصم المبلغ من صاحب الميدالية
        tagOwner.balance -= amount
        saveUsers()

        val tx = Transaction(
            id = "TX-WTH-${System.currentTimeMillis()}",
            type = TransactionType.WITHDRAWAL,
            amount = amount,
            senderWallet = tagOwner.walletNumber,
            receiverWallet = "نقداً / سحب عبر NFC",
            receiverName = tagOwner.name,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            notes = "سحب نقدي بميدالية NFC",
            nfcUsed = true
        )
        transactions.add(0, tx)
        saveTransactions()

        return Result.success(tx)
    }

    /**
     * محاكاة: الإيداع لحساب صاحب الميدالية عبر NFC
     */
    @Synchronized
    fun simulateNfcDeposit(
        senderWallet: String,
        tagOwnerWallet: String,
        amount: Double
    ): Result<Transaction> {
        val sender = getUser(senderWallet)
            ?: return Result.failure(Exception("حساب المودع غير موجود"))
        val tagOwner = getUser(tagOwnerWallet)
            ?: return Result.failure(Exception("صاحب الميدالية غير موجود"))

        if (amount <= 0) {
            return Result.failure(Exception("يرجى إدخال مبلغ صحيح"))
        }

        if (sender.balance < amount) {
            return Result.failure(Exception("رصيدك غير كافٍ لإتمام الإيداع"))
        }

        sender.balance -= amount
        tagOwner.balance += amount
        saveUsers()

        val tx = Transaction(
            id = "TX-DEP-${System.currentTimeMillis()}",
            type = TransactionType.DEPOSIT,
            amount = amount,
            senderWallet = sender.walletNumber,
            receiverWallet = tagOwner.walletNumber,
            receiverName = tagOwner.name,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            notes = "إيداع عبر NFC",
            nfcUsed = true
        )
        transactions.add(0, tx)
        saveTransactions()

        return Result.success(tx)
    }

    /**
     * محاكاة: مساهمة فرقتك عبر ميدالية NFC
     */
    @Synchronized
    fun simulateFirqatakContribution(
        hostUserWallet: String,
        participantWallet: String,
        amount: Double,
        tagUid: String?
    ): Result<FirqatakContribution> {
        val hostUser = getUser(hostUserWallet)
            ?: return Result.failure(Exception("المستخدم الحالي غير موجود"))
        val participant = getUser(participantWallet)
            ?: return Result.failure(Exception("صاحب الميدالية غير موجود"))

        val firqatakCode = hostUser.firqatakCode ?: "FRQ-DEMO"

        if (amount <= 0) {
            return Result.failure(Exception("يرجى إدخال مبلغ مساهمة صحيح"))
        }

        if (participant.balance < amount) {
            return Result.failure(Exception("رصيد ${participant.name} غير كافٍ للمساهمة (${participant.balance} ريال)"))
        }

        // خصم المساهمة من المشارك وإضافتها لرصيد جامع الفرقة
        participant.balance -= amount
        hostUser.balance += amount
        saveUsers()

        val contribution = FirqatakContribution(
            id = "FRQ-${System.currentTimeMillis()}",
            firqatakCode = firqatakCode,
            participantName = participant.name,
            participantWallet = participant.walletNumber,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            status = "معتمد",
            nfcTagUid = tagUid
        )
        firqatakContributions.add(0, contribution)
        saveFirqatak()

        val tx = Transaction(
            id = "TX-FRQ-${System.currentTimeMillis()}",
            type = TransactionType.FIRQATAK,
            amount = amount,
            senderWallet = participant.walletNumber,
            receiverWallet = hostUser.walletNumber,
            receiverName = "فرقتك (${firqatakCode})",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            notes = "مساهمة فرقتك عبر NFC من ${participant.name}",
            nfcUsed = true
        )
        transactions.add(0, tx)
        saveTransactions()

        return Result.success(contribution)
    }

    /**
     * إعادة تعيين البيانات إلى الحالة الافتراضية
     */
    @Synchronized
    fun resetDemoData() {
        prefs.edit().clear().apply()
        users.clear()
        users.addAll(MockCentralDatabase.getInitialUsers())
        merchants.clear()
        merchants.addAll(MockCentralDatabase.getInitialMerchants())
        transactions.clear()
        firqatakContributions.clear()
        tagMapping.clear()
        users.forEach { u ->
            u.nfcTagUid?.let { tagMapping[it] = u.walletNumber }
        }
        currentUser = users[0]
        saveUsers()
        saveMerchants()
        saveTagsMapping()
    }
}
