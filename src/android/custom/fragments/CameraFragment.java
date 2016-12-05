
package org.apache.cordova.camera.custom.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.cordova.camera.custom.BitmapUtils;
import org.apache.cordova.camera.custom.FakeR;
import org.apache.cordova.camera.custom.ImageSettings;
import org.apache.cordova.camera.custom.listeners.PictureTakenListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author ecpereira - Ewerton Cavalcante
 *         <p/>
 *         Fragment that manages the custom camera and handle the flow after the picture taken
 */
public class CameraFragment extends Fragment implements OnClickListener {

    private SurfaceView mPreview;

    private SurfaceHolder mPreviewHolder;

    private Camera mCamera;

    private ProgressDialog mDialog;

    private OrientationEventListener mOrientationListener;

    private PictureTakenListener mPictureTakenListener;

    private ImageView mFlash;

    private boolean mInPreview = false;

    private int mOrientationDegrees;

    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private FakeR fakeR;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Context context = getActivity();
        mPictureTakenListener = (PictureTakenListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        fakeR = new FakeR(getActivity());
        int fragment_camera = fakeR.getId("layout", "fragment_camera");
        int captureButton = fakeR.getId("id", "captureButton");
        int flipCamera = fakeR.getId("id", "flipCamera");
        int flashButton = fakeR.getId("id", "flashButton");
        int surface = fakeR.getId("id", "surface");

        View view = inflater.inflate(fragment_camera, container, false);

        ImageView image = (ImageView) view.findViewById(captureButton);
        ImageView flip = (ImageView) view.findViewById(flipCamera);
        mFlash = (ImageView) view.findViewById(flashButton);
        mPreview = (SurfaceView) view.findViewById(surface);

        FlashClickListener flashClick = new FlashClickListener();
        mFlash.setOnClickListener(flashClick);

        flip.setOnClickListener(new FlipClickListener(flashClick));

        image.setOnClickListener(this);

        return view;

    }

    @Override
    public void onResume() {

        super.onResume();

        configureCameraResources();

        configurePictureSize();

        try {

            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);

        } catch (RuntimeException e) {
            // Some devices crashes with FOCUS_MODE_CONTINUOUS_PICTURE (but it
            // is better than FOCUS_MODE_AUTO) so we kept it, and added a backup
            // method to keep it working
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
        }

