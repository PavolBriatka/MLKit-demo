package com.example.redmlkitdemo.scannerutils.common;

import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Barcode Detector Demo.
 */
public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {

    private static final String TAG = "BarcodeScanProc";

    //if a barcode is detected ignore any other results coming after.
    private final AtomicBoolean shouldIgnore = new AtomicBoolean(false);

    private final FirebaseVisionBarcodeDetector detector;
    BarcodeResultListener barcodeResultListener;

    public BarcodeScanningProcessor() {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        // new FirebaseVisionBarcodeDetectorOptions.Builder()
        //     .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
        //     .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
    }

    public BarcodeScanningProcessor(FirebaseVisionBarcodeDetector detector) {
        this.detector = detector;
    }

    public BarcodeResultListener getBarcodeResultListener() {
        return barcodeResultListener;
    }

    public void setBarcodeResultListener(BarcodeResultListener barcodeResultListener) {
        this.barcodeResultListener = barcodeResultListener;
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Barcode Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<FirebaseVisionBarcode> barcodes,
            @NonNull FrameMetadata frameMetadata) {
        if (shouldIgnore.get()){
            return;
        }


        if (barcodeResultListener != null && barcodes.size() > 0) {
            //once the app detects a barcode(s) we are no longer interested in the fed images.
            shouldIgnore.set(true);
            barcodeResultListener.onSuccess(barcodes, frameMetadata);
        }

    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
        if (barcodeResultListener != null)
            barcodeResultListener.onFailure(e);
    }

    public interface BarcodeResultListener {
        void onSuccess(
                @NonNull List<FirebaseVisionBarcode> barcodes,
                @NonNull FrameMetadata frameMetadata);

        void onFailure(@NonNull Exception e);
    }
}
