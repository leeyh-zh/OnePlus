package android.support.design.widget;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.design.widget.CoordinatorLayout.Behavior;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v4.widget.ViewDragHelper.Callback;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.android.volley.DefaultRetryPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import net.oneplus.weather.widget.openglbase.RainSurfaceView;

public class SwipeDismissBehavior<V extends View> extends Behavior<V> {
    private static final float DEFAULT_ALPHA_END_DISTANCE = 0.5f;
    private static final float DEFAULT_ALPHA_START_DISTANCE = 0.0f;
    private static final float DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_SETTLING = 2;
    public static final int SWIPE_DIRECTION_ANY = 2;
    public static final int SWIPE_DIRECTION_END_TO_START = 1;
    public static final int SWIPE_DIRECTION_START_TO_END = 0;
    float mAlphaEndSwipeDistance;
    float mAlphaStartSwipeDistance;
    private final Callback mDragCallback;
    float mDragDismissThreshold;
    private boolean mInterceptingEvents;
    OnDismissListener mListener;
    private float mSensitivity;
    private boolean mSensitivitySet;
    int mSwipeDirection;
    ViewDragHelper mViewDragHelper;

    public static interface OnDismissListener {
        void onDismiss(View view);

        void onDragStateChanged(int i);
    }

    private class SettleRunnable implements Runnable {
        private final boolean mDismiss;
        private final View mView;

        SettleRunnable(View view, boolean dismiss) {
            this.mView = view;
            this.mDismiss = dismiss;
        }