        mOrientationListener = new OrientationEventListener(getActivity(),
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                setupCameraOrientation();
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }

    }

    private void configureCameraResources() {
        mCamera = Camera.open();

        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);
        mPreviewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPreviewHolder.setFixedSize(getActivity().getWindow().getWindowManager()
                .getDefaultDisplay().getWidth(), getActivity().getWindow().getWindowManager()
                .getDefaultDisplay().getHeight());

        startPreview();
    }

    private void startPreview() {
        // Start fragment_camera
        Camera.Parameters parameters = mCamera.getParameters();
        Size size = getBestPreviewSize(getActivity().getWindowManager().getDefaultDisplay().getWidth(), getActivity().getWindowManager().getDefaultDisplay().getHeight(), parameters);

        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setJpegQuality(ImageSettings.JPEG_QUALITY);

        if (size != null) {
            parameters.setPreviewSize(size.width, size.height);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }

        mInPreview = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOrientationListener != null)
            mOrientationListener.disable();
    }

    private void configurePictureSize() {

        List<Size> sizes = mCamera.getParameters().getSupportedPictureSizes();
        Size maxSize = sizes.get(0);

        for (int i = 0; i < sizes.size(); i++) {

            if (sizes.get(i).width > maxSize.width) {
                maxSize = sizes.get(i);
            }
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setPictureSize(maxSize.width, maxSize.height);
        mCamera.setParameters(params);

    }

    @Override
    public void onPause() {
        releaseCameraResources();
        super.onPause();
    }

    private void releaseCameraResources() {
        if (mCamera != null) {
            mOrientationListener.disable();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mInPreview = false;
        }
    }

    private Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {

        Size sizeResult = null;

        for (Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (sizeResult == null) {
                    sizeResult = size;
                } else {
                    int resultArea = sizeResult.width * sizeResult.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        sizeResult = size;
                    }
                }
            }
        }
        return sizeResult;
    }

    private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (mCamera == null) {
                    configureCameraResources();
                }
                mCamera.setPreviewDisplay(mPreviewHolder);
            } catch (Throwable t) {
                Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCamera == null) {
                configureCameraResources();
            }
            Camera.Parameters parameters = mCamera.getParameters();
            Size size = getBestPreviewSize(width, height, parameters);

            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    /**
     * Responsible to transofm the image bytes into bitmap and continue the flow
     *
     * @param data
     */
    private void onPictureTake(byte[] data) {

        BitmapUtils.setDpi(data, ImageSettings.DENSITY);

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, ImageSettings.JPEG_QUALITY, baos);

        bitmap.recycle();
        System.gc();

        Bitmap bitmapCompressed = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length);

        if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT && mOrientationDegrees == 90) {
            mOrientationDegrees = -90;
        }

        Bitmap rotatedBitmap = BitmapUtils.rotateBitmap(bitmapCompressed, mOrientationDegrees);

        Bitmap roundedBitmap = BitmapUtils.drawAsRoundedCornerImage(rotatedBitmap);

        mPictureTakenListener.onPictureTaken(roundedBitmap);

        mOrientationListener.disable();

        mDialog.dismiss();

        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onClick(View v) {
        mCamera.takePicture(shutterCallback, null, mPhotoCallback);
        mDialog = ProgressDialog.show(getActivity(), "",
                "Processando");
    }

    private final ShutterCallback shutterCallback = new ShutterCallback() {
        @Override
        public void onShutter() {
            /* Empty Callbacks play a sound! */
        }
    };

    private final Camera.PictureCallback mPhotoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(final byte[] data, final Camera camera) {
            onPictureTake(data);
        }
    };

    /**
     * Detects the correct angle according to the orientation of the device
     */
    private void setupCameraOrientation() {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = getActivity().getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mOrientationDegrees = (info.orientation + degrees) % 360;
            mOrientationDegrees = (360 - mOrientationDegrees) % 360; // compensate the mirror
        } else {
            // selfie
            mOrientationDegrees = (info.orientation - degrees + 360) % 360;
        }

        if ((mCamera != null) && (mOrientationDegrees == 0 || mOrientationDegrees == 90 || mOrientationDegrees == 180 || mOrientationDegrees == 270)) {
            mCamera.setDisplayOrientation(mOrientationDegrees);
        }

    }

    /**
     * Responsible for handling the flash
     */
    private class FlashClickListener implements OnClickListener {

        boolean on = false;

        @Override
        public void onClick(View view) {
            if (on) {
                flashLightOff();
            } else {
                flashLightOn();
            }
        }

        public void flashLightOn() {
            on = true;
            try {
                if (getActivity().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_CAMERA_FLASH)) {
                    Camera.Parameters p = mCamera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(p);
                    mFlash.setImageResource(fakeR.getId("drawable", "btn_flash_on"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void flashLightOff() {
            on = false;
            try {
                if (getActivity().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_CAMERA_FLASH)) {
                    Camera.Parameters p = mCamera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                    mFlash.setImageResource(fakeR.getId("drawable", "btn_flash_no"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Listener responsible for alternate front and back camera
     */
    private class FlipClickListener implements OnClickListener {

        final FlashClickListener mFlashClick;

        public FlipClickListener(FlashClickListener flashClick) {
            mFlashClick = flashClick;
        }

        @Override
        public void onClick(View view) {

            mFlashClick.flashLightOff();

            if (mInPreview && mCamera != null) {
                mCamera.stopPreview();
            }

            // if you don't release the current camera before switching, you app will crash
            if (mCamera != null) {
                mCamera.release();
            }

            // swap the id of the camera to be used
            if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                disableFlash();
            } else {
                mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                disableFlash();
            }
            mCamera = Camera.open(mCurrentCameraId);

            setupCameraOrientation();

            try {

                mCamera.setPreviewDisplay(mPreviewHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }

        private void enableFlash() {
            mFlash.setVisibility(View.VISIBLE);
        }

        private void disableFlash() {
            mFlash.setVisibility(View.INVISIBLE);
        }
    }

}
