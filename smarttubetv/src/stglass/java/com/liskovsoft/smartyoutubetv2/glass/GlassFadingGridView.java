package com.liskovsoft.smartyoutubetv2.glass;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Side menu list with progressive top/bottom fades (requiresFadingEdge in lb_headers_fragment.xml).
 * Leanback's layout manager does not report scroll offsets, so the fade strength is derived from
 * which items are laid out and where.
 */
public class GlassFadingGridView extends VerticalGridView {
    public GlassFadingGridView(Context context) {
        super(context);
    }

    public GlassFadingGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GlassFadingGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    static float topStrength(int firstPosition, int firstTop, int topEdge) {
        return firstPosition > 0 || firstTop < topEdge ? 1f : 0f;
    }

    static float bottomStrength(int lastPosition, int itemCount, int lastBottom, int bottomEdge) {
        return lastPosition < itemCount - 1 || lastBottom > bottomEdge ? 1f : 0f;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0f;
        }
        View first = getChildAt(0);
        return topStrength(getChildAdapterPosition(first), first.getTop(), getPaddingTop());
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        RecyclerView.Adapter<?> adapter = getAdapter();
        if (getChildCount() == 0 || adapter == null) {
            return 0f;
        }
        View last = getChildAt(getChildCount() - 1);
        return bottomStrength(getChildAdapterPosition(last), adapter.getItemCount(), last.getBottom(), getHeight() - getPaddingBottom());
    }
}
