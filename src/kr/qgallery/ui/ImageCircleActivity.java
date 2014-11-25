package kr.qgallery.ui;

import kr.qgallery.R;
import kr.qgallery.provider.ImageProvider;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ImageCircleActivity extends ImageViewActivity {
  private static final boolean DEBUG = true;
  private static final String TAG = ImageCircleActivity.class.getName();

  public static final int EXTRA_ACCESSORY_COVER_OPENED = 0;
  public static final int EXTRA_ACCESSORY_COVER_CLOSED = 1;
  public static final String EXTRA_ACCESSORY_COVER_STATE =
      "com.lge.intent.extra.ACCESSORY_COVER_STATE";
  public static final String ACTION_ACCESSORY_COVER_EVENT =
      "com.lge.android.intent.action.ACCESSORY_COVER_EVENT";

  public static final String QUICKCOVERSETTINGS_QUICKCOVER_ENABLE = "quick_view_enable";
  public static final int QUICKCOVERSETTINGS_QUICKCIRCLE = 3;

  boolean quickCircleEnabled = false;
  int quickCaseType = 0;
  boolean quickCircleClosed = true;

  int circleWidth = 0;
  int circleHeight = 0;
  int circleXpos = 0;
  int circleYpos = 0;
  int circleDiameter = 0;

  private int mQuickCoverState = 0;
  private Context mContext;

  // For idle time detection and screen brightness adjustment
  private static final int IDLE_DETECTION_INTERVAL = 5000;
  private boolean mIsBrightnessOff;
  private Thread mIdleDetectionThread;
  private boolean mStopIdleDetection;
  private long mLastUserInteractionTimeMillis;
  private final long mIdlePeriodLimit = 10 * 1000; // screen brightness off after 10
  // seconds idle time
  Object mIdleDetectionLock = new Object();

  @Override
  protected int getLayoutResourceId() {
    return R.layout.image_circle_activity;
  }

  private static class ImageCircleFragment extends ImageViewFragment {
    @Override
    protected int getLayoutResourceId() {
      return R.layout.image_circle_fragment;
    }
  }

  @Override
  protected Fragment createFragment(int id, String imageUrl) {
    final ImageViewFragment f = new ImageCircleFragment();

    final Bundle args = new Bundle();
    args.putInt(ImageViewFragment.EXTRA_DATA_IMAGE_ID, id);
    args.putString(ImageViewFragment.EXTRA_DATA_IMAGE_URL, imageUrl);
    f.setArguments(args);

    return f;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);

    // Register an IntentFilter and a broadcast receiver
    mContext = getApplicationContext();
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_ACCESSORY_COVER_EVENT);
    mContext.registerReceiver(mIntentReceiver, filter);

    setQuickCircleWindowParam();

    fetchQuickCircleDimensions();
    
    final View circle = findViewById(R.id.circle);

    // Crops a layout for the QuickCircle window
    FrameLayout.LayoutParams layoutParam = (FrameLayout.LayoutParams) circle.getLayoutParams();

    // Set layout size same as a circle window size
    layoutParam.width = circleDiameter;
    layoutParam.height = circleDiameter;

    if (circleXpos < 0) {
      // Place a layout to the center
      // layoutParam.addRule(RelativeLayout.CENTER_HORIZONTAL,
      // RelativeLayout.CENTER_IN_PARENT);
      layoutParam.gravity = Gravity.CENTER_HORIZONTAL;
    } else {
      layoutParam.leftMargin = circleXpos;
    }

    // Set top margin to the offset
    if (("g3".equals(android.os.Build.DEVICE))||("tiger6".equals(android.os.Build.DEVICE))) {
      layoutParam.topMargin = circleYpos;
      Log.i(TAG, "topMargin :" + circleYpos);
    } else {
      layoutParam.topMargin = circleYpos + (circleHeight - circleDiameter) / 2;
      Log.i(TAG, "topMargin :" + (circleYpos + (circleHeight - circleDiameter) / 2));
    }

    circle.setLayoutParams(layoutParam);
    
    
    //sujin.cho
    //show information
    String info = "Open the cover to add pictures.";
    TextView tv = new TextView(mContext);
    tv.setText(info);
    tv.setWidth((int)(circleDiameter * 0.7));
    tv.setHeight((int)(circleDiameter * 0.3));
    tv.setBackgroundColor(Color.rgb(230, 240, 240));
    tv.setPadding(10, 10, 10, 10);
    tv.setGravity(Gravity.CENTER);
    tv.setTextColor(Color.BLACK);  
    Toast infoToast = new Toast(mContext);
    infoToast.setView(tv);
    infoToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, layoutParam.topMargin);
    infoToast.setDuration(Toast.LENGTH_LONG);
    infoToast.show();
    
    
    findViewById(R.id.back_btn).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ImageCircleActivity.this.finish();
      }
    });
  }

  private void fetchQuickCircleDimensions() {
    ContentResolver cr = getContentResolver();
    quickCircleEnabled =
        Settings.Global.getInt(cr, QUICKCOVERSETTINGS_QUICKCOVER_ENABLE, 1) == 1 ? true : false;
    if (DEBUG) {
      Log.d(TAG, "quickCircleEnabled:" + quickCircleEnabled);
    }
 

    quickCaseType = Settings.Global.getInt(cr, "cover_type", 0/* default value */);

    int id =
        getResources().getIdentifier("config_circle_window_width", "dimen", "com.lge.internal");
    circleWidth = getResources().getDimensionPixelSize(id);
    if (DEBUG) {
      Log.d(TAG, "circleWidth:" + circleWidth);
    }

    id = getResources().getIdentifier("config_circle_window_height", "dimen", "com.lge.internal");
    circleHeight = getResources().getDimensionPixelSize(id);
    if (DEBUG) {
      Log.d(TAG, "circleHeight:" + circleHeight);
    }

    id = getResources().getIdentifier("config_circle_window_x_pos", "dimen", "com.lge.internal");
    circleXpos = getResources().getDimensionPixelSize(id);
    if (DEBUG) {
      Log.d(TAG, "circleXpos:" + circleXpos);
    }

    id = getResources().getIdentifier("config_circle_window_y_pos", "dimen", "com.lge.internal");
    circleYpos = getResources().getDimensionPixelSize(id);
    if (DEBUG) {
      Log.d(TAG, "circleYpos:" + circleYpos);
    }

    id = getResources().getIdentifier("config_circle_diameter", "dimen", "com.lge.internal");
    circleDiameter = getResources().getDimensionPixelSize(id);
    if (DEBUG) {
      Log.d(TAG, "circleDiameter:" + circleDiameter);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    
  //sujin.cho
 // remove to follow the original QCircle UX Screnario
  //  startIdleDetectionThread();
  }

  @Override
  protected void onPause() {
    super.onPause();

//sujin.cho
// remove to follow the original QCircle UX Screnario
//    stopIdleDetectionThread();
  }

  
  @Override
  public void onUserInteraction() {
    super.onUserInteraction();
    setBrightnessFull();
    mLastUserInteractionTimeMillis = System.currentTimeMillis();
    synchronized (mIdleDetectionLock) {
      mIdleDetectionLock.notify();
    }
  }

  private void startIdleDetectionThread() {
    mStopIdleDetection = false;
    mLastUserInteractionTimeMillis = System.currentTimeMillis();
    mIdleDetectionThread = new Thread(new Runnable() {
      @Override
      public void run() {
        long idle = 0;
        while (!mStopIdleDetection) {
          idle = System.currentTimeMillis() - mLastUserInteractionTimeMillis;
          if (DEBUG) {
            Log.d(TAG, "Application is idle for " + idle + " ms");
          }
          if (idle >= mIdlePeriodLimit) {
            if (DEBUG) {
              Log.d(TAG, "Idle time(" + idle + " ms) exceeds the limt (" + mIdlePeriodLimit
                  + " ms)");
            }
            idle = 0;
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                setBrightnessOff();
                // setBrightnessCompletelyOff();
              }
            });
          }
          try {
            synchronized (mIdleDetectionLock) {
              mIdleDetectionLock.wait(IDLE_DETECTION_INTERVAL);
            }
          } catch (InterruptedException e) {
            if (DEBUG) {
              Log.d(TAG, "IdleDetectionThread interrupted!");
            }
          }
        }
        if (DEBUG) {
          Log.d(TAG, "Finishing IdleDetectionThread");
        }
      }
    });
    mIdleDetectionThread.start();
  }

  private void stopIdleDetectionThread() {
    mStopIdleDetection = true;
    mIdleDetectionThread.interrupt();
  }

  private void setBrightnessOff() {
    if (mIsBrightnessOff) {
      return;
    }
    Window win = getWindow();
    WindowManager.LayoutParams lp = win.getAttributes();
    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
    win.setAttributes(lp);
    mIsBrightnessOff = true;
  }

  @SuppressWarnings("unused")
  private void setBrightnessCompletelyOff() {
    if (!mIsBrightnessOff) {
      return;
    }
    Window win = getWindow();
    WindowManager.LayoutParams lp = win.getAttributes();
    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
    lp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    win.setAttributes(lp);
    mIsBrightnessOff = true;
  }

  private void setBrightnessFull() {
    if (!mIsBrightnessOff) {
      return;
    }
    Window win = getWindow();
    WindowManager.LayoutParams lp = win.getAttributes();
    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
    win.setAttributes(lp);
    mIsBrightnessOff = false;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mContext.unregisterReceiver(mIntentReceiver);
  }

  private void setQuickCircleWindowParam() {
    Window win = getWindow();
    if (win != null) {
      win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
          | WindowManager.LayoutParams.FLAG_FULLSCREEN
          | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) {
				return;
			}

			if (ACTION_ACCESSORY_COVER_EVENT.equals(action)) {
				if (DEBUG) {
					Log.d(TAG, "ACTION_ACCESSORY_COVER_EVENT");
				}

				mQuickCoverState = intent.getIntExtra(
						EXTRA_ACCESSORY_COVER_STATE,
						EXTRA_ACCESSORY_COVER_OPENED);

				if (DEBUG) {
					Log.d(TAG, "mQuickCoverState:" + mQuickCoverState);
				}

				if (mQuickCoverState == EXTRA_ACCESSORY_COVER_CLOSED) { // closed
					setQuickCircleWindowParam();
				} 
				else if (mQuickCoverState == EXTRA_ACCESSORY_COVER_OPENED) { // opened
					launchImageDetailActivity();
					ImageCircleActivity.this.finish();
				}
			}
		}
	};

  private void launchImageDetailActivity() {
    Intent intent = new Intent(this, ImageDetailActivity.class);
    intent.putExtra(ImageProvider.EXTRA_IMAGE, getViewPager().getCurrentItem());
    //sujin.cho
    //just start an activity
    startActivity(intent);
    
   // TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities();
  }
  
}
