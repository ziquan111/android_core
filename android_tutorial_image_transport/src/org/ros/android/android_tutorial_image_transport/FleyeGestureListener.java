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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;

import std_msgs.*;
import std_msgs.String;

import sensor_msgs.PointCloud;

/**
 * Created by ziquan on 1/29/15.
 *
 * Message meaning
 *
 */
public class FleyeGestureListener implements View.OnTouchListener, NodeMain {

    private Context context;

    private GestureDetectorCompat gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private ArrayList<float[]> trace;
    private Publisher<Float32MultiArray> gesturePublisher;
    private Publisher<String> orbitPubliser;

    private float scaleFactor;
    private float scaleSpanX, scaleSpanY, scaleSpan;
    private boolean isMultifingers;
    private int track_id;

    private Toast toast;

    private boolean isLongPress;
    private CountDownTimer timerLongPress;

    private CountDownTimer timerScaleOrScroll;
    private boolean isScalingProtected;

    private boolean canUnderstand;

    private ArrayList<MyTarget> targets;
    private boolean isDirectManipulation, isScaling, isScrolling;
    private int targetIdBeingManipulated;

    class MyTarget {
        int id = -1;
        RectF rect;

        MyTarget(float id, float left, float top, float right, float bottom) {
            this.id = (int)id;
            this.rect = new RectF(left, top, right, bottom);
        }
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (isDirectManipulation) {
                Float32MultiArray array = gesturePublisher.newMessage();
                array.setData(new float[]{'V', targetIdBeingManipulated});
                gesturePublisher.publish(array);
                toast.cancel();
                toast = Toast.makeText(context, "Confirm target " + targetIdBeingManipulated
                        , Toast.LENGTH_SHORT);
                toast.show();
                return true;
            }
            return false;
        }

//        @Override
//        public boolean onDoubleTap(MotionEvent e) {
//            if (isDirectManipulation) {
//                Float32MultiArray array = gesturePublisher.newMessage();
//                array.setData(new float[]{'N', targetIdBeingManipulated});
//                gesturePublisher.publish(array);
//                toast.cancel();
//                toast = Toast.makeText(context, "Reduce target priority" + targetIdBeingManipulated
//                        , Toast.LENGTH_SHORT);
//                toast.show();
//                return true;
//            }
//            return false;
//        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isScalingProtected || isScaling)
                return false;

            isScrolling = true;

            int index1 = MotionEventCompat.getActionIndex(e1);
            float x1 = MotionEventCompat.getX(e1, index1);
            float y1 = MotionEventCompat.getY(e1, index1);

            int index2 = MotionEventCompat.getActionIndex(e2);
            float x2 = MotionEventCompat.getX(e2, index2);
            float y2 = MotionEventCompat.getY(e2, index2);

            if (trace.isEmpty())
                trace.add(new float[]{x1, y1});
            trace.add(new float[]{x2, y2});

