package kr.qgallery.provider;

import android.net.Uri;
import android.provider.BaseColumns;


public final class ImageProviderContract implements BaseColumns {
  public static final String SCHEME = "content";
  public static final String AUTHORITY = "kr.qgallery";

  public static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY);

  public static final String MIME_TYPE_ROWS = "vnd.android.cursor.dir/vnd.kr.qgallery";
  public static final String MIME_TYPE_SINGLE_ROW = "vnd.android.cursor.item/vnd.kr.qgallery";

  public static final String ROW_ID = BaseColumns._ID;

  public static final String IMAGES_TABLE_NAME = "images";
  public static final Uri CONTENT_URI_IMAGES = Uri.withAppendedPath(CONTENT_URI, IMAGES_TABLE_NAME);

  public static final String IMAGE_URL_COLUMN = "image_url";

  // The content provider database name
  public static final String DATABASE_NAME = "kr.qgallery.db";

  // The starting version of the database
  public static final int DATABASE_VERSION = 1;
}
