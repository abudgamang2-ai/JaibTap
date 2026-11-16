package com.jaibtap.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.ui.firqatak.FirqatakActivity
import com.jaibtap.app.ui.history.HistoryActivity
import com.jaibtap.app.ui.login.LoginActivity
import com.jaibtap.app.ui.nfc.NfcActivity
import com.jaibtap.app.ui.pos.PosTransferActivity
import com.jaibtap.app.ui.transfer.TransferActivity
import java.text.DecimalFormat
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private var isBalanceVisible = true

    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvWalletNumber: TextView
    private lateinit var tvFirqatakCodeBadge: TextView
    private lateinit var ivToggleBalance: ImageView
    private lateinit var btnLogout: ImageView
    private lateinit var btnResetData: Button

    private lateinit var btnServiceNfc: View
    private lateinit var btnServiceTransfer: View
    private lateinit var btnServicePos: View
    private lateinit var btnServiceFirqatak: View
    private lateinit var btnHistory: View

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dataManager = DataManager.get()
        if (dataManager.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUserData()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvUserName = findViewById(R.id.tvUserName)
        tvBalance = findViewById(R.id.tvBalance)
        tvWalletNumber = findViewById(R.id.tvWalletNumber)
        tvFirqatakCodeBadge = findViewById(R.id.tvFirqatakCodeBadge)
        ivToggleBalance = findViewById(R.id.ivToggleBalance)
        btnLogout = findViewById(R.id.btnLogout)
        btnResetData = findViewById(R.id.btnResetData)

        btnServiceNfc = findViewById(R.id.btnServiceNfc)
        btnServiceTransfer = findViewById(R.id.btnServiceTransfer)
        btnServicePos = findViewById(R.id.btnServicePos)
        btnServiceFirqatak = findViewById(R.id.btnServiceFirqatak)
        btnHistory = findViewById(R.id.btnHistory)
    }

    private fun updateUserData() {
        val user = dataManager.currentUser ?: return

        // التحية بحسب الوقت
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = if (hour in 5..14) getString(R.string.greeting_morning) else getString(R.string.greeting_evening)

        tvUserName.text = user.name
        tvWalletNumber.text = "محفظة: ${user.walletNumber}"

        if (user.firqatakCode != null) {
            tvFirqatakCodeBadge.visibility = View.VISIBLE
            tvFirqatakCodeBadge.text = "الفرقة: ${user.firqatakCode}"
        } else {
            tvFirqatakCodeBadge.visibility = View.GONE
        }

        renderBalance()
    }

    private fun renderBalance() {
        val user = dataManager.currentUser ?: return
        if (isBalanceVisible) {
            tvBalance.text = currencyFormat.format(user.balance)
            ivToggleBalance.setImageResource(R.drawable.ic_eye)
        } else {
            tvBalance.text = "••••••"
            ivToggleBalance.setImageResource(R.drawable.ic_eye_off)
        }
    }

    private fun setupListeners() {
        ivToggleBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            renderBalance()
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل تريد تسجيل الخروج؟")
                .setPositiveButton("خروج") { _, _ ->
                    dataManager.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        btnResetData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("إعادة تعيين البيانات")
                .setMessage("هل تريد استعادة الأرصدة والبيانات التجريبية الأولية؟")
                .setPositiveButton("استعادة") { _, _ ->
                    dataManager.resetDemoData()
                    updateUserData()
                    Toast.makeText(this, "تمت استعادة البيانات التجريبية", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        // الانتقال للخدمات
        btnServiceNfc.setOnClickListener {
            startActivity(Intent(this, NfcActivity::class.java))
        }

        btnServiceTransfer.setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
        }

        btnServicePos.setOnClickListener {
            startActivity(Intent(this, PosTransferActivity::class.java))
        }

        btnServiceFirqatak.setOnClickListener {
            startActivity(Intent(this, FirqatakActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
}
