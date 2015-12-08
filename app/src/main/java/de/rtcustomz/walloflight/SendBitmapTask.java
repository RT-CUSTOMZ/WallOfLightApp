package de.rtcustomz.walloflight;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public class SendBitmapTask extends AsyncTask<Bitmap, Void, Void> {
    private final WeakReference<Client> clientReference;

    SendBitmapTask(Client client) {
        clientReference = new WeakReference<>(client);
    }

    @Override
    protected Void doInBackground(Bitmap... params) {
        Bitmap image = params[0];

        if(image == null)
            return null;

        Bitmap scaledImage = Bitmap.createScaledBitmap(image, 88, 88, false);
        Client client = clientReference.get();

        client.sendImage(scaledImage);

        return null;
    }
}
