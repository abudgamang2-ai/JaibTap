package com.jaibtap.app.ui.result

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaibtap.app.R
import com.jaibtap.app.ui.home.HomeActivity
import java.text.DecimalFormat

class ResultActivity : AppCompatActivity() {

    private lateinit var ivResultStatusIcon: ImageView
    private lateinit var tvResultTitle: TextView
    private lateinit var tvResultAmount: TextView
    private lateinit var tvResultDetails: TextView
    private lateinit var tvResultTxId: TextView
    private lateinit var btnBackToHome: Button

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        initViews()
        setupData()
    }

    private fun initViews() {
        ivResultStatusIcon = findViewById(R.id.ivResultStatusIcon)
        tvResultTitle = findViewById(R.id.tvResultTitle)
        tvResultAmount = findViewById(R.id.tvResultAmount)
        tvResultDetails = findViewById(R.id.tvResultDetails)
        tvResultTxId = findViewById(R.id.tvResultTxId)
        btnBackToHome = findViewById(R.id.btnBackToHome)

        btnBackToHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupData() {
        val isSuccess = intent.getBooleanExtra("SUCCESS", true)
        val title = intent.getStringExtra("TITLE") ?: if (isSuccess) "تمت العملية بنجاح ✓" else "فشلت العملية"
        val amount = intent.getDoubleExtra("AMOUNT", 0.0)
        val detail = intent.getStringExtra("DETAIL") ?: ""
        val txId = intent.getStringExtra("TX_ID")

        tvResultTitle.text = title
        tvResultDetails.text = detail

        if (amount > 0) {
            tvResultAmount.visibility = View.VISIBLE
            tvResultAmount.text = "${currencyFormat.format(amount)} ريال"
        } else {
            tvResultAmount.visibility = View.GONE
        }

        if (txId != null) {
            tvResultTxId.visibility = View.VISIBLE
            tvResultTxId.text = "الرقم المرجعي: $txId"
        } else {
            tvResultTxId.visibility = View.GONE
        }

        if (isSuccess) {
            ivResultStatusIcon.setImageResource(R.drawable.ic_check_circle)
            ivResultStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_green))
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnBackToHome.text = getString(R.string.back_to_home)
        } else {
            ivResultStatusIcon.setImageResource(R.drawable.ic_error_circle)
            ivResultStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red))
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            btnBackToHome.text = getString(R.string.try_again)
        }
    }
}
