/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_tutorial_image_transport;

import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Maps;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosAugmentedImageView;
import org.ros.android.view.RosButton;
import org.ros.android.view.RosShootButton;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.HashMap;

import sensor_msgs.CompressedImage;
import sensor_msgs.PointCloud;
import std_msgs.Float32MultiArray;


/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity {

    private RosAugmentedImageView<CompressedImage, Float32MultiArray, PointCloud, PointCloud> interactiveCameraView;
    private RosButton tlBtn;    //takeoff-land button
    private RosButton panoBtn, orbitBtn, zigzagBtn; //start orbiting button
    private RosShootButton shootBtn;  //shoot button
    private ImageView galleryBtn;

    private View pano_toast_view, orbit_toast_view, zigzag_toast_view;
    private Toast pano_toast, orbit_toast, zigzag_toast;

    private FleyeGestureListener fleyeGestureListener;

    public MainActivity() {
        super("MainActivityTicker", "MainActivityTitle");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        fleyeGestureListener = new FleyeGestureListener(this);

        interactiveCameraView = (RosAugmentedImageView<CompressedImage, Float32MultiArray, PointCloud, PointCloud>) findViewById(R.id.camera_view);
        interactiveCameraView.setTopicNames("bebop/image_raw/compressed", "fleye/keypoints", "fleye/targets", "fleye/targets");
        interactiveCameraView.setMessageTypes(sensor_msgs.CompressedImage._TYPE, Float32MultiArray._TYPE, PointCloud._TYPE, PointCloud._TYPE);
        interactiveCameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
        interactiveCameraView.setMessageToPointsCallable(new MessageCallable<float[], Float32MultiArray>() {
            @Override
            public float[] call(Float32MultiArray message) {
                return message.getData();
            }
        });
        interactiveCameraView.setMessageToLinesCallable(new MessageCallable<float[], PointCloud>() {
            @Override
            public float[] call(PointCloud message) {
                float[] channel_u = message.getChannels().get(2).getValues();
                float[] channel_v = message.getChannels().get(3).getValues();
                float[] channel_tu = message.getChannels().get(7).getValues();
                float[] channel_tv = message.getChannels().get(8).getValues();

                float[] result = new float[4 * channel_tu.length];
                for (int i = 0; i < channel_tu.length; i++) {
                    result[4 * i] = channel_u[i];
                    result[4 * i + 1] = channel_v[i];
                    result[4 * i + 2] = channel_tu[i];
                    result[4 * i + 3] = channel_tv[i];
                }
                return result;
            }
        });
        interactiveCameraView.setMessageToRectsCallable(new MessageCallable<HashMap<Integer, Pair<RectF, Integer>>, PointCloud>() {
            @Override
            public HashMap<Integer, Pair<RectF, Integer>> call(PointCloud message) {
                HashMap<Integer, Pair<RectF, Integer>> result = Maps.newHashMap();

                float[] channel_id = message.getChannels().get(1).getValues();
                float[] channel_u = message.getChannels().get(2).getValues();
                float[] channel_v = message.getChannels().get(3).getValues();
                float[] channel_w = message.getChannels().get(4).getValues();
                float[] channel_h = message.getChannels().get(5).getValues();
                float[] channel_c = message.getChannels().get(6).getValues();

                for (int i = 0; i < channel_id.length; i++) {
                    RectF rectF = new RectF(channel_u[i] - channel_w[i], channel_v[i] - channel_h[i], channel_u[i] + channel_w[i], channel_v[i] + channel_h[i]);
                    result.put((int) channel_id[i], new Pair<>(rectF, (int) channel_c[i]));
                }
                return result;
            }
        });
        interactiveCameraView.setOnTouchListener(fleyeGestureListener);

        galleryBtn = (ImageView) findViewById(R.id.gallery_button);

        tlBtn = (RosButton) findViewById(R.id.takeoff_land_button);
        tlBtn.setTopicName("fleye/takeoff_land");
//        tlBtn.setId('l');
        tlBtn.addResource("takeoff", R.drawable.ic_flight_takeoff_white_24dp, 't');
        tlBtn.addResource("land", R.drawable.ic_flight_land_white_24dp, 'l');
        tlBtn.setResourceName("takeoff");   // set takeoff as default

        panoBtn = (RosButton) findViewById(R.id.pano_button);
        panoBtn.setTopicName("fleye/pano");
        panoBtn.addResource("white", R.drawable.ic_panorama_horizontal_white_18dp, 'P');
        panoBtn.addResource("grey", R.drawable.ic_panorama_horizontal_grey_18dp, '9');
        panoBtn.addResource("yellow", R.drawable.ic_panorama_horizontal_yellow_18dp, 'p');
        panoBtn.setRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        TextView text = (TextView) pano_toast_view.findViewById(R.id.toast_text);
                        if (panoBtn.getResourceName().equals("white")) {
                            text.setText("Pano Start");
                            text.setTextColor(getResources().getColor(R.color.toast_yellow));
                        } else if (panoBtn.getResourceName().equals("yellow")) {
                            text.setText("Pano End");
                            text.setTextColor(getResources().getColor(R.color.toast_white));
                        } else {
                            return;
                        }

                        pano_toast.show();
                    }
                }
        );

        orbitBtn = (RosButton) findViewById(R.id.orbit_button);
        orbitBtn.setTopicName("fleye/orbit");
        orbitBtn.addResource("white", R.drawable.ic_orbit_white_18dp, 'O');
        orbitBtn.addResource("grey", R.drawable.ic_orbit_grey_18dp, '0');
        orbitBtn.addResource("yellow", R.drawable.ic_orbit_yellow_18dp, 'o');
        orbitBtn.setRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        TextView text = (TextView) orbit_toast_view.findViewById(R.id.toast_text);
                        if (orbitBtn.getResourceName().equals("white")) {
                            text.setText("Orbit Start");
                            text.setTextColor(getResources().getColor(R.color.toast_yellow));
                        } else if (orbitBtn.getResourceName().equals("yellow")) {
                            text.setText("Orbit End");
                            text.setTextColor(getResources().getColor(R.color.toast_white));
                        } else {
                            return;
                        }

                        orbit_toast.show();
                    }
                }
        );

        zigzagBtn = (RosButton) findViewById(R.id.zigzag_button);
        zigzagBtn.setTopicName("fleye/zigzag");
        zigzagBtn.addResource("white", R.drawable.ic_zigzag_white_18dp, 'Z');
        zigzagBtn.addResource("grey", R.drawable.ic_zigzag_grey_18dp, '2');
        zigzagBtn.addResource("yellow", R.drawable.ic_zigzag_yellow_18dp, 'z');
        zigzagBtn.setRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        TextView text = (TextView) zigzag_toast_view.findViewById(R.id.toast_text);
                        if (zigzagBtn.getResourceName().equals("white")) {
                            text.setText("Zigzag Start");
                            text.setTextColor(getResources().getColor(R.color.toast_yellow));
                        } else if (zigzagBtn.getResourceName().equals("yellow")) {
                            text.setText("Zigzag End");
                            text.setTextColor(getResources().getColor(R.color.toast_white));
                        } else {
                            return;
                        }

                        zigzag_toast.show();
                    }
                }
        );

        shootBtn = (RosShootButton) findViewById(R.id.shoot_button);
        shootBtn.setRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        String fileName = GlobalFunc.saveImageToExternalStorage(MainActivity.this, shootBtn.getScreenShot());
                        System.out.println("fileName is " + fileName);
                        if (fileName != null) {
                            GlobalFunc.saveSnapShotInfo(fileName, shootBtn.getSnapShotInfo());
//                            for (float f : shootBtn.getSnapShotInfo()) {
//                                System.out.print("\t" + f);
//                            }
//                            System.out.println();
                            galleryBtn.setImageBitmap(GlobalFunc.loadImageFromExternalStorage(GlobalFunc.getImageCount() - 1, 400, 225));
                        }
                        if (GlobalFunc.gridView != null) {
                            ((ImageAdapter) GlobalFunc.gridView.getAdapter()).notifyDataSetChanged();
                        }
                    }
                }
        );

        // toast
        LayoutInflater inflater = getLayoutInflater();
        pano_toast_view = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout_root));
        pano_toast = new Toast(getApplicationContext());
        pano_toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 2 * (int) (getResources().getDimension(R.dimen.button_width)), 0);
        pano_toast.setDuration(Toast.LENGTH_SHORT);
        pano_toast.setView(pano_toast_view);

        orbit_toast_view = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout_root));
        orbit_toast = new Toast(getApplicationContext());
        orbit_toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, (int) (getResources().getDimension(R.dimen.button_width)), 0);
        orbit_toast.setDuration(Toast.LENGTH_SHORT);
        orbit_toast.setView(orbit_toast_view);

        zigzag_toast_view = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout_root));
        zigzag_toast = new Toast(getApplicationContext());
        zigzag_toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 0, 0);
        zigzag_toast.setDuration(Toast.LENGTH_SHORT);
        zigzag_toast.setView(zigzag_toast_view);
    }

    public void galleryViewClicked(View unused) {
        Intent intent = new Intent(getApplicationContext(), GalleryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        intent.putExtra("tl", tlBtn.getResourceName());
//        intent.putExtra("spr", sprBtn.getText());
//        intent.putExtra("isSprVisible", sprBtn.getVisibility() == View.VISIBLE);
        startActivity(intent);
        overridePendingTransition(0,0);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
//    NodeConfiguration nodeConfiguration =
//        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
//            getMasterUri());

        NodeConfiguration nodeConfiguration =NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

        nodeMainExecutor.execute(interactiveCameraView, nodeConfiguration.setNodeName("android/video_view"));
        nodeMainExecutor.execute(tlBtn, nodeConfiguration.setNodeName("android/takeoff_land_button"));
        nodeMainExecutor.execute(panoBtn, nodeConfiguration.setNodeName("android/pano_button"));
        nodeMainExecutor.execute(orbitBtn, nodeConfiguration.setNodeName("android/orbit_button"));
        nodeMainExecutor.execute(zigzagBtn, nodeConfiguration.setNodeName("android/zigzag_button"));
        nodeMainExecutor.execute(shootBtn, nodeConfiguration.setNodeName("android/shoot_button"));
        nodeMainExecutor.execute(fleyeGestureListener, nodeConfiguration.setNodeName("android/gesture"));
    }

    @Override
    public void onResume() {
        super.onResume();
        super.splashScreen();
//    if (getIntent().getExtras() != null) {
//      tleBtn.setResourceName(getIntent().getExtras().getString("tle"));
////            sprBtn.setText(getIntent().getExtras().getString("spr"));
////            sprBtn.setVisibility(getIntent().getExtras().getBoolean("isSprVisible") ? View.VISIBLE : View.INVISIBLE);
//    }
    }
}
