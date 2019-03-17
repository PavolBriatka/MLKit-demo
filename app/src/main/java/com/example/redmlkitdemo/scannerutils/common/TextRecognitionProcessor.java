package com.example.redmlkitdemo.scannerutils.common;

import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Processor for the text recognition demo. */
public class TextRecognitionProcessor extends VisionProcessorBase<FirebaseVisionText> {

    private static final String TAG = "TextRecProc";

    private final FirebaseVisionTextRecognizer detector;
    OcrResultTrigger ocrResultTrigger;

    //if a text is captured ignore any other results coming after.
    private final AtomicBoolean shouldIgnore = new AtomicBoolean(false);

    public TextRecognitionProcessor() {
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    public void setOcrResultTrigger(OcrResultTrigger resultTrigger) {
        this.ocrResultTrigger = resultTrigger;
    }

    public void setShouldIgnore(Boolean value) {
        this.shouldIgnore.set(value);
    }
    public void decoupleTrigger() {ocrResultTrigger = null;}

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
        }
    }

    @Override
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }

    @Override
    protected void onSuccess(
            @NonNull FirebaseVisionText results,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {

        if (shouldIgnore.get()){
            return;
        }

        graphicOverlay.clear();
        List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
                    graphicOverlay.add(textGraphic);

                }
            }
        }

        if (ocrResultTrigger != null &! results.getText().isEmpty()) {
            shouldIgnore.set(true);
            ocrResultTrigger.onSuccess(results);
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
        if (ocrResultTrigger != null) {
            ocrResultTrigger.onFailure(e);
        }
    }

    public interface OcrResultTrigger {
        void onSuccess(@NonNull FirebaseVisionText result);
        void onFailure(@NonNull Exception e);
    }
}
