package android.support.v4.view.animation;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.v4.widget.AutoScrollHelper;
import android.view.animation.Interpolator;
import com.android.volley.DefaultRetryPolicy;

class PathInterpolatorApi14 implements Interpolator {
    private static final float PRECISION = 0.002f;
    private final float[] mX;
    private final float[] mY;

    PathInterpolatorApi14(Path path) {
        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();
        int numPoints = ((int) (pathLength / 0.002f)) + 1;
        this.mX = new float[numPoints];
        this.mY = new float[numPoints];
        float[] position = new float[2];
        for (int i = 0; i < numPoints; i++) {
            pathMeasure.getPosTan((((float) i) * pathLength) / ((float) (numPoints - 1)), position, null);
            this.mX[i] = position[0];
            this.mY[i] = position[1];
        }
    }

    PathInterpolatorApi14(float controlX, float controlY) {
        this(createQuad(controlX, controlY));
    }

    PathInterpolatorApi14(float controlX1, float controlY1, float controlX2, float controlY2) {
        this(createCubic(controlX1, controlY1, controlX2, controlY2));
    }

    public float getInterpolation(float t) {
        if (t <= 0.0f) {
            return AutoScrollHelper.RELATIVE_UNSPECIFIED;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        int startIndex = 0;
        int endIndex = this.mX.length - 1;
        while (endIndex - startIndex > 1) {
            int midIndex = (startIndex + endIndex) / 2;
            if (t < this.mX[midIndex]) {
                endIndex = midIndex;
            } else {
                startIndex = midIndex;
            }
        }
        float xRange = this.mX[endIndex] - this.mX[startIndex];
        if (xRange == 0.0f) {
            return this.mY[startIndex];
        }
        float fraction = (t - this.mX[startIndex]) / xRange;
        float startY = this.mY[startIndex];
        return ((this.mY[endIndex] - startY) * fraction) + startY;
    }

    private static Path createQuad(float controlX, float controlY) {
        Path path = new Path();
        path.moveTo(AutoScrollHelper.RELATIVE_UNSPECIFIED, AutoScrollHelper.RELATIVE_UNSPECIFIED);
        path.quadTo(controlX, controlY, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        return path;
    }

    private static Path createCubic(float controlX1, float controlY1, float controlX2, float controlY2) {
        Path path = new Path();
        path.moveTo(AutoScrollHelper.RELATIVE_UNSPECIFIED, AutoScrollHelper.RELATIVE_UNSPECIFIED);
        path.cubicTo(controlX1, controlY1, controlX2, controlY2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT, 1.0f);
        return path;
    }
}