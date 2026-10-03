package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Entry point of the SmartTube Glass code, declared in the stglass manifest so that no upstream class
 * has to call it. Android creates it at startup; it only registers activity callbacks.
 */
public final class GlassInitProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(new Callbacks());
        return true;
    }

    private static final class Callbacks implements Application.ActivityLifecycleCallbacks {
        private final Map<Activity, GlassAmbientController> mControllers = new WeakHashMap<>();

        @Override
        public void onActivityResumed(Activity activity) {
            if (!(activity instanceof LeanbackActivity)) {
                return;
            }
            GlassAmbientController controller = mControllers.get(activity);
            if (controller == null) {
                controller = new GlassAmbientController(activity);
                mControllers.put(activity, controller);
            }
            controller.attach();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            GlassAmbientController controller = mControllers.get(activity);
            if (controller != null) {
                controller.detach();
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            mControllers.remove(activity);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
