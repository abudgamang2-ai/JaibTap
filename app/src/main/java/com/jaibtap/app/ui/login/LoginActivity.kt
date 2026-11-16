package com.jaibtap.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.ui.home.HomeActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private var isPinMode = false

    private lateinit var tabPassword: TextView
    private lateinit var tabPin: TextView
    private lateinit var tvSecretLabel: TextView
    private lateinit var etPhone: EditText
    private lateinit var etSecret: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var btnQuickAhmed: Button
    private lateinit var btnQuickMohammed: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dataManager = DataManager.get()

        // إذا كان هناك مستخدم مسجل مسبقاً وتذكر الدخول مفعل
        if (dataManager.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tabPassword = findViewById(R.id.tabPassword)
        tabPin = findViewById(R.id.tabPin)
        tvSecretLabel = findViewById(R.id.tvSecretLabel)
        etPhone = findViewById(R.id.etPhone)
        etSecret = findViewById(R.id.etSecret)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        btnQuickAhmed = findViewById(R.id.btnQuickUserAhmed)
        btnQuickMohammed = findViewById(R.id.btnQuickUserMohammed)

        // الوضع الافتراضي: كلمة المرور
        setMode(false)
        etPhone.setText("771000001")
        etSecret.setText("password123")
    }

    private fun setupListeners() {
        tabPassword.setOnClickListener { setMode(false) }
        tabPin.setOnClickListener { setMode(true) }

        btnLogin.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val secret = etSecret.text.toString().trim()

            if (phone.isEmpty() || secret.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال جميع الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = dataManager.authenticate(phone, secret, isPinMode)
            if (user != null) {
                Toast.makeText(this, "أهلاً بك يا ${user.name}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_LONG).show()
            }
        }

        btnQuickAhmed.setOnClickListener {
            etPhone.setText("771000001")
            if (isPinMode) {
                etSecret.setText("1234")
            } else {
                etSecret.setText("password123")
            }
        }

        btnQuickMohammed.setOnClickListener {
            etPhone.setText("771000002")
            if (isPinMode) {
                etSecret.setText("5678")
            } else {
                etSecret.setText("password123")
            }
        }
    }

    private fun setMode(pin: Boolean) {
        isPinMode = pin
        if (pin) {
            tabPin.background = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
            tabPin.setTextColor(ContextCompat.getColor(this, R.color.white))
            tabPassword.background = ContextCompat.getDrawable(this, R.drawable.bg_button_secondary)
            tabPassword.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            tvSecretLabel.text = getString(R.string.pin_code)
            etSecret.hint = "1234"
            etSecret.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            if (etPhone.text.toString() == "771000001") etSecret.setText("1234")
            if (etPhone.text.toString() == "771000002") etSecret.setText("5678")
        } else {
            tabPassword.background = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
            tabPassword.setTextColor(ContextCompat.getColor(this, R.color.white))
            tabPin.background = ContextCompat.getDrawable(this, R.drawable.bg_button_secondary)
            tabPin.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            tvSecretLabel.text = getString(R.string.password)
            etSecret.hint = "••••••"
            etSecret.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            if (etPhone.text.toString() == "771000001") etSecret.setText("password123")
            if (etPhone.text.toString() == "771000002") etSecret.setText("password123")
        }
    }
}
