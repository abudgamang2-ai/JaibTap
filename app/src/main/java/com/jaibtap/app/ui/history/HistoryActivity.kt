package com.jaibtap.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaibtap.app.R
import com.jaibtap.app.data.DataManager
import com.jaibtap.app.data.model.Transaction
import com.jaibtap.app.data.model.TransactionStatus
import com.jaibtap.app.data.model.TransactionType
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var btnBack: ImageView
    private lateinit var tvEmptyHistory: TextView
    private lateinit var rvTransactions: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dataManager = DataManager.get()
        btnBack = findViewById(R.id.btnBack)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        rvTransactions = findViewById(R.id.rvTransactions)

        btnBack.setOnClickListener { finish() }

        rvTransactions.layoutManager = LinearLayoutManager(this)
        loadTransactions()
    }

    private fun loadTransactions() {
        val list = dataManager.getTransactions()
        if (list.isEmpty()) {
            tvEmptyHistory.visibility = View.VISIBLE
            rvTransactions.visibility = View.GONE
        } else {
            tvEmptyHistory.visibility = View.GONE
            rvTransactions.visibility = View.VISIBLE
            rvTransactions.adapter = TransactionAdapter(list, dataManager.currentUser?.walletNumber)
        }
    }

    class TransactionAdapter(
        private val items: List<Transaction>,
        private val currentWallet: String?
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        private val currencyFormat = DecimalFormat("#,##0.00")
        private val dateFormat = SimpleDateFormat("yyyy/MM/dd (HH:mm)", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tx = items[position]
            val context = holder.itemView.context

            // نوع العملية
            when (tx.type) {
                TransactionType.TRANSFER_SUBSCRIBER -> {
                    holder.tvTxType.text = "تحويل مشترك"
                    holder.ivTxIcon.setImageResource(R.drawable.ic_transfer)
                }
                TransactionType.WITHDRAWAL -> {
                    holder.tvTxType.text = "سحب نقدي"
                    holder.ivTxIcon.setImageResource(R.drawable.ic_pos)
                }
                TransactionType.DEPOSIT -> {
                    holder.tvTxType.text = "إيداع"
                    holder.ivTxIcon.setImageResource(R.drawable.ic_transfer)
                }
                TransactionType.POS_PAYMENT -> {
                    holder.tvTxType.text = "دفع مشتريات"
                    holder.ivTxIcon.setImageResource(R.drawable.ic_pos)
                }
                TransactionType.FIRQATAK -> {
                    holder.tvTxType.text = "فرقتك"
                    holder.ivTxIcon.setImageResource(R.drawable.ic_firqatak)
                }
            }

            // التفاصيل
            val detail = when (tx.type) {
                TransactionType.TRANSFER_SUBSCRIBER -> "إلى: ${tx.receiverName} (${tx.receiverWallet})"
                TransactionType.WITHDRAWAL -> "من صاحب الميدالية: ${tx.receiverName}"
                TransactionType.DEPOSIT -> "إيداع لحساب: ${tx.receiverName}"
                TransactionType.POS_PAYMENT -> "متجر: ${tx.receiverName}"
                TransactionType.FIRQATAK -> tx.notes ?: "مساهمة فرقتك"
            }
            holder.tvTxDetails.text = detail
            holder.tvTxDate.text = dateFormat.format(Date(tx.timestamp))

            // شارة NFC
            holder.tvNfcBadge.visibility = if (tx.nfcUsed) View.VISIBLE else View.GONE

            // المبلغ والموجب/السالب
            val isDebit = tx.senderWallet == currentWallet
            if (tx.status == TransactionStatus.FAILED) {
                holder.tvTxAmount.text = "${currencyFormat.format(tx.amount)}"
                holder.tvTxAmount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                holder.tvTxStatus.text = "فشلت (${tx.failureReason ?: "مرفوضة"})"
                holder.tvTxStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red))
            } else {
                if (isDebit) {
                    holder.tvTxAmount.text = "- ${currencyFormat.format(tx.amount)}"
                    holder.tvTxAmount.setTextColor(ContextCompat.getColor(context, R.color.primary_red_light))
                } else {
                    holder.tvTxAmount.text = "+ ${currencyFormat.format(tx.amount)}"
                    holder.tvTxAmount.setTextColor(ContextCompat.getColor(context, R.color.success_green))
                }
                holder.tvTxStatus.text = "ناجحة ✓"
                holder.tvTxStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvTxType: TextView = v.findViewById(R.id.tvTxType)
            val tvNfcBadge: TextView = v.findViewById(R.id.tvNfcBadge)
            val tvTxDetails: TextView = v.findViewById(R.id.tvTxDetails)
            val tvTxDate: TextView = v.findViewById(R.id.tvTxDate)
            val tvTxAmount: TextView = v.findViewById(R.id.tvTxAmount)
            val tvTxStatus: TextView = v.findViewById(R.id.tvTxStatus)
            val ivTxIcon: ImageView = v.findViewById(R.id.ivTxIcon)
        }
    }
}
