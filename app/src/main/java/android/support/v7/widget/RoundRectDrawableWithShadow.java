package android.support.v7.widget;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.widget.AutoScrollHelper;
import android.support.v7.cardview.R;
import com.oneplus.sdk.utils.OpBoostFramework;

class RoundRectDrawableWithShadow extends Drawable {
    private static final double COS_45;
    private static final float SHADOW_MULTIPLIER = 1.5f;
    static RoundRectHelper sRoundRectHelper;
    private boolean mAddPaddingForCorners;
    private ColorStateList mBackground;
    private final RectF mCardBounds;
    private float mCornerRadius;
    private Paint mCornerShadowPaint;
    private Path mCornerShadowPath;
    private boolean mDirty;
    private Paint mEdgeShadowPaint;
    private final int mInsetShadow;
    private Paint mPaint;
    private boolean mPrintedShadowClipWarning;
    private float mRawMaxShadowSize;
    private float mRawShadowSize;
    private final int mShadowEndColor;
    private float mShadowSize;
    private final int mShadowStartColor;

    static interface RoundRectHelper {
        void drawRoundRect(Canvas canvas, RectF rectF, float f, Paint paint);
    }

    static {
        COS_45 = Math.cos(Math.toRadians(45.0d));
    }

    RoundRectDrawableWithShadow(Resources resources, ColorStateList backgroundColor, float radius, float shadowSize, float maxShadowSize) {
        this.mDirty = true;
        this.mAddPaddingForCorners = true;
        this.mPrintedShadowClipWarning = false;
        this.mShadowStartColor = resources.getColor(R.color.cardview_shadow_start_color);
        this.mShadowEndColor = resources.getColor(R.color.cardview_shadow_end_color);
        this.mInsetShadow = resources.getDimensionPixelSize(R.dimen.cardview_compat_inset_shadow);
        this.mPaint = new Paint(5);
        setBackground(backgroundColor);
        this.mCornerShadowPaint = new Paint(5);
        this.mCornerShadowPaint.setStyle(Style.FILL);
        this.mCornerRadius = (float) ((int) (0.5f + radius));
        this.mCardBounds = new RectF();
        this.mEdgeShadowPaint = new Paint(this.mCornerShadowPaint);
        this.mEdgeShadowPaint.setAntiAlias(false);
        setShadowSize(shadowSize, maxShadowSize);
    }

    private void setBackground(ColorStateList color) {
        if (color == null) {
            color = ColorStateList.valueOf(0);
        }
        this.mBackground = color;
        this.mPaint.setColor(this.mBackground.getColorForState(getState(), this.mBackground.getDefaultColor()));
    }

    private int toEven(float value) {
        int i = (int) (0.5f + value);
        return i % 2 == 1 ? i - 1 : i;
    }

    void setAddPaddingForCorners(boolean addPaddingForCorners) {
        this.mAddPaddingForCorners = addPaddingForCorners;
        invalidateSelf();
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
        this.mCornerShadowPaint.setAlpha(alpha);
        this.mEdgeShadowPaint.setAlpha(alpha);
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.mDirty = true;
    }

    private void setShadowSize(float shadowSize, float maxShadowSize) {
        if (shadowSize < 0.0f) {
            throw new IllegalArgumentException("Invalid shadow size " + shadowSize + ". Must be >= 0");
        } else if (maxShadowSize < 0.0f) {
            throw new IllegalArgumentException("Invalid max shadow size " + maxShadowSize + ". Must be >= 0");
        } else {
            shadowSize = (float) toEven(shadowSize);
            maxShadowSize = (float) toEven(maxShadowSize);
            if (shadowSize > maxShadowSize) {
                shadowSize = maxShadowSize;
                if (!this.mPrintedShadowClipWarning) {
                    this.mPrintedShadowClipWarning = true;
                }
            }
            if (this.mRawShadowSize != shadowSize || this.mRawMaxShadowSize != maxShadowSize) {
                this.mRawShadowSize = shadowSize;
                this.mRawMaxShadowSize = maxShadowSize;
                this.mShadowSize = (float) ((int) (((1.5f * shadowSize) + ((float) this.mInsetShadow)) + 0.5f));
                this.mDirty = true;
                invalidateSelf();
            }
        }
    }

