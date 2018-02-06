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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.Face;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private String TAG = "FaceGraphic";

    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;
    private  int gender=-1;
    private  int age=-1;
    private  Context context;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        Log.i("FaceGraphic: " , "Current" );
    }
    final CameraSource.PictureCallback jpegCallback = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data) {
            // Json request
            Log.i(TAG, "onPictureTaken: ");
            StringRequest strRequest = new StringRequest(Request.Method.POST, "https://api-us.faceplusplus.com/facepp/v3/detect",
                    new Response.Listener<String>()
                    {
                        @Override
                        public void onResponse(String response)
                        {
                            Log.i(TAG, "Capture: " + response);

                            // Convert string -> json
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                JSONObject att=jsonObject.optJSONArray("faces").optJSONObject(0).optJSONObject("attributes");
                                if (att.optJSONObject("gender").getString("value").compareTo("Male") == 0)
                                {
                                    gender = 0;
                                } else {
                                    gender = 1;
                                }

                                age = att.optJSONObject("age").getInt("value");

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
                    // get the base 64 string
                    String imgString = Base64.encodeToString(data, Base64.NO_WRAP);
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("api_key", "sLZBKzLUyTQdT3WCnK3hulQ4sAKFelAI");
                    params.put("api_secret", "NVS8_mbFv5uJltszmYkdaqClgnhqgs8T");
                    params.put("image_base64", imgString);
                    params.put("return_attributes", "gender,age");
                    return params;
                }

            };

            RequestSingleton.getInstance(context).addToRequestQueue(strRequest);
        }

    };
    void setId(int id, CameraSource camera, Context ctx) {
        mFaceId = id;
        age = -1;
        gender = -1;
        context=ctx;
        camera.takePicture(null,jpegCallback);
        //chup lay du lieu/
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
        String tmp="";
        if(this.age>-1){
          tmp=" | "+age;
        }
        if(this.gender>-1){
            if(this.gender==0)
                canvas.drawText("M"+tmp, x + ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
            else
                canvas.drawText("F"+tmp, x + ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
        }

//        canvas.drawText("happiness: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
//        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET*2, y - ID_Y_OFFSET*2, mIdPaint);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);
    }
}
