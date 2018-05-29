package android.support.v7.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.AutoScrollHelper;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.MotionEvent;
import com.android.volley.DefaultRetryPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@VisibleForTesting
class FastScroller extends ItemDecoration implements OnItemTouchListener {
    private static final int ANIMATION_STATE_FADING_IN = 1;
    private static final int ANIMATION_STATE_FADING_OUT = 3;
    private static final int ANIMATION_STATE_IN = 2;
    private static final int ANIMATION_STATE_OUT = 0;
    private static final int DRAG_NONE = 0;
    private static final int DRAG_X = 1;
    private static final int DRAG_Y = 2;
    private static final int[] EMPTY_STATE_SET;
    private static final int HIDE_DELAY_AFTER_DRAGGING_MS = 1200;
    private static final int HIDE_DELAY_AFTER_VISIBLE_MS = 1500;
    private static final int HIDE_DURATION_MS = 500;
    private static final int[] PRESSED_STATE_SET;
    private static final int SCROLLBAR_FULL_OPAQUE = 255;
    private static final int SHOW_DURATION_MS = 500;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_HIDDEN = 0;
    private static final int STATE_VISIBLE = 1;
    private int mAnimationState;
    private int mDragState;
    private final Runnable mHideRunnable;
    @VisibleForTesting
    float mHorizontalDragX;
    private final int[] mHorizontalRange;
    @VisibleForTesting
    int mHorizontalThumbCenterX;
    private final StateListDrawable mHorizontalThumbDrawable;
    private final int mHorizontalThumbHeight;
    @VisibleForTesting
    int mHorizontalThumbWidth;
    private final Drawable mHorizontalTrackDrawable;
    private final int mHorizontalTrackHeight;
    private final int mMargin;
    private boolean mNeedHorizontalScrollbar;
    private boolean mNeedVerticalScrollbar;
    private final OnScrollListener mOnScrollListener;
    private RecyclerView mRecyclerView;
    private int mRecyclerViewHeight;
    private int mRecyclerViewWidth;
    private final int mScrollbarMinimumRange;
    private final ValueAnimator mShowHideAnimator;
    private int mState;
    @VisibleForTesting
    float mVerticalDragY;
    private final int[] mVerticalRange;
    @VisibleForTesting
    int mVerticalThumbCenterY;
    private final StateListDrawable mVerticalThumbDrawable;
    @VisibleForTesting
    int mVerticalThumbHeight;
    private final int mVerticalThumbWidth;
    private final Drawable mVerticalTrackDrawable;
    private final int mVerticalTrackWidth;

    @Retention(RetentionPolicy.SOURCE)
    private static @interface AnimationState {
    }

    private class AnimatorListener extends AnimatorListenerAdapter {
        private boolean mCanceled;

        private AnimatorListener() {
            this.mCanceled = false;
        }

        public void onAnimationEnd(Animator animation) {
            if (this.mCanceled) {
                this.mCanceled = false;
            } else if (((Float) FastScroller.this.mShowHideAnimator.getAnimatedValue()).floatValue() == 0.0f) {
                FastScroller.this.mAnimationState = STATE_HIDDEN;
                FastScroller.this.setState(STATE_HIDDEN);
            } else {
                FastScroller.this.mAnimationState = STATE_DRAGGING;
                FastScroller.this.requestRedraw();
            }
        }

        public void onAnimationCancel(Animator animation) {
            this.mCanceled = true;
        }
    }

    private class AnimatorUpdater implements AnimatorUpdateListener {
        private AnimatorUpdater() {
        }

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int alpha = (int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * 255.0f);
            FastScroller.this.mVerticalThumbDrawable.setAlpha(alpha);
            FastScroller.this.mVerticalTrackDrawable.setAlpha(alpha);
            FastScroller.this.requestRedraw();
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private static @interface DragState {
    }

    @Retention(RetentionPolicy.SOURCE)
    private static @interface State {
    }

    static {
        PRESSED_STATE_SET = new int[]{16842919};
        EMPTY_STATE_SET = new int[0];
    }

