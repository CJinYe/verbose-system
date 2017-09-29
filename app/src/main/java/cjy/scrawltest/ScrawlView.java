package cjy.scrawltest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * @author 陈锦业
 * @version $Rev$
 * @time 2017-9-27 14:04
 * @des ${TODO}
 * @updateAuthor $Author$
 * @updateDate $Date$
 * @updateDes ${TODO}
 */
public class ScrawlView extends View {
    private int mScreenWidth;
    private int mScreenHeight;
    private int currentWidth = 5;
    private int currentColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private final int STYLE_PAINT = 1;
    private final int STYLE_ERASER = 0;
    private int currentStyle = STYLE_PAINT;
    private Canvas mCanvas;
    private Paint mPaint;
    private Paint mPaintUp;
    private Paint mPaintDown;
    private Bitmap mBitmap;
    private float mDownX;
    private float mDownY;
    private float mEndX;
    private float mEndY;
    private long mEndTime;
    private Path mPath;
    private DrawPath mDrawPath;
    private long mStartDownTime;
    private long mFirstTouchTime;
    private ArrayList<DrawPath> mSaveDrawPaths = new ArrayList<>();
    private Path mPathDown;
    private Path mPathUp;
    private float mStartX;
    private float mStartY;

    public ScrawlView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrawlView(Context context, int screenWidth, int screenHeight) {
        super(context);
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mBitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_4444);
        mCanvas = new Canvas(mBitmap);
        initPaint();
    }

    private void initPaint() {
        mPaint = getNewPaint();
        mPaintUp = getNewPaint();
        mPaintDown = getNewPaint();
    }

    private Paint getNewPaint() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        if (currentStyle == STYLE_PAINT) {
            paint.setStrokeWidth(currentWidth);
            paint.setColor(currentColor);
        } else if (currentStyle == STYLE_ERASER) {
            paint.setStrokeWidth(60);
            paint.setColor(backgroundColor);
        }
        return paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.drawBitmap(mBitmap, 0, 0, null);
        if (mPathUp != null)
            canvas.drawPath(mPathUp, mPaintUp);
        if (mPathDown != null)
            canvas.drawPath(mPathDown, mPaintDown);
        if (mPath != null)
            canvas.drawPath(mPath, mPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                mStartDownTime = System.currentTimeMillis();
                mFirstTouchTime = System.currentTimeMillis();
                mPath = new Path();
                mDrawPath = new DrawPath();
                mPath.moveTo(x, y);
                mDownX = x;
                mDownY = y;
                mStartX = x;
                mStartY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                mEndX = x;
                mEndY = y;
                mEndTime = System.currentTimeMillis();
                if (currentStyle == STYLE_PAINT &&
                        (Math.abs(x - mDownX) > 3 || Math.abs(y - mDownY) > 3)) {
                    calculate();
                }

                mStartX = x;
                mStartY = y;
                mStartDownTime = System.currentTimeMillis();

                if (Math.abs(x - mDownX) > 3 || Math.abs(y - mDownY) > 3) {
                    float endX = (mDownX + x) / 2;
                    float endY = (mDownY + y) / 2;
                    mPath.quadTo(mDownX, mDownY, endX, endY);
                }

                mDownX = x;
                mDownY = y;
                break;
            case MotionEvent.ACTION_UP:
                float endX = (mDownX + x) / 2;
                float endY = (mDownY + y) / 2;
                mPath.quadTo(mDownX, mDownY, endX, endY);

                mCanvas.drawPath(mPath, mPaint);

                mEndTime = System.currentTimeMillis();
                mEndX = x;
                mEndY = y;

                if (currentStyle == STYLE_PAINT) {
                    calculate();
                    PathMeasure pathMeasure = new PathMeasure(mPath, false);
                    if (pathMeasure.getLength() < 60) {
                        mPath.addPath(mPathUp, 1.1f, 1.1f);
                        mPath.addPath(mPathDown, -1f, -1f);
                    }
                    mCanvas.drawPath(mPathUp, mPaintUp);
                    mCanvas.drawPath(mPathDown, mPaintDown);
                }

                mDrawPath.paint = mPaint;
                mDrawPath.path = mPath;
                mSaveDrawPaths.add(mDrawPath);

                mPath = null;// 重新置空
                mPathUp = null;
                mPathDown = null;
                lastResult = 0;
                lastCalculateTime = 0;
                break;

            default:
                break;
        }
        invalidate();
        return true;
    }

    public void undo() {
        if (mSaveDrawPaths != null && mSaveDrawPaths.size() > 0) {
            mSaveDrawPaths.remove(mSaveDrawPaths.size() - 1);
            mCanvas.drawColor(backgroundColor);
            for (DrawPath drawPath : mSaveDrawPaths) {
                mCanvas.drawPath(drawPath.path, drawPath.paint);
                if (drawPath.paintUp != null) {
                    mCanvas.drawPath(drawPath.pathUp, drawPath.paintUp);
                    mCanvas.drawPath(drawPath.pathDown, drawPath.paintDown);
                }
            }
        }
        invalidate();
    }

    public void redo() {
        mSaveDrawPaths.clear();
        mCanvas.drawColor(backgroundColor);
        invalidate();
    }

    public String saveBitmap(String time, String content) throws FileNotFoundException {
        String path = Environment.getExternalStorageDirectory() + "/Scrawl";
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        String filePath = path + "/" + content + "_" + time + ".png";
        File file = new File(filePath);
        FileOutputStream fos = null;
        fos = new FileOutputStream(file);
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        return filePath;
    }

    //上一次计算的时间,如果间隔太长就不执行
    private long lastCalculateTime = 0;

    private void calculate() {

        //如果两点相隔时间太长就返回
        if (lastCalculateTime != 0 && (System.currentTimeMillis() - lastCalculateTime) > 500) {
            lastCalculateTime = System.currentTimeMillis();
            mPathDown.moveTo(mEndX, mEndY);
            mPathUp.moveTo(mEndX, mEndY);
            return;
        }

        float velocity = velocityFrom();
        float radius = (float) (controlPaint(velocity, currentWidth) / 1.8);
        float ratio;
        if (mScreenWidth > mScreenHeight)
            ratio = (float) mScreenWidth / 1280;
        else
            ratio = (float) mScreenWidth / 1280;

        radius = radius * ratio;

        // 根据角度算出四边形的四个点
        float offsetX = (float) (radius * Math.sin(Math.atan((mEndY - mStartY) / (mEndX - mStartX))));
        float offsetY = (float) (radius * Math.sin(Math.atan((mEndY - mStartY) / (mEndX - mStartX))));

        offsetX = Math.abs(offsetX);
        offsetY = Math.abs(offsetY);
        if (Float.isNaN(offsetX) || Float.isInfinite(offsetX)) {
            offsetX = 1;
        }
        if (Float.isNaN(offsetY) || Float.isInfinite(offsetY)) {
            offsetY = 1;
        }

        float x1 = mStartX - offsetX;
        float y1 = mStartY + offsetY;

        float x2 = mEndX - offsetX;
        float y2 = mEndY + offsetY;

        float x3 = mEndX + offsetX;
        float y3 = mEndY - offsetY;

        float x4 = mStartX + offsetX;
        float y4 = mStartY - offsetY;

        if (mPathUp == null) {
            mPathUp = new Path();
            mPathUp.moveTo(x1, y1);
            //            mPathUp.lineTo(x2,y2);
            float endX1 = (x2 + x1) / 2;
            float endY1 = (y2 + y1) / 2;
            mPathUp.quadTo(x1, y1, endX1, endY1);

            mPathDown = new Path();
            mPathDown.moveTo(x4, y4);
            //            mPathDown.lineTo(x3,y3);
            float endX2 = (x3 + x4) / 2;
            float endY2 = (y4 + y3) / 2;
            mPathDown.quadTo(x4, y4, endX2, endY2);

            mDrawPath.paintUp = mPaintUp;
            mDrawPath.pathUp = mPathUp;
            mDrawPath.paintDown = mPaintDown;
            mDrawPath.pathDown = mPathDown;
        } else {
            float endX1 = (x2 + x1) / 2;
            float endY1 = (y2 + y1) / 2;
            mPathUp.quadTo(x1, y1, endX1, endY1);

            float endX2 = (x3 + x4) / 2;
            float endY2 = (y4 + y3) / 2;
            mPathDown.quadTo(x4, y4, endX2, endY2);
        }

        lastCalculateTime = System.currentTimeMillis();
    }

    /**
     * 计算笔画的速度
     *
     * @return 速度
     */
    public float velocityFrom() {
        float distanceTo = (float) Math.sqrt(Math.pow(mStartX - mEndX, 2) + Math.pow(mStartY - mEndY, 2));
        float velocity = distanceTo / (mEndTime - mStartDownTime);
        //        if (velocity != velocity)
        //            return 0f;
        return velocity;
    }

    private final float KEY_PAINT_WIDTH = 2.1f;
    private float lastResult = 0;

    private float controlPaint(double velocity, float paintSize) {
        //余弦函数
        //y=0.5*[cos(x*PI)+1]
        float result;
        if (velocity <= 0.2) {
            result = ((float) velocity / 3);
        } else if (velocity < 0.5) {
            result = ((float) velocity / 2);

        } else if (velocity < 0.7) {
            result = (float) ((float) velocity / 1.5);

        } else if (velocity < 0.9) {
            result = ((float) velocity / 1);

        } else if (velocity < 1) {
            result = (float) ((float) velocity * 1.2);

        } else if (velocity <= 2) {
            result = (float) velocity * 1.5f;

        } else if (velocity > 2) {
            result = (float) ((float) velocity * 1.8);

        } else if (velocity > 3) {
            result = (float) (0.12 * paintSize * KEY_PAINT_WIDTH * (Math.cos(velocity * Math.PI) + 1));


        } else if (velocity > 4) {
            result = (float) (0.09 * paintSize * KEY_PAINT_WIDTH * (Math.cos(velocity * Math.PI) + 1));


        } else if (velocity > 5) {
            result = (float) (0.08 * paintSize * KEY_PAINT_WIDTH * (Math.cos(velocity * Math.PI) + 1));


        } else if (velocity > 6) {
            result = (float) (0.07 * paintSize * KEY_PAINT_WIDTH * (Math.cos(velocity * Math.PI) + 1));


        } else if (velocity > 7) {
            result = (float) (0.06 * paintSize * KEY_PAINT_WIDTH * (Math.cos(velocity * Math.PI) + 1));


        } else {
            result = (float) (0.05 * paintSize * KEY_PAINT_WIDTH * (Math.cos(velocity * Math.PI) + 1));

        }

        if (result > paintSize / 1.1) {
            result = (float) (paintSize / 1.1);
        }

        if (lastResult != 0) {
            if (lastResult > result * 3) {
                result = ((lastResult / 3 > result * 2 ? lastResult / 3 : result * 2));
            } else if (lastResult < result / 3) {
                result = ((lastResult * 3 < result / 2 ? result / 2 : lastResult * 3));
            }
        }
        lastResult = result;
        return result;
    }

    public class DrawPath {
        public Path path;// 路径
        public Paint paint;// 画笔
        public Path pathUp;// 上面的路径
        public Paint paintUp;// 上面的画笔
        public Path pathDown;// 下面的路径路径
        public Paint paintDown;// 下面的画笔
        public int rotate = 0;
    }
}
