package de.rtcustomz.walloflight;

import android.content.ContentResolver;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;

public class BitmapHelperClass {
    public static String getMimeType(Uri uri, ContentResolver contentResolver) {
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = contentResolver.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
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

    public static int getOrientation(Uri photoUri) {
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