            if (isDirectManipulation) {
                Float32MultiArray array = gesturePublisher.newMessage();
                array.setData(new float[]{'V', targetIdBeingManipulated, getDroneImageXFromTouchXInPx(x2), getDroneImageYFromTouchYInPx(y2)});
                gesturePublisher.publish(array);
                toast.cancel();
                toast = Toast.makeText(context, "Composing target " + targetIdBeingManipulated + " to " + getDroneImageXFromTouchXInPx(x2) + ", " + getDroneImageYFromTouchYInPx(y2)
                        , Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Float32MultiArray array = gesturePublisher.newMessage();
                array.setData(new float[]{(isMultifingers ? 'I' : 'i'),  getDistanceRatioAlongXFromTouchXInPx(x2 - x1), getDistanceRatioAlongYFromTouchYInPx(y2 - y1)});
                gesturePublisher.publish(array);
                toast.cancel();
                toast = Toast.makeText(context, (isMultifingers ? "Doing pan-tilt" : "Moving around") //+ ": " + getDistanceRatioAlongXFromTouchXInPx(x2 - x1) + ", " + getDistanceRatioAlongYFromTouchYInPx(y2 - y1)
                        , Toast.LENGTH_SHORT);
                toast.show();
            }
            return true;
        }
    }

    class MyScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!isScalingProtected && !isScaling)
                return false;

            isScaling = true;

            scaleFactor *= detector.getScaleFactor();
            scaleSpanX = detector.getCurrentSpanX();
            scaleSpanY = detector.getCurrentSpanY();
            scaleSpan = detector.getCurrentSpan();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            System.out.println("scaleFactor, focusX, focusY " + scaleFactor + ", " + focusX + ", " + focusY);


            Float32MultiArray array = gesturePublisher.newMessage();

            array.setData(new float[]{'Z', scaleFactor});

            gesturePublisher.publish(array);


            toast.cancel();
            toast = Toast.makeText(context, "Zooming: " + (scaleFactor > 1? "in" : "out"), Toast.LENGTH_SHORT);
            toast.show();

            return true;
        }
    }

    public FleyeGestureListener(final Context context) {
        gestureDetector = new GestureDetectorCompat(context, new MyGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new MyScaleGestureListener());
        scaleGestureDetector.setQuickScaleEnabled(true);
        trace = new ArrayList<float[]>();
        targets = new ArrayList<MyTarget>();
        this.context = context;

        isLongPress = false;
        timerLongPress = new CountDownTimer(500, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
//                toast.cancel();
//                toast = Toast.makeText(context, "counting down", Toast.LENGTH_SHORT);
//                toast.show();
                return;
            }

            @Override
            public void onFinish() {
                isLongPress = true;
                toast.cancel();
                toast = Toast.makeText(context, "1. Circle your target\n2. Draw a stroke to explore", Toast.LENGTH_LONG);
                toast.show();
////                System.out.println("timer finish");
////                parseGesture();
            }
        };
        timerScaleOrScroll = new CountDownTimer(200, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                return;
            }

            @Override
            public void onFinish() {
                isScalingProtected = false;
            }
        };
        isScalingProtected = false;


        isDirectManipulation = false;
        targetIdBeingManipulated = -1;
        isScrolling = false;
        isScaling = false;

        toast = Toast.makeText(context, "Press and hold to start", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        int index = MotionEventCompat.getActionIndex(event);
        int id = MotionEventCompat.getPointerId(event, index);

        if (scaleGestureDetector.onTouchEvent(event)) {

        }

        if (gestureDetector.onTouchEvent(event)) {

        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                reset();
//                track_id = id;
                float pointerX = MotionEventCompat.getX(event, index);
                float pointerY = MotionEventCompat.getY(event, index);
                float droneImageXFromTouchXInPx = getDroneImageXFromTouchXInPx(pointerX);
                float droneImageYFromTouchYInPx = getDroneImageYFromTouchYInPx(pointerY);
                timerLongPress.start();
//                trace.add(new float[]{pointerX, pointerY});
                // long press on the target rect
//                System.out.println("x " + pointerX + "\ty:" + pointerY + "\t");
//                if (targetRect != null) {
//                    System.out.println("left, right, top, bottom " + targetRect.left + "\t" + targetRect.right + "\t" + targetRect.top + "\t" + targetRect.bottom);
//                }

                if (!targets.isEmpty()) {
                    for (MyTarget target : targets) {
                        if (droneImageXFromTouchXInPx > target.rect.left && droneImageXFromTouchXInPx < target.rect.right && droneImageYFromTouchYInPx > target.rect.top && droneImageYFromTouchYInPx < target.rect.bottom) {
                            isDirectManipulation = true;
                            targetIdBeingManipulated = target.id;
                        }

                        if (distanceBetween(new float[] {droneImageXFromTouchXInPx, droneImageYFromTouchYInPx}, new float[]{target.rect.right, target.rect.top}) < 15f) {
                            Float32MultiArray array = gesturePublisher.newMessage();
                            array.setData(new float[]{'N', target.id});
                            gesturePublisher.publish(array);

                            toast.cancel();
                            toast = Toast.makeText(context, "Target " + target.id + " canceled", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }

//                if (targetRect != null)
//                    System.out.println(getDroneImageXFromTouchXInPx(pointerX) + "\t" + getDroneImageYFromTouchYInPx(pointerY) + "\t" + targetRect.right + "\t" + targetRect.top);
//                System.out.println("touch at [x,y]=" + pointerX + "\t" + pointerY);
//                float droneImageX = getDroneImageXFromTouchXInPx(pointerX);
//                float droneImageY = getDroneImageYFromTouchYInPx(pointerY);
//                System.out.println("droneImageX, droneImageY = " + droneImageX + "\t" + droneImageY);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                isLongPress = false;
                timerLongPress.cancel();

                if (!isScrolling)
                    isMultifingers = true;

                timerScaleOrScroll.start();
                isScalingProtected = true;

                isDirectManipulation = false;
                return true;
//            case MotionEvent.ACTION_MOVE:
//                if (!isLongPress && track_id == id) {
//                    trace.add(new float[]{MotionEventCompat.getX(event, index), MotionEventCompat.getY(event, index)});
//                    timerLongPress.cancel();
//                    timerLongPress.start();
//                }
//                return true;
            case MotionEvent.ACTION_MOVE:
                if (isLongPress) {
                    trace.add(new float[]{MotionEventCompat.getX(event, index), MotionEventCompat.getY(event, index)});
                } else {
                    isLongPress = false;
                    timerLongPress.cancel();
                }
                return true;
//                if (isDirectManipulation && track_id == id) {
//                    trace.add(new float[]{MotionEventCompat.getX(event, index), MotionEventCompat.getY(event, index)});
//                }
//                return true;
            case MotionEvent.ACTION_UP:
                // send one time gesture event
                if (!isScrolling)
                    parseGesture();
//                if (!isLongPress) {
//                    parseGesture();
//                }
//                // stop the long press event
//                else if (canUnderstand) {
                Float32MultiArray array = gesturePublisher.newMessage();
                array.setData(new float[] {'Q'});
                gesturePublisher.publish(array);
//                }
                reset();
                return true;
        }
        return false;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fleye_gesture");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        gesturePublisher = connectedNode.newPublisher("fleye/gesture", Float32MultiArray._TYPE);
        orbitPubliser = connectedNode.newPublisher("fleye/orbit", String._TYPE);
        // the subscriber below is added for dragging
        Subscriber<PointCloud> subscriber = connectedNode.newSubscriber("fleye/targets", PointCloud._TYPE);
        subscriber.addMessageListener(new MessageListener<PointCloud>() {
            @Override
            public void onNewMessage(final PointCloud message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        targets.clear();

                        float[] channel_id = message.getChannels().get(1).getValues();
                        float[] channel_u = message.getChannels().get(2).getValues();
                        float[] channel_v = message.getChannels().get(3).getValues();
                        float[] channel_w = message.getChannels().get(4).getValues();
                        float[] channel_h = message.getChannels().get(5).getValues();

                        for (int i = 0; i < channel_id.length; i++) {
                            targets.add(new MyTarget((int) channel_id[i], channel_u[i] - channel_w[i], channel_v[i] - channel_h[i], channel_u[i] + channel_w[i], channel_v[i] + channel_h[i]));
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    private double distanceBetween(float[] p1, float[] p2) {
        return Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]));
    }

    private float distanceX(float[] p1, float[] p2) {
        return Math.abs(p1[0] - p2[0]);
    }

    private float distanceY(float[] p1, float[] p2) {
        return Math.abs(p1[1] - p2[1]);
    }

    private float averageY() {
//        float[] result = {0, 0};
        float result = 0;
        for (float[] p : trace) {
            result += p[1];
//            result[1] += p[1];
        }
        result /= trace.size();
//        result[1] /= trace.size();
        return result;
    }

    private float averageY(float[] p1, float[] p2) {
        return (p1[1] + p2[1]) / 2;
    }

    private void reset() {
        trace.clear();
        scaleFactor = 1;
        scaleSpanX = scaleSpanY = scaleSpan = 0;
        isMultifingers = false;
        track_id = -1;
        isLongPress = false;
        timerLongPress.cancel();
        timerScaleOrScroll.cancel();
        isScalingProtected=false;
        isDirectManipulation = false;
        targetIdBeingManipulated = -1;
        canUnderstand = false;
        isScaling = false;
        isScrolling = false;
    }

    private void parseGesture() {
        canUnderstand = true;
        if (isScaling) {

        }
//        else if (isDirectManipulation) {
//            System.out.println("trace size " + trace.size());
//            if (trace.size() == 1)
//                System.out.println(trace.get(0)[0] + "\t" + trace.get(0)[1]);
//            if (trace.size() >= 2) {
//                Float32MultiArray array = gesturePublisher.newMessage();
//                float x_diff = trace.get(trace.size() - 1)[0] - trace.get(0)[0];
//                float y_diff = trace.get(trace.size() - 1)[1] - trace.get(0)[1];
//                array.setData(new float[] {'V', getDroneImageXFromTouchXInPx(x_diff) , getDroneImageYFromTouchYInPx(y_diff)});    // here 1 is to follow the 'once' convention [ziquan]
//                gesturePublisher.publish(array);
//
//                toast.cancel();
//                toast = Toast.makeText(context, "Direct Manipulation: " + getDroneImageXFromTouchXInPx(x_diff) + "," + getDroneImageYFromTouchYInPx(y_diff), Toast.LENGTH_SHORT);
//                toast.show();
//            }
//        }
        // circle
        else if (!isMultifingers && trace.size() >= 20 && distanceBetween(trace.get(0), trace.get(trace.size() - 1)) < 300) {
            float[] traceData = new float[trace.size() * 2 + 1];
            traceData[0] = 'c';
            for (int i = 0; i < trace.size(); i++) {
                Resources res = context.getResources();
                traceData[2*i + 1] = Math.max(0.01f * res.getDimension(R.dimen.drone_image_width),
                        Math.min(0.99f * res.getDimension(R.dimen.drone_image_width), getDroneImageXFromTouchXInPx(trace.get(i)[0]))); //Math.max(0, Math.min(1, trace.get(i)[0] / 1600f));
                traceData[2*i + 2] = Math.max(0.01f * res.getDimension(R.dimen.drone_image_height) ,
                        Math.min(0.99f * res.getDimension(R.dimen.drone_image_height), getDroneImageYFromTouchYInPx(trace.get(i)[1]))); //Math.max(0, Math.min(1, trace.get(i)[1] / 900f));
            }

            Float32MultiArray array = gesturePublisher.newMessage();
            array.setData(traceData);

            gesturePublisher.publish(array);

            toast.cancel();
            toast = Toast.makeText(context, "Circle", Toast.LENGTH_SHORT);
            toast.show();
        }
        // U shape
        else if (!isMultifingers && trace.size() >= 10 && distanceY(trace.get(0), trace.get(trace.size() - 1)) < 200 && distanceX(trace.get(0), trace.get(trace.size() - 1)) > 200 && averageY() - averageY(trace.get(0), trace.get(trace.size() - 1)) >= 100) {
            String str = orbitPubliser.newMessage();
            str.setData("visible");
            orbitPubliser.publish(str);
//            System.out.println("ushape " + (int)'U');

            toast.cancel();
            toast = Toast.makeText(context, "Ready to orbit", Toast.LENGTH_SHORT);
            toast.show();
        }
//        // A shape
//        else if (!isMultifingers && trace.size() >= 10 && distanceY(trace.get(0), trace.get(trace.size() - 1)) < 200 && distanceX(trace.get(0), trace.get(trace.size() - 1)) > 200 &&  averageY(trace.get(0), trace.get(trace.size() - 1)) - averageY() >= 100) {
//            Float32MultiArray array = gesturePublisher.newMessage();
//            array.setData(new float[] {'A'});
//            gesturePublisher.publish(array);
////            System.out.println("ashape " + (int)'A');
//
//            toast.cancel();
//            toast = Toast.makeText(context, "A shape: pano", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        // Z shape
//        else if (!isMultifingers && trace.size() >= 10 && distanceX(trace.get(0), trace.get(trace.size() - 1)) > 200 && distanceY(trace.get(0), trace.get(trace.size() -1)) > 200) {
//            Float32MultiArray array = gesturePublisher.newMessage();
//            array.setData(new float[] {'Z'});
//            gesturePublisher.publish(array);
//
//            toast.cancel();
//            toast = Toast.makeText(context, "Z shape: zigzag scan", Toast.LENGTH_SHORT);
//            toast.show();
//        }
        // scale
//        else if (scaleFactor > 1.1 && scaleSpanX > scaleSpanY) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'Z', 'X', scaleSpanX / 900f});
////            } else {
//            array.setData(new float[]{'Z', 'X', scaleSpanX / 900f, 1});   // 1 means once
////            }
//            gesturePublisher.publish(array);
////            System.out.println("spread X " + (int)'Z' + " " + (int)'X' + " " + scaleFactor);
//
//            toast.cancel();
//            toast = Toast.makeText(context, "Horizontally Zoom +", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if (scaleFactor > 1.1 && scaleSpanX <= scaleSpanY) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'Z', 'Y', scaleSpanY / 1600f});
////            } else {
//            array.setData(new float[]{'Z', 'Y', scaleSpanY / 1600f, 1});   // 1 means once
////            }
//            gesturePublisher.publish(array);
////            System.out.println("spread Y " + (int)'Z' + " " + (int)'Y' + " " + scaleFactor);
//
//            toast.cancel();
//            toast = Toast.makeText(context, "Vertically Zoom +", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if (scaleFactor < 0.9 && scaleSpanX > scaleSpanY) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[] {'Z', 'x', scaleSpanX / 900f});
////            } else {
//            array.setData(new float[]{'Z', 'x', scaleSpanX / 900f, 1});   // 1 means once
////            }
//            gesturePublisher.publish(array);
////            System.out.println("pinch X " + (int)'Z' + " " + (int)'x' + " " + scaleFactor);
//
//            toast.cancel();
//            toast = Toast.makeText(context, "Horizontally Zoom -", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if (scaleFactor < 0.9 && scaleSpanX <= scaleSpanY) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'Z', 'y', scaleSpanY / 1600f});
////            } else {
//            array.setData(new float[]{'Z', 'y', scaleSpanY / 1600f, 1});   // 1 means once
////            }
//            gesturePublisher.publish(array);
////            System.out.println("pinch Y " + (int)'Z' + " " + (int)'y' + " " + scaleFactor);
//
//            toast.cancel();
//            toast = Toast.makeText(context, "Vertically Zoom -", Toast.LENGTH_SHORT);
//            toast.show();
//        }
        // swipe

//        else if (trace.size() > 2 && distanceX(trace.get(0), trace.get(trace.size() - 1)) < 100 && trace.get(trace.size() - 1)[1] - trace.get(0)[1] >= 100) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'I', (isMultifingers ? 'D' : 'd'), traceLengthInPx()});
////            } else {
//            array.setData(new float[]{'I', (isMultifingers ? 'D' : 'd'), traceLengthInPx()});
////            }
//            gesturePublisher.publish(array);
////            System.out.println(isMultifingers ? "double swap down " + (int)'I' + " " + (int)'D' : "swap down " + (int)'I' + " " + (int)'d');
//
//            toast.cancel();
//            toast = Toast.makeText(context, (isMultifingers ? "Multi-finger DOWN: moving backward" : "DOWN: moving upward"), Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if (trace.size() > 2 && distanceX(trace.get(0), trace.get(trace.size() - 1)) < 100 && trace.get(0)[1] - trace.get(trace.size() - 1)[1] >= 100) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'I', (isMultifingers ? 'U' : 'u'), traceLengthInPx()});
////            } else {
//            array.setData(new float[]{'I', (isMultifingers ? 'U' : 'u'), traceLengthInPx()});
////            }
//            gesturePublisher.publish(array);
////            System.out.println(isMultifingers ? "double swap up " + (int)'I' + " " + (int)'U' : "swap up " + (int)'I' + " " + (int)'u' );
//
//            toast.cancel();
//            toast = Toast.makeText(context, (isMultifingers ? "Multi-finger UP: moving forward" : "UP: moving downward"), Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if (trace.size() > 2 && distanceY(trace.get(0), trace.get(trace.size() - 1)) < 100 && trace.get(trace.size() - 1)[0] - trace.get(0)[0] >= 100) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'I', (isMultifingers ? 'R' : 'r'), traceLengthInPx()});
////            } else {
//            array.setData(new float[]{'I', (isMultifingers ? 'R' : 'r'), traceLengthInPx()});
////            }
//            gesturePublisher.publish(array);
////            System.out.println(isMultifingers ? "double swap right " + (int)'I' + " " + (int)'R' : "swap right " + (int)'I' + " " + (int)'r' );
//
//            toast.cancel();
//            toast = Toast.makeText(context, (isMultifingers ? "Multi-finger RIGHT: turning left" : "RIGHT: moving left"), Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if (trace.size() > 2 && distanceY(trace.get(0), trace.get(trace.size() - 1)) < 100 && trace.get(0)[0] - trace.get(trace.size() - 1)[0] >= 100) {
//            Float32MultiArray array = gesturePublisher.newMessage();
////            if (isLongPress) {
////                array.setData(new float[]{'I', (isMultifingers ? 'L' : 'l'), traceLengthInPx()});
////            } else {
//            array.setData(new float[]{'I', (isMultifingers ? 'L' : 'l'), traceLengthInPx()});
////            }
//            gesturePublisher.publish(array);
////            System.out.println(isMultifingers ? "double swap left "  + (int)'I' + " " + (int)'L'  : "swap left " + (int)'I' + " " + (int)'l' );
//
//            toast.cancel();
//            toast = Toast.makeText(context, (isMultifingers ? "Multi-finger LEFT: turning right" : "LEFT: moving right"), Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else {
//            canUnderstand = false;
//        }
    }

    private float traceLengthInPx() {
        float result = 0.f;
        for (int i = 0; i < this.trace.size() - 1; i++) {
            result += distanceBetween(trace.get(i), trace.get(i+1));
        }
        return result;
    }

    private float traceRatioInX() {
        Resources res = context.getResources();
        return (trace.get(trace.size() - 1)[0] - trace.get(0)[0]) / res.getDimension(R.dimen.image_view_width);
    }

    private float traceRatioInY() {
        Resources res = context.getResources();
        return (trace.get(trace.size() - 1)[1] - trace.get(0)[1]) / res.getDimension(R.dimen.image_view_height);
    }

    private float getDistanceRatioAlongXFromTouchXInPx(float xInPx) {
        Resources res = context.getResources();
        return xInPx / res.getDimension(R.dimen.image_view_width);
    }

    private float getDistanceRatioAlongYFromTouchYInPx(float yInPx) {
        Resources res = context.getResources();
        return yInPx / res.getDimension(R.dimen.image_view_height);
    }

    private float getDroneImageXFromTouchXInPx(float xInPx) {
        Resources res = context.getResources();
//        System.out.println("R.dimen.image_view_width " + res.getDimension(R.dimen.image_view_width));
//        System.out.println("R.dimen.drone_image_width " +  res.getDimension(R.dimen.drone_image_width));
        return xInPx / res.getDimension(R.dimen.image_view_width) * res.getDimension(R.dimen.drone_image_width);
    }

    private float getDroneImageYFromTouchYInPx(float yInPx) {
        Resources res = context.getResources();
        return yInPx / res.getDimension(R.dimen.image_view_height) * res.getDimension(R.dimen.drone_image_height);
    }
}