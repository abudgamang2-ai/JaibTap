package com.jaibtap.app.data

import com.jaibtap.app.data.model.Merchant
import com.jaibtap.app.data.model.User

/**
 * المستودع المركزي للبيانات التجريبية
 * Central Mock Database - قابل للتعديل بسهولة من مكان واحد
 */
object MockCentralDatabase {

    // المستخدم الأول: أحمد
    val INITIAL_USER_AHMED = User(
        id = "user_1",
        name = "أحمد",
        phone = "771000001",
        walletNumber = "771000001",
        balance = 10000.0,
        firqatakCode = "FRQ100",
        pin = "1234",
        password = "password123",
        withdrawalLimit = 5000.0,
        authCode = "9988",
        nfcTagUid = "04:5A:21:B3:9C:60:80" // المعرف الافتراضي لميدالية أحمد
    )

    // المستخدم الثاني: محمد
    val INITIAL_USER_MOHAMMED = User(
        id = "user_2",
        name = "محمد",
        phone = "771000002",
        walletNumber = "771000002",
        balance = 8000.0,
        firqatakCode = null,
        pin = "5678",
        password = "password123",
        withdrawalLimit = 4000.0,
        authCode = "1122",
        nfcTagUid = "04:88:C2:5F:1A:70:81" // المعرف الافتراضي لميدالية محمد
    )

    // التاجر: متجر التجربة
    val INITIAL_MERCHANT = Merchant(
        id = "merchant_1",
        name = "متجر التجربة",
        posNumber = "771000003",
        balance = 25000.0
    )

    // قائمة المستخدمين الأولية
    fun getInitialUsers(): MutableList<User> = mutableListOf(
        INITIAL_USER_AHMED.copy(),
        INITIAL_USER_MOHAMMED.copy()
    )

    // قائمة التجار الأولية
    fun getInitialMerchants(): MutableList<Merchant> = mutableListOf(
        INITIAL_MERCHANT.copy()
    )
}
