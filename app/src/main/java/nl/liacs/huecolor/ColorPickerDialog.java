package nl.liacs.huecolor;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Based on https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/graphics/ColorPickerDialog.java
 */
public class ColorPickerDialog extends Dialog {
    public interface OnColorChangedListener {
        void colorChanged(int color);
    }
    private OnColorChangedListener mListener;
    private int mInitialColor;
    protected ColorPickerView view;

    private static class ColorPickerView extends View {
        private Paint mPaint;
        private Paint mCenterPaint;
        private RectF oval;
        private final int[] mColors;
        private OnColorChangedListener mListener;

        private boolean mTrackingCenter;
        private boolean mHighlightCenter;
        private int canvasLeft = 0, canvasTop = 0;

        private static final int CENTER_X = 100;
        private static final int CENTER_Y = 200;
        private static final int CENTER_RADIUS = 40;

        ColorPickerView(Context c) {
            this(c, null, 0);
        }

        ColorPickerView(Context c, OnColorChangedListener l, int color) {
            super(c);
            mListener = l;
            mColors = new int[] {
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000
            };
            Shader s = new SweepGradient(0, 0, mColors, null);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setShader(s);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(40);
            mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCenterPaint.setColor(color);
            mCenterPaint.setStrokeWidth(5);
            float r = CENTER_X - mPaint.getStrokeWidth() * 0.05f;
            oval = new RectF(-r, -r, r, r);
        }

        public int getColor() {
            return mCenterPaint.getColor();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
            super.onSizeChanged(w, h, oldWidth, oldHeight);
            canvasLeft = w / 2 - CENTER_X;
            canvasTop = h / 2 - CENTER_Y;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.translate(canvasLeft + CENTER_X, canvasTop + CENTER_Y);
            canvas.drawOval(oval, mPaint);
            canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);
            if (mTrackingCenter) {
                int c = mCenterPaint.getColor();
                mCenterPaint.setStyle(Paint.Style.STROKE);
                if (mHighlightCenter) {
                    mCenterPaint.setAlpha(0xFF);
                } else {
                    mCenterPaint.setAlpha(0x80);
                }
                canvas.drawCircle(0, 0, CENTER_RADIUS + mCenterPaint.getStrokeWidth(), mCenterPaint);
                mCenterPaint.setStyle(Paint.Style.FILL);
                mCenterPaint.setColor(c);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), CENTER_Y * 2);
        }

        private int floatToByte(float x) {
            return java.lang.Math.round(x);
        }

        private int clampToByte(int n) {
            if (n < 0) {
                n = 0;
            } else if (n > 255) {
                n = 255;
            }
            return n;
        }

        private int ave(int s, int d, float p) {
            return s + java.lang.Math.round(p * (d - s));
        }

        private int interpretColor(int colors[], float unit) {
            if (unit <= 0) {
                return colors[0];
            }
            if (unit >= 1) {
                return colors[colors.length - 1];
            }
            float p = unit * (colors.length - 1);
            int i = (int)p;
            p -= i;

            // Now p is just the fractional part [0...1) and i is the index
            int c0 = colors[i];
            int c1 = colors[i+1];
            int a = ave(Color.alpha(c0), Color.alpha(c1), p);
            int r = ave(Color.red(c0), Color.red(c1), p);
            int g = ave(Color.green(c0), Color.green(c1), p);
            int b = ave(Color.blue(c0), Color.blue(c1), p);
            return Color.argb(a, r, g, b);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX() - CENTER_X - canvasLeft;
            float y = event.getY() - CENTER_Y - canvasTop;
            boolean inCenter = java.lang.Math.hypot(x, y) <= CENTER_RADIUS;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTrackingCenter = inCenter;
                    if (inCenter) {
                        mHighlightCenter = true;
                        invalidate();
                        break;
                    }
                case MotionEvent.ACTION_MOVE:
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter;
                            invalidate();
                        }
                    } else {
                        float angle = (float)java.lang.Math.atan2(y, x);
                        // We need to turn angle [-PI ... PI] into unit [0....1].
                        float unit = (float) (angle/(2*Math.PI));
                        if (unit < 0) {
                            unit += 1;
                        }
                        mCenterPaint.setColor(interpretColor(mColors, unit));
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mTrackingCenter) {
                        if (inCenter) {
                            mListener.colorChanged(mCenterPaint.getColor());
                        }
                        mTrackingCenter = false;
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    }

    public ColorPickerDialog(Context context, OnColorChangedListener listener, int initialColor) {
        super(context);
        mListener = listener;
        mInitialColor = initialColor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnColorChangedListener l = new OnColorChangedListener() {
            public void colorChanged(int color) {
                dismiss();
            }
        };
        view = new ColorPickerView(getContext(), l, mInitialColor);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        view.setLayoutParams(layoutParams);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mListener.colorChanged(view.getColor());
            }
        });
        setContentView(view);

        setTitle(getContext().getString(R.string.colorpicker_title));
    }
}