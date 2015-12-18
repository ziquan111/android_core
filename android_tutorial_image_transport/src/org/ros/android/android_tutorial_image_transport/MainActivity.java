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

import android.graphics.RectF;
import android.os.Bundle;

import com.google.common.collect.Lists;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosAugmentedImageView;
import org.ros.android.view.RosButton;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.ArrayList;

import sensor_msgs.CompressedImage;
import std_msgs.Float32MultiArray;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity {

    private RosAugmentedImageView<CompressedImage, Float32MultiArray, Float32MultiArray> interactiveCameraView;
    private RosButton tlBtn;    //takeoff-land button
    public MainActivity() {
        super("MainActivityTicker", "MainActivityTitle");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        interactiveCameraView = (RosAugmentedImageView<CompressedImage, Float32MultiArray, Float32MultiArray>) findViewById(R.id.camera_view);
        interactiveCameraView.setTopicNames("bebop/image_raw/compressed", "fleye/tld_tracked_object", "fleye/tld_tracked_object");
        interactiveCameraView.setMessageTypes(sensor_msgs.CompressedImage._TYPE, Float32MultiArray._TYPE, Float32MultiArray._TYPE);
        interactiveCameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
        interactiveCameraView.setMessageToPointsCallable(new MessageCallable<float[], Float32MultiArray>() {
            @Override
            public float[] call(Float32MultiArray message) {
                return new float[]{
                        message.getData()[0] + message.getData()[2] / 2f,
                        message.getData()[1] + message.getData()[3] / 2f};
            }
        });
        interactiveCameraView.setMessageToRectsCallable(new MessageCallable<ArrayList<RectF>, Float32MultiArray>() {
            @Override
            public ArrayList<RectF> call(Float32MultiArray message) {
                ArrayList<RectF> result = Lists.newArrayList();
                result.add(new RectF(
                        message.getData()[0],
                        message.getData()[1],
                        message.getData()[0] + message.getData()[2],
                        message.getData()[1] + message.getData()[3]));
                return result;
            }
        });

        tlBtn = (RosButton) findViewById(R.id.takeoff_land_button);
        tlBtn.setTopicName("fleye/takeoff_land");

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
//    NodeConfiguration nodeConfiguration =
//        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
//            getMasterUri());

        NodeConfiguration nodeConfiguration =NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

        nodeMainExecutor.execute(interactiveCameraView, nodeConfiguration.setNodeName("android/video_view"));
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
