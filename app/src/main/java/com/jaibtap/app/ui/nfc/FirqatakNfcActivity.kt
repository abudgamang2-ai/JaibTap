package com.jaibtap.app.ui.nfc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.data.model.User
import com.jaibtap.app.nfc.NfcHelper
import java.text.DecimalFormat

class FirqatakNfcActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var nfcHelper: NfcHelper
    private var currentParticipant: User? = null
    private var currentTagUid: String? = null

    private lateinit var btnBack: ImageView
    private lateinit var tvFirqatakCode: TextView
    private lateinit var tvCollectorName: TextView
    private lateinit var etContributionAmount: EditText
    private lateinit var tvParticipantName: TextView
    private lateinit var tvParticipantWallet: TextView
    private lateinit var btnCollectContribution: Button
    private lateinit var btnTapMohammed: Button
    private lateinit var btnTapAhmed: Button
    private lateinit var layoutContributionsList: LinearLayout

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firqatak_nfc)

        dataManager = DataManager.get()
        nfcHelper = NfcHelper(this)

        initViews()
        setupListeners()

        val participantWallet = intent.getStringExtra("TAG_OWNER_WALLET")
        val tagUid = intent.getStringExtra("TAG_UID")
        if (participantWallet != null) {
            setParticipant(dataManager.getUser(participantWallet), tagUid)
        } else {
            // افتراضياً نضع المستخدم محمد كمشارك جاهز
            setParticipant(dataManager.getUser("771000002"), "04:88:C2:5F:1A:70:81")
        }

        renderContributions()
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uid = nfcHelper.extractTagUid(intent)
        if (uid != null) {
            val info = dataManager.resolveNfcTag(uid)
            if (info.isKnown && info.ownerUser != null) {
                setParticipant(info.ownerUser, uid)
                Toast.makeText(this, "تم التعرف على ميدالية: ${info.ownerUser.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "ميدالية غير مسجلة (UID: $uid)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvFirqatakCode = findViewById(R.id.tvFirqatakCode)
        tvCollectorName = findViewById(R.id.tvCollectorName)
        etContributionAmount = findViewById(R.id.etContributionAmount)
        tvParticipantName = findViewById(R.id.tvParticipantName)
        tvParticipantWallet = findViewById(R.id.tvParticipantWallet)
        btnCollectContribution = findViewById(R.id.btnCollectContribution)
        btnTapMohammed = findViewById(R.id.btnTapMohammed)
        btnTapAhmed = findViewById(R.id.btnTapAhmed)
        layoutContributionsList = findViewById(R.id.layoutContributionsList)

        val hostUser = dataManager.currentUser
        val code = hostUser?.firqatakCode ?: "FRQ100"
        tvFirqatakCode.text = code
        tvCollectorName.text = "المسؤول: ${hostUser?.name} (${hostUser?.walletNumber})"
    }

    private fun setParticipant(user: User?, uid: String?) {
        currentParticipant = user
        currentTagUid = uid
        if (user != null) {
            tvParticipantName.text = user.name
            tvParticipantWallet.text = "محفظة: ${user.walletNumber} (الرصيد: ${currencyFormat.format(user.balance)} ريال)"
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnTapMohammed.setOnClickListener {
            setParticipant(dataManager.getUser("771000002"), "04:88:C2:5F:1A:70:81")
            Toast.makeText(this, "تم تحضير مساهمة محمد", Toast.LENGTH_SHORT).show()
        }

        btnTapAhmed.setOnClickListener {
            setParticipant(dataManager.getUser("771000001"), "04:5A:21:B3:9C:60:80")
            Toast.makeText(this, "تم تحضير مساهمة أحمد", Toast.LENGTH_SHORT).show()
        }

        btnCollectContribution.setOnClickListener {
            val host = dataManager.currentUser
            val participant = currentParticipant

            if (host == null || participant == null) {
                Toast.makeText(this, "يرجى تحديد مشارك أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (host.walletNumber == participant.walletNumber) {
                Toast.makeText(this, "أنت مسؤول الفرقة! قرب ميدالية مشارك آخر", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val amountStr = etContributionAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "يرجى إدخال مبلغ مساهمة صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = dataManager.simulateFirqatakContribution(
                hostUserWallet = host.walletNumber,
                participantWallet = participant.walletNumber,
                amount = amount,
                tagUid = currentTagUid
            )

            if (result.isSuccess) {
                Toast.makeText(this, "✓ تم استلام مساهمة ${participant.name} بنجاح", Toast.LENGTH_SHORT).show()
                setParticipant(participant, currentTagUid) // تحديث عرض الرصيد
                renderContributions()
            } else {
                val err = result.exceptionOrNull()?.message ?: "فشلت المساهمة"
                Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderContributions() {
        layoutContributionsList.removeAllViews()
        val host = dataManager.currentUser ?: return
        val code = host.firqatakCode ?: "FRQ100"
        val list = dataManager.getFirqatakContributions(code)

        if (list.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "لا توجد مساهمات مسجلة بعد في هذه الفرقة."
                setTextColor(getColor(R.color.text_secondary))
                textSize = 13f
                setPadding(10, 16, 10, 16)
            }
            layoutContributionsList.addView(emptyTv)
            return
        }

        list.forEach { item ->
            val row = LinearLayout(this).apply {
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

            val nameCol = LinearLayout(this).apply {
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
            nameCol.addView(tvName)
            nameCol.addView(tvWallet)

            val tvAmount = TextView(this).apply {
                text = "+ ${currencyFormat.format(item.amount)} ريال"
                setTextColor(getColor(R.color.success_green))
                textSize = 14f
            }

            val tvStatus = TextView(this).apply {
                text = "معتمد ✓"
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(16, 0, 0, 0)
            }

            row.addView(nameCol)
            row.addView(tvAmount)
            row.addView(tvStatus)
            layoutContributionsList.addView(row)
        }
    }
}
