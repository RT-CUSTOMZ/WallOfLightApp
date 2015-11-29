package de.rtcustomz.walloflight;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
    private final WeakReference<Bitmap> scaledImageReference;
    private final WeakReference<ContentResolver> contentResolverReference;
    private final WeakReference<ImageView> imageViewReference;
    private Bitmap image;

    public BitmapWorkerTask(Bitmap scaledImage, ImageView imageView, ContentResolver contentResolver) {
        scaledImageReference = new WeakReference<>(scaledImage);
        contentResolverReference = new WeakReference<>(contentResolver);
        imageViewReference = new WeakReference<>(imageView);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        // show image if ready to show
        final ImageView imageView = imageViewReference.get();

        imageView.setImageBitmap(image);
    }

    @Override
    protected Bitmap doInBackground(Uri... params) {
        Uri imageUri = params[0];

        Bitmap scaledImage = null;

        try {
            //image = MediaStore.Images.Media.getBitmap(contentResolverReference.get(), imageUri);
            image = decodeBitmapFromUri(imageUri, 200, 200);

            File croppedImage = new File(imageUri.getPath());
            if(croppedImage.exists()) {
                croppedImage.delete();
            }

            // we can show image, because we have decoded it
            publishProgress();

            scaledImage = Bitmap.createScaledBitmap(image, 88, 88, false);

        }  catch (IOException ignore) {} // someone was very stupid ...

        return scaledImage;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            //TODO: that's not very efficient...
            final Bitmap scaledBitmap = scaledImageReference.get();

            int bytes = bitmap.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);

            bitmap.copyPixelsToBuffer(buffer);
            buffer.rewind();

            scaledBitmap.copyPixelsFromBuffer(buffer);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) throws IOException {
        ContentResolver contentResolver = contentResolverReference.get();

        if(contentResolver == null) {
            throw new IOException("There was an error opening the image, please try again later!");
        }

        InputStream is = contentResolver.openInputStream(imageUri);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

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
        Cursor cursor = contentResolver.query(photoUri,
               new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();

        int orientation = cursor.getInt(0);
        cursor.close();

        return orientation;
    }
}
