package com.google.android.gms.samples.vision.face.facetracker;

import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.vision.CameraSource;

import java.lang.reflect.Field;

/**
 * Created by computer on 2/7/2018.
 */

public class VisionApiCameraFix {
    /**
     * <p>Returns the supplied {@link com.google.android.gms.vision.CameraSource}'s {@link android.hardware.Camera} instance that is being used.</p>
     * <p>
     * If you want to set any of the camera parameters, here's an example that sets the focus mode to continuous auto focus and enables the flashlight:
     * <blockquote>
     * <code>
     * Camera.Parameters params = camera.getParameters();
     * params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
     * params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
     * camera.setParameters(params);
     * </code>
     * </blockquote>
     * Note that your app might have to hold the permission {@code android.permission.FLASHLIGHT}, so put this in your manifest:
     * <blockquote>
     * <code>
     * &lt;uses-permission android:name="android.permission.FLASHLIGHT" /&gt;
     * </code>
     * </blockquote>
     * </p>
     * <p>
     * Also note that the CameraSource's {@link CameraSource#start()} or
     * {@link CameraSource#start(SurfaceHolder)} has to be called and the camera image has to be
     * showing prior using this method as the CameraSource only creates the camera after calling
     * one of those methods and the camera is not available immediately. You could implement some
     * kind of a callback method for the SurfaceHolder that notifies you when the imaging is ready
     * or use a direct action (e.g. button press) to get the camera and change its parameters.
     * </p>
     * <p>
     * Check out <a href="https://github.com/googlesamples/android-vision/blob/master/face/multi-tracker/app/src/main/java/com/google/android/gms/samples/vision/face/multitracker/ui/camera/CameraSourcePreview.java#L84">CameraSourcePreview.java</a>
     * which contains the method <code>startIfReady()</code> that has the following line:
     * <blockquote><code>mCameraSource.start(mSurfaceView.getHolder());</code></blockquote><br>
     * After this call you can use our <code>cameraFocus(...)</code> method because the camera is ready.
     * </p>
     * <p>
     *     <b>DO NOT</b> set the following parameters using this instance as it might break the detectors:
     *     <ul>
     *         <li>picture size (<code>setPictureSize(...)</code>)</li>
     *         <li>preview size (<code>setPreviewSize(...)</code>)</li>
     *         <li>preview FPS range (<code>setPreviewFpsRange(...)</code>)</li>
     *         <li>preview format (<code>setPreviewFormat(...)</code>)</li>
     *         <li>rotation (<code>setRotation(...)</code>)</li>
     *     </ul>
     * </p>
     *
     * @param cameraSource The CameraSource built with {@link com.google.android.gms.vision.CameraSource.Builder}.
     * @return the actual {@link android.hardware.Camera} instance used by the supplied {@link com.google.android.gms.vision.CameraSource}, or {@code null} on failure.
     * @see android.hardware.Camera
     */
    public static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    Log.d("TAG","ssss");
                    if (camera != null) {
                        Log.d("TAG","NOT NULL");
                        return camera;
                    }

                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        return null;
    }
}
