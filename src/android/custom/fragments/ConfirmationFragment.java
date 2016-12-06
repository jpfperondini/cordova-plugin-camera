package org.apache.cordova.camera.custom.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.apache.cordova.camera.custom.BitmapUtils;
import org.apache.cordova.camera.custom.FakeR;
import org.apache.cordova.camera.custom.listeners.ConfirmationListener;

public class ConfirmationFragment extends Fragment {

    private ImageView mSourceImageView;

    private FrameLayout mSourceFrame;

    private View mView;

    private ConfirmationListener mConfirmationListener;

    private Bitmap mOriginal;
    private FakeR fakeR;
    private boolean previewOverlay = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mConfirmationListener = (ConfirmationListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fakeR = new FakeR(this.getActivity());
        mView = inflater.inflate(fakeR.getId("layout", "fragment_confirmation"), container, false);
        init();
        return mView;
    }

    private void init() {


        mSourceImageView = (ImageView) mView.findViewById(fakeR.getId("id", "sourceImageView"));

        View confirmationButton = mView.findViewById(fakeR.getId("id", "confirmButton"));
        confirmationButton.setOnClickListener(new ConfirmButtonClickListener());

        View cancelButton = mView.findViewById(fakeR.getId("id", "cancelButton"));
        cancelButton.setOnClickListener(new CancelButtonClickListener());

        mSourceFrame = (FrameLayout) mView.findViewById(fakeR.getId("id", "sourceFrame"));
        mSourceFrame.post(new Runnable() {
            @Override
            public void run() {
                if (mOriginal != null) {
                    configureBitmapToView();
                }
            }
        });

    }

    /**
     * Configure image to show in scan fragment (rotate, resize, scale...)
     */
    private void configureBitmapToView() {
        if (previewOverlay) {
            mSourceImageView.setImageBitmap(BitmapUtils.drawAsRoundedCornerImage(mOriginal));
        } else {
            mSourceImageView.setImageBitmap(mOriginal);
        }
    }

    public void setOriginalBitmap(Bitmap bitmap) {
        mOriginal = bitmap;
    }

    public void setPreviewOverlay(boolean previewOverlay) {
        this.previewOverlay = previewOverlay;
    }

    private class ConfirmButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            mConfirmationListener.confirm(mOriginal);
        }
    }

    private class CancelButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            mOriginal.recycle();
            System.gc();
            mConfirmationListener.openCamera();
            getActivity().getFragmentManager().beginTransaction().remove(ConfirmationFragment.this).commit();
        }
    }

}
