package com.example.redmlkitdemo.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.example.redmlkitdemo.R
import com.example.redmlkitdemo.scannerutils.common.BarcodeScanningProcessor
import com.example.redmlkitdemo.scannerutils.common.CameraSource
import com.example.redmlkitdemo.scannerutils.common.FrameMetadata
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
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
    private lateinit var barcodeScanningProcessor: BarcodeScanningProcessor

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        if (allPermissionsGranted()) {
            createCameraSource()
        } else {
            getRuntimePermissions()
        }
    }

    override fun onResume() {
        super.onResume()
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

    private fun createCameraSource() {

        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_QR_CODE
            )
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionBarcodeDetector(options)


        if (cameraSource == null) {
            cameraSource = CameraSource(this)
        }

        barcodeScanningProcessor = BarcodeScanningProcessor(detector)
        barcodeScanningProcessor.setBarcodeResultListener(getBarcodeResultListener())

        try {
            cameraSource?.setMachineLearningFrameProcessor(barcodeScanningProcessor)
        } catch (e: FirebaseMLException) {

        }
    }

    private fun startCameraSource() {
        cameraSource?.let {
            try {
                preview?.start(cameraSource)
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
            createCameraSource()
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

                startActivity(intent)

                finish()
            }

            override fun onFailure(e: java.lang.Exception) {

            }
        }

    }

    companion object {
        private const val PERMISSION_REQUESTS = 1

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                return true
            }
            return false
        }
    }


}