    public boolean getPadding(Rect padding) {
        int vOffset = (int) Math.ceil((double) calculateVerticalPadding(this.mRawMaxShadowSize, this.mCornerRadius, this.mAddPaddingForCorners));
        int hOffset = (int) Math.ceil((double) calculateHorizontalPadding(this.mRawMaxShadowSize, this.mCornerRadius, this.mAddPaddingForCorners));
        padding.set(hOffset, vOffset, hOffset, vOffset);
        return true;
    }

    static float calculateVerticalPadding(float maxShadowSize, float cornerRadius, boolean addPaddingForCorners) {
        return addPaddingForCorners ? (float) (((double) (1.5f * maxShadowSize)) + ((1.0d - COS_45) * ((double) cornerRadius))) : 1.5f * maxShadowSize;
    }

    static float calculateHorizontalPadding(float maxShadowSize, float cornerRadius, boolean addPaddingForCorners) {
        return addPaddingForCorners ? (float) (((double) maxShadowSize) + ((1.0d - COS_45) * ((double) cornerRadius))) : maxShadowSize;
    }

    protected boolean onStateChange(int[] stateSet) {
        int newColor = this.mBackground.getColorForState(stateSet, this.mBackground.getDefaultColor());
        if (this.mPaint.getColor() == newColor) {
            return false;
        }
        this.mPaint.setColor(newColor);
        this.mDirty = true;
        invalidateSelf();
        return true;
    }

