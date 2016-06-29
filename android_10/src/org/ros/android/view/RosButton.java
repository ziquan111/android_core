package org.ros.android.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.google.common.collect.Maps;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.String;
import java.util.Map;
import java.util.concurrent.Callable;

import std_msgs.*;

/**
 * Created by ziquan on 2/27/15.
 */
public class RosButton extends RosImageView {

    private static Map<Integer, String> mapUniqueGestureId = Maps.newHashMap();    // to make sure gesture id is unique when it is created

//    private int id;
    private String resourceName;
    private Map<String, Integer> mapSkin;           // one button may have different skins, e.g, takeoff/land
    private Map<String, Integer> mapGestureId;      // each skin has a unique gesture id

    private Publisher<Float32MultiArray> publisher; // all RosButtons publish to a fixed topic "fleye/gesture"
    private String topicName;                       // but subscribe to different topics

    private Callable<float[]> callable;
    private Runnable runnable;


    public RosButton(Context context) {
        super(context);
        this.resourceName = "";
        mapSkin = Maps.newHashMap();
        mapGestureId = Maps.newHashMap();
    }

    public RosButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resourceName = "";
        mapSkin = Maps.newHashMap();
        mapGestureId = Maps.newHashMap();
    }

    public RosButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.resourceName = "";
        mapSkin = Maps.newHashMap();
        mapGestureId = Maps.newHashMap();
    }

//    public void setId(char id) {
//        this.id = id;
//    }

    @Override
    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void addResource(String name, int resId, int gestureId) {
        if (mapUniqueGestureId.containsKey(gestureId)) {
            System.out.println("Gesture id " + (char) gestureId + " has been used for " + mapUniqueGestureId.get(gestureId));
            return;
        }
        mapUniqueGestureId.put(gestureId, name);
        this.mapSkin.put(name, resId);
        this.mapGestureId.put(name, gestureId);

    }

    public String getResourceName() {
        return this.resourceName;
    }

    // change the skin of a button, if resource name is empty string "", the button is invisible
    public void setResourceName(String resourceName) {
//        System.out.println(this.resourceName);
        if (resourceName.equals("")) {
            setVisibility(View.INVISIBLE);
        }
        else {
            setVisibility(View.VISIBLE);
            setImageResource(mapSkin.get(resourceName));
        }
        this.resourceName = resourceName;
    }

//    public void setMessageType(String messageType) {
//        this.messageType = messageType;
//    }

//    @Override
//    public void setMessageToStringCallable(MessageCallable callable) {
//        this.callable = callable;
//    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_10/ros_button");
    }

    public void setCallable(Callable<float[]> callable) {
        this.callable = callable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher("fleye/gesture", Float32MultiArray._TYPE);
        setOnClickListener(new OnClickListener() {
            public void onClick(View unused) {
                Float32MultiArray array = publisher.newMessage();
                try {
                    if (callable != null) {
                        float[] temp = callable.call();
                        array.setData(temp);
                        for (float f : temp) {
                            System.out.print(f + "\t");
                        }
                        System.out.println();
                    } else {
                        array.setData(new float[]{mapGestureId.get(resourceName)});
                        System.out.println("ROS Button send " + mapGestureId.get(resourceName));
                    }
                    publisher.publish(array);
                    if (runnable != null) {
                        runnable.run();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // listen to a topic to change skin
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(topicName, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(final std_msgs.String message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        setResourceName(message.getData().toString());
                    }
                });
                postInvalidate();
            }
        });
    }
}