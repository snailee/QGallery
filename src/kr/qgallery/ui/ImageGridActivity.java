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
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Simple FragmentActivity to hold the main {@link ImageGridFragment} and not much else.
 */
public class ImageGridActivity extends FragmentActivity {
  private static final String TAG = "QG_ImageGridActivity";

  private static int ADD_PICTURE = 1;
  private static int TAKE_PICTURE = 2;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
      final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.add(android.R.id.content, new ImageGridFragment(), TAG);
      ft.commit();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    // launchImageCircleActivityIfNecessary();

    // give a delay if you want
    (new Handler()).postDelayed(new Runnable() {
      @Override
      public void run() {
        launchImageCircleActivityIfNecessary();
      }
    }, 10);
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
      startActivity(intent);
      am.moveTaskToFront(recentTaskInfo.persistentId, ActivityManager.MOVE_TASK_WITH_HOME);
      return;
    }
  }

  public static boolean isIntentAvailable(Context context, String action) {
    final PackageManager packageManager = context.getPackageManager();
    final Intent intent = new Intent(action);
    List<ResolveInfo> list =
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return list.size() > 0;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.add_photo:
        dispatchChoosePictureIntent();
        return true;
      case R.id.take_photo:
        dispatchTakePictureIntent();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.image_grid_menu, menu);
    if (!isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
      menu.findItem(R.id.take_photo).setEnabled(false);
      menu.findItem(R.id.take_photo).setVisible(false);
      menu.findItem(R.id.take_photo).setActionView(View.GONE);
    }
    return true;
  }

  public void dispatchChoosePictureIntent() {
    Intent i =
        new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(i, ADD_PICTURE);
  }

  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(takePictureIntent, TAKE_PICTURE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ADD_PICTURE || requestCode == TAKE_PICTURE) {
      if (resultCode == RESULT_OK && null != data) {
        Uri imageUri = data.getData();

        ImageProvider.getInstance().addImage(getContentResolver(), getPathFromUri(imageUri));
      }
    }
  }

  public String getPathFromUri(Uri imageUrl) {
    String[] filePathColumn = {MediaStore.Images.Media.DATA};
    Cursor cursor = getContentResolver().query(imageUrl, filePathColumn, null, null, null);
    cursor.moveToFirst();
    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
    String picturePath = cursor.getString(columnIndex);
    Log.i(TAG, "getPathFromUri " + imageUrl + " => " + picturePath);
    return picturePath;
  }

}
