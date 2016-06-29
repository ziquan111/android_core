package org.ros.android.view;

/**
 * Created by ziquan on 17/12/15.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.ImageView;

import com.google.common.collect.Maps;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MessageCallable;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import java.util.HashMap;

/**
 * Created by ziquan on 2/13/15.
 */
public class RosAugmentedImageView<T, S, U, V> extends ImageView implements NodeMain {
    private String imageTopicName, pointsTopicName, linesTopicName,rectsTopicName;
    private String imageMessageType, pointsMessageType, linesMessageType, rectsMessageType;
    private MessageCallable<Bitmap, T> callableToImage;
    private MessageCallable<float[], S> callableToPoints;
    private MessageCallable<float[], U> callableToLines;
    private MessageCallable<HashMap<Integer, Pair<RectF, Integer> >, V> callableToRects;

    private Bitmap bitmap_copy;

    private static float[] points;
//    private static ArrayList<RectF> targetRects = Lists.newArrayList();   // [left, top, right, bottom, ...]
//    private static ArrayList<RectF> otherTargetRects = Lists.newArrayList();  // [left, top, right, bottom, ...]
    private static HashMap<Integer, Pair<RectF, Integer> > targetRects = Maps.newHashMap();  // [left, top, right, bottom, ...]
    private static float[] lines;

    private final Paint pointPaint, linePaint, composedTargetRectPaint, noneComposedTargetRectPaint, crossPaint;
    private final Paint redMaskPaint, greenMaskPaint, blueMaskPaint, orangeMaskPaint;

    private final float crossSize = 14f, maskSize = 75f, maskVerticalMargin = 100f, maskHorizontalMargin = 180f;

    private final static boolean toDrawMask = false;

