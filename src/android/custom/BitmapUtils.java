package org.apache.cordova.camera.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;

public class BitmapUtils {

    private BitmapUtils() {
    }

    /**
     * Save image to device gallery
     *
     * @param context
     * @param bitmap
     */
    public static void saveBitmap(Context context, Bitmap bitmap) {

        File sdcard = Environment.getExternalStorageDirectory();
        boolean canWrite = (sdcard != null);

        if (canWrite) {
            File mediaDir = new File(sdcard, "DCIM/Camera/");
            if (!mediaDir.exists()) {
                canWrite = mediaDir.mkdirs();
            }
        }

        if (canWrite) {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap,
                    "Titulo", null);
        }
    }

    /**
     * Set density resolution in metadata from JPEG 13 - dpi 14 e 15 -
     * xResolution 16 e 17 - yResolution
     *
     * @param imageData
     * @param dpi
     */
    public static void setDpi(byte[] imageData, int dpi) {
        imageData[13] = 1;
        imageData[14] = (byte) (dpi >> 8);
        imageData[15] = (byte) (dpi & 0xff);
        imageData[16] = (byte) (dpi >> 8);
        imageData[17] = (byte) (dpi & 0xff);
    }

    /**
     * Returns a {@link Bitmap} from a {@link Uri}
     *
     * @param context
     * @param uri
     * @return
     * @throws IOException
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    /**
     * Rotates the bitmap according to rotation parameter
     *
     * @param source
     * @param rotation
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap source, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Transorm a square bitmap image into a rounded iamge
     *
     * @param bitmap
     * @return
     */
    public static Bitmap drawAsRoundedCornerImage(final Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width < height) {

            // Rounding for portrait images

            Bitmap output = Bitmap.createBitmap(width,
                    height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, width, height);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawCircle(width / 2, height / 2,
                    width / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;

        } else {

            // Rounding for landscape images

            final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            final Path path = new Path();
            path.addCircle(
                    (float) (width / 2)
                    , (float) (height / 2)
                    , (float) Math.min(width, (height / 2))
                    , Path.Direction.CCW);

            final Canvas canvas = new Canvas(outputBitmap);
            canvas.clipPath(path);
            canvas.drawBitmap(bitmap, 0, 0, null);
            return outputBitmap;
        }

    }
}
