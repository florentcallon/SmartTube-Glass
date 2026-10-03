package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Marks the current section in the side menu: "activated" pill (see glass_header_pill.xml) and accent icon.
 * Leanback only highlights the focused header, so once focus moves to the content the icon rail would
 * otherwise show no current section.
 */
public final class GlassHeaders {
    public interface PositionOf {
        int positionOf(View child);
    }

    private final Activity mActivity;
    private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = this::update;
    private VerticalGridView mGrid;
    private boolean mAttached;

    public GlassHeaders(Activity activity) {
        mActivity = activity;
    }

    public static void activateSelected(ViewGroup grid, int selectedPosition, PositionOf positionOf) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            child.setActivated(positionOf.positionOf(child) == selectedPosition);
        }
    }

    /**
     * Accent colour on the current section's icon. Channel avatars (loaded by Glide) are left untouched.
     */
    public static void tintIcon(View item, boolean activated, int accentColor) {
        View view = item.findViewById(R.id.header_icon);
        if (!(view instanceof ImageView)) {
            return;
        }
        ImageView icon = (ImageView) view;
        boolean isRemoteImage = icon.getTag(com.bumptech.glide.R.id.glide_custom_view_target_tag) != null;
        if (activated && !isRemoteImage) {
            icon.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        } else {
            icon.clearColorFilter();
        }
    }

    public void attach() {
        if (mAttached || !isGlassScheme()) {
            return;
        }
        // Headers are inflated by a fragment after the activity resumes and items are recycled while
        // scrolling: re-apply on every layout pass (a handful of children, cheap).
        mActivity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
        mAttached = true;
    }

    public void detach() {
        if (!mAttached) {
            return;
        }
        mActivity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
        mAttached = false;
    }

    private void update() {
        View view = mActivity.findViewById(R.id.browse_headers);
        if (!(view instanceof VerticalGridView)) {
            return;
        }
        VerticalGridView grid = (VerticalGridView) view;
        if (grid != mGrid) { // first time, or the headers fragment was recreated
            mGrid = grid;
            grid.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
                @Override
                public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                    mark(parent, position);
                }
            });
        }
        mark(grid, grid.getSelectedPosition());
    }

    private void mark(RecyclerView grid, int selectedPosition) {
        activateSelected(grid, selectedPosition, grid::getChildAdapterPosition);
        int accent = accentColor();
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            tintIcon(child, child.isActivated(), accent);
        }
    }

    private boolean isGlassScheme() {
        TypedValue value = new TypedValue();
        return mActivity.getTheme().resolveAttribute(R.attr.glassAmbient, value, true) && value.data != 0;
    }

    private int accentColor() {
        TypedValue value = new TypedValue();
        return mActivity.getTheme().resolveAttribute(R.attr.glassAccent, value, true) ? value.data : 0xFFFF0033;
    }
}
