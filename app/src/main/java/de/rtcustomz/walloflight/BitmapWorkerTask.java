package de.rtcustomz.walloflight;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class BitmapWorkerTask extends AsyncTask<Uri, Void, Void> {
    private final WeakReference<ContentResolver> contentResolverReference;
    private final WeakReference<ImageView> imageViewReference;
    private final WeakReference<Client> clientReference;
    private Bitmap image;

    public BitmapWorkerTask(ImageView imageView, ContentResolver contentResolver, Client client) {
        contentResolverReference = new WeakReference<>(contentResolver);
        imageViewReference = new WeakReference<>(imageView);
        clientReference = new WeakReference<>(client);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        // show image if ready to show
        final ImageView imageView = imageViewReference.get();

        imageView.setImageBitmap(image);
    }

    @Override
    protected Void doInBackground(Uri... params) {
        Uri imageUri = params[0];

        Bitmap scaledImage = null;
        final Client client = clientReference.get();

        try {
            image = decodeBitmapFromUri(imageUri, 200, 200);

            File croppedImage = new File(imageUri.getPath());
            if (croppedImage.exists()) {
                croppedImage.delete();
            }

            // we can show image, because we have decoded it
            publishProgress();

            scaledImage = Bitmap.createScaledBitmap(image, 88, 88, false);

            client.sendImage(scaledImage);

        } catch (IOException ignore) {
        } // someone was very stupid ..
        return null;
    }

//    @Override
//    protected void onPostExecute(Bitmap bitmap) {
//        if (bitmap != null) {
//            //TODO: that's not very efficient...
//            final Bitmap scaledBitmap = scaledImageReference.get();
//
//            int bytes = bitmap.getByteCount();
//            ByteBuffer buffer = ByteBuffer.allocate(bytes);
//
//            bitmap.copyPixelsToBuffer(buffer);
//            buffer.rewind();
//
//            scaledBitmap.copyPixelsFromBuffer(buffer);
//        }
//    }

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
