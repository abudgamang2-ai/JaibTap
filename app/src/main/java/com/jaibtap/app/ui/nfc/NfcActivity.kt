package com.jaibtap.app.ui.nfc

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.data.model.NfcTagInfo
import com.jaibtap.app.nfc.NfcHelper

class NfcActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var nfcHelper: NfcHelper

    private var currentTagInfo: NfcTagInfo? = null

    private lateinit var btnBack: ImageView
    private lateinit var layoutNfcStatusAlert: LinearLayout
    private lateinit var tvNfcStatusMessage: TextView
    private lateinit var btnEnableNfcSettings: Button

    private lateinit var layoutScanning: LinearLayout
    private lateinit var tvScanTitle: TextView
    private lateinit var ivNfcRadar: ImageView

    private lateinit var layoutTagResult: LinearLayout
    private lateinit var ivTagStatusIcon: ImageView
    private lateinit var tvTagOwnerName: TextView
    private lateinit var tvTagTypeBadge: TextView
    private lateinit var tvTagUid: TextView
    private lateinit var tvTagWalletNumber: TextView

    private lateinit var layoutServicesContainer: LinearLayout
    private lateinit var btnNfcWithdrawal: View
    private lateinit var btnNfcDeposit: View
    private lateinit var btnNfcFirqatak: View

    private lateinit var btnSimulateAhmedTag: Button
    private lateinit var btnSimulateMohammedTag: Button
    private lateinit var btnSimulateUnknownTag: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        dataManager = DataManager.get()
        nfcHelper = NfcHelper(this)

        initViews()
        setupListeners()
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        checkNfcAvailability()
        nfcHelper.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        layoutNfcStatusAlert = findViewById(R.id.layoutNfcStatusAlert)
        tvNfcStatusMessage = findViewById(R.id.tvNfcStatusMessage)
        btnEnableNfcSettings = findViewById(R.id.btnEnableNfcSettings)

        layoutScanning = findViewById(R.id.layoutScanning)
        tvScanTitle = findViewById(R.id.tvScanTitle)
        ivNfcRadar = findViewById(R.id.ivNfcRadar)

        layoutTagResult = findViewById(R.id.layoutTagResult)
        ivTagStatusIcon = findViewById(R.id.ivTagStatusIcon)
        tvTagOwnerName = findViewById(R.id.tvTagOwnerName)
        tvTagTypeBadge = findViewById(R.id.tvTagTypeBadge)
        tvTagUid = findViewById(R.id.tvTagUid)
        tvTagWalletNumber = findViewById(R.id.tvTagWalletNumber)

        layoutServicesContainer = findViewById(R.id.layoutServicesContainer)
        btnNfcWithdrawal = findViewById(R.id.btnNfcWithdrawal)
        btnNfcDeposit = findViewById(R.id.btnNfcDeposit)
        btnNfcFirqatak = findViewById(R.id.btnNfcFirqatak)

        btnSimulateAhmedTag = findViewById(R.id.btnSimulateAhmedTag)
        btnSimulateMohammedTag = findViewById(R.id.btnSimulateMohammedTag)
        btnSimulateUnknownTag = findViewById(R.id.btnSimulateUnknownTag)
    }

    private fun checkNfcAvailability() {
        if (!nfcHelper.isNfcSupported()) {
            layoutNfcStatusAlert.visibility = View.VISIBLE
            tvNfcStatusMessage.text = getString(R.string.nfc_not_supported)
            btnEnableNfcSettings.visibility = View.GONE
        } else if (!nfcHelper.isNfcEnabled()) {
            layoutNfcStatusAlert.visibility = View.VISIBLE
            tvNfcStatusMessage.text = getString(R.string.nfc_disabled)
            btnEnableNfcSettings.visibility = View.VISIBLE
        } else {
            layoutNfcStatusAlert.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnEnableNfcSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }

        // أزرار المحاكاة السريعة
        btnSimulateAhmedTag.setOnClickListener {
            processTagUid("04:5A:21:B3:9C:60:80", "NTAG213")
        }

        btnSimulateMohammedTag.setOnClickListener {
            processTagUid("04:88:C2:5F:1A:70:81", "NTAG213")
        }

        btnSimulateUnknownTag.setOnClickListener {
            processTagUid("04:99:AA:BB:CC:DD:EE", "NTAG213")
        }

        // الخدمات الثلاث
        btnNfcWithdrawal.setOnClickListener {
            val tag = currentTagInfo ?: return@setOnClickListener
            val intent = Intent(this, WithdrawalActivity::class.java).apply {
                putExtra("TAG_OWNER_WALLET", tag.ownerUser?.walletNumber)
                putExtra("TAG_OWNER_NAME", tag.ownerUser?.name)
                putExtra("TAG_UID", tag.tagUid)
            }
            startActivity(intent)
        }

        btnNfcDeposit.setOnClickListener {
            val tag = currentTagInfo ?: return@setOnClickListener
            val intent = Intent(this, DepositActivity::class.java).apply {
                putExtra("TAG_OWNER_WALLET", tag.ownerUser?.walletNumber)
                putExtra("TAG_OWNER_NAME", tag.ownerUser?.name)
                putExtra("TAG_UID", tag.tagUid)
            }
            startActivity(intent)
        }

        btnNfcFirqatak.setOnClickListener {
            val tag = currentTagInfo ?: return@setOnClickListener
            val intent = Intent(this, FirqatakNfcActivity::class.java).apply {
                putExtra("TAG_OWNER_WALLET", tag.ownerUser?.walletNumber)
                putExtra("TAG_OWNER_NAME", tag.ownerUser?.name)
                putExtra("TAG_UID", tag.tagUid)
            }
            startActivity(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uid = nfcHelper.extractTagUid(intent)
        if (uid != null) {
            val tagType = nfcHelper.detectTagType(intent)
            processTagUid(uid, tagType)
        }
    }

    private fun processTagUid(uid: String, tagType: String) {
        val info = dataManager.resolveNfcTag(uid)
        currentTagInfo = info

        layoutScanning.visibility = View.GONE
        layoutTagResult.visibility = View.VISIBLE

        tvTagUid.text = "معرف الميدالية (UID): ${info.tagUid}"
        tvTagTypeBadge.text = tagType

        if (info.isKnown && info.ownerUser != null) {
            // ميدالية مسجلة ومعروفة
            ivTagStatusIcon.setImageResource(R.drawable.ic_check_circle)
            ivTagStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_green))
            tvTagOwnerName.text = "صاحب الميدالية: ${info.ownerUser.name}"
            tvTagWalletNumber.visibility = View.VISIBLE
            tvTagWalletNumber.text = "رقم المحفظة: ${info.ownerUser.walletNumber}"

            layoutServicesContainer.visibility = View.VISIBLE
            Toast.makeText(this, "تم التعرف على ميدالية: ${info.ownerUser.name}", Toast.LENGTH_SHORT).show()
        } else {
            // ميدالية غير مسجلة في قاعدة البيانات التجريبية
            ivTagStatusIcon.setImageResource(R.drawable.ic_error_circle)
            ivTagStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning_yellow))
            tvTagOwnerName.text = getString(R.string.nfc_tag_unknown)
            tvTagWalletNumber.visibility = View.GONE
            layoutServicesContainer.visibility = View.GONE

            // إتاحة خيار ربط الميدالية بحساب تجريبي لتسهيل الاختبار بالبطاقات الحقيقية
            showLinkTagDialog(info.tagUid)
        }
    }

    private fun showLinkTagDialog(tagUid: String) {
        val users = dataManager.getAllUsers()
        val names = users.map { "${it.name} (${it.walletNumber})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("ميدالية جديدة غير مسجلة")
            .setMessage("تمت قراءة الشريحة بنجاح (UID: $tagUid).\nهل تود ربط هذه الشريحة الفيزيائية بأحد الحسابات التجريبية؟")
            .setItems(names) { _, which ->
                val selectedUser = users[which]
                dataManager.linkTagToUser(tagUid, selectedUser.walletNumber)
                Toast.makeText(this, "تم ربط الشريحة بحساب ${selectedUser.name}", Toast.LENGTH_SHORT).show()
                processTagUid(tagUid, "NTAG213 (شريحة حقيقية مربوطة)")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
