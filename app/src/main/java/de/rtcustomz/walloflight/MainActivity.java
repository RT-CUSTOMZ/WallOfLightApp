package de.rtcustomz.walloflight;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService.Result;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static final int CHOOSE_PICTURE_REQUEST = 1;
    private ImageView imageView;
    private Bitmap scaledImage = null;
    private Button sendbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = (ImageView) findViewById(R.id.imageView);
        sendbutton = (Button) findViewById(R.id.sendPacketsButton);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_PICTURE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                imageView.setImageURI(imageUri);

                try {
                    Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    scaledImage = Bitmap.createScaledBitmap(image, 88, 88, false);
                    //imageView.setImageBitmap(Bitmap.createScaledBitmap(image, 88, 88, false));
                    sendbutton.setVisibility(Button.VISIBLE);
                } catch (IOException ignore) {}  // someone was very stupid ...

            }
        }
    }

    public void selectPicture(View view) {
        Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        imageIntent.setType("image/*");

        PackageManager packageManager = getPackageManager();
        List activities = packageManager.queryIntentActivities(imageIntent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean isIntentSafe = activities.size() > 0;

        if(isIntentSafe) {
            startActivityForResult(Intent.createChooser(imageIntent, "Bild auswählen"), CHOOSE_PICTURE_REQUEST);
        }
    }

    public void sendUDPPacket(View view) {
        //
        boolean wifiConnected;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
        } else {
            wifiConnected=false;
        }
        //
        if(wifiConnected) {
            if(scaledImage!=null) {
                Thread t = new Thread(new Client(scaledImage));
                t.start();
            }
            else {
                Toast.makeText(getApplicationContext(), "Fehler mit Bild", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Kein WLAN", Toast.LENGTH_LONG).show();
        }
    }
}
