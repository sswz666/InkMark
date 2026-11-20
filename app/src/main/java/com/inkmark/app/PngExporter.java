package com.inkmark.app;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 把标注图层保存成透明 PNG，写到相册 Pictures/墨点。 */
public final class PngExporter {

    private PngExporter() {
    }

    public static boolean save(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        }
        String name = "InkMark_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/墨点");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = context.getContentResolver().insert(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
                if (uri == null) {
                    return false;
                }
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os == null) {
                    return false;
                }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
                return true;
            }

            String path = MediaStore.Images.Media.insertImage(
                    context.getContentResolver(), bitmap, name, "InkMark annotation");
            return path != null;
        } catch (Exception e) {
            return false;
        }
    }
}
