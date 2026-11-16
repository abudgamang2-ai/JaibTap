package com.jaibtap.app.ui.nfc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.data.model.User
import com.jaibtap.app.ui.result.ResultActivity
import java.text.DecimalFormat

class WithdrawalActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private var tagOwner: User? = null

    private lateinit var btnBack: ImageView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvOwnerWallet: TextView
    private lateinit var tvAuthLimit: TextView
    private lateinit var btnEditLimit: TextView
    private lateinit var etAmount: EditText
    private lateinit var etAuthCode: EditText
    private lateinit var btnExecuteWithdrawal: Button

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_withdrawal)

        dataManager = DataManager.get()

        val wallet = intent.getStringExtra("TAG_OWNER_WALLET")
        if (wallet != null) {
            tagOwner = dataManager.getUser(wallet)
        }

        if (tagOwner == null) {
            Toast.makeText(this, "تعذر العثور على بيانات صاحب الميدالية", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvOwnerWallet = findViewById(R.id.tvOwnerWallet)
        tvAuthLimit = findViewById(R.id.tvAuthLimit)
        btnEditLimit = findViewById(R.id.btnEditLimit)
        etAmount = findViewById(R.id.etAmount)
        etAuthCode = findViewById(R.id.etAuthCode)
        btnExecuteWithdrawal = findViewById(R.id.btnExecuteWithdrawal)

        val owner = tagOwner ?: return
        tvOwnerName.text = owner.name
        tvOwnerWallet.text = "محفظة: ${owner.walletNumber}"
        updateLimitText()

        // تعبئة افتراضية لرمز التفويض لتسهيل العرض
        etAuthCode.setText(owner.authCode)
    }

    private fun updateLimitText() {
        val owner = tagOwner ?: return
        tvAuthLimit.text = "حد التفويض: ${currencyFormat.format(owner.withdrawalLimit)} ريال"
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnEditLimit.setOnClickListener {
            val owner = tagOwner ?: return@setOnClickListener
            val input = EditText(this).apply {
                setText(owner.withdrawalLimit.toInt().toString())
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            AlertDialog.Builder(this)
                .setTitle("تعديل حد التفويض التجريبي")
                .setMessage("أدخل الحد الأقصى للسحب من هذه الميدالية بدون الرجوع للخادم:")
                .setView(input)
                .setPositiveButton("حفظ") { _, _ ->
                    val newLimit = input.text.toString().toDoubleOrNull()
                    if (newLimit != null && newLimit > 0) {
                        owner.withdrawalLimit = newLimit
                        updateLimitText()
                        Toast.makeText(this, "تم تحديث حد التفويض", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        btnExecuteWithdrawal.setOnClickListener {
            val owner = tagOwner ?: return@setOnClickListener
            val amountStr = etAmount.text.toString().trim()
            val authCodeInput = etAuthCode.text.toString().trim()

            if (amountStr.isEmpty() || authCodeInput.isEmpty()) {
                Toast.makeText(this, "يرجى تعبئة المبلغ ورمز التفويض", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // تنفيذ محاكاة السحب
            val result = dataManager.simulateNfcWithdrawal(
                tagOwnerWallet = owner.walletNumber,
                amount = amount,
                authCodeInput = authCodeInput
            )

            if (result.isSuccess) {
                val tx = result.getOrThrow()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", true)
                    putExtra("TITLE", "تمت عملية السحب بنجاح")
                    putExtra("AMOUNT", tx.amount)
                    putExtra("RECIPIENT", owner.name)
                    putExtra("TX_ID", tx.id)
                    putExtra("DETAIL", "تم خصم ${currencyFormat.format(tx.amount)} ريال من محفظة ${owner.name} بموجب تفويض الميدالية.")
                }
                startActivity(intent)
                finish()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "فشلت عملية السحب"
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", false)
                    putExtra("TITLE", "فشلت عملية السحب")
                    putExtra("AMOUNT", amount)
                    putExtra("DETAIL", errorMsg)
                }
                startActivity(intent)
            }
        }
    }
}
