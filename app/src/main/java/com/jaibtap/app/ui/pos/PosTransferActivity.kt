package com.jaibtap.app.ui.pos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.ui.result.ResultActivity
import java.text.DecimalFormat

class PosTransferActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var btnBack: ImageView
    private lateinit var tvAvailableBalance: TextView
    private lateinit var etPosNumber: EditText
    private lateinit var btnQuickFillMerchant: Button
    private lateinit var tvMerchantName: TextView
    private lateinit var etPosAmount: EditText
    private lateinit var etPosNotes: EditText
    private lateinit var btnConfirmPosPayment: Button

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        dataManager = DataManager.get()
        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvAvailableBalance = findViewById(R.id.tvAvailableBalance)
        etPosNumber = findViewById(R.id.etPosNumber)
        btnQuickFillMerchant = findViewById(R.id.btnQuickFillMerchant)
        tvMerchantName = findViewById(R.id.tvMerchantName)
        etPosAmount = findViewById(R.id.etPosAmount)
        etPosNotes = findViewById(R.id.etPosNotes)
        btnConfirmPosPayment = findViewById(R.id.btnConfirmPosPayment)

        val user = dataManager.currentUser
        if (user != null) {
            tvAvailableBalance.text = "ريال يمني | ${currencyFormat.format(user.balance)}"
        }

        // تعبئة افتراضية لمتجر التجربة لتسهيل العرض
        etPosNumber.setText("771000003")
        tvMerchantName.text = "المتجر: متجر التجربة"
        tvMerchantName.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnQuickFillMerchant.setOnClickListener {
            etPosNumber.setText("771000003")
            tvMerchantName.text = "المتجر: متجر التجربة"
            tvMerchantName.visibility = View.VISIBLE
        }

        etPosNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString()?.trim() ?: ""
                val merchant = dataManager.getMerchant(input)
                if (merchant != null) {
                    tvMerchantName.text = "المتجر: ${merchant.name}"
                    tvMerchantName.visibility = View.VISIBLE
                } else {
                    tvMerchantName.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirmPosPayment.setOnClickListener {
            val user = dataManager.currentUser
            if (user == null) {
                Toast.makeText(this, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val posNum = etPosNumber.text.toString().trim()
            val amountStr = etPosAmount.text.toString().trim()
            val notes = etPosNotes.text.toString().trim()

            if (posNum.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "يرجى تعبئة رقم نقطة البيع والمبلغ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = dataManager.simulatePosPayment(
                senderWallet = user.walletNumber,
                posNumber = posNum,
                amount = amount,
                description = notes.ifEmpty { null }
            )

            if (result.isSuccess) {
                val tx = result.getOrThrow()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", true)
                    putExtra("TITLE", "تم دفع المشتريات بنجاح")
                    putExtra("AMOUNT", tx.amount)
                    putExtra("RECIPIENT", tx.receiverName)
                    putExtra("TX_ID", tx.id)
                    putExtra("DETAIL", "تم دفع ${currencyFormat.format(tx.amount)} ريال لـ ${tx.receiverName} (نقطة: ${tx.receiverWallet}).")
                }
                startActivity(intent)
                finish()
            } else {
                val err = result.exceptionOrNull()?.message ?: "فشلت عملية الدفع"
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", false)
                    putExtra("TITLE", "فشلت عملية الدفع")
                    putExtra("AMOUNT", amount)
                    putExtra("DETAIL", err)
                }
                startActivity(intent)
            }
        }
    }
}
