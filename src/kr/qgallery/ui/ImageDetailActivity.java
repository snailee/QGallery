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



import java.util.List;

import kr.qgallery.R;
import kr.qgallery.provider.ImageProvider;
import kr.qgallery.provider.ImageProviderContract;
import uk.co.senab.photoview.PhotoViewAttacher.OnViewTapListener;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.example.android.displayingbitmaps.util.Utils;

public class ImageDetailActivity extends ImageViewActivity implements OnViewTapListener {
  private static final String TAG = ImageDetailActivity.class.getName();
	


  @Override
  protected int getLayoutResourceId() {
    return R.layout.image_detail_activity;
  }

  private static class ImageDetailFragment extends ImageViewFragment {

    @Override
    protected int getLayoutResourceId() {
      return R.layout.image_detail_fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      
      //sujin.cho
      //start
      SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(getActivity());
      boolean isFirstRun = wmbPreference.getBoolean("FIRSTRUN", true);
      Context context = getActivity();
 
      if (isFirstRun)
      {
    	// Code to run once	  
          String NOTICE_URL = "file:///android_asset/OSSNotice-141014-QGallery.html";
          
          WebView wv = new WebView(context);
          wv.loadUrl(NOTICE_URL);
          wv.setWebViewClient(new WebViewClient() {
              @Override
              public boolean shouldOverrideUrlLoading(WebView view, String url) {
                  view.loadUrl(url);
                  return true;
              }
          });
          wv.getSettings().setLoadWithOverviewMode(true);
          wv.getSettings().setUseWideViewPort(true);
          	
          
          AlertDialog.Builder alert = new AlertDialog.Builder(context); 
          TextView title = new TextView(context);
          title.setText("OSSNotice");
          title.setBackgroundColor(Color.DKGRAY);
          title.setPadding(10, 10, 10, 10);
          title.setGravity(Gravity.CENTER);
          title.setTextColor(Color.WHITE);
          title.setTextSize(20);

          alert.setCustomTitle(title);
          alert.setCancelable(true);
          alert.setView(wv);
          alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                  dialog.dismiss();
              }
          });
          alert.show();
  	   	  
        SharedPreferences.Editor editor = wmbPreference.edit();
        editor.putBoolean("FIRSTRUN", false);
        editor.commit();
      
      }
      //end

      if (ZoomableRecyclingImageView.class.isInstance(mImageView)
          && OnViewTapListener.class.isInstance(getActivity())) {
        ((ZoomableRecyclingImageView) mImageView).mAttacher
            .setOnViewTapListener((OnViewTapListener) getActivity());
      }
    }
  }

  @Override
  protected Fragment createFragment(int id, String imageUrl) {
    final ImageViewFragment f = new ImageDetailFragment();

    final Bundle args = new Bundle();
    args.putInt(ImageViewFragment.EXTRA_DATA_IMAGE_ID, id);
    args.putString(ImageViewFragment.EXTRA_DATA_IMAGE_URL, imageUrl);
    f.setArguments(args);

    return f;
  }

  private class ConfirmDeleteDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.confirm_delete).setPositiveButton(R.string.delete,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              deleteCurrentPhoto();
            }
          }).setNegativeButton(R.string.cancel,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              ConfirmDeleteDialogFragment.this.dismiss();
            }
          });
      return builder.create();
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set up activity to go full screen
    getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);
    
    // Enable some additional newer visibility and ActionBar features to create a more
    // immersive photo viewing experience
    if (Utils.hasHoneycomb()) {
      final ActionBar actionBar = getActionBar();

      // Hide title text and set home as up
      actionBar.setDisplayShowTitleEnabled(false);
      actionBar.setDisplayHomeAsUpEnabled(true);

      final ViewPager pager = getViewPager();
      // Hide and show the ActionBar as the visibility changes
      pager.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int vis) {
          if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
            actionBar.hide();
          } else {
            actionBar.show();
          }
        }
      });

      // Start low profile mode and hide ActionBar
      pager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
      actionBar.hide();
    }
    
  }

  @Override
  protected void onPause() {
    super.onPause();
    // launchImageCircleActivityIfNecessary();

    //sujin.cho
    //just pause to follow the original QCircle UX scenario
    /*
    // give a delay if you want
    (new Handler()).postDelayed(new Runnable() {
      @Override
      public void run() {
        launchImageCircleActivityIfNecessary();
      }
    }, 10);
    */
  }

  private void launchImageCircleActivityIfNecessary() {
    Log.i(TAG, "launchImageCircleActivityIfNecessary()");
    final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    final String thisPackageName = getApplicationInfo().packageName;
    // get the top activity's package
    final RunningTaskInfo topTask = am.getRunningTasks(1).get(0);
    Log.i(TAG, "Top task : " + topTask.baseActivity.getPackageName());
    if (topTask.baseActivity.getPackageName().equals(thisPackageName)) {
      // Must not start any other activity
      Log.i(TAG, "Do not launch ImageCircleActivity");
      return;
    }

    final List<RecentTaskInfo> recentTasks =
        am.getRecentTasks(Integer.MAX_VALUE, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
    RecentTaskInfo recentTaskInfo = null;
    for (int i = 0; i < recentTasks.size(); i++) {
      Log.i(TAG, "recentTasks " + i + " : "
          + recentTasks.get(i).baseIntent.getComponent().getPackageName());
      if (recentTasks.get(i).baseIntent.getComponent().getPackageName().equals(thisPackageName)) {
        recentTaskInfo = recentTasks.get(i);
        Log.i(TAG, "Found " + recentTaskInfo.toString());
        break;
      }
    }

    if (recentTaskInfo != null && recentTaskInfo.id > -1) {
      Log.i(TAG, "Starting the ImageCicleActivity and moving it to front");
      Intent intent = new Intent(this, ImageCircleActivity.class);
      intent.putExtra(ImageProvider.EXTRA_IMAGE, getViewPager().getCurrentItem());
      startActivity(intent);
      am.moveTaskToFront(recentTaskInfo.persistentId, ActivityManager.MOVE_TASK_WITH_HOME);
      return;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;
      case R.id.delete_photo:
        DialogFragment df = new ConfirmDeleteDialogFragment();
        df.show(getFragmentManager(), "confirm_delete");
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void deleteCurrentPhoto() {
    final Cursor cursor = getImageAdapter().getCursor();
    cursor.moveToPosition(getViewPager().getCurrentItem());
    ImageProvider.getInstance().deleteImage(getContentResolver(),
        cursor.getInt(cursor.getColumnIndex(ImageProviderContract._ID)));

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.image_detail_menu, menu);
    return true;
  }

  @Override
  public void onViewTap(View view, float x, float y) {
    final ViewPager pager = getViewPager();
    final int vis = pager.getSystemUiVisibility();
    if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
      pager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    } else {
      pager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }
  }
  

}
