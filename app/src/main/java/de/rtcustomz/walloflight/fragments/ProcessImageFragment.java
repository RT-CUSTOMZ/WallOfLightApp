package de.rtcustomz.walloflight.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import com.felipecsl.gifimageview.library.GifImageView;
import com.soundcloud.android.crop.Crop;

import java.io.File;

import de.rtcustomz.walloflight.MainActivity;
import de.rtcustomz.walloflight.R;
import de.rtcustomz.walloflight.exceptions.WifiNotConnectedException;
import de.rtcustomz.walloflight.model.ImageMode;
import de.rtcustomz.walloflight.model.ImageType;
import de.rtcustomz.walloflight.util.BitmapHelperClass;
import de.rtcustomz.walloflight.util.BitmapWorkerTask;
import de.rtcustomz.walloflight.util.SendBitmapTask;

class ProcessImageFragment extends Fragment {
    private GifImageView imageView;
    private Button sendImageButton;
    private Button choosePictureButton;
    private SendBitmapTask sendBitmapTask;
    private Bitmap scaledImage;
    private ImageMode mode;

    private static final int REQUEST_GIF = 42;

	private ProcessImageFragmentArgs args;

    public ProcessImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = ProcessImageFragmentArgs.fromBundle(requireArguments());
    }

    @Override
    public void onPause() {
        stopAllAnimations();
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_process_image, container, false);
        mode = args.getMode();

        imageView = rootView.findViewById(R.id.imageView);
        sendImageButton = rootView.findViewById(R.id.sendImageButton);
        choosePictureButton = rootView.findViewById(R.id.choosePictureButton);

        switch(mode) {
            case NORMAL:
                initForNormalMode();
                break;
            case ANIMATING:
                initForAnimatingMode();
                break;
            case GIF:
                initForGifMode();
        }

        sendBitmapTask = new SendBitmapTask(getContext(), mode == ImageMode.ANIMATING);

        return rootView;
    }

    private void initForNormalMode() {
        choosePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicture();
            }
        });

        sendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendImage();
                } catch (WifiNotConnectedException e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        sendImageButton.setText(R.string.sendImageButton);
    }

    private void initForAnimatingMode() {
        choosePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicture();
            }
        });

        sendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    toggleAnimation();
                } catch (WifiNotConnectedException e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        sendImageButton.setText(R.string.toggleAnimationButton);
    }

    private void initForGifMode() {
        imageView.setOnFrameAvailable(new GifImageView.OnFrameAvailable() {
            @Override
            public Bitmap onFrameAvailable(Bitmap bitmap) {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int sampleSize = BitmapHelperClass.calculateInSampleSize(width, height, 400, 400);

                bitmap = Bitmap.createScaledBitmap(bitmap, width / sampleSize, height / sampleSize, true);

                switch (sendBitmapTask.getStatus()) {
                    case FINISHED:
                        sendBitmapTask = new SendBitmapTask(getContext(), false);
                    case PENDING:
                        sendBitmapTask.execute(bitmap);
                        break;
                }

                return bitmap;
            }
        });

        choosePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGif();
            }
        });

        sendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    toggleGifAnimation();
                } catch (WifiNotConnectedException e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        sendImageButton.setText(R.string.toggleAnimationButton);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(resultCode) {
            case Crop.RESULT_ERROR:
                Toast.makeText(getContext(), Crop.getError(data).getMessage(), Toast.LENGTH_SHORT).show();
                return;
            case AppCompatActivity.RESULT_OK:
                // everything is ok
                break;
            case AppCompatActivity.RESULT_CANCELED:
                // user has cancelled the request
                return;
            default:
                // something went wrong
                Toast.makeText(getContext(), getString(R.string.unknownError), Toast.LENGTH_SHORT).show();
                return;
        }

        switch (requestCode) {
            case Crop.REQUEST_PICK:
                final Uri imageUri = data.getData();
                if(imageUri == null)
                    return;

                final String mimeType = BitmapHelperClass.getMimeType(imageUri, getContext().getContentResolver());

                if(mimeType == null)
                    return;

                beginCrop(imageUri, mimeType);
                break;
            case Crop.REQUEST_CROP:
                new BitmapWorkerTask(getContext().getContentResolver()) {
                    @Override
                    protected void onPostExecute(ImageType imageType) {
                        processImage(image);
                    }
                }.execute(Crop.getOutput(data));
                break;
            case REQUEST_GIF:
                new BitmapWorkerTask(getContext().getContentResolver()) {
                    @Override
                    protected void onPostExecute(ImageType imageType) {
                        processGif(imageData);
                    }
                }.execute(data.getData());
                break;
        }
    }

    private void processGif(byte[] gif) {
        sendImageButton.setVisibility(Button.VISIBLE);

        imageView.setBytes(gif);
        imageView.startAnimation();
    }

    private void processImage(Bitmap image) {
        sendImageButton.setVisibility(Button.VISIBLE);

        imageView.setImageBitmap(image);
        scaledImage = image;
    }

    private void beginCrop(Uri source, String mimeType) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        Uri destination = Uri.fromFile(new File(getContext().getCacheDir(), "cropped." + extension));

        if(mode == ImageMode.ANIMATING) {
            Crop.of(source, destination).start(getContext(), this);
        } else {
            Crop.of(source, destination).asSquare().start(getContext(), this);
        }
    }

    private void selectPicture() {
        stopAllAnimations();

        if(hasPermissions()) {
            // let the user choose a picture to sent
            Crop.pickImage(getContext(), this);
        } else {
            askForPermission();
        }
    }

    private void selectGif() {
        stopAllAnimations();

        if(hasPermissions()) {
            Intent chooseGif = new Intent(Intent.ACTION_GET_CONTENT).setType("image/gif");
            startActivityForResult(chooseGif, REQUEST_GIF);
        } else {
            askForPermission();
        }
    }

    private boolean hasPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void askForPermission() {
        // ask user for READ_EXTERNAL_STORAGE permission
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MainActivity.READ_EXTERNAL_STORAGE_REQUEST);
    }

    private void stopAllAnimations() {
        if(imageView.isAnimating())
            imageView.stopAnimation();

        if(sendBitmapTask != null && !sendBitmapTask.isCancelled()) {
            sendBitmapTask.cancel(true);
        }

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void sendImage() throws WifiNotConnectedException {
        checkWifi();

        new SendBitmapTask(getContext(), false).execute(scaledImage);
    }

    private void toggleAnimation() throws WifiNotConnectedException {
        checkWifi();

        switch(sendBitmapTask.getStatus()) {
            case FINISHED:
                sendBitmapTask = new SendBitmapTask(getContext(), true);
            case PENDING:
                sendBitmapTask.execute(scaledImage);
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case RUNNING:
                stopAllAnimations();
                break;
        }
    }

    private void toggleGifAnimation() throws WifiNotConnectedException {
        if(imageView.isAnimating()) {
            stopAllAnimations();
        } else {
            checkWifi();
            imageView.startAnimation();
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void checkWifi() throws WifiNotConnectedException {
        ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

        boolean wifiConnected = false;

        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }

        if(!wifiConnected)
            throw new WifiNotConnectedException(getString(R.string.wifiErrorToast));
    }
}
