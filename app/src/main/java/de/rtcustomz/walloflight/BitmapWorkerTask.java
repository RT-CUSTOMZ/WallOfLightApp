package de.rtcustomz.walloflight;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.FileNameMap;
import java.net.URLConnection;

public class BitmapWorkerTask extends AsyncTask<Uri, Void, BitmapWorkerTask.Type> {
    private static final String TAG = "BitmapWorkerTask";

    private final WeakReference<ContentResolver> contentResolverReference;
    public Bitmap image;
    public byte[] imageData;

    public static final String GIF_MIMETYPE = "image/gif";

    public static enum Type {
        GIF, OTHER
    }

    public BitmapWorkerTask(ContentResolver contentResolver) {
        contentResolverReference = new WeakReference<>(contentResolver);
    }

    @Override
    protected Type doInBackground(Uri... params) {
        final Uri imageUri = params[0];

        ContentResolver contentResolver = contentResolverReference.get();

        String mimeType = getMimeType(imageUri, contentResolver);
        Log.e(TAG, "MIMEType: " + mimeType);

        if(mimeType == null)
            return null;

        if(mimeType.equals(GIF_MIMETYPE)) {
            InputStream is = null;

            try {
                is = contentResolver.openInputStream(imageUri);

                imageData = IOUtils.toByteArray(is);

                return Type.GIF;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Couldn't find file", e);
            } catch (IOException e) {
                Log.e(TAG, "IO exception", e);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory during decoding image " + imageUri.getPath(), e);
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) { }
            }
        } else {
            try {
                image = decodeBitmapFromUri(imageUri, 200, 200);

                /*File croppedImage = new File(imageUri.getPath());
                if (croppedImage.exists()) {
                    croppedImage.delete();
                }*/

                return Type.OTHER;
            } catch (IOException e) {
                Log.e(TAG, "IO exception", e);
            }
        }

        return null;
    }

    public static int calculateInSampleSize(int origWidth, int origHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (origHeight > reqHeight || origWidth > reqWidth) {

            final int halfHeight = origHeight / 2;
            final int halfWidth = origWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static String getMimeType(Uri uri, ContentResolver contentResolver) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = contentResolver.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }

    private Bitmap decodeBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) throws IOException {
        ContentResolver contentResolver = contentResolverReference.get();

        if (contentResolver == null) {
            throw new IOException("There was an error opening the image, please try again later!");
        }

        InputStream is = contentResolver.openInputStream(imageUri);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap srcBitmap;
        is = contentResolver.openInputStream(imageUri);

        srcBitmap = BitmapFactory.decodeStream(is, null, options);

        is.close();

        int orientation = getOrientation(contentResolver, imageUri);

        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), matrix, true);
        }
        return srcBitmap;
    }

    private int getOrientation(ContentResolver contentResolver, Uri photoUri) {
        File imageFile = new File(photoUri.getPath());

        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            // We only recognize a subset of orientation tag values
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            Log.e("WallOfLightApp", e.getMessage());
            return 0;
        }
    }
}
