package de.rtcustomz.walloflight;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import com.felipecsl.gifimageview.library.GifImageView;
import com.google.common.net.MediaType;
import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final int READ_EXTERNAL_STORAGE_REQUEST = 1;
    private GifImageView imageView;
    private Button sendImageButton;
    private Button toggleAnimationButton;
    private Client client = new Client();
    private SharedPreferences sharedPref;
    private AlertDialog.Builder alertDialogBuilder;
    private SendBitmapTask sendBitmapTask;
    private Bitmap scaledImage;

    private boolean animateImage = false;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    Log.d("WallOfLightApp", "Preference changed: " + key);
                    updateClientSettings();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = (GifImageView) findViewById(R.id.imageView);
        sendImageButton = (Button) findViewById(R.id.sendImageButton);
        toggleAnimationButton = (Button) findViewById(R.id.toggleAnimationButton);

        imageView.setOnFrameAvailable(new GifImageView.OnFrameAvailable() {
            @Override
            public Bitmap onFrameAvailable(Bitmap bitmap) {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int sampleSize = BitmapHelperClass.calculateInSampleSize(width, height, 200, 200);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width/sampleSize, height/sampleSize, true);

                switch(sendBitmapTask.getStatus()) {
                    case FINISHED:
                        sendBitmapTask = new SendBitmapTask(MainActivity.this, client, animateImage);
                    case PENDING:
                        sendBitmapTask.execute(scaledBitmap);
                        break;
                }

                return scaledBitmap;
            }
        });

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        alertDialogBuilder = new AlertDialog.Builder(this);

        updateClientSettings();
    }

    @Override
    protected  void onPause() {
        super.onPause();

        stopAllAnimations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sharedPref.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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
            stopAllAnimations();

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

    private void processGif(byte[] gif) {
        sendImageButton.setVisibility(Button.INVISIBLE);
        toggleAnimationButton.setVisibility(Button.VISIBLE);

        animateImage = false;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageView.setBytes(gif);
    }

    private void processImage(Bitmap image) {
        toggleAnimationButton.setVisibility(Button.INVISIBLE);
        sendImageButton.setVisibility(Button.VISIBLE);

        if(animateImage) {
            sendImageButton.setText(R.string.toggleAnimationButton);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            sendImageButton.setText(R.string.sendImageButton);
        }

        imageView.setImageBitmap(image);
        scaledImage = image;
    }

    private void chooseAnimate(final Uri imageUri, final String mimeType) {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        animateImage = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        animateImage = false;
                        break;
                }

                beginCrop(imageUri, mimeType);
            }
        };

        alertDialogBuilder.setMessage(R.string.dialog_animate_image)
                .setPositiveButton(R.string.yes, buttonListener)
                .setNegativeButton(R.string.no, buttonListener)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(getContentResolver()) {
            @Override
            protected void onPostExecute(MediaType imageType) {
                if(imageType.equals(MediaType.GIF)) {
                    processGif(imageData);
                } else if(imageType.equals(MediaType.ANY_IMAGE_TYPE)) {
                    processImage(image);
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.imageErrorToast), Toast.LENGTH_SHORT).show();
                }

                sendBitmapTask = new SendBitmapTask(MainActivity.this, client, animateImage);
            }
        };

        if (requestCode == Crop.REQUEST_PICK) {
            if (resultCode == RESULT_OK) {
                final Uri imageUri = data.getData();

                final String mimeType = BitmapHelperClass.getMimeType(imageUri, getContentResolver());

                if(mimeType == null)
                    return;

                if(mimeType.equals(MediaType.GIF.toString())) {
                    bitmapWorkerTask.execute(imageUri);
                } else {
                    // let the user choose if the image should be animated
                    chooseAnimate(imageUri, mimeType);
                }
            }
        } else if (requestCode == Crop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                bitmapWorkerTask.execute(Crop.getOutput(data));
            } else if (resultCode == Crop.RESULT_ERROR) {
                Toast.makeText(this, Crop.getError(data).getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void selectPicture(View view) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // let the user choose a picture to sent
            Crop.pickImage(this);

            stopAllAnimations();
        } else {
            // ask user for READ_EXTERNAL_STORAGE permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST);
        }
    }

    private void stopAllAnimations() {
        if(imageView.isAnimating())
            imageView.stopAnimation();

        if(sendBitmapTask != null && !sendBitmapTask.isCancelled()) {
            sendBitmapTask.cancel(true);
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void beginCrop(Uri source, String mimeType) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped." + extension));

        if(animateImage) {
            Crop.of(source, destination).start(this);
        } else {
            Crop.of(source, destination).asSquare().start(this);
        }
    }

    private boolean wifiConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.isConnected()) {
            return activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
        } else {
            return false;
        }
    }

    public void sendImage(View view) {
        switch(sendBitmapTask.getStatus()) {
            case FINISHED:
                sendBitmapTask = new SendBitmapTask(MainActivity.this, client, animateImage);
            case PENDING:
                if(!wifiConnected()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.wifiErrorToast), Toast.LENGTH_LONG).show();
                    return;
                }

                sendBitmapTask.execute(scaledImage);
                if(animateImage) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;
            case RUNNING:
                stopAllAnimations();
                break;
        }
    }

    public void toggleGifAnimation(View view) {
        if(imageView.isAnimating()) {
            stopAllAnimations();
        }else {
            if(!wifiConnected()) {
                Toast.makeText(getApplicationContext(), getString(R.string.wifiErrorToast), Toast.LENGTH_LONG).show();
                return;
            }

            imageView.startAnimation();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void updateClientSettings() {
        client.setGamma(Float.valueOf(sharedPref.getString(getString(R.string.pref_key_gamma), getString(R.string.pref_default_gamma))));
        client.setIP(sharedPref.getString(getString(R.string.pref_key_ip), getString(R.string.pref_default_ip)));
    }
}
