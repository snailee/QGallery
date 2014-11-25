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
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.displayingbitmaps.ui.RecyclingImageView;
import com.example.android.displayingbitmaps.util.ImageCache;
import com.example.android.displayingbitmaps.util.ImageFetcher;
import com.example.android.displayingbitmaps.util.Utils;

/**
 * The main fragment that powers the ImageGridActivity screen. Fairly straight forward GridView
 * implementation with the key addition being the ImageWorker class w/ImageCache to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly if, for example, the user rotates the device.
 */
public class ImageGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
    AdapterView.OnItemClickListener {
  private static final String TAG = ImageGridFragment.class.getName();
  private static final String IMAGE_CACHE_DIR = "thumbs";
  private static final int IMAGE_URLS_LOADER = 0;
  private static final String[] PROJECTION = {ImageProviderContract._ID,
      ImageProviderContract.IMAGE_URL_COLUMN};

  private int mImageThumbSize;
  private int mImageThumbSpacing;
  private ImageCursorAdapter mAdapter;
  private ImageFetcher mImageFetcher;

  /**
   * Empty constructor as per the Fragment documentation
   */
  public ImageGridFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
    mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

    mAdapter = new ImageCursorAdapter(getActivity(), null, 0);

    ImageCache.ImageCacheParams cacheParams =
        new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);

    cacheParams.setMemCacheSizePercent(0.25f);

    // The ImageFetcher takes care of loading images into our ImageView
    // children asynchronously
    mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
    mImageFetcher.setLoadingImage(R.drawable.empty_photo);
    mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    final View v = inflater.inflate(R.layout.image_grid_fragment, container, false);
    final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
    mGridView.setAdapter(mAdapter);
    mGridView.setOnItemClickListener(this);
    mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        // Pause fetcher to ensure smoother scrolling when flinging
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
          // Before Honeycomb pause image loading on scroll to help
          // with performance
          if (!Utils.hasHoneycomb()) {
            mImageFetcher.setPauseWork(true);
          }
        } else {
          mImageFetcher.setPauseWork(false);
        }
      }

      @Override
      public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
          int totalItemCount) {}
    });

    // This listener is used to get the final width of the GridView and then
    // calculate the
    // number of columns and the width of each column. The width of each
    // column is variable
    // as the GridView has stretchMode=columnWidth. The column width is used
    // to set the height
    // of each view so we get nice square thumbnails.
    mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @SuppressWarnings("deprecation")
          @TargetApi(VERSION_CODES.JELLY_BEAN)
          @Override
          public void onGlobalLayout() {
            if (mAdapter.getNumColumns() == 0) {
              final int numColumns =
                  (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
              if (numColumns > 0) {
                final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                mAdapter.setNumColumns(numColumns);
                mAdapter.setItemHeight(columnWidth);
                Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
                if (Utils.hasJellyBean()) {
                  mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                  mGridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
              }
            }
          }
        });

    getLoaderManager().initLoader(IMAGE_URLS_LOADER, null, this);

    return v;
  }

  @Override
  public void onResume() {
    super.onResume();
    mImageFetcher.setExitTasksEarly(false);
    mAdapter.notifyDataSetChanged();
  }

  @Override
  public void onPause() {
    super.onPause();
    mImageFetcher.setPauseWork(false);
    mImageFetcher.setExitTasksEarly(true);
    mImageFetcher.flushCache();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mImageFetcher.closeCache();
  }

  @Override
  public void onDetach() {
    try {
      getLoaderManager().destroyLoader(0);
      if (mAdapter != null) {
        mAdapter.changeCursor(null);
        mAdapter = null;
      }
    } catch (Throwable localThrowable) {
    }

    // Always call the super method last
    super.onDetach();
    return;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
    final Intent i = new Intent(getActivity(), ImageDetailActivity.class);
    i.putExtra(ImageProvider.EXTRA_IMAGE, position - mAdapter.getNumColumns());
    if (Utils.hasJellyBean()) {
      // makeThumbnailScaleUpAnimation() looks kind of ugly here as the
      // loading spinner may
      // show plus the thumbnail image in GridView is cropped. so using
      // makeScaleUpAnimation() instead.
      ActivityOptions options =
          ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
      getActivity().startActivity(i, options.toBundle());
    } else {
      startActivity(i);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.clear_cache:
        mImageFetcher.clearCache();
        Toast.makeText(getActivity(), R.string.clear_cache_complete_toast, Toast.LENGTH_SHORT)
            .show();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * The main adapter that backs the GridView. This is fairly standard except the number of columns
   * in the GridView is used to create a fake top row of empty views as we use a transparent
   * ActionBar and don't want the real top row of images to start off covered by it.
   */
  private class ImageCursorAdapter extends CursorAdapter {

    private final Context mContext;
    private int mItemHeight = 0;
    private int mNumColumns = 0;
    private int mActionBarHeight = 0;
    private GridView.LayoutParams mImageViewLayoutParams;

    public ImageCursorAdapter(Context context, Cursor cursor, int flags) {
      super(context, cursor, flags);
      mContext = context;
      mImageViewLayoutParams =
          new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
      // Calculate ActionBar height
      TypedValue tv = new TypedValue();
      if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
        mActionBarHeight =
            TypedValue.complexToDimensionPixelSize(tv.data, context.getResources()
                .getDisplayMetrics());
      }
    }

    @Override
    public int getCount() {
      // If columns have yet to be determined, return no items
      if (getNumColumns() == 0) {
        return 0;
      }

      // Size + number of columns for top empty row
      return super.getCount() + mNumColumns;
    }

    @Override
    public Object getItem(int position) {
      if (position < mNumColumns) {
        return null;
      }
      return super.getItem(position - mNumColumns);
    }

    @Override
    public long getItemId(int position) {
      return position < mNumColumns ? 0 : super.getItemId(position - mNumColumns);
    }

    @Override
    public int getViewTypeCount() {
      // Two types of views, the normal ImageView and the top row of empty
      // views
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      return (position < mNumColumns) ? 1 : 0;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
      // First check if this is the top row
      if (position < mNumColumns) {
        if (convertView == null) {
          convertView = new View(mContext);
        }
        // Set empty view with height of ActionBar
        convertView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
            mActionBarHeight));
        return convertView;
      }

      return super.getView(position - mNumColumns, convertView, container);
    }

    /**
     * Sets the item height. Useful for when we know the column width so the height can be set to
     * match.
     *
     * @param height
     */
    public void setItemHeight(int height) {
      if (height == mItemHeight) {
        return;
      }
      mItemHeight = height;
      mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
      mImageFetcher.setImageSize(height);
      notifyDataSetChanged();
    }

    public void setNumColumns(int numColumns) {
      Log.i(TAG, "setNumColumns(" + numColumns + ")");
      mNumColumns = numColumns;
    }

    public int getNumColumns() {
      return mNumColumns;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      // Now handle the main ImageView thumbnails
      ImageView imageView = (ImageView) view;
      // Check the height matches our calculated column width
      if (imageView.getLayoutParams().height != mItemHeight) {
        imageView.setLayoutParams(mImageViewLayoutParams);
      }
      // Finally load the image asynchronously into the ImageView, this
      // also takes care of
      // setting a placeholder image while the background thread runs
      mImageFetcher.loadImage(
          cursor.getString(cursor.getColumnIndex(ImageProviderContract.IMAGE_URL_COLUMN)),
          imageView);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      ImageView imageView = new RecyclingImageView(mContext);
      imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
      imageView.setLayoutParams(mImageViewLayoutParams);
      return imageView;
    }

  }

  @Override
  public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
    switch (loaderID) {
      case IMAGE_URLS_LOADER:
        return new CursorLoader(getActivity(), ImageProviderContract.CONTENT_URI_IMAGES,
            PROJECTION, null, null, null);
      default:
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor returnCursor) {
    mAdapter.changeCursor(returnCursor);
  }

  /*
   * Invoked when the CursorLoader is being reset. For example, this is called if the data in the
   * provider changes and the Cursor becomes stale.
   */
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.changeCursor(null);
  }

}
