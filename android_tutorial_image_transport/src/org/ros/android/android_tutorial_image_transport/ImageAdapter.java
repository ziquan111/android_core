package org.ros.android.android_tutorial_image_transport;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Created by ziquan on 1/23/15.
 */
public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private final int width;
    private final int height;

    public ImageAdapter(Context c) {
        mContext = c;
        width = 480;
        height = 276;
    }

    @Override
    public int getCount() {
        return GlobalFunc.getImageCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(480, 300)); // in px, not in dp
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(0,0,0,24);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageBitmap(GlobalFunc.loadImageFromExternalStorage(position, this.width, this.height));
        return imageView;
    }
}