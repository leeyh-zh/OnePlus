package android.support.v7.widget;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import com.oneplus.sdk.utils.OpBoostFramework;

@RequiresApi(21)
class RoundRectDrawable extends Drawable {
    private ColorStateList mBackground;
    private final RectF mBoundsF;
    private final Rect mBoundsI;
    private boolean mInsetForPadding;
    private boolean mInsetForRadius;
    private float mPadding;
    private final Paint mPaint;
    private float mRadius;
    private ColorStateList mTint;
    private PorterDuffColorFilter mTintFilter;
    private Mode mTintMode;

    RoundRectDrawable(ColorStateList backgroundColor, float radius) {
        this.mInsetForPadding = false;
        this.mInsetForRadius = true;
        this.mTintMode = Mode.SRC_IN;
        this.mRadius = radius;
        this.mPaint = new Paint(5);
        setBackground(backgroundColor);
        this.mBoundsF = new RectF();
        this.mBoundsI = new Rect();
    }

    private void setBackground(ColorStateList color) {
        if (color == null) {
            color = ColorStateList.valueOf(0);
        }
        this.mBackground = color;
        this.mPaint.setColor(this.mBackground.getColorForState(getState(), this.mBackground.getDefaultColor()));
    }

    void setPadding(float padding, boolean insetForPadding, boolean insetForRadius) {
        if (padding != this.mPadding || this.mInsetForPadding != insetForPadding || this.mInsetForRadius != insetForRadius) {
            this.mPadding = padding;
            this.mInsetForPadding = insetForPadding;
            this.mInsetForRadius = insetForRadius;
            updateBounds(null);
            invalidateSelf();
        }
    }

    float getPadding() {
        return this.mPadding;
    }

    public void draw(Canvas canvas) {
        boolean clearColorFilter;
        Paint paint = this.mPaint;
        if (this.mTintFilter == null || paint.getColorFilter() != null) {
            clearColorFilter = false;
        } else {
            paint.setColorFilter(this.mTintFilter);
            clearColorFilter = true;
        }
        canvas.drawRoundRect(this.mBoundsF, this.mRadius, this.mRadius, paint);
        if (clearColorFilter) {
            paint.setColorFilter(null);
        }
    }

    private void updateBounds(Rect bounds) {
        if (bounds == null) {
            bounds = getBounds();
        }
        this.mBoundsF.set((float) bounds.left, (float) bounds.top, (float) bounds.right, (float) bounds.bottom);
        this.mBoundsI.set(bounds);
        if (this.mInsetForPadding) {
            float vInset = RoundRectDrawableWithShadow.calculateVerticalPadding(this.mPadding, this.mRadius, this.mInsetForRadius);
            this.mBoundsI.inset((int) Math.ceil((double) RoundRectDrawableWithShadow.calculateHorizontalPadding(this.mPadding, this.mRadius, this.mInsetForRadius)), (int) Math.ceil((double) vInset));
            this.mBoundsF.set(this.mBoundsI);
        }
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateBounds(bounds);
    }

    public void getOutline(Outline outline) {
        outline.setRoundRect(this.mBoundsI, this.mRadius);
    }

    void setRadius(float radius) {
        if (radius != this.mRadius) {
            this.mRadius = radius;
            updateBounds(null);
            invalidateSelf();
        }
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return OpBoostFramework.REQUEST_FAILED_UNKNOWN_POLICY;
    }

    public float getRadius() {
        return this.mRadius;
    }

    public void setColor(@Nullable ColorStateList color) {
        setBackground(color);
        invalidateSelf();
    }

    public ColorStateList getColor() {
        return this.mBackground;
    }

    public void setTintList(ColorStateList tint) {
        this.mTint = tint;
        this.mTintFilter = createTintFilter(this.mTint, this.mTintMode);
        invalidateSelf();
    }

    public void setTintMode(Mode tintMode) {
        this.mTintMode = tintMode;
        this.mTintFilter = createTintFilter(this.mTint, this.mTintMode);
        invalidateSelf();
    }

    protected boolean onStateChange(int[] stateSet) {
        int newColor = this.mBackground.getColorForState(stateSet, this.mBackground.getDefaultColor());
        boolean colorChanged = newColor != this.mPaint.getColor();
        if (colorChanged) {
            this.mPaint.setColor(newColor);
        }
        if (this.mTint == null || this.mTintMode == null) {
            return colorChanged;
        }
        this.mTintFilter = createTintFilter(this.mTint, this.mTintMode);
        return true;
    }

    public boolean isStateful() {
        if (this.mTint == null || !this.mTint.isStateful()) {
            if ((this.mBackground == null || !this.mBackground.isStateful()) && !super.isStateful()) {
                return false;
            }
        }
        return true;
    }

    private PorterDuffColorFilter createTintFilter(ColorStateList tint, Mode tintMode) {
        return (tint == null || tintMode == null) ? null : new PorterDuffColorFilter(tint.getColorForState(getState(), 0), tintMode);
    }
}