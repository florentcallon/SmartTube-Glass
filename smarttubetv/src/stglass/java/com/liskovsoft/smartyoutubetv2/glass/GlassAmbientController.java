package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.widget.ImageCardView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackActivity;

/**
 * Sets the activity background to the blurred thumbnail of the focused card, under the theme scrim.
 * Reuses the bitmap the card already shows: no extra network request.
 */
public final class GlassAmbientController {
    private static final String TAG = "Glass";

    private final Activity mActivity;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final GlassAmbient.Debouncer mDebouncer =
            new GlassAmbient.Debouncer(SystemClock::uptimeMillis, GlassAmbient.DEBOUNCE_MS);
    private final Runnable mCheck = this::applyPending;
    private final ViewTreeObserver.OnGlobalFocusChangeListener mFocusListener = (oldFocus, newFocus) -> onFocus(newFocus);
    private int mScrimColor;
    private boolean mAttached;
    private int mRetries;

    public GlassAmbientController(Activity activity) {
        mActivity = activity;
    }

    public void attach() {
        if (mAttached || mActivity instanceof PlaybackActivity) {
            return;
        }

        TypedValue value = new TypedValue();
        if (!mActivity.getTheme().resolveAttribute(R.attr.glassScrim, value, true)) {
            return; // classic theme: keep the upstream background
        }
        mScrimColor = value.data;

        mActivity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(mFocusListener);
        mAttached = true;
        // Coming back to this screen, upstream resets the background in onStart and focus does not move.
        onFocus(mActivity.getCurrentFocus());
    }

    public void detach() {
        if (!mAttached) {
            return;
        }
        mActivity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalFocusChangeListener(mFocusListener);
        mHandler.removeCallbacks(mCheck);
        mAttached = false;
    }

    private void onFocus(View newFocus) {
        ImageCardView card = findCard(newFocus);
        if (card == null) {
            return;
        }
        mRetries = 0;
        mDebouncer.request(card);
        mHandler.removeCallbacks(mCheck);
        mHandler.postDelayed(mCheck, GlassAmbient.DEBOUNCE_MS);
    }

    private static ImageCardView findCard(View view) {
        View current = view;
        while (current != null) {
            if (current instanceof ImageCardView) {
                return (ImageCardView) current;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private void applyPending() {
        Object pending = mDebouncer.poll();
        if (!(pending instanceof ImageCardView) || !mAttached) {
            return;
        }

        ImageCardView card = (ImageCardView) pending;
        Bitmap sample = GlassThumbnailSampler.sample(card.getMainImageView().getDrawable());
        if (sample == null) {
            // Thumbnail not loaded yet: try again a few times, then keep the current background.
            if (mRetries < GlassAmbient.MAX_RETRIES) {
                mRetries++;
                mDebouncer.request(card);
                mHandler.postDelayed(mCheck, GlassAmbient.DEBOUNCE_MS);
            }
            return;
        }

        try {
            int width = GlassAmbient.SAMPLE_WIDTH;
            int height = GlassAmbient.SAMPLE_HEIGHT;
            int[] pixels = new int[width * height];
            sample.getPixels(pixels, 0, width, 0, 0, width, height);
            sample.recycle();

            Bitmap ambient = Bitmap.createBitmap(GlassAmbient.render(pixels, mScrimColor), width, height, Bitmap.Config.ARGB_8888);
            BitmapDrawable drawable = new BitmapDrawable(mActivity.getResources(), ambient);
            drawable.setFilterBitmap(true);
            BackgroundManager.getInstance(mActivity).setDrawable(drawable);
        } catch (RuntimeException e) {
            Log.w(TAG, "Ambient background skipped", e);
        }
    }
}
