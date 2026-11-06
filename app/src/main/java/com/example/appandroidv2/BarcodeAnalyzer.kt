package com.example.appandroidv2

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->

                val codigo = barcodes
                    .firstOrNull {
                        !it.rawValue.isNullOrBlank()
                    }
                    ?.rawValue

                if (!codigo.isNullOrBlank()) {
                    onBarcodeDetected(codigo)
                }
            }
            .addOnFailureListener { erro ->
                Log.e(
                    "BarcodeAnalyzer",
                    "Erro ao ler código de barras",
                    erro
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun fechar() {
        scanner.close()
    }
}