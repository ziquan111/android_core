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

package org.ros.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class BitmapFromCompressedImage implements
        MessageCallable<Bitmap, sensor_msgs.CompressedImage> {


    private static BitmapFactory.Options options = new BitmapFactory.Options();
    private static Bitmap last_bm;

    static {
        options.inSampleSize = 1;
        options.inMutable = true;
    }

    public void addBitmapBuffer(Bitmap buffer) {
        if (options.inBitmap == null) {
            options.inBitmap = buffer;
        }
    }

    @Override
    public Bitmap call(sensor_msgs.CompressedImage message) {
        ChannelBuffer buffer = message.getData();
        if (buffer.readableBytes() > 0) {
            last_bm = BitmapFactory.decodeByteArray(buffer.array(), buffer.arrayOffset(), buffer.readableBytes(), options);
        }
//        System.out.println(last_bm.getWidth() + " by " + last_bm.getHeight());
        return last_bm;
    }
}
