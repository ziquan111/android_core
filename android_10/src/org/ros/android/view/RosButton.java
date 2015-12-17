package org.ros.android.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.google.common.collect.Maps;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.String;
import java.util.Map;

import std_msgs.*;

/**
 * Created by ziquan on 2/27/15.
 */
public class RosButton extends RosImageView {

    private static int idCount = 0;                 // auto increment

    private int id;
    private String resourceName;
    private Map<String, Integer> mapSkin;           // one button may have different skins, e.g, takeoff/land

    private Publisher<Float32MultiArray> publisher; // all RosButtons publish to a fixed topic "fleye/gesture"
    private String topicName;                       // but subscribe to different topics


    public RosButton(Context context) {
        super(context);
        this.id = idCount++;
        this.resourceName = "";
        mapSkin = Maps.newHashMap();
    }

    public RosButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.id = idCount++;
        this.resourceName = "";
        mapSkin = Maps.newHashMap();
    }

    public RosButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.id = idCount++;
        this.resourceName = "";
        mapSkin = Maps.newHashMap();
    }

//    public void setIds(char id, char idOnLongPress) {
//        this.id = id;
//    }

    @Override
    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void addResource(String name, int resId) {
        this.mapSkin.put(name, resId);
    }

    public String getResourceName() {
        return this.resourceName;
    }

    // change the skin of a button, if resource name is empty string "", the button is invisible
    public void setResourceName(String resourceName) {
        System.out.println(this.resourceName);
        if (resourceName.equals("")) {
            setVisibility(View.INVISIBLE);
        }
        else {
            setVisibility(View.VISIBLE);
            setImageResource(mapSkin.get(resourceName));
        }
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

    @Override
    public void onStart(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher("fleye/gesture", Float32MultiArray._TYPE);
        setOnClickListener(new OnClickListener() {
            public void onClick(View unused) {
                Float32MultiArray array = publisher.newMessage();
                array.setData(new float[]{id});
                publisher.publish(array);
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