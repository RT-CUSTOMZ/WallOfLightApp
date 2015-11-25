package de.rtcustomz.walloflight;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
    private final WeakReference<Bitmap> scaledImageReference;
    private final WeakReference<ContentResolver> contentResolverReference;
    private final WeakReference<ImageView> imageViewReference;
    private Uri imageUri;
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

        imageView.setImageURI(imageUri);
    }

    @Override
    protected Bitmap doInBackground(Uri... params) {
        imageUri = params[0];

        // we can show image, because we have the path to it
        publishProgress();

        Bitmap scaledImage = null;

        try {
            image = MediaStore.Images.Media.getBitmap(contentResolverReference.get(), imageUri);

            // TODO: scale image, because we don't want to show full sized image in imageView
            // TODO: rotate image if wrong orientation

            //ExifInterface exif = new ExifInterface(imageUri.getPath());
            //int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            scaledImage = Bitmap.createScaledBitmap(image, 88, 88, false);

        }  catch (IOException ignore) {} // someone was very stupid ...

        return scaledImage;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            final Bitmap scaledBitmap = scaledImageReference.get();

            int bytes = bitmap.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);

            bitmap.copyPixelsToBuffer(buffer);
            buffer.rewind();

            scaledBitmap.copyPixelsFromBuffer(buffer);
        }
    }
}
