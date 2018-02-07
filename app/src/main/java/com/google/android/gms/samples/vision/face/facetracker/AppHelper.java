package com.google.android.gms.samples.vision.face.facetracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;

import java.io.ByteArrayOutputStream;

/**
 * Created by computer on 2/7/2018.
 */

public class AppHelper {
    public static Bitmap decodeByteArray(Bitmap o, int top, int left, int w, int  h ){
//        Bitmap o= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        int right= Math.min(o.getWidth(),w+left) ;
        int bottom=Math.min(o.getHeight(),h+top);

        if (left < 0)
        {
            left = 0;
        }
        if (top < 0)
        {
            top = 0;
        }

        return Bitmap.createBitmap(o,left,top,right-left,bottom-top);
    }

    public  static byte[] toByteArray(Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }
}

