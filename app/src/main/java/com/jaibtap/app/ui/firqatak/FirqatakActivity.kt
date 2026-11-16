package com.jaibtap.app.ui.firqatak

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.ui.nfc.FirqatakNfcActivity
import java.text.DecimalFormat

class FirqatakActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var btnBack: ImageView
    private lateinit var tvCode: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvContributionsCount: TextView
    private lateinit var btnCollectNfc: Button
    private lateinit var layoutParticipants: LinearLayout

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firqatak)

        dataManager = DataManager.get()
        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvCode = findViewById(R.id.tvCode)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvContributionsCount = findViewById(R.id.tvContributionsCount)
        btnCollectNfc = findViewById(R.id.btnCollectNfc)
        layoutParticipants = findViewById(R.id.layoutParticipants)
    }

    private fun loadData() {
        val user = dataManager.currentUser
        val code = user?.firqatakCode ?: "FRQ100"
        tvCode.text = code

        val contributions = dataManager.getFirqatakContributions(code)
        val total = contributions.sumOf { it.amount }
        tvTotalAmount.text = "${currencyFormat.format(total)} ريال"
        tvContributionsCount.text = contributions.size.toString()

        layoutParticipants.removeAllViews()

        if (contributions.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "لا توجد مساهمات حتى الآن في هذه الفرقة.\nاضغط على 'بدء جمع المساهمات' للبدء بقراءة الميداليات."
                setTextColor(getColor(R.color.text_secondary))
                textSize = 13f
                setPadding(10, 20, 10, 20)
            }
            layoutParticipants.addView(emptyTv)
            return
        }

        contributions.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = getDrawable(R.drawable.bg_card_dark)
                setPadding(24, 20, 24, 20)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 8)
                layoutParams = params
            }

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvName = TextView(this).apply {
                text = item.participantName
                setTextColor(getColor(R.color.white))
                textSize = 15f
            }

            val tvWallet = TextView(this).apply {
                text = item.participantWallet
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
            }
            col.addView(tvName)
            col.addView(tvWallet)

            val tvAmount = TextView(this).apply {
                text = "+ ${currencyFormat.format(item.amount)} ريال"
                setTextColor(getColor(R.color.success_green))
                textSize = 15f
            }

            card.addView(col)
            card.addView(tvAmount)
            layoutParticipants.addView(card)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnCollectNfc.setOnClickListener {
            startActivity(Intent(this, FirqatakNfcActivity::class.java))
        }
    }
}
