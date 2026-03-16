package com.example.emergencylastjournal.ui.emergency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.example.emergencylastjournal.R;

/**
 * Custom View that draws a circular countdown progress bar.
 */
public class CircularCountdownView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;
    private float progress = 1.0f;
    private int colorOrange;
    private int colorRed;
    private int colorDeepRed;

    public CircularCountdownView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        colorOrange = getContext().getColor(R.color.history_orange);
        colorRed = getContext().getColor(R.color.alert_red);
        colorDeepRed = getContext().getColor(R.color.primary); // Deep Red from primary

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(getContext().getColor(R.color.bg_primary_alpha));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(20f);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(20f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float diameter = Math.min(width, height) - progressPaint.getStrokeWidth();
        
        rectF.set((width - diameter) / 2, (height - diameter) / 2, (width + diameter) / 2, (height + diameter) / 2);

        canvas.drawOval(rectF, backgroundPaint);

        // Transition color based on progress (30s total)
        if (progress > 0.66f) { // > 20s
            progressPaint.setColor(colorOrange);
        } else if (progress > 0.16f) { // > 5s
            progressPaint.setColor(colorRed);
        } else {
            progressPaint.setColor(colorDeepRed);
        }

        float angle = 360 * progress;
        canvas.drawArc(rectF, -90, angle, false, progressPaint);
    }

    /**
     * Sets the progress from 0.0 to 1.0.
     */
    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }
}