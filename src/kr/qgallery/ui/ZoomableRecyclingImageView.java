/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package kr.qgallery.ui;


import uk.co.senab.photoview.PhotoViewAttacher;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.example.android.displayingbitmaps.ui.RecyclingImageView;

public class ZoomableRecyclingImageView extends RecyclingImageView {
  @SuppressWarnings("unused")
  private static final String TAG = ZoomableRecyclingImageView.class.getName();

  PhotoViewAttacher mAttacher;

  public ZoomableRecyclingImageView(Context context) {
    super(context);
  }

  public ZoomableRecyclingImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void setImageDrawable(Drawable drawable) {
    super.setImageDrawable(drawable);
    if (mAttacher == null) {
      mAttacher = new PhotoViewAttacher(this);
    } else {
      mAttacher.update();
    }
  }

}
