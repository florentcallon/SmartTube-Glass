package com.liskovsoft.smartyoutubetv2.glass;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Soft accent glow behind a card thumbnail, shown while the card is selected (focused).
 * The view is laid out GLOW_DP larger than the thumbnail on every side; colour and corner radius come from
 * the theme (?attr/glassCardGlow, ?attr/glassCardRadius), so classic schemes (transparent glow) draw nothing.
 */
public class GlassGlowView extends View {
    public static final int GLOW_DP = 14;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mGlowPx;
    private final float mCornerRadiusPx;
    private final int mGlowColor;

    public GlassGlowView(Context context) {
        this(context, null);
    }

    public GlassGlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        mGlowPx = GLOW_DP * density;
        TypedValue value = new TypedValue();
        mGlowColor = context.getTheme().resolveAttribute(R.attr.glassCardGlow, value, true) ? value.data : Color.TRANSPARENT;
        mCornerRadiusPx = context.getTheme().resolveAttribute(R.attr.glassCardRadius, value, true)
                ? value.getDimension(getResources().getDisplayMetrics()) : 0;
        mPaint.setColor(mGlowColor);
        mPaint.setMaskFilter(new BlurMaskFilter(mGlowPx * 0.8f, BlurMaskFilter.Blur.NORMAL));
    }

    public int getGlowColor() {
        return mGlowColor;
    }

    public boolean isGlowing() {
        return isSelected() && Color.alpha(mGlowColor) > 0;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        // BlurMaskFilter needs a software layer; keep it only on the glowing card (one at a time).
        setLayerType(isGlowing() ? LAYER_TYPE_SOFTWARE : LAYER_TYPE_NONE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isGlowing()) {
            return;
        }
        canvas.drawRoundRect(mGlowPx, mGlowPx, getWidth() - mGlowPx, getHeight() - mGlowPx,
                mCornerRadiusPx, mCornerRadiusPx, mPaint);
    }
}
