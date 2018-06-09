package com.example.ashish.mlkitdemo;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v8.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;

import io.github.silvaren.easyrs.tools.Nv21Image;

public class MainActivity extends AppCompatActivity {
    CameraView cameraView;
    FloatingActionButton cameraSwitch;
    Bitmap bitmap;
    private boolean frameIsProcessing = false;
    private RenderScript rs;
    FirebaseVisionImage image;
    FirebaseVisionFaceDetectorOptions options;
    String resultString;
    TextView resultTV;
    Boolean camerBack = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera_view);
        cameraSwitch = findViewById(R.id.imageView);
        resultTV = findViewById(R.id.result);
        rs = RenderScript.create(this);
        cameraSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.switchCamera();
                camerBack = !camerBack;
            }
        });
        fireBaseVisionFaceDetectorOptions();
        setupCameraCallbacks();

    }

    public void fireBaseVisionFaceDetectorOptions() {
        options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .setTrackingEnabled(true)
                        .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
        setupCameraCallbacks();
    }


    private void setupCameraCallbacks() {
        cameraView.setOnFrameListener(new CameraViewImpl.OnFrameListener() {
            @Override
            public void onFrame(final byte[] data, final int width, final int height, final int rotationDegrees) {


                if (frameIsProcessing) return;
                frameIsProcessing = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bitmap = Nv21Image.nv21ToBitmap(rs, data, width, height);
                        Matrix matrix = new Matrix();
                        if (camerBack) {
                            matrix.postRotate(-rotationDegrees);
                        } else {
                            matrix.postRotate(rotationDegrees);
                        }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                      image = FirebaseVisionImage.fromBitmap(bitmap);
                        FirebaseApp.initializeApp(MainActivity.this);
                        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                                .getVisionFaceDetector(options);
                        Task<List<FirebaseVisionFace>> result =
                                detector.detectInImage(image)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                                    @Override
                                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                                        for (FirebaseVisionFace face : faces) {
                                                            Rect bounds = face.getBoundingBox();
                                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                                            // nose available):
                                                            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                                                            if (leftEar != null) {
                                                                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
                                                            }

                                                            // If classification was enabled:
                                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                                float smileProb = face.getSmilingProbability();
                                                                resultString = "Smile Prob : " + smileProb;
                                                            }
                                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                                                resultString = resultString + "\nRight eye : " + rightEyeOpenProb;
                                                            }
                                                            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                                float leftEyeOpenProb = face.getLeftEyeOpenProbability();
                                                                resultString = resultString + "\nLeft eye : " + leftEyeOpenProb;
                                                            }
                                                            // If face tracking was enabled:
                                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                                                int id = face.getTrackingId();
                                                            }
                                                            Log.e("RESULT_STRING", resultString);
                                                            resultTV.setText(resultString);
                                                        }

                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(MainActivity.this, "Failed to load the Ml kit", Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                        frameIsProcessing = false;
                    }
                });
            }
        });
    }



    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }
}