    FastScroller(RecyclerView recyclerView, StateListDrawable verticalThumbDrawable, Drawable verticalTrackDrawable, StateListDrawable horizontalThumbDrawable, Drawable horizontalTrackDrawable, int defaultWidth, int scrollbarMinimumRange, int margin) {
        this.mRecyclerViewWidth = 0;
        this.mRecyclerViewHeight = 0;
        this.mNeedVerticalScrollbar = false;
        this.mNeedHorizontalScrollbar = false;
        this.mState = 0;
        this.mDragState = 0;
        this.mVerticalRange = new int[2];
        this.mHorizontalRange = new int[2];
        this.mShowHideAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mAnimationState = 0;
        this.mHideRunnable = new Runnable() {
            public void run() {
                FastScroller.this.hide(SHOW_DURATION_MS);
            }
        };
        this.mOnScrollListener = new OnScrollListener() {
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                FastScroller.this.updateScrollPosition(recyclerView.computeHorizontalScrollOffset(), recyclerView.computeVerticalScrollOffset());
            }
        };
        this.mVerticalThumbDrawable = verticalThumbDrawable;
        this.mVerticalTrackDrawable = verticalTrackDrawable;
        this.mHorizontalThumbDrawable = horizontalThumbDrawable;
        this.mHorizontalTrackDrawable = horizontalTrackDrawable;
        this.mVerticalThumbWidth = Math.max(defaultWidth, verticalThumbDrawable.getIntrinsicWidth());
        this.mVerticalTrackWidth = Math.max(defaultWidth, verticalTrackDrawable.getIntrinsicWidth());
        this.mHorizontalThumbHeight = Math.max(defaultWidth, horizontalThumbDrawable.getIntrinsicWidth());
        this.mHorizontalTrackHeight = Math.max(defaultWidth, horizontalTrackDrawable.getIntrinsicWidth());
        this.mScrollbarMinimumRange = scrollbarMinimumRange;
        this.mMargin = margin;
        this.mVerticalThumbDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE);
        this.mVerticalTrackDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE);
        this.mShowHideAnimator.addListener(new AnimatorListener());
        this.mShowHideAnimator.addUpdateListener(new AnimatorUpdater());
        attachToRecyclerView(recyclerView);
    }

    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (this.mRecyclerView != recyclerView) {
            if (this.mRecyclerView != null) {
                destroyCallbacks();
            }
            this.mRecyclerView = recyclerView;
            if (this.mRecyclerView != null) {
                setupCallbacks();
            }
        }
    }

    private void setupCallbacks() {
        this.mRecyclerView.addItemDecoration(this);
        this.mRecyclerView.addOnItemTouchListener(this);
        this.mRecyclerView.addOnScrollListener(this.mOnScrollListener);
    }

    private void destroyCallbacks() {
        this.mRecyclerView.removeItemDecoration(this);
        this.mRecyclerView.removeOnItemTouchListener(this);
        this.mRecyclerView.removeOnScrollListener(this.mOnScrollListener);
        cancelHide();
    }

    private void requestRedraw() {
        this.mRecyclerView.invalidate();
    }

    private void setState(int state) {
        if (state == 2 && this.mState != 2) {
            this.mVerticalThumbDrawable.setState(PRESSED_STATE_SET);
            cancelHide();
        }
        if (state == 0) {
            requestRedraw();
        } else {
            show();
        }
        if (this.mState == 2 && state != 2) {
            this.mVerticalThumbDrawable.setState(EMPTY_STATE_SET);
            resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS);
        } else if (state == 1) {
            resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS);
        }
        this.mState = state;
    }

    private boolean isLayoutRTL() {
        return ViewCompat.getLayoutDirection(this.mRecyclerView) == 1;
    }

    public boolean isDragging() {
        return this.mState == 2;
    }

    @VisibleForTesting
    boolean isVisible() {
        return this.mState == 1;
    }

    @VisibleForTesting
    boolean isHidden() {
        return this.mState == 0;
    }

    public void show() {
        switch (this.mAnimationState) {
            case STATE_HIDDEN:
                this.mAnimationState = 1;
                this.mShowHideAnimator.setFloatValues(new float[]{((Float) this.mShowHideAnimator.getAnimatedValue()).floatValue(), 1.0f});
                this.mShowHideAnimator.setDuration(500);
                this.mShowHideAnimator.setStartDelay(0);
                this.mShowHideAnimator.start();
            case ANIMATION_STATE_FADING_OUT:
                this.mShowHideAnimator.cancel();
                this.mAnimationState = 1;
                this.mShowHideAnimator.setFloatValues(new float[]{((Float) this.mShowHideAnimator.getAnimatedValue()).floatValue(), 1.0f});
                this.mShowHideAnimator.setDuration(500);
                this.mShowHideAnimator.setStartDelay(0);
                this.mShowHideAnimator.start();
            default:
                break;
        }
    }

    public void hide() {
        hide(STATE_HIDDEN);
    }

    @VisibleForTesting
    void hide(int duration) {
        switch (this.mAnimationState) {
            case STATE_VISIBLE:
                this.mShowHideAnimator.cancel();
                this.mAnimationState = 3;
                this.mShowHideAnimator.setFloatValues(new float[]{((Float) this.mShowHideAnimator.getAnimatedValue()).floatValue(), 0.0f});
                this.mShowHideAnimator.setDuration((long) duration);
                this.mShowHideAnimator.start();
            case STATE_DRAGGING:
                this.mAnimationState = 3;
                this.mShowHideAnimator.setFloatValues(new float[]{((Float) this.mShowHideAnimator.getAnimatedValue()).floatValue(), 0.0f});
                this.mShowHideAnimator.setDuration((long) duration);
                this.mShowHideAnimator.start();
            default:
                break;
        }
    }

    private void cancelHide() {
        this.mRecyclerView.removeCallbacks(this.mHideRunnable);
    }

    private void resetHideDelay(int delay) {
        cancelHide();
        this.mRecyclerView.postDelayed(this.mHideRunnable, (long) delay);
    }

    public void onDrawOver(Canvas canvas, RecyclerView parent, android.support.v7.widget.RecyclerView.State state) {
        if (this.mRecyclerViewWidth != this.mRecyclerView.getWidth() || this.mRecyclerViewHeight != this.mRecyclerView.getHeight()) {
            this.mRecyclerViewWidth = this.mRecyclerView.getWidth();
            this.mRecyclerViewHeight = this.mRecyclerView.getHeight();
            setState(STATE_HIDDEN);
        } else if (this.mAnimationState != 0) {
            if (this.mNeedVerticalScrollbar) {
                drawVerticalScrollbar(canvas);
            }
            if (this.mNeedHorizontalScrollbar) {
                drawHorizontalScrollbar(canvas);
            }
        }
    }

    private void drawVerticalScrollbar(Canvas canvas) {
        int left = this.mRecyclerViewWidth - this.mVerticalThumbWidth;
        int top = this.mVerticalThumbCenterY - (this.mVerticalThumbHeight / 2);
        this.mVerticalThumbDrawable.setBounds(STATE_HIDDEN, STATE_HIDDEN, this.mVerticalThumbWidth, this.mVerticalThumbHeight);
        this.mVerticalTrackDrawable.setBounds(STATE_HIDDEN, STATE_HIDDEN, this.mVerticalTrackWidth, this.mRecyclerViewHeight);
        if (isLayoutRTL()) {
            this.mVerticalTrackDrawable.draw(canvas);
            canvas.translate((float) this.mVerticalThumbWidth, (float) top);
            canvas.scale(-1.0f, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            this.mVerticalThumbDrawable.draw(canvas);
            canvas.scale(DefaultRetryPolicy.DEFAULT_BACKOFF_MULT, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            canvas.translate((float) (-this.mVerticalThumbWidth), (float) (-top));
            return;
        }
        canvas.translate((float) left, AutoScrollHelper.RELATIVE_UNSPECIFIED);
        this.mVerticalTrackDrawable.draw(canvas);
        canvas.translate(AutoScrollHelper.RELATIVE_UNSPECIFIED, (float) top);
        this.mVerticalThumbDrawable.draw(canvas);
        canvas.translate((float) (-left), (float) (-top));
    }

    private void drawHorizontalScrollbar(Canvas canvas) {
        int top = this.mRecyclerViewHeight - this.mHorizontalThumbHeight;
        int left = this.mHorizontalThumbCenterX - (this.mHorizontalThumbWidth / 2);
        this.mHorizontalThumbDrawable.setBounds(STATE_HIDDEN, STATE_HIDDEN, this.mHorizontalThumbWidth, this.mHorizontalThumbHeight);
        this.mHorizontalTrackDrawable.setBounds(STATE_HIDDEN, STATE_HIDDEN, this.mRecyclerViewWidth, this.mHorizontalTrackHeight);
        canvas.translate(AutoScrollHelper.RELATIVE_UNSPECIFIED, (float) top);
        this.mHorizontalTrackDrawable.draw(canvas);
        canvas.translate((float) left, AutoScrollHelper.RELATIVE_UNSPECIFIED);
        this.mHorizontalThumbDrawable.draw(canvas);
        canvas.translate((float) (-left), (float) (-top));
    }

    void updateScrollPosition(int offsetX, int offsetY) {
        boolean z;
        int verticalContentLength = this.mRecyclerView.computeVerticalScrollRange();
        int verticalVisibleLength = this.mRecyclerViewHeight;
        if (verticalContentLength - verticalVisibleLength <= 0 || this.mRecyclerViewHeight < this.mScrollbarMinimumRange) {
            z = false;
        } else {
            z = true;
        }
        this.mNeedVerticalScrollbar = z;
        int horizontalContentLength = this.mRecyclerView.computeHorizontalScrollRange();
        int horizontalVisibleLength = this.mRecyclerViewWidth;
        if (horizontalContentLength - horizontalVisibleLength <= 0 || this.mRecyclerViewWidth < this.mScrollbarMinimumRange) {
            z = false;
        } else {
            z = true;
        }
        this.mNeedHorizontalScrollbar = z;
        if (this.mNeedVerticalScrollbar || this.mNeedHorizontalScrollbar) {
            if (this.mNeedVerticalScrollbar) {
                this.mVerticalThumbCenterY = (int) ((((float) verticalVisibleLength) * (((float) offsetY) + (((float) verticalVisibleLength) / 2.0f))) / ((float) verticalContentLength));
                this.mVerticalThumbHeight = Math.min(verticalVisibleLength, (verticalVisibleLength * verticalVisibleLength) / verticalContentLength);
            }
            if (this.mNeedHorizontalScrollbar) {
                this.mHorizontalThumbCenterX = (int) ((((float) horizontalVisibleLength) * (((float) offsetX) + (((float) horizontalVisibleLength) / 2.0f))) / ((float) horizontalContentLength));
                this.mHorizontalThumbWidth = Math.min(horizontalVisibleLength, (horizontalVisibleLength * horizontalVisibleLength) / horizontalContentLength);
            }
            if (this.mState == 0 || this.mState == 1) {
                setState(STATE_VISIBLE);
            }
        } else if (this.mState != 0) {
            setState(STATE_HIDDEN);
        }
    }

    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent ev) {
        if (this.mState != 1) {
            return this.mState == 2;
        } else {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(ev.getX(), ev.getY());
            boolean insideHorizontalThumb = isPointInsideHorizontalThumb(ev.getX(), ev.getY());
            if (ev.getAction() != 0 || (!insideVerticalThumb && !insideHorizontalThumb)) {
                return false;
            }
            if (insideHorizontalThumb) {
                this.mDragState = 1;
                this.mHorizontalDragX = (float) ((int) ev.getX());
            } else if (insideVerticalThumb) {
                this.mDragState = 2;
                this.mVerticalDragY = (float) ((int) ev.getY());
            }
            setState(STATE_DRAGGING);
            return true;
        }
    }

    public void onTouchEvent(RecyclerView recyclerView, MotionEvent me) {
        if (this.mState != 0) {
            if (me.getAction() == 0) {
                boolean insideVerticalThumb = isPointInsideVerticalThumb(me.getX(), me.getY());
                boolean insideHorizontalThumb = isPointInsideHorizontalThumb(me.getX(), me.getY());
                if (insideVerticalThumb || insideHorizontalThumb) {
                    if (insideHorizontalThumb) {
                        this.mDragState = 1;
                        this.mHorizontalDragX = (float) ((int) me.getX());
                    } else if (insideVerticalThumb) {
                        this.mDragState = 2;
                        this.mVerticalDragY = (float) ((int) me.getY());
                    }
                    setState(STATE_DRAGGING);
                }
            } else if (me.getAction() == 1 && this.mState == 2) {
                this.mVerticalDragY = 0.0f;
                this.mHorizontalDragX = 0.0f;
                setState(STATE_VISIBLE);
                this.mDragState = 0;
            } else if (me.getAction() == 2 && this.mState == 2) {
                show();
                if (this.mDragState == 1) {
                    horizontalScrollTo(me.getX());
                }
                if (this.mDragState == 2) {
                    verticalScrollTo(me.getY());
                }
            }
        }
    }

    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    private void verticalScrollTo(float y) {
        int[] scrollbarRange = getVerticalRange();
        y = Math.max((float) scrollbarRange[0], Math.min((float) scrollbarRange[1], y));
        if (Math.abs(((float) this.mVerticalThumbCenterY) - y) >= 2.0f) {
            int scrollingBy = scrollTo(this.mVerticalDragY, y, scrollbarRange, this.mRecyclerView.computeVerticalScrollRange(), this.mRecyclerView.computeVerticalScrollOffset(), this.mRecyclerViewHeight);
            if (scrollingBy != 0) {
                this.mRecyclerView.scrollBy(STATE_HIDDEN, scrollingBy);
            }
            this.mVerticalDragY = y;
        }
    }

    private void horizontalScrollTo(float x) {
        int[] scrollbarRange = getHorizontalRange();
        x = Math.max((float) scrollbarRange[0], Math.min((float) scrollbarRange[1], x));
        if (Math.abs(((float) this.mHorizontalThumbCenterX) - x) >= 2.0f) {
            int scrollingBy = scrollTo(this.mHorizontalDragX, x, scrollbarRange, this.mRecyclerView.computeHorizontalScrollRange(), this.mRecyclerView.computeHorizontalScrollOffset(), this.mRecyclerViewWidth);
            if (scrollingBy != 0) {
                this.mRecyclerView.scrollBy(scrollingBy, STATE_HIDDEN);
            }
            this.mHorizontalDragX = x;
        }
    }

    private int scrollTo(float oldDragPos, float newDragPos, int[] scrollbarRange, int scrollRange, int scrollOffset, int viewLength) {
        int scrollbarLength = scrollbarRange[1] - scrollbarRange[0];
        if (scrollbarLength == 0) {
            return 0;
        }
        int totalPossibleOffset = scrollRange - viewLength;
        int scrollingBy = (int) (((float) totalPossibleOffset) * ((newDragPos - oldDragPos) / ((float) scrollbarLength)));
        int absoluteOffset = scrollOffset + scrollingBy;
        return (absoluteOffset >= totalPossibleOffset || absoluteOffset < 0) ? 0 : scrollingBy;
    }

    @VisibleForTesting
    boolean isPointInsideVerticalThumb(float x, float y) {
        if (isLayoutRTL() ? x <= ((float) (this.mVerticalThumbWidth / 2)) : x >= ((float) (this.mRecyclerViewWidth - this.mVerticalThumbWidth))) {
            if (y >= ((float) (this.mVerticalThumbCenterY - (this.mVerticalThumbHeight / 2))) && y <= ((float) (this.mVerticalThumbCenterY + (this.mVerticalThumbHeight / 2)))) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean isPointInsideHorizontalThumb(float x, float y) {
        return y >= ((float) (this.mRecyclerViewHeight - this.mHorizontalThumbHeight)) && x >= ((float) (this.mHorizontalThumbCenterX - (this.mHorizontalThumbWidth / 2))) && x <= ((float) (this.mHorizontalThumbCenterX + (this.mHorizontalThumbWidth / 2)));
    }

    @VisibleForTesting
    Drawable getHorizontalTrackDrawable() {
        return this.mHorizontalTrackDrawable;
    }

    @VisibleForTesting
    Drawable getHorizontalThumbDrawable() {
        return this.mHorizontalThumbDrawable;
    }

    @VisibleForTesting
    Drawable getVerticalTrackDrawable() {
        return this.mVerticalTrackDrawable;
    }

    @VisibleForTesting
    Drawable getVerticalThumbDrawable() {
        return this.mVerticalThumbDrawable;
    }

    private int[] getVerticalRange() {
        this.mVerticalRange[0] = this.mMargin;
        this.mVerticalRange[1] = this.mRecyclerViewHeight - this.mMargin;
        return this.mVerticalRange;
    }

    private int[] getHorizontalRange() {
        this.mHorizontalRange[0] = this.mMargin;
        this.mHorizontalRange[1] = this.mRecyclerViewWidth - this.mMargin;
        return this.mHorizontalRange;
    }
}