package kr.qgallery.ui;


import com.example.android.displayingbitmaps.util.ImageFetcher;
import com.example.android.displayingbitmaps.util.ImageWorker;
import com.example.android.displayingbitmaps.util.Utils;

import kr.qgallery.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public abstract class ImageViewFragment extends Fragment {
  protected static final String EXTRA_DATA_IMAGE_URL = "extra_image_url";
  protected static final String EXTRA_DATA_IMAGE_ID = "extra_image_id";

  protected int mId;
  protected String mImageUrl;
  protected ImageView mImageView;
  protected ImageFetcher mImageFetcher;

  protected abstract int getLayoutResourceId();

  /**
   * Populate image using a url from extras, use the convenience factory method
   * {@link ImageDetailFragment#newInstance(String)} to create this fragment.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mId = getArguments() != null ? getArguments().getInt(EXTRA_DATA_IMAGE_ID) : -1;
    mImageUrl = getArguments() != null ? getArguments().getString(EXTRA_DATA_IMAGE_URL) : null;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate and locate the main ImageView
    final View v = inflater.inflate(getLayoutResourceId(), container, false);
    mImageView = (ImageView) v.findViewById(R.id.imageView);
    return v;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // Use the parent activity to load the image asynchronously into the
    // ImageView (so a single
    // cache can be used over all pages in the ViewPager
    if (ImageViewActivity.class.isInstance(getActivity())) {
      mImageFetcher = ((ImageViewActivity) getActivity()).getImageFetcher();
      mImageFetcher.loadImage(mImageUrl, mImageView);
    }

    // Pass clicks on the ImageView to the parent activity to handle
    if (OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
      mImageView.setOnClickListener((OnClickListener) getActivity());
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mImageView != null) {
      // Cancel any pending image work
      ImageWorker.cancelWork(mImageView);
      mImageView.setImageDrawable(null);
    }
  }
}
