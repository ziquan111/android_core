package org.ros.android.android_tutorial_image_transport;


import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosButton;
//import org.ros.android.view.RosHybridImageView;
import org.ros.android.view.RosImageView;
import org.ros.android.view.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import sensor_msgs.CompressedImage;
import std_msgs.Float32;
import std_msgs.Int32;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class GalleryActivity extends RosActivity {

    //    private RosImageView<CompressedImage> image;
    private RosTextView<Int32> batteryPercent;
    private RosButton tlBtn;// sprBtn;

    public GalleryActivity() {
        super("GalleryTicker", "GalleryTitle");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

//        image = (RosImageView<CompressedImage>) findViewById(R.id.rosImageView_controlMode);
//        image.setTopicName("/ardrone/image_raw/compressed");
//        image.setMessageType(CompressedImage._TYPE);
//        image.setMessageToBitmapCallable(new BitmapFromCompressedImage());

//        batteryPercent = (RosTextView<Int32>) findViewById(R.id.rosTextView_batteryPercent);
//        batteryPercent.setTopicName("bebop/battery");
//        batteryPercent.setMessageType(Int32._TYPE);
//        batteryPercent.setMessageToStringCallable(new MessageCallable<String, Int32>() {
//            @Override
//            public java.lang.String call(Int32 message) {
//               return "Battery : " + (int)message.getData() + "%";
//            }
//        });


//        tlBtn = (RosButton) findViewById(R.id.takeoff_land_button);
//        tlBtn.setTopicName("fleye/takeoff_land");
//        tlBtn.setId('l');
//        tlBtn.addResource("takeoff", R.drawable.ic_flight_takeoff_white_24dp);
//        tlBtn.addResource("land", R.drawable.ic_flight_land_white_24dp);
//        tlBtn.setResourceName(getIntent().getExtras().getString("tl"));

        GlobalFunc.gridView = (GridView) findViewById(R.id.grid_view);
        GlobalFunc.gridView.setAdapter(new ImageAdapter(this));

//         click to photo view
        GlobalFunc.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//                Toast.makeText(GalleryActivity.this, "" + position, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), PhotoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                intent.putExtra("tl", tlBtn.getResourceName());
                intent.putExtra("photo", position);
                System.out.println("GalleryActivity: gridView.setOnItemClickListener: intent:" + intent);
                System.out.println("GalleryActivity: gridView.setOnItemClickListener: photo position is " + position);
//              intent.putExtra("spr", sprBtn.getText());
//              intent.putExtra("isSprVisible", sprBtn.getVisibility() == View.VISIBLE);
                startActivity(intent);
            }
        });

        if (nodeMainExecutorService != null) {
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                            getMasterUri());
//            nodeMainExecutorService.execute(batteryPercent, nodeConfiguration.setNodeName("android/batteryPercent2"));
//            nodeMainExecutorService.execute(tlBtn, nodeConfiguration.setNodeName("android/takeoff_land_button_in_gallery_view"));
        }

        super.splashScreen();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void mainViewClicked(View unused) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        intent.putExtra("tl", tlBtn.getResourceName());
//        intent.putExtra("spr", sprBtn.getText());
//        intent.putExtra("isSprVisible", sprBtn.getVisibility() == View.VISIBLE);
        startActivity(intent);
    }

//    public void shootButtonClicked(View unused) {
//        GlobalFunc.saveImageToExternalStorage(this, ((BitmapDrawable)image.getDrawable()).getBitmap());
//        ((ImageAdapter)GlobalFunc.gridView.getAdapter()).notifyDataSetChanged();
//    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) { return ;}

    @Override
    public void onResume() {
        super.onResume();
        super.splashScreen();
//        tlBtn.setResourceName(getIntent().getExtras().getString("tl"));
//        sprBtn.setText(getIntent().getExtras().getString("spr"));
//        sprBtn.setVisibility(getIntent().getExtras().getBoolean("isSprVisible") ? View.VISIBLE : View.INVISIBLE);
    }
}
