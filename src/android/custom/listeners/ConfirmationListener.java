package org.apache.cordova.camera.custom.listeners;

import android.graphics.Bitmap;

public interface ConfirmationListener {

    void confirm(Bitmap bitmap);

    void openCamera();

}
