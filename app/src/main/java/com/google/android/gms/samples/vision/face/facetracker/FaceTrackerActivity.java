/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private int faceCount = 0;
    private String string, finalString;
    private int gender=-1;
    private int age=-1;
    final Handler handler = new Handler();

    private TextView mTextView;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    Runnable captureRunnable;

    private boolean flagCapture = true;

    final android.hardware.Camera.PictureCallback jpegCameraCallback = new Camera.PictureCallback() {
         @Override
         public void onPictureTaken(final byte[] bytes, Camera camera) {
             Log.i(TAG, "onPictureTaken: ");
             //Test pic size
//             BufferedOutputStream bos = null;
//             try {
//                 bos = new BufferedOutputStream(new FileOutputStream("/sdcard/pic.jpeg"));
//                 bos.write(bytes);
//                 bos.flush();
//                 bos.close();
//             } catch (FileNotFoundException e) {
//                 e.printStackTrace();
//             }
//             catch (IOException e) {
//                 e.printStackTrace();
//             }

             // request

             VolleyMultipartRequest strRequest = new VolleyMultipartRequest(Request.Method.POST, "https://api-us.faceplusplus.com/facepp/v3/detect",
                     new Response.Listener<NetworkResponse>()
                     {
                         @Override
                         public void onResponse(NetworkResponse netResponse)
                         {
                             String response= new String(netResponse.data);
                             Log.i(TAG, "Capture: " + response);
                             try {
                                 JSONObject jsonObject = new JSONObject(response);
                                 JSONArray faceObject = jsonObject.optJSONArray("faces");
                                 string = "";
                                 finalString = "";
                                 faceCount = 0;
                                 for (int i = 0; i < faceObject.length(); i ++)
                                 {
                                     JSONObject att=jsonObject.optJSONArray("faces").optJSONObject(i).optJSONObject("attributes");
                                     if (att.optJSONObject("gender").getString("value").compareTo("Male") == 0)
                                     {
                                         gender = 0;
                                     } else {
                                         gender = 1;
                                     }
                                     age = att.optJSONObject("age").getInt("value");

                                     string += (" | Gender: " + att.optJSONObject("gender").getString("value") + " | Age: " + age);
                                     faceCount++;
                                 }
                                 finalString += ("Number of face: " + faceCount);
                                 finalString += string;
                                 Log.i("Testing", finalString);
                                 mTextView.setText(finalString);

                             } catch (Exception e)
                             {
                                 Log.i(TAG, e.toString());
                             }

                         }
                     },
                     new Response.ErrorListener()
                     {
                         @Override
                         public void onErrorResponse(VolleyError error)
                         {
                             Log.i(TAG, "Capture: " + error);
                         }
                     })
             {
                 @Override
                 protected Map<String, String> getParams()
                 {
                     Map<String, String> params = new HashMap<String, String>();
                     params.put("api_key", "sLZBKzLUyTQdT3WCnK3hulQ4sAKFelAI");
                     params.put("api_secret", "NVS8_mbFv5uJltszmYkdaqClgnhqgs8T");
                     params.put("return_attributes", "gender,age");
                     return params;
                 }

                 @Override
                 protected Map<String, DataPart> getByteData() {
                     Map<String, DataPart> params = new HashMap<>();
                     params.put("image_file", new DataPart("file_avatar.jpg", bytes, "image/jpeg"));
                     return params;
                 }

             };
             RequestSingleton.getInstance(FaceTrackerActivity.this).addToRequestQueue(strRequest);


             camera.startPreview();
             flagCapture = true;
         }
     };

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mTextView = (TextView) findViewById(R.id.textView);
        captureRunnable= new Runnable() {
            @Override
            public void run() {

                if (flagCapture)
                {
                    try {
                        mPreview.camera.takePicture(null, null, jpegCameraCallback);
                        flagCapture = false;
                    }
                    catch (RuntimeException e)
                    {
                        e.printStackTrace();
                        handler.postDelayed(captureRunnable,1000);
                    }
                }
            }
        };
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(25.0f)
                .build();


    }
   final CameraSource.ShutterCallback shutterCallback= new CameraSource.ShutterCallback() {
       @Override
       public void onShutter() {

       }
   };

    final CameraSource.PictureCallback jpegCallback = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data) {
            // Json request

        }

    };

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
            handler.removeCallbacks(null);
            handler.postDelayed(captureRunnable,1000);

        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults,  Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }


        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
            if (mOverlay.getOverlaySize() > 0)
            {
                handler.removeCallbacks(null);
                handler.postDelayed(captureRunnable,1000);
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText("");
                    }
                });

            }
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
            if (mOverlay.getOverlaySize() > 0)
            {
                handler.removeCallbacks(null);
                handler.postDelayed(captureRunnable,1000);
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText("");
                    }
                });
            }
        }
    }
}
