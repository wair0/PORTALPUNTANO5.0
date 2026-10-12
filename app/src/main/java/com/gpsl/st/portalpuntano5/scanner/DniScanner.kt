package com.gpsl.st.portalpuntano5.scanner

import android.content.Context
import com.google.android.gms.codescanner.GmsBarcodeScannerOptions
import com.google.android.gms.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

@Serializable
data class ScannedDni(
    val procedureNumber: String,
    val lastName: String,
    val firstName: String,
    val sex: String,
    val docNumber: String,
    val birthDate: String,
)

class DniScanner(private val context: Context) {
    suspend fun scan(): ScannedDni {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_PDF417)
            .enableAutoZoom()
            .build()
        val result = GmsBarcodeScanning.getClient(context, options).startScan().await()
        val raw = result.rawValue ?: error("No se pudo leer el código")
        return parse(raw)
    }

    companion object {
        fun parse(raw: String): ScannedDni {
            val p = raw.split('@')
            require(p.size >= 7) { "Código no reconocido como DNI" }
            return ScannedDni(
                procedureNumber = p[0].trim(),
                lastName = p[1].trim(),
                firstName = p[2].trim(),
                sex = p[3].trim().uppercase(),
                docNumber = p[4].filter { it.isDigit() },
                birthDate = p[6].trim(),
            )
        }
    }
}