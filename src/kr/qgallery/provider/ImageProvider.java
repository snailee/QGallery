package kr.qgallery.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

public class ImageProvider extends ContentProvider {
  private static final String TAG = ImageProvider.class.getName();
  List<String> mImageUrls = new ArrayList<String>();
  int mCurrentItem;

  public static final String EXTRA_IMAGE = "extra_image";

  public static final String IMAGE_CACHE_DIR = "images";

  private static ImageProvider sInstance;

  public static ImageProvider getInstance() {
    if (sInstance == null) {
      sInstance = new ImageProvider();
    }
    return sInstance;
  }

  public void addImage(String imageUrl) {
    mImageUrls.add(imageUrl);
  }

  public void addImage(ContentResolver cr, String imageUrl) {
    ContentValues values = new ContentValues(1);
    values.put(ImageProviderContract.IMAGE_URL_COLUMN, imageUrl);
    cr.insert(ImageProviderContract.CONTENT_URI_IMAGES, values);
  }

  public void deleteImage(ContentResolver cr, int id) {
    cr.delete(Uri.withAppendedPath(ImageProviderContract.CONTENT_URI_IMAGES, "" + id), null, null);
  }

  public String getImage(int position) {
    return mImageUrls.get(position);
  }

  public int count() {
    return mImageUrls.size();
  }

  private static final int IMAGE_URLS = 1;
  private static final int IMAGE_URLS_ID = 2;

  public static final int INVALID_URI = -1;

  private static final UriMatcher sUriMatcher;
  private static final SparseArray<String> sMimeTypes;

  static {
    sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    sUriMatcher.addURI(ImageProviderContract.AUTHORITY, ImageProviderContract.IMAGES_TABLE_NAME,
        IMAGE_URLS);
    sUriMatcher.addURI(ImageProviderContract.AUTHORITY, ImageProviderContract.IMAGES_TABLE_NAME
        + "/#", IMAGE_URLS_ID);

    sMimeTypes = new SparseArray<String>();
    sMimeTypes.put(IMAGE_URLS, "vnd.android.cursor.dir/vnd." + ImageProviderContract.AUTHORITY
        + "." + ImageProviderContract.IMAGES_TABLE_NAME);
  }

  private DatabaseHelper mDbHelper;

  @Override
  public boolean onCreate() {
    mDbHelper = new DatabaseHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder) {
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    // Decodes the content URI and maps it to a code
    switch (sUriMatcher.match(uri)) {
      case IMAGE_URLS:
        // Does the query against a read-only version of the database
        Cursor returnCursor =
            db.query(ImageProviderContract.IMAGES_TABLE_NAME, projection, null, null, null, null,
                null);
        // Sets the ContentResolver to watch this content URI for data changes
        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return returnCursor;
      case INVALID_URI:
        throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
    }

    return null;
  }

  @Override
  public String getType(Uri uri) {
    return sMimeTypes.get(sUriMatcher.match(uri));
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    switch (sUriMatcher.match(uri)) {
      case IMAGE_URLS:
        SQLiteDatabase localSQLiteDatabase = mDbHelper.getWritableDatabase();

        // Inserts the row into the table and returns the new row's _id value
        long id =
            localSQLiteDatabase.insert(ImageProviderContract.IMAGES_TABLE_NAME,
                ImageProviderContract.IMAGE_URL_COLUMN, values);

        // If the insert succeeded, notify a change and return the new row's content URI.
        if (-1 != id) {
          getContext().getContentResolver().notifyChange(uri, null);
          return Uri.withAppendedPath(uri, Long.toString(id));
        } else {
          throw new SQLiteException("Insert error:" + uri);
        }
      case INVALID_URI:
        throw new IllegalArgumentException("Insert: Invalid URI" + uri);
    }
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    // Opens the database object in "write" mode.
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    String finalWhere;

    int count;

    Log.i(TAG, "delete: " + uri);

    // Does the delete based on the incoming URI pattern.
    switch (sUriMatcher.match(uri)) {
    // If the incoming pattern matches the general pattern for notes, does a delete
    // based on the incoming "where" columns and arguments.
      case IMAGE_URLS:
        count = db.delete(ImageProviderContract.IMAGES_TABLE_NAME, // The database table name
            selection, // The incoming where clause column names
            selectionArgs // The incoming where clause values
            );
        break;

      // If the incoming URI matches a single note ID, does the delete based on the
      // incoming data, but modifies the where clause to restrict it to the
      // particular note ID.
      case IMAGE_URLS_ID:
        /*
         * Starts a final WHERE clause by restricting it to the desired note ID.
         */
        finalWhere = ImageProviderContract._ID + // The ID column name
            " = " + // test for equality
            uri.getLastPathSegment();

        // If there were additional selection criteria, append them to the final
        // WHERE clause
        if (selection != null) {
          finalWhere = finalWhere + " AND " + selection;
        }

        // Performs the delete.
        count = db.delete(ImageProviderContract.IMAGES_TABLE_NAME, // The database table name.
            finalWhere, // The final WHERE clause
            selectionArgs // The incoming where clause values.
            );
        break;

      // If the incoming pattern is invalid, throws an exception.
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }

    /*
     * Gets a handle to the content resolver object for the current context, and notifies it that
     * the incoming URI changed. The object passes this along to the resolver framework, and
     * observers that have registered themselves for the provider are notified.
     */
    getContext().getContentResolver().notifyChange(uri, null);

    // Returns the number of rows deleted.
    return count;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException("Delete -- unsupported operation " + uri);
  }

  private static final String TEXT_TYPE = "TEXT";
  private static final String PRIMARY_KEY_TYPE = "INTEGER PRIMARY KEY";
  @SuppressWarnings("unused")
  private static final String INTEGER_TYPE = "INTEGER";

  // Defines an SQLite statement that builds the Picasa picture URL table
  private static final String CREATE_IMAGE_URL_TABLE_SQL = "CREATE TABLE" + " "
      + ImageProviderContract.IMAGES_TABLE_NAME + " " + "(" + " " + ImageProviderContract.ROW_ID
      + " " + PRIMARY_KEY_TYPE + " ," + ImageProviderContract.IMAGE_URL_COLUMN + " " + TEXT_TYPE
      + ")";

  /**
   * Helper class that actually creates and manages the provider's underlying data repository.
   */
  protected static final class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
      super(context, ImageProviderContract.DATABASE_NAME, null,
          ImageProviderContract.DATABASE_VERSION);
    }

    private void dropTables(SQLiteDatabase db) {
      // If the table doesn't exist, don't throw an error
      db.execSQL("DROP TABLE IF EXISTS " + ImageProviderContract.IMAGES_TABLE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(CREATE_IMAGE_URL_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(DatabaseHelper.class.getName(), "Upgrading database from version " + oldVersion
          + " to " + newVersion + ", which will destroy all the existing data");
      dropTables(db);
      onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int newVersion, int oldVersion) {
      Log.w(DatabaseHelper.class.getName(), "Downgrading database from version " + newVersion
          + " to " + oldVersion + ", which will destroy all the existing data");
      dropTables(db);
      onCreate(db);
    }
  }
}