        public void run() {
            if (SwipeDismissBehavior.this.mViewDragHelper != null && SwipeDismissBehavior.this.mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(this.mView, this);
            } else if (this.mDismiss && SwipeDismissBehavior.this.mListener != null) {
                SwipeDismissBehavior.this.mListener.onDismiss(this.mView);
            }
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    private static @interface SwipeDirection {
    }

    public SwipeDismissBehavior() {
        this.mSensitivity = 0.0f;
        this.mSwipeDirection = 2;
        this.mDragDismissThreshold = 0.5f;
        this.mAlphaStartSwipeDistance = 0.0f;
        this.mAlphaEndSwipeDistance = 0.5f;
        this.mDragCallback = new Callback() {
            private static final int INVALID_POINTER_ID = -1;
            private int mActivePointerId;
            private int mOriginalCapturedViewLeft;

            {
                this.mActivePointerId = -1;
            }

            public boolean tryCaptureView(View child, int pointerId) {
                return this.mActivePointerId == -1 && SwipeDismissBehavior.this.canSwipeDismissView(child);
            }

            public void onViewCaptured(View capturedChild, int activePointerId) {
                this.mActivePointerId = activePointerId;
                this.mOriginalCapturedViewLeft = capturedChild.getLeft();
                ViewParent parent = capturedChild.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            }

            public void onViewDragStateChanged(int state) {
                if (SwipeDismissBehavior.this.mListener != null) {
                    SwipeDismissBehavior.this.mListener.onDragStateChanged(state);
                }
            }

            public void onViewReleased(View child, float xvel, float yvel) {
                int targetLeft;
                this.mActivePointerId = -1;
                int childWidth = child.getWidth();
                boolean dismiss = false;
                if (shouldDismiss(child, xvel)) {
                    targetLeft = child.getLeft() < this.mOriginalCapturedViewLeft ? this.mOriginalCapturedViewLeft - childWidth : this.mOriginalCapturedViewLeft + childWidth;
                    dismiss = true;
                } else {
                    targetLeft = this.mOriginalCapturedViewLeft;
                }
                if (SwipeDismissBehavior.this.mViewDragHelper.settleCapturedViewAt(targetLeft, child.getTop())) {
                    ViewCompat.postOnAnimation(child, new SettleRunnable(child, dismiss));
                } else if (dismiss && SwipeDismissBehavior.this.mListener != null) {
                    SwipeDismissBehavior.this.mListener.onDismiss(child);
                }
            }

            private boolean shouldDismiss(View child, float xvel) {
                if (xvel != 0.0f) {
                    boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
                    if (SwipeDismissBehavior.this.mSwipeDirection == 2) {
                        return true;
                    }
                    if (SwipeDismissBehavior.this.mSwipeDirection == 0) {
                        return isRtl ? xvel < 0.0f : xvel > 0.0f;
                    } else {
                        if (SwipeDismissBehavior.this.mSwipeDirection == 1) {
                            return isRtl ? xvel > 0.0f : xvel < 0.0f;
                        } else {
                            return false;
                        }
                    }
                }
                return Math.abs(child.getLeft() - this.mOriginalCapturedViewLeft) >= Math.round(((float) child.getWidth()) * SwipeDismissBehavior.this.mDragDismissThreshold);
            }

            public int getViewHorizontalDragRange(View child) {
                return child.getWidth();
            }

            public int clampViewPositionHorizontal(View child, int left, int dx) {
                int min;
                int max;
                boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
                if (SwipeDismissBehavior.this.mSwipeDirection == 0) {
                    if (isRtl) {
                        min = this.mOriginalCapturedViewLeft - child.getWidth();
                        max = this.mOriginalCapturedViewLeft;
                    } else {
                        min = this.mOriginalCapturedViewLeft;
                        max = this.mOriginalCapturedViewLeft + child.getWidth();
                    }
                } else if (SwipeDismissBehavior.this.mSwipeDirection != 1) {
                    min = this.mOriginalCapturedViewLeft - child.getWidth();
                    max = this.mOriginalCapturedViewLeft + child.getWidth();
                } else if (isRtl) {
                    min = this.mOriginalCapturedViewLeft;
                    max = this.mOriginalCapturedViewLeft + child.getWidth();
                } else {
                    min = this.mOriginalCapturedViewLeft - child.getWidth();
                    max = this.mOriginalCapturedViewLeft;
                }
                return SwipeDismissBehavior.clamp(min, left, max);
            }

            public int clampViewPositionVertical(View child, int top, int dy) {
                return child.getTop();
            }

            public void onViewPositionChanged(View child, int left, int top, int dx, int dy) {
                float startAlphaDistance = ((float) this.mOriginalCapturedViewLeft) + (((float) child.getWidth()) * SwipeDismissBehavior.this.mAlphaStartSwipeDistance);
                float endAlphaDistance = ((float) this.mOriginalCapturedViewLeft) + (((float) child.getWidth()) * SwipeDismissBehavior.this.mAlphaEndSwipeDistance);
                if (((float) left) <= startAlphaDistance) {
                    child.setAlpha(DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                } else if (((float) left) >= endAlphaDistance) {
                    child.setAlpha(DEFAULT_ALPHA_START_DISTANCE);
                } else {
                    child.setAlpha(SwipeDismissBehavior.clamp((float) DEFAULT_ALPHA_START_DISTANCE, 1.0f - SwipeDismissBehavior.fraction(startAlphaDistance, endAlphaDistance, (float) left), (float) DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                }
            }
        };
    }

    public void setListener(OnDismissListener listener) {
        this.mListener = listener;
    }

    public void setSwipeDirection(int direction) {
        this.mSwipeDirection = direction;
    }

    public void setDragDismissDistance(float distance) {
        this.mDragDismissThreshold = clamp((float) DEFAULT_ALPHA_START_DISTANCE, distance, (float) DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    public void setStartAlphaSwipeDistance(float fraction) {
        this.mAlphaStartSwipeDistance = clamp((float) DEFAULT_ALPHA_START_DISTANCE, fraction, (float) DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    public void setEndAlphaSwipeDistance(float fraction) {
        this.mAlphaEndSwipeDistance = clamp((float) DEFAULT_ALPHA_START_DISTANCE, fraction, (float) DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    public void setSensitivity(float sensitivity) {
        this.mSensitivity = sensitivity;
        this.mSensitivitySet = true;
    }

    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean dispatchEventToHelper = this.mInterceptingEvents;
        switch (event.getActionMasked()) {
            case STATE_IDLE:
                this.mInterceptingEvents = parent.isPointInChildBounds(child, (int) event.getX(), (int) event.getY());
                dispatchEventToHelper = this.mInterceptingEvents;
                break;
            case SWIPE_DIRECTION_END_TO_START:
            case RainSurfaceView.RAIN_LEVEL_DOWNPOUR:
                this.mInterceptingEvents = false;
                break;
        }
        if (!dispatchEventToHelper) {
            return false;
        }
        ensureViewDragHelper(parent);
        return this.mViewDragHelper.shouldInterceptTouchEvent(event);
    }

    public boolean onTouchEvent(CoordinatorLayout parent, V v, MotionEvent event) {
        if (this.mViewDragHelper == null) {
            return false;
        }
        this.mViewDragHelper.processTouchEvent(event);
        return true;
    }

    public boolean canSwipeDismissView(@NonNull View view) {
        return true;
    }

    private void ensureViewDragHelper(ViewGroup parent) {
        if (this.mViewDragHelper == null) {
            ViewDragHelper create;
            if (this.mSensitivitySet) {
                create = ViewDragHelper.create(parent, this.mSensitivity, this.mDragCallback);
            } else {
                create = ViewDragHelper.create(parent, this.mDragCallback);
            }
            this.mViewDragHelper = create;
        }
    }

    static float clamp(float min, float value, float max) {
        return Math.min(Math.max(min, value), max);
    }

    static int clamp(int min, int value, int max) {
        return Math.min(Math.max(min, value), max);
    }

    public int getDragState() {
        return this.mViewDragHelper != null ? this.mViewDragHelper.getViewDragState() : STATE_IDLE;
    }

    static float fraction(float startValue, float endValue, float value) {
        return (value - startValue) / (endValue - startValue);
    }
}
