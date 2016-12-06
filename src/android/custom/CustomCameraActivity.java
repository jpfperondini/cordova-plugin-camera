package org.apache.cordova.camera.custom;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.cordova.camera.CordovaUri;
import org.apache.cordova.camera.custom.fragments.CameraFragment;
import org.apache.cordova.camera.custom.fragments.ConfirmationFragment;
import org.apache.cordova.camera.custom.listeners.ConfirmationListener;
import org.apache.cordova.camera.custom.listeners.PictureTakenListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CustomCameraActivity extends Activity implements PictureTakenListener, ConfirmationListener {

    private static final String TAG = CustomCameraActivity.class.getName();
    public static final String PREVIEW_OVERLAY = "previewOverlay";

    private Uri mUriOutput;
    private FakeR fakeR;
    private boolean previewOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (this.getIntent().getExtras().containsKey(PREVIEW_OVERLAY)) {
            previewOverlay = this.getIntent().getExtras().getBoolean(PREVIEW_OVERLAY);
        } else previewOverlay = false;

        fakeR = new FakeR(this);

        setContentView(fakeR.getId("layout", "scan_layout"));

        mUriOutput = (Uri) getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);

        openCamera();

    }

    public void openCamera() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(MediaStore.EXTRA_OUTPUT, mUriOutput);
        Fragment cameraFragment = new CameraFragment();
        cameraFragment.setArguments(bundle);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(fakeR.getId("id", "content"), cameraFragment, "Camera_Fragment");
        transaction.commit();
    }

    /**
     * Called after picture taken. Calls the fragment to crop.
     */
    @Override
    public void onPictureTaken(Bitmap bitmap) {

        ConfirmationFragment fragment = new ConfirmationFragment();

        fragment.setOriginalBitmap(bitmap);
        fragment.setPreviewOverlay(previewOverlay);

        android.app.FragmentManager fragmentManager = getFragmentManager();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.remove(getFragmentManager().findFragmentByTag("Camera_Fragment"));

        fragmentTransaction.replace(fakeR.getId("id", "content"), fragment);

        fragmentTransaction.addToBackStack(ConfirmationFragment.class.toString());

        fragmentTransaction.commit();
    }

    /**
     * Called after crop
     */
    @Override
    public void confirm(Bitmap bitmap) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // At this point, the image was already compressed

        try {
            baos.flush();
            baos.close();
        } catch (IOException e) {
            // Ignore
        }

        byte[] imageData = baos.toByteArray();

        CordovaUri cordovaUri = new CordovaUri(mUriOutput);

        File pictureFile = new File(cordovaUri.getFilePath());
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(imageData);
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        setResult(RESULT_OK);

        System.gc();

        finish();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

}
