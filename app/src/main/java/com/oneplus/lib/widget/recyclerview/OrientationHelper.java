package com.oneplus.lib.widget.recyclerview;

import android.view.View;
import com.oneplus.lib.widget.recyclerview.RecyclerView.LayoutManager;
import com.oneplus.lib.widget.recyclerview.RecyclerView.LayoutParams;

abstract class OrientationHelper {
    public static final int HORIZONTAL = 0;
    private static final int INVALID_SIZE = Integer.MIN_VALUE;
    public static final int VERTICAL = 1;
    private int mLastTotalSpace;
    protected final LayoutManager mLayoutManager;

    static class AnonymousClass_1 extends OrientationHelper {
        AnonymousClass_1(LayoutManager layoutManager) {
            super(null);
        }

        public int getEndAfterPadding() {
            return this.mLayoutManager.getWidth() - this.mLayoutManager.getPaddingRight();
        }

        public int getEnd() {
            return this.mLayoutManager.getWidth();
        }

        public void offsetChildren(int amount) {
            this.mLayoutManager.offsetChildrenHorizontal(amount);
        }

        public int getStartAfterPadding() {
            return this.mLayoutManager.getPaddingLeft();
        }

        public int getDecoratedMeasurement(View view) {
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            return (this.mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin) + params.rightMargin;
        }

        public int getDecoratedMeasurementInOther(View view) {
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            return (this.mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin) + params.bottomMargin;
        }

        public int getDecoratedEnd(View view) {
            return this.mLayoutManager.getDecoratedRight(view) + ((LayoutParams) view.getLayoutParams()).rightMargin;
        }

        public int getDecoratedStart(View view) {
            return this.mLayoutManager.getDecoratedLeft(view) - ((LayoutParams) view.getLayoutParams()).leftMargin;
        }

        public int getTotalSpace() {
            return (this.mLayoutManager.getWidth() - this.mLayoutManager.getPaddingLeft()) - this.mLayoutManager.getPaddingRight();
        }

        public void offsetChild(View view, int offset) {
            view.offsetLeftAndRight(offset);
        }

        public int getEndPadding() {
            return this.mLayoutManager.getPaddingRight();
        }
    }

    static class AnonymousClass_2 extends OrientationHelper {
        AnonymousClass_2(LayoutManager layoutManager) {
            super(null);
        }

        public int getEndAfterPadding() {
            return this.mLayoutManager.getHeight() - this.mLayoutManager.getPaddingBottom();
        }

        public int getEnd() {
            return this.mLayoutManager.getHeight();
        }

        public void offsetChildren(int amount) {
            this.mLayoutManager.offsetChildrenVertical(amount);
        }

        public int getStartAfterPadding() {
            return this.mLayoutManager.getPaddingTop();
        }

        public int getDecoratedMeasurement(View view) {
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            return (this.mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin) + params.bottomMargin;
        }

        public int getDecoratedMeasurementInOther(View view) {
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            return (this.mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin) + params.rightMargin;
        }

        public int getDecoratedEnd(View view) {
            return this.mLayoutManager.getDecoratedBottom(view) + ((LayoutParams) view.getLayoutParams()).bottomMargin;
        }

        public int getDecoratedStart(View view) {
            return this.mLayoutManager.getDecoratedTop(view) - ((LayoutParams) view.getLayoutParams()).topMargin;
        }

        public int getTotalSpace() {
            return (this.mLayoutManager.getHeight() - this.mLayoutManager.getPaddingTop()) - this.mLayoutManager.getPaddingBottom();
        }

        public void offsetChild(View view, int offset) {
            view.offsetTopAndBottom(offset);
        }

        public int getEndPadding() {
            return this.mLayoutManager.getPaddingBottom();
        }
    }

    public abstract int getDecoratedEnd(View view);

    public abstract int getDecoratedMeasurement(View view);

    public abstract int getDecoratedMeasurementInOther(View view);

    public abstract int getDecoratedStart(View view);

    public abstract int getEnd();

    public abstract int getEndAfterPadding();

    public abstract int getEndPadding();

    public abstract int getStartAfterPadding();

    public abstract int getTotalSpace();

    public abstract void offsetChild(View view, int i);

    public abstract void offsetChildren(int i);

    private OrientationHelper(LayoutManager layoutManager) {
        this.mLastTotalSpace = Integer.MIN_VALUE;
        this.mLayoutManager = layoutManager;
    }

    public void onLayoutComplete() {
        this.mLastTotalSpace = getTotalSpace();
    }

    public int getTotalSpaceChange() {
        return Integer.MIN_VALUE == this.mLastTotalSpace ? HORIZONTAL : getTotalSpace() - this.mLastTotalSpace;
    }

    public static OrientationHelper createOrientationHelper(LayoutManager layoutManager, int orientation) {
        switch (orientation) {
            case HORIZONTAL:
                return createHorizontalHelper(layoutManager);
            case VERTICAL:
                return createVerticalHelper(layoutManager);
            default:
                throw new IllegalArgumentException("invalid orientation");
        }
    }

    public static OrientationHelper createHorizontalHelper(LayoutManager layoutManager) {
        return new AnonymousClass_1(layoutManager);
    }

    public static OrientationHelper createVerticalHelper(LayoutManager layoutManager) {
        return new AnonymousClass_2(layoutManager);
    }
}
