package com.example.casinolalo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class DiceSliderView extends View
{
    private Paint winPaint, lossPaint, thumbPaint, textPaint;
    private float winChance = 0.5f;
    private float currentValue = 0.5f;
    private final RectF trackRect = new RectF();
    private final float cornerRadius = 20f;

    public DiceSliderView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init()
    {
        //green
        winPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        winPaint.setColor(Color.parseColor("#4CAF50"));

        //red
        lossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lossPaint.setColor(Color.parseColor("#F44336"));

        //blue cursor
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(Color.parseColor("#2196F3"));
        thumbPaint.setShadowLayer(10, 0, 0, Color.BLACK);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setWinChance(float chance)
    {
        this.winChance = chance;
        invalidate();
    }

    public void setCurrentValue(float value)
    {
        this.currentValue = value;
        invalidate();
    }

    public float getCurrentValue()
    {
        return this.currentValue;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        float padding = 40;
        float width = getWidth() - (padding * 2);
        float height = 40;
        float centerY = getHeight() / 2f;

        trackRect.set(padding, centerY - height/2, padding + width, centerY + height/2);

        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, lossPaint);

        RectF winRect = new RectF(padding, centerY - height/2, padding + (width * winChance), centerY + height/2);
        canvas.drawRoundRect(winRect, cornerRadius, cornerRadius, winPaint);

        float thumbX = padding + (width * currentValue);
        canvas.drawCircle(thumbX, centerY, 35, thumbPaint);
        canvas.drawText(String.format(Locale.US, "%.0f", currentValue * 100), thumbX, centerY - 50, textPaint);
        
        canvas.drawText("0", padding, centerY + 80, textPaint);
        canvas.drawText("50", padding + width/2, centerY + 80, textPaint);
        canvas.drawText("100", padding + width, centerY + 80, textPaint);

        float boundaryX = padding + (width * winChance);
        textPaint.setColor(Color.parseColor("#FFD700"));
        canvas.drawText(String.format(Locale.US, "%.0f", winChance * 100), boundaryX, centerY + 50, textPaint);
        textPaint.setColor(Color.WHITE);
    }
}
