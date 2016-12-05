package org.apache.cordova.camera.custom;

import android.app.Activity;
import android.content.Context;

public class FakeR {

    private Context context;
    private String packageName;

    public FakeR(Activity activity) {
        this.context = activity.getApplicationContext();
        this.packageName = this.context.getPackageName();
    }

    public FakeR(Context context) {
        this.context = context;
        this.packageName = this.context.getPackageName();
    }

    public int getId(String group, String key) {
        return context.getResources().getIdentifier(key, group, packageName);
    }
}
