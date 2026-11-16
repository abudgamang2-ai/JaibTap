package com.jaibtap.app.ui.nfc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.data.model.User
import com.jaibtap.app.ui.result.ResultActivity
import java.text.DecimalFormat

class DepositActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private var beneficiaryUser: User? = null

    private lateinit var btnBack: ImageView
    private lateinit var tvBeneficiaryName: TextView
    private lateinit var tvBeneficiaryWallet: TextView
    private lateinit var etDepositAmount: EditText
    private lateinit var btnConfirmDeposit: Button

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deposit)

        dataManager = DataManager.get()

        val wallet = intent.getStringExtra("TAG_OWNER_WALLET")
        if (wallet != null) {
            beneficiaryUser = dataManager.getUser(wallet)
        }

        if (beneficiaryUser == null) {
            Toast.makeText(this, "تعذر العثور على بيانات المستفيد", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvBeneficiaryName = findViewById(R.id.tvBeneficiaryName)
        tvBeneficiaryWallet = findViewById(R.id.tvBeneficiaryWallet)
        etDepositAmount = findViewById(R.id.etDepositAmount)
        btnConfirmDeposit = findViewById(R.id.btnConfirmDeposit)

        val beneficiary = beneficiaryUser ?: return
        tvBeneficiaryName.text = beneficiary.name
        tvBeneficiaryWallet.text = "رقم المحفظة: ${beneficiary.walletNumber}"
        etDepositAmount.setText("500")
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnConfirmDeposit.setOnClickListener {
            val sender = dataManager.currentUser
            val beneficiary = beneficiaryUser

            if (sender == null || beneficiary == null) {
                Toast.makeText(this, "بيانات الحساب غير متوفرة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountStr = etDepositAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // محاكاة الإيداع
            val result = dataManager.simulateNfcDeposit(
                senderWallet = sender.walletNumber,
                tagOwnerWallet = beneficiary.walletNumber,
                amount = amount
            )

            if (result.isSuccess) {
                val tx = result.getOrThrow()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", true)
                    putExtra("TITLE", "تم الإيداع بنجاح")
                    putExtra("AMOUNT", tx.amount)
                    putExtra("RECIPIENT", beneficiary.name)
                    putExtra("TX_ID", tx.id)
                    putExtra("DETAIL", "تم إيداع ${currencyFormat.format(tx.amount)} ريال في محفظة ${beneficiary.name} عبر NFC.")
                }
                startActivity(intent)
                finish()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "فشلت عملية الإيداع"
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", false)
                    putExtra("TITLE", "فشلت عملية الإيداع")
                    putExtra("AMOUNT", amount)
                    putExtra("DETAIL", errorMsg)
                }
                startActivity(intent)
            }
        }
    }
}
