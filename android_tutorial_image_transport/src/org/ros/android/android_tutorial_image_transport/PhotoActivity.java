package org.ros.android.android_tutorial_image_transport;


import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.RosButton;
//import org.ros.android.view.RosRetakeButton;
import org.ros.android.view.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.concurrent.Callable;

import std_msgs.Int32;

/**
 * Created by ziquan on 3/2/15.
 */
public class PhotoActivity  extends RosActivity {

    //    private RosImageView<CompressedImage> image;
    private RosTextView<Int32> batteryPercent;
    private RosButton tlBtn;// sprBtn;
    private ImageView photo;
    private RosButton restoreBtn;
//    private TextView photoDirectory;

    private int photo_id;

    public PhotoActivity() {
        super("PhotoTicker", "PhotoTitle");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo);

//        batteryPercent = (RosTextView<Int32>) findViewById(R.id.rosTextView_batteryPercent);
//        batteryPercent.setTopicName("bebop/battery");
//        batteryPercent.setMessageType(Int32._TYPE);
//        batteryPercent.setMessageToStringCallable(new MessageCallable<String, Int32>() {
//            @Override
//            public java.lang.String call(Int32 message) {
//                return "Battery : " + (int)message.getData() + "%";
//            }
//        });

//        tlBtn = (RosButton) findViewById(R.id.takeoff_land_button);
//        tlBtn.setTopicName("fleye/takeoff_land");
//        tlBtn.setId('l');
//        tlBtn.addResource("takeoff", R.drawable.ic_flight_takeoff_white_24dp);
//        tlBtn.addResource("land", R.drawable.ic_flight_land_white_24dp);

//        sprBtn = (RosButton) findViewById(R.id.button_start_pause_resume);
//        sprBtn.setTopicName("fleye/start_pause_resume");
//        sprBtn.setIds('r', 'R');

//        tlBtn.setResourceName(getIntent().getExtras().getString("tl"));
//        sprBtn.setText(getIntent().getExtras().getString("spr"));
//        sprBtn.setVisibility(getIntent().getExtras().getBoolean("isSprVisible") ? View.VISIBLE : View.INVISIBLE);

//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//                Toast.makeText(GalleryActivity.this, "" + position, Toast.LENGTH_SHORT).show();
//            }
//        });

        photo = (ImageView) findViewById(R.id.photo);
        photo.setImageBitmap(GlobalFunc.loadImageFromExternalStorage(getIntent().getExtras().getInt("position"), photo.getWidth(), photo.getHeight()));
        photo.setOnTouchListener(new PhotoNavigationGestureListener(this) {
            public void onSwipeTop() {
//                Toast.makeText(PhotoActivity.this, "top", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeRight() {
//                Toast.makeText(PhotoActivity.this, "right", Toast.LENGTH_SHORT).show();
                photo_id = Math.max(0, Math.min(GlobalFunc.getImageCount() - 1, photo_id - 1));
//                photoDirectory.setText(GlobalFunc.getName(photo_id));
                photo.setImageBitmap(GlobalFunc.loadImageFromExternalStorage(photo_id, photo.getWidth(), photo.getHeight()));
            }

            public void onSwipeLeft() {
//                Toast.makeText(PhotoActivity.this, "left", Toast.LENGTH_SHORT).show();
                photo_id = Math.max(0, Math.min(GlobalFunc.getImageCount() - 1, photo_id + 1));
//                photoDirectory.setText(GlobalFunc.getName(photo_id));
                photo.setImageBitmap(GlobalFunc.loadImageFromExternalStorage(photo_id, photo.getWidth(), photo.getHeight()));
            }

            public void onSwipeBottom() {
//                Toast.makeText(PhotoActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }
        });


        restoreBtn = (RosButton) findViewById(R.id.restore_button);
        restoreBtn.setTopicName("fleye/restore");
        restoreBtn.addResource("restore", R.drawable.ic_restore_white_48dp, 'R');
        restoreBtn.setCallable(new Callable<float[]>() {
            @Override
            public float[] call() throws Exception {
                float[] snapShotInfo = GlobalFunc.getSnapShotInfo(photo_id);
                System.out.println("restoreBtn setCallable: photo_id pose is " + GlobalFunc.getSnapShotInfoInStr(photo_id));
                if (snapShotInfo.length != 0) {
                    float[] result = new float[snapShotInfo.length + 1];
                    result[0] = 'R';
                    for (int i = 0; i < snapShotInfo.length; i++) {
                        result[i + 1] = snapShotInfo[i];
                        System.out.println("snapShotInfo[" + i + "] is " + snapShotInfo[i]);
                    }
                    return result;
                } else {
                    System.out.println("===========================================Q");
                    return new float[]{'Q'};    // no-op action
                }
            }
        });
        restoreBtn.setRunnable(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                intent.putExtra("tl", tlBtn.getResourceName());
                startActivity(intent);
            }
        });

//        photoDirectory = (TextView) findViewById(R.id.photo_directory);
//        photoDirectory.setText(GlobalFunc.getName(photo_id));

        if (nodeMainExecutorService != null) {
            System.out.println("PhotoActivity nodeMainExecutorService is not null");
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                            getMasterUri());
//            nodeMainExecutorService.execute(batteryPercent, nodeConfiguration.setNodeName("android/batteryPercent3"));
//            nodeMainExecutorService.execute(tlBtn, nodeConfiguration.setNodeName("android/takeoff_land_button_in_photo_view"));
            nodeMainExecutorService.execute(restoreBtn, nodeConfiguration.setNodeName("android/restore_button"));
        }

        super.splashScreen();
    }

    public void mainViewClicked(View unused) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        intent.putExtra("tl", tlBtn.getResourceName());
//        intent.putExtra("spr", sprBtn.getText());
//        intent.putExtra("isSprVisible", sprBtn.getVisibility() == View.VISIBLE);
        startActivity(intent);
    }

    public void backButtonOnClicked(View unused) {
        Intent intent = new Intent(getApplicationContext(), GalleryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        intent.putExtra("tl", tlBtn.getResourceName());
        startActivity(intent);
    }

    public void restoreOnClicked(View unused) {
        Toast.makeText(PhotoActivity.this, GlobalFunc.getSnapShotInfoInStr(photo_id), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        return;
    }

    @Override
    public void onResume() {
        super.onResume();
        super.splashScreen();
//        tlBtn.setResourceName(getIntent().getExtras().getString("tl"));
        photo_id = getIntent().getExtras().getInt("photo");
        System.out.println("PhotoActivity: onResume: photo_id is " + photo_id);
        photo.setImageBitmap(GlobalFunc.loadImageFromExternalStorage(photo_id, photo.getWidth(), photo.getHeight()));
//        photoDirectory.setText(GlobalFunc.getName(photo_id) + GlobalFunc.getSnapShotInfoInStr(photo_id));
//        sprBtn.setText(getIntent().getExtras().getString("spr"));
//        sprBtn.setVisibility(getIntent().getExtras().getBoolean("isSprVisible") ? View.VISIBLE : View.INVISIBLE);
    }
}
