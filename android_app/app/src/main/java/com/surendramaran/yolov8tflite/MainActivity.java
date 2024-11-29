//package com.surendramaran.yolov8tflite;
//
//import static com.surendramaran.yolov8tflite.Constants.LABELS_PATH;
//import static com.surendramaran.yolov8tflite.Constants.MODEL_PATH;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.Matrix;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.EditText;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.*;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import org.json.JSONObject;
//
//import com.google.common.util.concurrent.ListenableFuture;
//import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MainActivity extends AppCompatActivity implements Detector.DetectorListener {
//
//    private ActivityMainBinding binding;
//    private boolean isFrontCamera = false;
//
//    private Preview preview;
//    private ImageAnalysis imageAnalyzer;
//    private Camera camera;
//    private ProcessCameraProvider cameraProvider;
//    private Detector detector;
//
//    private ExecutorService cameraExecutor;
//    private Set<String> previouslyDetectedObjects;
//
//    {
//        previouslyDetectedObjects = new HashSet<>();
//    }
//
//    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
//                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) {
//                    try {
//                        startCamera();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//
//        detector = new Detector(getBaseContext(), MODEL_PATH, LABELS_PATH, this);
//        detector.setup();
//
//        if (allPermissionsGranted()) {
//            try {
//                startCamera();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
//        }
//
//        cameraExecutor = Executors.newSingleThreadExecutor();
//    }
//
//    private void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//
//        // Use ListenableFuture to add a listener
//        cameraProviderFuture.addListener(() -> {
//            try {
//                cameraProvider = cameraProviderFuture.get(); // This is now safe to use within the listener
//                bindCameraUseCases();
//            } catch (ExecutionException | InterruptedException e) {
//                Log.e(TAG, "Error starting camera", e);
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//
//    private void bindCameraUseCases() {
//        if (cameraProvider == null) {
//            throw new IllegalStateException("Camera initialization failed.");
//        }
//
//        int rotation = binding.viewFinder.getDisplay().getRotation();
//
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build();
//
//        preview = new Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetRotation(rotation)
//                .build();
//
//        imageAnalyzer = new ImageAnalysis.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .setTargetRotation(rotation)
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                .build();
//
//        imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
//            // Create a bitmap buffer from the image proxy
//            Bitmap bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
//            bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
//
//            // Prepare rotation matrix
//            Matrix matrix = new Matrix();
//            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
//
//            if (isFrontCamera) {
//                matrix.postScale(-1f, 1f, imageProxy.getWidth(), imageProxy.getHeight());
//            }
//
//            // Create the rotated bitmap
//            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(), matrix, true);
//
//            // Perform detection
//            detector.detect(rotatedBitmap);
//
//            // Close imageProxy after processing
//            imageProxy.close();
//        });
//
//        cameraProvider.unbindAll();
//
//        try {
//            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
//            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
//        } catch (Exception exc) {
//            Log.e(TAG, "Use case binding failed", exc);
//        }
//    }
//
//    private boolean allPermissionsGranted() {
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        detector.clear();
//        cameraExecutor.shutdown();
//    }
//    @Override
//    protected void onPause() {
//        super.onPause();
//        cameraProvider.unbindAll();  // Unbind camera when not in the foreground
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (allPermissionsGranted()) {
//            try {
//                startCamera();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS);
//        }
//    }
//
//    @Override
//    public void onEmptyDetect() {
//        binding.overlay.invalidate();
//    }
//
//    //    @Override
////    public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
////        runOnUiThread(() -> {
////            binding.inferenceTime.setText(inferenceTime + "ms");
////            binding.overlay.setResults(boundingBoxes);
////            binding.overlay.invalidate();
////        });
////
////        for (BoundingBox box : boundingBoxes) {
////            Log.d(TAG, "Detected Object: " + box.getClsName() + ", Confidence: " + box.getCnf());
////            System.out.println("Detected Object: " + box.getClsName() + ", Confidence: " + box.getCnf());
////        }
////        StringBuilder detectedObjects = new StringBuilder();
////        for (BoundingBox box : boundingBoxes) {
////            if (detectedObjects.length() > 0) {
////                detectedObjects.append(", ");
////            }
////            detectedObjects.append(box.getClsName());
////        }
////    }
////    @Override
////    public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
////        runOnUiThread(() -> {
////            binding.inferenceTime.setText(inferenceTime + "ms");
////            binding.overlay.setResults(boundingBoxes);
////            binding.overlay.invalidate();
////        });
////
////        // Collect all detected class names
////        StringBuilder detectedObj = new StringBuilder();
////        for (BoundingBox box : boundingBoxes) {
////            if (detectedObj.length() > 0) {
////                detectedObj.append(", ");
////            }
////            detectedObj.append(box.getClsName());
////        }
////
////        // Format as JSON string
////        String jsonString = "{objects: " + detectedObj.toString() + "}";
////
////        // Print JSON string
////        Log.d(TAG, "Detected Objects JSON: " + jsonString);
////        System.out.println("Detected Objects JSON: " + jsonString);
////    }
////    @Override
////    public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
////        runOnUiThread(() -> {
////            binding.inferenceTime.setText(inferenceTime + "ms");
////            binding.overlay.setResults(boundingBoxes);
////            binding.overlay.invalidate();
////        });
////
////        // Collect all detected class names and update detection status
////        Set<String> currentDetectedObjects = new HashSet<>();
////        for (BoundingBox box : boundingBoxes) {
////            String className = box.getClsName();
////            currentDetectedObjects.add(className);
////
////            // Check for specific classes and display toast for addition
////            if (className.equals("mobile") && !previouslyDetectedObjects.contains("mobile")) {
////                System.out.println("1 mobile added");
////            } else if (className.equals("laptop") && !previouslyDetectedObjects.contains("laptop")) {
////                System.out.println("1 laptop added");;
////            }
////        }
////
////        // Check for removals by comparing previous and current sets
////        if (previouslyDetectedObjects.contains("mobile") && !currentDetectedObjects.contains("mobile")) {
////            System.out.println("1 mobile removed");
////        }
////        if (previouslyDetectedObjects.contains("laptop") && !currentDetectedObjects.contains("laptop")) {
////            System.out.println("1 laptop removed");
////        }
////
////        // Update previously detected objects
////        previouslyDetectedObjects = currentDetectedObjects;
////
////        // Optional: Log JSON format of detected objects
////        StringBuilder detectedObj = new StringBuilder();
////        for (String obj : currentDetectedObjects) {
////            if (detectedObj.length() > 0) {
////                detectedObj.append(", ");
////            }
////            detectedObj.append(obj);
////        }
////        String jsonString = "{objects: " + detectedObj.toString() + "}";
////        Log.d(TAG, "Detected Objects JSON: " + jsonString);
////        TextView th = findViewById(R.id.editTextText2);
////        th.setText(jsonString);
////
////    }
//    @Override
//    public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
//        runOnUiThread(() -> {
//            binding.inferenceTime.setText(inferenceTime + "ms");
//            binding.overlay.setResults(boundingBoxes);
//            binding.overlay.invalidate();
//        });
//
//        Set<String> currentDetectedObjects = new HashSet<>();
//        for (BoundingBox box : boundingBoxes) {
//            String className = box.getClsName();
//            currentDetectedObjects.add(className);
//        }
//
//        StringBuilder detectedObj = new StringBuilder();
//        for (String obj : currentDetectedObjects) {
//            if (detectedObj.length() > 0) {
//                detectedObj.append(", ");
//            }
//            detectedObj.append(obj);
//        }
//        String jsonString = "{objects: " + detectedObj.toString() + "}";
//
//        // Return JSON string to HomeActivity
//        Intent resultIntent = new Intent();
//        resultIntent.putExtra("detectedObjects", jsonString);
//        setResult(RESULT_OK, resultIntent);
//        finish();  // End MainActivity and go back to HomeActivity
//    }
//
//
//    private static final String TAG = "Camera";
//    private static final int REQUEST_CODE_PERMISSIONS = 10;
//    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
//}
package com.surendramaran.yolov8tflite;

import static com.surendramaran.yolov8tflite.Constants.LABELS_PATH;
import static com.surendramaran.yolov8tflite.Constants.MODEL_PATH;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements Detector.DetectorListener {

    private ActivityMainBinding binding;
    private boolean isFrontCamera = false;

    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private Detector detector;

    private ExecutorService cameraExecutor;
    private Set<String> previouslyDetectedObjects = new HashSet<>();
    private static final String TAG = "YOLOApp";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) {
                    Log.d(TAG, "Camera permission granted.");
                    try {
                        startCamera();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in startCamera(): ", e);
                    }
                } else {
                    Log.d(TAG, "Camera permission denied.");
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "Activity created and layout set.");

        detector = new Detector(getBaseContext(), MODEL_PATH, LABELS_PATH, this);
        detector.setup();
        Log.d(TAG, "Detector setup completed.");

        if (allPermissionsGranted()) {
            Log.d(TAG, "All permissions granted. Starting camera...");
            try {
                startCamera();
            } catch (Exception e) {
                Log.e(TAG, "Exception in startCamera(): ", e);
            }
        } else {
            Log.d(TAG, "Requesting permissions...");
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        Log.d(TAG, "Starting camera...");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Log.d(TAG, "CameraProvider obtained.");
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Log.d(TAG, "Binding camera use cases...");
        if (cameraProvider == null) {
            Log.e(TAG, "Camera initialization failed.");
            throw new IllegalStateException("Camera initialization failed.");
        }

        int rotation = binding.viewFinder.getDisplay().getRotation();
        Log.d(TAG, "Display rotation: " + rotation);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
            Log.d(TAG, "Analyzing image...");
            Bitmap bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
            Log.d(TAG, "Image rotation degrees: " + imageProxy.getImageInfo().getRotationDegrees());

            if (isFrontCamera) {
                matrix.postScale(-1f, 1f, imageProxy.getWidth(), imageProxy.getHeight());
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(), matrix, true);
            detector.detect(rotatedBitmap);
            Log.d(TAG, "Detection performed on rotated bitmap.");
            imageProxy.close();
        });

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
            Log.d(TAG, "Camera bind complete.");
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : new String[]{Manifest.permission.CAMERA}) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission " + permission + " not granted.");
                return false;
            }
        }
        Log.d(TAG, "All required permissions granted.");
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.clear();
        cameraExecutor.shutdown();
        Log.d(TAG, "Activity destroyed, resources cleaned up.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            Log.d(TAG, "Camera unbound on pause.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming activity...");
        if (allPermissionsGranted()) {
            try {
                startCamera();
            } catch (Exception e) {
                Log.e(TAG, "Exception in startCamera(): ", e);
            }
        } else {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    @Override
    public void onEmptyDetect() {
        Log.d(TAG, "No objects detected.");
        binding.overlay.invalidate();
    }

    @Override
    public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
        Log.d(TAG, "Objects detected. Inference time: " + inferenceTime + "ms");

        runOnUiThread(() -> {
            binding.inferenceTime.setText(inferenceTime + "ms");
            binding.overlay.setResults(boundingBoxes);
            binding.overlay.invalidate();
        });

        Set<String> currentDetectedObjects = new HashSet<>();
        for (BoundingBox box : boundingBoxes) {
            String className = box.getClsName();
            currentDetectedObjects.add(className);
            Log.d(TAG, "Detected Object: " + className);
        }

        StringBuilder detectedObj = new StringBuilder();
        for (String obj : currentDetectedObjects) {
            if (detectedObj.length() > 0) {
                detectedObj.append(", ");
            }
            detectedObj.append(obj);
        }

        String jsonString = "{objects: " + detectedObj.toString() + "}";
        Log.d(TAG, "Detected Objects JSON: " + jsonString);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("detectedObjects", jsonString);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

