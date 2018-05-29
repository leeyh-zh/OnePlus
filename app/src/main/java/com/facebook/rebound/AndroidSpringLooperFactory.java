package com.facebook.rebound;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;

abstract class AndroidSpringLooperFactory {

    @TargetApi(16)
    private static class ChoreographerAndroidSpringLooper extends SpringLooper {
        private final Choreographer mChoreographer;
        private final FrameCallback mFrameCallback;
        private long mLastTime;
        private boolean mStarted;

        public static ChoreographerAndroidSpringLooper create() {
            return new ChoreographerAndroidSpringLooper(Choreographer.getInstance());
        }

        public ChoreographerAndroidSpringLooper(Choreographer choreographer) {
            this.mChoreographer = choreographer;
            this.mFrameCallback = new FrameCallback() {
                public void doFrame(long frameTimeNanos) {
                    if (ChoreographerAndroidSpringLooper.this.mStarted && ChoreographerAndroidSpringLooper.this.mSpringSystem != null) {
                        long currentTime = SystemClock.uptimeMillis();
                        ChoreographerAndroidSpringLooper.this.mSpringSystem.loop((double) (currentTime - ChoreographerAndroidSpringLooper.this.mLastTime));
                        ChoreographerAndroidSpringLooper.this.mLastTime = currentTime;
                        ChoreographerAndroidSpringLooper.this.mChoreographer.postFrameCallback(ChoreographerAndroidSpringLooper.this.mFrameCallback);
                    }
                }
            };
        }

        public void start() {
            if (!this.mStarted) {
                this.mStarted = true;
                this.mLastTime = SystemClock.uptimeMillis();
                this.mChoreographer.removeFrameCallback(this.mFrameCallback);
                this.mChoreographer.postFrameCallback(this.mFrameCallback);
            }
        }

        public void stop() {
            this.mStarted = false;
            this.mChoreographer.removeFrameCallback(this.mFrameCallback);
        }
    }

    private static class LegacyAndroidSpringLooper extends SpringLooper {
        private final Handler mHandler;
        private long mLastTime;
        private final Runnable mLooperRunnable;
        private boolean mStarted;

        public static SpringLooper create() {
            return new LegacyAndroidSpringLooper(new Handler());
        }

        public LegacyAndroidSpringLooper(Handler handler) {
            this.mHandler = handler;
            this.mLooperRunnable = new Runnable() {
                public void run() {
                    if (LegacyAndroidSpringLooper.this.mStarted && LegacyAndroidSpringLooper.this.mSpringSystem != null) {
                        LegacyAndroidSpringLooper.this.mSpringSystem.loop((double) (SystemClock.uptimeMillis() - LegacyAndroidSpringLooper.this.mLastTime));
                        LegacyAndroidSpringLooper.this.mHandler.post(LegacyAndroidSpringLooper.this.mLooperRunnable);
                    }
                }
            };
        }

        public void start() {
            if (!this.mStarted) {
                this.mStarted = true;
                this.mLastTime = SystemClock.uptimeMillis();
                this.mHandler.removeCallbacks(this.mLooperRunnable);
                this.mHandler.post(this.mLooperRunnable);
            }
        }

        public void stop() {
            this.mStarted = false;
            this.mHandler.removeCallbacks(this.mLooperRunnable);
        }
    }

    AndroidSpringLooperFactory() {
    }

    public static SpringLooper createSpringLooper() {
        return VERSION.SDK_INT >= 16 ? ChoreographerAndroidSpringLooper.create() : LegacyAndroidSpringLooper.create();
    }
}
