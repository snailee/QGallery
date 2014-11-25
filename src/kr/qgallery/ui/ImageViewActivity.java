/*
 * Copyright (C) 2012 The Android Open Source Project
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


import kr.qgallery.R;
import kr.qgallery.provider.ImageProvider;
import kr.qgallery.provider.ImageProviderContract;
import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.android.displayingbitmaps.util.ImageCache;
import com.example.android.displayingbitmaps.util.ImageFetcher;

public abstract class ImageViewActivity extends FragmentActivity implements
    LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {
  private static final String TAG = ImageViewActivity.class.getName();

  private static final int IMAGE_URLS_LOADER = 0;
  private static final String[] PROJECTION = {ImageProviderContract._ID,
      ImageProviderContract.IMAGE_URL_COLUMN};

  private ImageCursorPagerAdapter mAdapter;
  private ImageFetcher mImageFetcher;
  private ViewPager mPager;


  protected abstract Fragment createFragment(int id, String imageUrl);

  protected abstract int getLayoutResourceId();

  @TargetApi(VERSION_CODES.HONEYCOMB)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutResourceId());

    // Fetch screen height and width, to use as our max size when loading
    // images as this
    // activity runs full screen
    final DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    final int height = displayMetrics.heightPixels;
    final int width = displayMetrics.widthPixels;

    // For this sample we'll use half of the longest width to resize our
    // images. As the
    // image scaling ensures the image is larger than this, we should be
    // left with a
    // resolution that is appropriate for both portrait and landscape. For
    // best image quality
    // we shouldn't divide by 2, but this will use more memory and require a
    // larger memory
    // cache.
    final int longest = (height > width ? height : width) / 2;

    ImageCache.ImageCacheParams cacheParams =
        new ImageCache.ImageCacheParams(this, ImageProvider.IMAGE_CACHE_DIR);
    cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of
    // app memory

    // The ImageFetcher takes care of loading images into our ImageView
    // children asynchronously
    mImageFetcher = new ImageFetcher(this, longest);
    mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
    mImageFetcher.setImageFadeIn(false);

    // Set up ViewPager and backing adapter
    mAdapter = new ImageCursorPagerAdapter(getSupportFragmentManager(), null);
    mPager = (ViewPager) findViewById(R.id.pager);
    mPager.setAdapter(mAdapter);
    mPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
    mPager.setOffscreenPageLimit(2);

    // Set the current item based on the extra passed in to this activity
    final int extraCurrentItem = getIntent().getIntExtra(ImageProvider.EXTRA_IMAGE, -1);
    if (extraCurrentItem != -1) {
      mPager.setCurrentItem(extraCurrentItem);
    }

    getLoaderManager().initLoader(IMAGE_URLS_LOADER, null, this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mImageFetcher.setExitTasksEarly(false);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mImageFetcher.setExitTasksEarly(true);
    mImageFetcher.flushCache();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mImageFetcher.closeCache();
  }

  public ViewPager getViewPager() {
    return mPager;
  }

  public ImageCursorPagerAdapter getImageAdapter() {
    return mAdapter;
  }

  /**
   * Called by the ViewPager child fragments to load images via the one ImageFetcher
   */
  public ImageFetcher getImageFetcher() {
    return mImageFetcher;
  }

  class ImageCursorPagerAdapter extends FragmentStatePagerAdapter {
    Cursor mCursor;

    public ImageCursorPagerAdapter(FragmentManager fm, Cursor cursor) {
      super(fm);
      mCursor = cursor;
    }

    @Override
    public int getCount() {
      if (mCursor == null) {
        return 0;
      } else {
        return mCursor.getCount();
      }
    }

    public void swapCursor(Cursor c) {
      if (mCursor == c) {
        return;
      }
      mCursor = c;
      Log.i(TAG, "swapCursor");
      notifyDataSetChanged();
    }

    public Cursor getCursor() {
      return mCursor;
    }

    @Override
    public Fragment getItem(int position) {
      if (mCursor == null) { // shouldn't happen
        return null;
      }
      mCursor.moveToPosition(position);

      return createFragment(mCursor.getInt(mCursor
          .getColumnIndex(ImageProviderContract._ID)),
          mCursor.getString(mCursor
              .getColumnIndex(ImageProviderContract.IMAGE_URL_COLUMN)));
    }

    @Override
    public int getItemPosition(Object object) {
      return mCursor == null ? PagerAdapter.POSITION_UNCHANGED : PagerAdapter.POSITION_NONE;
    }

  }

  /**
   * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
   * when the ImageView is touched.
   */
  @TargetApi(VERSION_CODES.HONEYCOMB)
  @Override
  public void onClick(View v) {
    Log.i(TAG, "onClick");
    final int vis = mPager.getSystemUiVisibility();
    if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
      mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    } else {
      mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
    switch (loaderID) {
      case IMAGE_URLS_LOADER:
        return new CursorLoader(this, ImageProviderContract.CONTENT_URI_IMAGES, PROJECTION, null,
            null, null);
      default:
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor returnCursor) {
    mAdapter.swapCursor(returnCursor);
    // Set the current item based on the extra passed in to this activity
    final int extraCurrentItem = getIntent().getIntExtra(ImageProvider.EXTRA_IMAGE, -1);
    if (extraCurrentItem != -1) {
      mPager.setCurrentItem(extraCurrentItem, false);
    }
  }

  /*
   * Invoked when the CursorLoader is being reset. For example, this is called if the data in the
   * provider changes and the Cursor becomes stale.
   */
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.swapCursor(null);
  }

}
