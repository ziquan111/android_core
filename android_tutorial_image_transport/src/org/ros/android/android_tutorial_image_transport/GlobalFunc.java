package org.ros.android.android_tutorial_image_transport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.GridView;

import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import std_msgs.Empty;

/**
 * Created by ziquan on 1/23/15.
 */
public class GlobalFunc {

    private final static String SD_CARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private final static String APP_FOLDER = "fleye";
    private final static File APP_DIR = new File(SD_CARD_PATH, APP_FOLDER);
    private static int IMAGE_NAME_COUNT = 0;
    private static Map<Integer, String> names;  // maps from position to name
    private static Map<String, Bitmap> images; // maps from position to image
    private static Map<String, float[]> snapShotInfos; // maps from image_name to snap shot info

    static GridView gridView;

    static {
        if (!APP_DIR.exists())
            APP_DIR.mkdirs();
        names = Maps.newHashMap();
        images = Maps.newHashMap();
        snapShotInfos = Maps.newHashMap();
    }

    static int getImageCount() {
        return IMAGE_NAME_COUNT;
    }

    static String saveImageToExternalStorage(Context context, Bitmap image) {
        try {
            // refer to: http://stackoverflow.com/questions/8182041/android-get-date-and-insert-to-filename
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            Date now = new Date();
            String fileName = APP_DIR + "/" + formatter.format(now) + ".png";

            names.put(IMAGE_NAME_COUNT++, fileName);
            System.out.println("GlobalFunc: IMAGE_NAME_COUNT incremented to " + IMAGE_NAME_COUNT);


            OutputStream fOut = null;
            File file = new File(fileName);
            file.createNewFile();
            fOut = new FileOutputStream(file);

            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);   // 100 means no compression, the lower you go, the stronger the compression
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

            return fileName;

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e("saveImageToExternalStorage()", e.getMessage());
            return null;
        }
    }

    static void saveSnapShotInfo(String imageName, float[] pose) {
        snapShotInfos.put(imageName, pose);
    }

    static float[] getSnapShotInfo(int position) {
        return snapShotInfos.get(names.get(position));
    }

    static String getSnapShotInfoInStr(int position) {
        float[] snapShotInfo = getSnapShotInfo(position);
        String result = "[";
        for (int i = 0; i < snapShotInfo.length; i++) {
            result += snapShotInfo[i];
            result += " ";
        }
        result += "]";
        return result;
    }

    static String getName(int position) {
        return names.get(position);
    }

    static Bitmap loadImageFromExternalStorage(int position, int reqWidth, int reqHeight) {

        if (names.containsKey(position)) {
            // cache
            if (images.containsKey(names.get(position))) {
                return images.get(names.get(position));
            }

            File imgFile = new File(names.get(position));
            if (imgFile.exists()) {
                images.put(names.get(position), decodeSampledBitmapFromFile(names.get(position), reqWidth, reqHeight));
                return images.get(names.get(position));
            }
        }
        return null;
    }

    private static Bitmap decodeSampledBitmapFromFile(String pathName, int reqWidth, int reqHeight) {
        final  BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        return BitmapFactory.decodeFile(pathName, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}