    public RosAugmentedImageView(Context context) {
        super(context);
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setARGB(255, 255, 255, 255);
        pointPaint.setStrokeWidth(1.5f);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setARGB(255, 77, 166, 255);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setStyle(Paint.Style.STROKE);

        composedTargetRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        composedTargetRectPaint.setARGB(255, 255, 165, 0);
        composedTargetRectPaint.setStrokeWidth(2.0f);
        composedTargetRectPaint.setStyle(Paint.Style.STROKE);

        noneComposedTargetRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noneComposedTargetRectPaint.setARGB(255, 77, 166, 255);
        noneComposedTargetRectPaint.setStrokeWidth(2.0f);
        noneComposedTargetRectPaint.setStyle(Paint.Style.STROKE);
        noneComposedTargetRectPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crossPaint.setARGB(255, 255, 0, 0);
        crossPaint.setStrokeWidth(1.5f);
        crossPaint.setStyle(Paint.Style.STROKE);

        redMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redMaskPaint.setARGB(100, 255, 0, 0);
        redMaskPaint.setStrokeWidth(1.5f);
        redMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        greenMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenMaskPaint.setARGB(100, 0, 255, 0);
        greenMaskPaint.setStrokeWidth(1.5f);
        greenMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        blueMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueMaskPaint.setARGB(100, 0, 0, 255);
        blueMaskPaint.setStrokeWidth(1.5f);
        blueMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        orangeMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orangeMaskPaint.setARGB(100, 255, 165, 0);
        orangeMaskPaint.setStrokeWidth(1.5f);
        orangeMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public RosAugmentedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setARGB(255, 255, 255, 255);
        pointPaint.setStrokeWidth(1.5f);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setARGB(255, 77, 166, 255);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setStyle(Paint.Style.STROKE);

        composedTargetRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        composedTargetRectPaint.setARGB(255, 255, 165, 0);
        composedTargetRectPaint.setStrokeWidth(2.0f);
        composedTargetRectPaint.setStyle(Paint.Style.STROKE);

        noneComposedTargetRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noneComposedTargetRectPaint.setARGB(255, 77, 166, 255);
        noneComposedTargetRectPaint.setStrokeWidth(2.0f);
        noneComposedTargetRectPaint.setStyle(Paint.Style.STROKE);
        noneComposedTargetRectPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crossPaint.setARGB(255, 255, 0, 0);
        crossPaint.setStrokeWidth(1.5f);
        crossPaint.setStyle(Paint.Style.STROKE);

        redMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redMaskPaint.setARGB(100, 255, 0, 0);
        redMaskPaint.setStrokeWidth(1.5f);
        redMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        greenMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenMaskPaint.setARGB(100, 0, 255, 0);
        greenMaskPaint.setStrokeWidth(1.5f);
        greenMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        blueMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueMaskPaint.setARGB(100, 0, 0, 255);
        blueMaskPaint.setStrokeWidth(1.5f);
        blueMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        orangeMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orangeMaskPaint.setARGB(100, 255, 165, 0);
        orangeMaskPaint.setStrokeWidth(1.5f);
        orangeMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public RosAugmentedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setARGB(255, 255, 255, 255);
        pointPaint.setStrokeWidth(1.5f);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setARGB(255, 77, 166, 255);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setStyle(Paint.Style.STROKE);

        composedTargetRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        composedTargetRectPaint.setARGB(255, 255, 165, 0);
        composedTargetRectPaint.setStrokeWidth(2.0f);
        composedTargetRectPaint.setStyle(Paint.Style.STROKE);

        noneComposedTargetRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noneComposedTargetRectPaint.setARGB(255, 77, 166, 255);
        noneComposedTargetRectPaint.setStrokeWidth(2.0f);
        noneComposedTargetRectPaint.setStyle(Paint.Style.STROKE);
        noneComposedTargetRectPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crossPaint.setARGB(255, 255, 0, 0);
        crossPaint.setStrokeWidth(1.5f);
        crossPaint.setStyle(Paint.Style.STROKE);

        redMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redMaskPaint.setARGB(100, 255, 0, 0);
        redMaskPaint.setStrokeWidth(1.5f);
        redMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        greenMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenMaskPaint.setARGB(100, 0, 255, 0);
        greenMaskPaint.setStrokeWidth(1.5f);
        greenMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        blueMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueMaskPaint.setARGB(100, 0, 0, 255);
        blueMaskPaint.setStrokeWidth(1.5f);
        blueMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        orangeMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orangeMaskPaint.setARGB(100, 255, 165, 0);
        orangeMaskPaint.setStrokeWidth(1.5f);
        orangeMaskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setTopicNames(String imageTopicName, String pointsTopicName, String linesTopicName, String rectsTopicName) {
        this.imageTopicName = imageTopicName;
        this.pointsTopicName = pointsTopicName;
        this.linesTopicName = linesTopicName;
        this.rectsTopicName = rectsTopicName;
    }

    public void setMessageTypes(String imageMessageType, String pointsMessageType, String linesMessageType, String rectsMessageType) {
        this.imageMessageType = imageMessageType;
        this.pointsMessageType = pointsMessageType;
        this.linesMessageType = linesMessageType;
        this.rectsMessageType = rectsMessageType;
    }

    public void setMessageToBitmapCallable(MessageCallable<Bitmap, T> callable) {
        this.callableToImage = callable;
    }

    public void setMessageToPointsCallable(MessageCallable<float[], S> callable) {
        this.callableToPoints = callable;
    }

    public void setMessageToLinesCallable(MessageCallable<float[], U> callable) {
        this.callableToLines = callable;
    }


    public void setMessageToRectsCallable(MessageCallable<HashMap<Integer, Pair<RectF, Integer> >, V> callable) {
        this.callableToRects = callable;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ros_augmented_image_view");
    }

    public Bitmap getBitmap_copy() {
        return bitmap_copy;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<T> imageSubscriber = connectedNode.newSubscriber(imageTopicName, imageMessageType);
        Subscriber<S> pointsSubscriber = connectedNode.newSubscriber(pointsTopicName, pointsMessageType);
        Subscriber<U> linesSubscriber = connectedNode.newSubscriber(linesTopicName, linesMessageType);
        Subscriber<V> rectsSubscriber = connectedNode.newSubscriber(rectsTopicName, rectsMessageType);
        imageSubscriber.addMessageListener(new MessageListener<T>() {
            @Override
            public void onNewMessage(final T message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = callableToImage.call(message);
                        ((BitmapFromCompressedImage) callableToImage).addBitmapBuffer(bitmap);
                        Canvas canvas = new Canvas(bitmap);
                        // draw points
                        if (points != null) {
                            canvas.drawPoints(points, pointPaint);
                        }
                        // draw targetrects (with red cross)
                        for (int key : targetRects.keySet()) {
                            canvas.drawRoundRect(targetRects.get(key).first, 10f, 10f, (targetRects.get(key).second > 0 ? composedTargetRectPaint : noneComposedTargetRectPaint));
                            canvas.drawCircle(targetRects.get(key).first.right, targetRects.get(key).first.top, crossSize, crossPaint);
//                            canvas.drawText("" + key, targetRects.get(key).first.right, targetRects.get(key).first.top, crossPaint);
                            canvas.drawLines(
                                    new float[]{targetRects.get(key).first.right - crossSize / (float) Math.sqrt(2), targetRects.get(key).first.top + crossSize / (float) Math.sqrt(2),
                                                targetRects.get(key).first.right + crossSize / (float) Math.sqrt(2), targetRects.get(key).first.top - crossSize / (float) Math.sqrt(2),
                                                targetRects.get(key).first.right - crossSize / (float) Math.sqrt(2), targetRects.get(key).first.top - crossSize / (float) Math.sqrt(2),
                                                targetRects.get(key).first.right + crossSize / (float) Math.sqrt(2), targetRects.get(key).first.top + crossSize / (float) Math.sqrt(2)},
                                    crossPaint);
                        }
                        // draw line
                        if (lines != null) {
                            canvas.drawLines(lines, linePaint);
                        }
//                        for (RectF targetRect : targetRects.values()) {
//                            canvas.drawRoundRect(targetRect, 10f, 10f, composedTargetRectPaint);
//                            canvas.drawCircle(targetRect.right, targetRect.top, crossSize, crossPaint);
//                            canvas.drawLines(
//                                    new float[]{targetRect.right - crossSize / (float) Math.sqrt(2), targetRect.top + crossSize / (float) Math.sqrt(2),
//                                            targetRect.right + crossSize / (float) Math.sqrt(2), targetRect.top - crossSize / (float) Math.sqrt(2),
//                                            targetRect.right - crossSize / (float) Math.sqrt(2), targetRect.top - crossSize / (float) Math.sqrt(2),
//                                            targetRect.right + crossSize / (float) Math.sqrt(2), targetRect.top + crossSize / (float) Math.sqrt(2)},
//                                    crossPaint);
//                        }
                        // draw otherTargetRects
//                        if (otherTargetRects != null) {
////                            for(RectF rectF : otherTargetRects) {
////                                canvas.drawRoundRect(rectF, 10f, 10f, noneComposedTargetRectPaint);
////                            }
//                        }
//                        if (toDrawMask) {
//                            canvas.drawCircle(maskHorizontalMargin, maskVerticalMargin, maskSize, redMaskPaint);
//                            canvas.drawCircle(640f - maskHorizontalMargin, maskVerticalMargin, maskSize, greenMaskPaint);
//                            canvas.drawCircle(maskHorizontalMargin, 368f - maskVerticalMargin, maskSize, blueMaskPaint);
//                            canvas.drawCircle(640f - maskHorizontalMargin, 368f - maskVerticalMargin, maskSize, orangeMaskPaint);
//                        }
                        setImageBitmap(bitmap);
                    }
                });
                postInvalidate();
            }
        });

        pointsSubscriber.addMessageListener(new MessageListener<S>() {
            @Override
            public void onNewMessage(final S message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        points = callableToPoints.call(message);
                    }
                });
            }
        });

        linesSubscriber.addMessageListener(new MessageListener<U>() {
            @Override
            public void onNewMessage(final U message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        lines = callableToLines.call(message);
                    }
                });
            }
        });

        rectsSubscriber.addMessageListener(new MessageListener<V>() {
            @Override
            public void onNewMessage(final V message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        targetRects = callableToRects.call(message);
                    }
                });
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
}