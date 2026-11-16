package com.jaibtap.app.ui.transfer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.ui.result.ResultActivity
import java.text.DecimalFormat

class TransferActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var btnBack: ImageView
    private lateinit var tvAvailableBalance: TextView
    private lateinit var etRecipientWallet: EditText
    private lateinit var btnQuickPick: ImageButton
    private lateinit var tvResolvedName: TextView
    private lateinit var etAmount: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnContinue: Button

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        dataManager = DataManager.get()
        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvAvailableBalance = findViewById(R.id.tvAvailableBalance)
        etRecipientWallet = findViewById(R.id.etRecipientWallet)
        btnQuickPick = findViewById(R.id.btnQuickPick)
        tvResolvedName = findViewById(R.id.tvResolvedName)
        etAmount = findViewById(R.id.etAmount)
        etNotes = findViewById(R.id.etNotes)
        btnContinue = findViewById(R.id.btnContinue)

        val user = dataManager.currentUser
        if (user != null) {
            tvAvailableBalance.text = "ريال يمني | ${currencyFormat.format(user.balance)}"
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // التحقق التلقائي من اسم المستفيد عند كتابة الرقم
        etRecipientWallet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString()?.trim() ?: ""
                val found = dataManager.getUser(input)
                if (found != null && found.walletNumber != dataManager.currentUser?.walletNumber) {
                    tvResolvedName.text = "المستفيد: ${found.name}"
                    tvResolvedName.visibility = View.VISIBLE
                } else {
                    tvResolvedName.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // اختيار مستخدم تجريبي سريعاً
        btnQuickPick.setOnClickListener {
            val users = dataManager.getAllUsers().filter { it.walletNumber != dataManager.currentUser?.walletNumber }
            val options = users.map { "${it.name} (${it.walletNumber})" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("اختر مستفيد تجريبي")
                .setItems(options) { _, which ->
                    val selected = users[which]
                    etRecipientWallet.setText(selected.walletNumber)
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        btnContinue.setOnClickListener {
            val sender = dataManager.currentUser
            if (sender == null) {
                Toast.makeText(this, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val recipientWallet = etRecipientWallet.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (recipientWallet.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال رقم المحفظة والمبلغ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // محاكاة التحويل
            val result = dataManager.simulateTransferToSubscriber(
                senderWallet = sender.walletNumber,
                receiverWallet = recipientWallet,
                amount = amount,
                notes = notes.ifEmpty { null }
            )

            if (result.isSuccess) {
                val tx = result.getOrThrow()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", true)
                    putExtra("TITLE", "تم التحويل بنجاح")
                    putExtra("AMOUNT", tx.amount)
                    putExtra("RECIPIENT", tx.receiverName)
                    putExtra("TX_ID", tx.id)
                    putExtra("DETAIL", "تم تحويل ${currencyFormat.format(tx.amount)} ريال يمني إلى ${tx.receiverName} (${tx.receiverWallet}).")
                }
                startActivity(intent)
                finish()
            } else {
                val err = result.exceptionOrNull()?.message ?: "فشلت عملية التحويل"
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SUCCESS", false)
                    putExtra("TITLE", "فشلت عملية التحويل")
                    putExtra("AMOUNT", amount)
                    putExtra("DETAIL", err)
                }
                startActivity(intent)
            }
        }
    }
}
