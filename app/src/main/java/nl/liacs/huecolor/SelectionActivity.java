package nl.liacs.huecolor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class SelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SelectionView(this));

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(12);
    }

    private Paint       paint;

    public class SelectionView extends View {
        private Bitmap bitmap;
        private Canvas canvas;
        private Path path;
        private Paint bitmapPaint;

        public SelectionView(Context c) {
            super(c);

            BitmapFactory.Options myOptions = new BitmapFactory.Options();
            myOptions.inDither = true;
            myOptions.inScaled = false;
            myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            myOptions.inPurgeable = true;

            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.example, myOptions);
            canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));
            path = new Path();
            bitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //canvas.drawColor(0x0);
            canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
            canvas.drawPath(path, paint);
        }

        private float startX, startY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            path.reset();
            path.moveTo(x, y);
            startX = x;
            startY = y;
        }
        private void touch_move(float x, float y) {
            float dx = Math.abs(x - startX);
            float dy = Math.abs(y - startY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                path.quadTo(startX, startY, (x + startX) / 2, (y + startY) / 2);
                startX = x;
                startY = y;
            }
        }
        private void touch_up() {
            path.lineTo(startX, startY);
            // commit the path to our offscreen
            canvas.drawPath(path, paint);
            // kill this so we don't double draw
            path.reset();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }
}