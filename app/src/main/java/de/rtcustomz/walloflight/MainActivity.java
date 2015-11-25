package de.rtcustomz.walloflight;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int CHOOSE_PICTURE_REQUEST = 1;
    public static final int READ_EXTERNAL_STORAGE_REQUEST = 1;
    private ImageView imageView;
    private Bitmap scaledImage = Bitmap.createBitmap(88,88, Bitmap.Config.ARGB_8888);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // user is stupid, without permissions we cannot open image, so just close the app
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_PICTURE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();

                BitmapWorkerTask task = new BitmapWorkerTask(scaledImage, imageView, getContentResolver());
                task.execute(imageUri);

                sendbutton.setVisibility(Button.VISIBLE);
            }
        }
    }

    public void selectPicture(View view) {
        Intent imageIntent = new Intent(Intent.ACTION_PICK/*Intent.ACTION_GET_CONTENT*/);
        imageIntent.setType("image/*");

        PackageManager packageManager = getPackageManager();
        List activities = packageManager.queryIntentActivities(imageIntent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean isIntentSafe = activities.size() > 0;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if(isIntentSafe) {
            if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
                // let the user choose a picture to sent
                startActivityForResult(Intent.createChooser(imageIntent, getString(R.string.choosePictureRequest)), CHOOSE_PICTURE_REQUEST);
            } else {
                // ask user for READ_EXTERNAL_STORAGE permission
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, READ_EXTERNAL_STORAGE_REQUEST);
            }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.selectPictureErrorToast), Toast.LENGTH_LONG).show();
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
                Thread t = new Thread(new Client(scaledImage,2.8));
                t.start();
            }
            else {
                Toast.makeText(getApplicationContext(), getString(R.string.imageErrorToast), Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), getString(R.string.wifiErrorToast), Toast.LENGTH_LONG).show();
        }
    }
}
