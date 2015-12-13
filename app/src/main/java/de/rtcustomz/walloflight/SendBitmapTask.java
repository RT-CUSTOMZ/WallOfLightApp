package de.rtcustomz.walloflight;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Date;

public class SendBitmapTask extends AsyncTask<Bitmap, Void, Void> {
    private final WeakReference<Client> clientReference;
    private boolean animateImage;

    SendBitmapTask(Client client, boolean animateImage) {
        clientReference = new WeakReference<>(client);
        this.animateImage = animateImage;
    }

    @Override
    protected Void doInBackground(Bitmap... params) {
        Bitmap image = params[0];

        if(image == null)
            return null;

        int sampleSize = BitmapHelperClass.calculateInSampleSize(image.getWidth(), image.getHeight(), 88, 88);
        Client client = clientReference.get();

        if(animateImage) {
            int mode = 1, x = 0, y = 0;
            int snippetScale = 88 * sampleSize;

            int pixelsMoving = sampleSize > 1 ? sampleSize / 2 : 1;

            Log.e("WallOfLightApp", "pixelsMoving: " + pixelsMoving + " imageSize: " + image.getWidth() + "x" + image.getHeight());

            Bitmap scaledImage;

            while(!isCancelled()) {
                Date start = new Date(System.currentTimeMillis());

                Bitmap snippet = Bitmap.createBitmap(image, x, y, snippetScale, snippetScale);
                scaledImage = Bitmap.createScaledBitmap(snippet, 88, 88, true);


                client.sendImage(scaledImage);

                switch(mode) {
                    case 1:
                        x+=pixelsMoving;
                        if(x+snippetScale >= image.getWidth()) {
                            x = image.getWidth()-snippetScale;
                            mode = 2;
                        }
                        break;
                    case 2:
                        y+=pixelsMoving;
                        if(y+snippetScale >= image.getHeight()) {
                            y = image.getHeight()-snippetScale;
                            mode = 3;
                        }
                        break;
                    case 3:
                        x-=pixelsMoving;
                        if(x<0) x=0;
                        if(x == 0)
                            mode = 4;
                        break;
                    case 4:
                        y-=pixelsMoving;
                        if(y<0) y=0;
                        if(y == 0)
                            mode = 1;
                        break;
                }

                Date end = new Date(System.currentTimeMillis());

                int duration = (int)(end.getTime()-start.getTime());

                if(duration < 30) {
                    try {
                        Thread.sleep(30 - duration);
                    } catch (InterruptedException e) {}
                }
            }
        } else {
            Bitmap scaledImage = Bitmap.createScaledBitmap(image, 88, 88, true);
            client.sendImage(scaledImage);
        }

        return null;
    }
}
