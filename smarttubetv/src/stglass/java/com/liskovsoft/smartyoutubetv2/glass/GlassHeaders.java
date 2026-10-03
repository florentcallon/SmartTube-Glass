package com.liskovsoft.smartyoutubetv2.glass;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Marks the current section in the side menu as "activated" (accent pill, see glass_header_pill.xml).
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

    public void attach() {
        if (mAttached) {
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
                    activateSelected(parent, position, parent::getChildAdapterPosition);
                }
            });
        }
        activateSelected(grid, grid.getSelectedPosition(), grid::getChildAdapterPosition);
    }
}
