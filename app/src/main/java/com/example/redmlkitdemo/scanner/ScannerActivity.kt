package com.example.redmlkitdemo.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.example.redmlkitdemo.MainActivity.Companion.MODEL_TAG
import com.example.redmlkitdemo.scannerutils.common.BarcodeScanningProcessor
import com.example.redmlkitdemo.scannerutils.common.CameraSource
import com.example.redmlkitdemo.scannerutils.common.FrameMetadata
import com.example.redmlkitdemo.scannerutils.common.TextRecognitionProcessor
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.activity_scanner.*
import java.io.IOException

class ScannerActivity : AppCompatActivity() {

    private var cameraSource: CameraSource? = null
    private val requiredPermissions: Array<String?>
        get() {
            return try {
                val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }
        }
    private var barcodeScanningProcessor: BarcodeScanningProcessor? = null
    private var textRecognitionProcessor: TextRecognitionProcessor? = null
    private var model: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(com.example.redmlkitdemo.R.layout.activity_scanner)

        model = intent.getStringExtra(MODEL_TAG)

        if (allPermissionsGranted()) {
            createCameraSource(model)
        } else {
            getRuntimePermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        /*This has to be restored to default (false) state every time the activity resumes.
        * Otherwise, the scanner would detect only one code per activity lifecycle*/
        barcodeScanningProcessor?.setShouldIgnore(false)

        /*Restore default state for the OCR processor*/
        textRecognitionProcessor?.setShouldIgnore(false)
        textRecognitionProcessor?.setIsValidSource(false)
        textRecognitionProcessor?.decoupleTrigger()

        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
    }

    private fun createCameraSource(model: String) {

        when (model) {
            "" -> {
                Toast.makeText(applicationContext, "No valid model has been selected.", Toast.LENGTH_SHORT).show()
                finish()
            }
            BARCODE_MODEL -> {
                camera_button.visibility = View.GONE

                val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_QR_CODE
                    )
                    .build()

                val detector = FirebaseVision.getInstance()
                    .getVisionBarcodeDetector(options)


                if (cameraSource == null) {
                    cameraSource = CameraSource(this, barcodeOverlay)
                }

                barcodeScanningProcessor = BarcodeScanningProcessor(detector)
                barcodeScanningProcessor?.setBarcodeResultListener(getBarcodeResultListener())

                try {
                    cameraSource?.setMachineLearningFrameProcessor(barcodeScanningProcessor)
                } catch (e: FirebaseMLException) {
                }
            }

            OCR_MODEL -> {
                camera_button.visibility = View.VISIBLE

                if (cameraSource == null) {
                    cameraSource = CameraSource(this, barcodeOverlay)
                }

                textRecognitionProcessor = TextRecognitionProcessor()
                camera_button.setOnClickListener {
                    textRecognitionProcessor?.setOcrResultTrigger(getOcrResultTrigger())
                }

                try {
                    cameraSource?.setMachineLearningFrameProcessor(textRecognitionProcessor)
                } catch (e: FirebaseMLException) {
                }
            }
        }


    }

    private fun startCameraSource() {
        cameraSource?.let {
            try {
                preview?.start(cameraSource, barcodeOverlay)
            } catch (e: IOException) {
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission!!)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = arrayListOf<String>()
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission!!)) {
                allNeededPermissions.add(permission)
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (allPermissionsGranted()) {
            createCameraSource(model)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getBarcodeResultListener(): BarcodeScanningProcessor.BarcodeResultListener {

        return object : BarcodeScanningProcessor.BarcodeResultListener {
            override fun onSuccess(
                barcodes: MutableList<FirebaseVisionBarcode>,
                frameMetadata: FrameMetadata
            ) {
                var raw = ""
                for (barcode in barcodes) {
                    raw = barcode.rawValue ?: ""
                }

                val intent = Intent(applicationContext, ScanningResultActivity::class.java).apply {
                    putExtra("scanning_result", raw)
                }

                vibrate()
                startActivity(intent)
            }

            override fun onFailure(e: java.lang.Exception) {}
        }

    }

    private fun getOcrResultTrigger(): TextRecognitionProcessor.OcrResultTrigger {

        return object : TextRecognitionProcessor.OcrResultTrigger {
            override fun onSuccess(result: FirebaseVisionText) {
                val raw = result.text

                val intent = Intent(applicationContext, ScanningResultActivity::class.java).apply {
                    putExtra("scanning_result", raw)
                }

                vibrate()
                startActivity(intent)
            }

            override fun onFailure(e: java.lang.Exception) {}
        }
    }

    private fun vibrate() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(50)
        }
    }

    companion object {
        private const val PERMISSION_REQUESTS = 1
        private const val BARCODE_MODEL = "barcode_model"
        private const val OCR_MODEL = "ocr_model"

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                return true
            }
            return false
        }
    }


}
