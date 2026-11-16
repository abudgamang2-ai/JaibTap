package com.jaibtap.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Build

/**
 * مساعد قراءة شرائح NFC وهندسة التعامل مع التقنيات المختلفة
 * يدعم حالياً NTAG213 بشكل كامل ومُهيأ برمجياً لإضافة NTAG 424 DNA لاحقاً
 */
class NfcHelper(private val context: Context) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isNfcSupported(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * تفعيل استقبال إشارات NFC عند وضع التطبيق في الواجهة
     */
    fun enableForegroundDispatch(activity: Activity) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled) return

        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (_: Exception) {}
        }

        val filters = arrayOf(techFilter, tagFilter, ndefFilter)
        val techLists = arrayOf(
            arrayOf(NfcA::class.java.name),
            arrayOf(Ndef::class.java.name),
            arrayOf(MifareUltralight::class.java.name),
            arrayOf(IsoDep::class.java.name)
        )

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    /**
     * إيقاف استقبال إشارات NFC عند الخروج من الواجهة
     */
    fun disableForegroundDispatch(activity: Activity) {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (_: Exception) {}
    }

    /**
     * استخراج المعرف الفريد UID للشريحة وتحويله لصيغة Hex واضحة
     * مثال: 04:5A:21:B3:9C:60:80
     */
    fun extractTagUid(intent: Intent): String? {
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        val idBytes = tag?.id ?: return null
        return bytesToHex(idBytes)
    }

    /**
     * تحديد نوع الشريحة والتقنيات المدعومة فيها
     */
    fun detectTagType(intent: Intent): String {
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return "UNKNOWN"

        val techList = tag.techList.map { it.substringAfterLast(".") }

        // التحقق من تقنيات NTAG 424 DNA (تعتمد على IsoDep بروتوكول ISO/IEC 14443-4)
        if (techList.contains("IsoDep")) {
            return "NTAG 424 DNA (مجهزة للتشفير)"
        }

        // التحقق من NTAG213 / NTAG215 / NTAG216 (تعتمد على MifareUltralight و NfcA)
        if (techList.contains("MifareUltralight") || techList.contains("NfcA")) {
            return "NTAG213 (اكتشاف معرف UID)"
        }

        return "NFC Tag: ${techList.joinToString(", ")}"
    }

    companion object {
        fun bytesToHex(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (i in bytes.indices) {
                sb.append(String.format("%02X", bytes[i]))
                if (i < bytes.size - 1) {
                    sb.append(":")
                }
            }
            return sb.toString()
        }
    }
}