    public boolean isStateful() {
        return (this.mBackground != null && this.mBackground.isStateful()) || super.isStateful();
    }

    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return OpBoostFramework.REQUEST_FAILED_UNKNOWN_POLICY;
    }

    void setCornerRadius(float radius) {
        if (radius < 0.0f) {
            throw new IllegalArgumentException("Invalid radius " + radius + ". Must be >= 0");
        }
        radius = (float) ((int) (0.5f + radius));
        if (this.mCornerRadius != radius) {
            this.mCornerRadius = radius;
            this.mDirty = true;
            invalidateSelf();
        }
    }

    public void draw(Canvas canvas) {
        if (this.mDirty) {
            buildComponents(getBounds());
            this.mDirty = false;
        }
        canvas.translate(AutoScrollHelper.RELATIVE_UNSPECIFIED, this.mRawShadowSize / 2.0f);
        drawShadow(canvas);
        canvas.translate(AutoScrollHelper.RELATIVE_UNSPECIFIED, (-this.mRawShadowSize) / 2.0f);
        sRoundRectHelper.drawRoundRect(canvas, this.mCardBounds, this.mCornerRadius, this.mPaint);
    }

    private void drawShadow(Canvas canvas) {
        boolean drawHorizontalEdges;
        boolean drawVerticalEdges;
        float edgeShadowTop = (-this.mCornerRadius) - this.mShadowSize;
        float inset = (this.mCornerRadius + ((float) this.mInsetShadow)) + (this.mRawShadowSize / 2.0f);
        if (this.mCardBounds.width() - (2.0f * inset) > 0.0f) {
            drawHorizontalEdges = true;
        } else {
            drawHorizontalEdges = false;
        }
        if (this.mCardBounds.height() - (2.0f * inset) > 0.0f) {
            drawVerticalEdges = true;
        } else {
            drawVerticalEdges = false;
        }
        int saved = canvas.save();
        canvas.translate(this.mCardBounds.left + inset, this.mCardBounds.top + inset);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawHorizontalEdges) {
            canvas.drawRect(AutoScrollHelper.RELATIVE_UNSPECIFIED, edgeShadowTop, this.mCardBounds.width() - (2.0f * inset), -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);
        saved = canvas.save();
        canvas.translate(this.mCardBounds.right - inset, this.mCardBounds.bottom - inset);
        canvas.rotate(180.0f);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawHorizontalEdges) {
            Canvas canvas2 = canvas;
            canvas2.drawRect(AutoScrollHelper.RELATIVE_UNSPECIFIED, edgeShadowTop, this.mCardBounds.width() - (2.0f * inset), this.mShadowSize + (-this.mCornerRadius), this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);
        saved = canvas.save();
        canvas.translate(this.mCardBounds.left + inset, this.mCardBounds.bottom - inset);
        canvas.rotate(270.0f);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawVerticalEdges) {
            canvas.drawRect(AutoScrollHelper.RELATIVE_UNSPECIFIED, edgeShadowTop, this.mCardBounds.height() - (2.0f * inset), -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);
        saved = canvas.save();
        canvas.translate(this.mCardBounds.right - inset, this.mCardBounds.top + inset);
        canvas.rotate(90.0f);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawVerticalEdges) {
            canvas.drawRect(AutoScrollHelper.RELATIVE_UNSPECIFIED, edgeShadowTop, this.mCardBounds.height() - (2.0f * inset), -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);
    }

    private void buildShadowCorners() {
        RectF innerBounds = new RectF(-this.mCornerRadius, -this.mCornerRadius, this.mCornerRadius, this.mCornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-this.mShadowSize, -this.mShadowSize);
        if (this.mCornerShadowPath == null) {
            this.mCornerShadowPath = new Path();
        } else {
            this.mCornerShadowPath.reset();
        }
        this.mCornerShadowPath.setFillType(FillType.EVEN_ODD);
        this.mCornerShadowPath.moveTo(-this.mCornerRadius, AutoScrollHelper.RELATIVE_UNSPECIFIED);
        this.mCornerShadowPath.rLineTo(-this.mShadowSize, AutoScrollHelper.RELATIVE_UNSPECIFIED);
        this.mCornerShadowPath.arcTo(outerBounds, 180.0f, 90.0f, false);
        this.mCornerShadowPath.arcTo(innerBounds, 270.0f, -90.0f, false);
        this.mCornerShadowPath.close();
        float startRatio = this.mCornerRadius / (this.mCornerRadius + this.mShadowSize);
        this.mCornerShadowPaint.setShader(new RadialGradient(0.0f, 0.0f, this.mCornerRadius + this.mShadowSize, new int[]{this.mShadowStartColor, this.mShadowStartColor, this.mShadowEndColor}, new float[]{0.0f, startRatio, 1.0f}, TileMode.CLAMP));
        this.mEdgeShadowPaint.setShader(new LinearGradient(0.0f, (-this.mCornerRadius) + this.mShadowSize, 0.0f, (-this.mCornerRadius) - this.mShadowSize, new int[]{this.mShadowStartColor, this.mShadowStartColor, this.mShadowEndColor}, new float[]{0.0f, 0.5f, 1.0f}, TileMode.CLAMP));
        this.mEdgeShadowPaint.setAntiAlias(false);
    }

    private void buildComponents(Rect bounds) {
        float verticalOffset = this.mRawMaxShadowSize * 1.5f;
        this.mCardBounds.set(((float) bounds.left) + this.mRawMaxShadowSize, ((float) bounds.top) + verticalOffset, ((float) bounds.right) - this.mRawMaxShadowSize, ((float) bounds.bottom) - verticalOffset);
        buildShadowCorners();
    }

    float getCornerRadius() {
        return this.mCornerRadius;
    }

    void getMaxShadowAndCornerPadding(Rect into) {
        getPadding(into);
    }

    void setShadowSize(float size) {
        setShadowSize(size, this.mRawMaxShadowSize);
    }

    void setMaxShadowSize(float size) {
        setShadowSize(this.mRawShadowSize, size);
    }

    float getShadowSize() {
        return this.mRawShadowSize;
    }

    float getMaxShadowSize() {
        return this.mRawMaxShadowSize;
    }

    float getMinWidth() {
        return ((this.mRawMaxShadowSize + ((float) this.mInsetShadow)) * 2.0f) + (2.0f * Math.max(this.mRawMaxShadowSize, (this.mCornerRadius + ((float) this.mInsetShadow)) + (this.mRawMaxShadowSize / 2.0f)));
    }

    float getMinHeight() {
        return (((this.mRawMaxShadowSize * 1.5f) + ((float) this.mInsetShadow)) * 2.0f) + (2.0f * Math.max(this.mRawMaxShadowSize, (this.mCornerRadius + ((float) this.mInsetShadow)) + ((this.mRawMaxShadowSize * 1.5f) / 2.0f)));
    }

    void setColor(@Nullable ColorStateList color) {
        setBackground(color);
        invalidateSelf();
    }

    ColorStateList getColor() {
        return this.mBackground;
    }
}
