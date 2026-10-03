package com.liskovsoft.smartyoutubetv2.glass;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Clips its content to rounded corners (attribute glassCornerRadius). The rounding survives code that
 * replaces the background of a child with a plain colour (e.g. ComplexImageView.setBadgeColor).
 */
public class GlassRoundedFrameLayout extends FrameLayout {
    private float mCornerRadius;

    public GlassRoundedFrameLayout(Context context) {
        this(context, null);
    }

    public GlassRoundedFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassRoundedFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassRoundedFrameLayout, defStyleAttr, 0);
        try {
            mCornerRadius = a.getDimension(R.styleable.GlassRoundedFrameLayout_glassCornerRadius, 0);
        } finally {
            a.recycle();
        }
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mCornerRadius);
            }
        });
        setClipToOutline(true);
    }

    public float getCornerRadius() {
        return mCornerRadius;
    }
}
