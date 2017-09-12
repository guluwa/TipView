package indi.yjk.tipview;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by 俊康 on 2017/9/9.
 */

public class TipView extends View {

    private int mWidth, mHeight;
    private Paint mPaint;

    private int startAngle = -90;//画弧线开始角度
    private int sweepAngle = 0;//弧线旋转角度
    private int curAngle = 0;//当前角度
    private boolean isGrow;//标记当前弧度是增加还是减小
    private int type;//0:loading;1:success;2:failed;3:warning;
    private FinishListener listener;

    private float progressRadius;   //圆环半径
    private int progressColor, successColor, failColor, warnColor;    //进度颜色、成功、失败、警告
    private float progressWidth;    //进度宽度
    private ValueAnimator circleAnimator, sleepAnimator;
    private float circleValue, sleepValue, successValue, failValueLeft, failValueRight, warnValueTop, warnValueDown;
    private Path mCirclePath, mDstPath, mSuccessPath, mFailLeftPath, mFailRightPath, mWarnTopPath, mWarnDownPath;
    private PathMeasure mPathMeasure;

    public TipView(Context context) {
        this(context, null);
    }

    public TipView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TipView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomStatusView, defStyleAttr, 0);
        progressColor = array.getColor(R.styleable.CustomStatusView_progress_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        successColor = array.getColor(R.styleable.CustomStatusView_load_success_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        failColor = array.getColor(R.styleable.CustomStatusView_load_failure_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        warnColor = array.getColor(R.styleable.CustomStatusView_load_warn_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        progressRadius = array.getDimension(R.styleable.CustomStatusView_progress_circle_radius, dp2px(getContext(), 50));
        progressWidth = array.getDimension(R.styleable.CustomStatusView_progress_circle_width, dp2px(getContext(), 3));
        array.recycle();

        initStatus();
        initPaths();
        initPaint();
        initAnim();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(progressColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(progressWidth);
        mPaint.setStrokeCap(Paint.Cap.ROUND);    //设置画笔为圆角笔触
    }

    private void initPaths() {
        mCirclePath = new Path();
        mDstPath = new Path();
        mSuccessPath = new Path();
        mFailLeftPath = new Path();
        mFailRightPath = new Path();
        mWarnTopPath = new Path();
        mWarnDownPath = new Path();
        mPathMeasure = new PathMeasure();
    }

    private void initStatus() {
        isGrow = true;
        type = 0;
    }

    private void initAnim() {
        circleAnimator = ValueAnimator.ofFloat(0f, 1f);
        circleAnimator.setDuration(500);
        circleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circleValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        sleepAnimator = ValueAnimator.ofFloat(0f, 1f);
        sleepAnimator.setDuration(500);
        sleepAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                sleepValue = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            width = size;
        } else {
            width = (int) (2 * progressRadius + progressWidth);
        }
        size = MeasureSpec.getSize(heightMeasureSpec);
        mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            height = size;
        } else {
            height = (int) (2 * progressRadius + progressWidth);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(mWidth / 2, mHeight / 2);
        switch (type) {
            case 0://loading
                mPaint.setColor(progressColor);
                if (isGrow) {//处于增加状态，增加角度
                    sweepAngle += 6;
                } else {//处于减少状态，减少角度，并通过减少起始角度，控制最终角度不变
                    startAngle += 6;
                    sweepAngle -= 6;
                }
                //旋转范围20~270
                if (sweepAngle >= 270) {
                    isGrow = false;
                }
                if (sweepAngle <= 20) {
                    isGrow = true;
                }
                canvas.rotate(curAngle += 4, 0, 0);  //旋转的弧长为4
                canvas.drawArc(new RectF(-progressRadius, -progressRadius, progressRadius, progressRadius),
                        startAngle, sweepAngle, false, mPaint);
                invalidate();
                break;
            case 1://success
                mPaint.setColor(successColor);
                mCirclePath.addCircle(0, 0, progressRadius, Path.Direction.CW);
                mPathMeasure.setPath(mCirclePath, false);
                mPathMeasure.getSegment(0, circleValue * mPathMeasure.getLength(), mDstPath, true);
                canvas.drawPath(mDstPath, mPaint);
                if (circleValue == 1) {//圆已完成，开始画勾
                    mSuccessPath.moveTo(-progressRadius / 3, 0);
                    mSuccessPath.lineTo(0, progressRadius / 4);
                    mSuccessPath.lineTo(
                            Float.valueOf(String.valueOf(progressRadius * 4 * Math.sqrt(2) / 18)),
                            -Float.valueOf(String.valueOf(progressRadius * 4 * Math.sqrt(2) / 18)));
                    mPathMeasure.nextContour();
                    mPathMeasure.setPath(mSuccessPath, false);
                    mPathMeasure.getSegment(0, mPathMeasure.getLength() * successValue, mDstPath, true);
                    canvas.drawPath(mDstPath, mPaint);
                }
                if (sleepValue == 1)
                    listener.finish();
                break;
            case 2://fail
                mPaint.setColor(failColor);
                mCirclePath.addCircle(0, 0, progressRadius, Path.Direction.CW);
                mPathMeasure.setPath(mCirclePath, false);
                mPathMeasure.getSegment(0, circleValue * mPathMeasure.getLength(), mDstPath, true);
                canvas.drawPath(mDstPath, mPaint);
                if (circleValue == 1) {//圆已完成，开始画左下划线
                    mFailLeftPath.moveTo(-progressRadius / 4, -progressRadius / 4);
                    mFailLeftPath.lineTo(progressRadius / 4, progressRadius / 4);
                    mPathMeasure.nextContour();
                    mPathMeasure.setPath(mFailLeftPath, false);
                    mPathMeasure.getSegment(0, mPathMeasure.getLength() * failValueLeft * 2, mDstPath, true);
                    canvas.drawPath(mDstPath, mPaint);
                }
                if (failValueLeft >= 0.5) {//开始画右下划线
                    mFailRightPath.moveTo(progressRadius / 4, -progressRadius / 4);
                    mFailRightPath.lineTo(-progressRadius / 4, progressRadius / 4);
                    mPathMeasure.nextContour();
                    mPathMeasure.setPath(mFailRightPath, false);
                    mPathMeasure.getSegment(0, mPathMeasure.getLength() * failValueRight * 2, mDstPath, true);
                    canvas.drawPath(mDstPath, mPaint);
                }
                if (sleepValue == 1)
                    listener.finish();
                break;
            case 3://warn
                mPaint.setColor(warnColor);
                mCirclePath.addCircle(0, 0, progressRadius, Path.Direction.CW);
                mPathMeasure.setPath(mCirclePath, false);
                mPathMeasure.getSegment(0, circleValue * mPathMeasure.getLength(), mDstPath, true);
                canvas.drawPath(mDstPath, mPaint);
                if (circleValue == 1) {//圆已完成，开始画上面部分
                    mWarnTopPath.moveTo(0, -progressRadius / 2);
                    mWarnTopPath.lineTo(0, -progressRadius / 3);
                    mPathMeasure.nextContour();
                    mPathMeasure.setPath(mWarnTopPath, false);
                    mPathMeasure.getSegment(0, mPathMeasure.getLength() * warnValueTop, mDstPath, true);
                    canvas.drawPath(mDstPath, mPaint);
                }
                if (warnValueTop == 1) {//开始画下面部分
                    mWarnDownPath.moveTo(0, -progressRadius / 5);
                    mWarnDownPath.lineTo(0, progressRadius / 2);
                    mPathMeasure.nextContour();
                    mPathMeasure.setPath(mWarnDownPath, false);
                    mPathMeasure.getSegment(0, mPathMeasure.getLength() * warnValueDown * 2, mDstPath, true);
                    canvas.drawPath(mDstPath, mPaint);
                }
                if (sleepValue == 1)
                    listener.finish();
                break;
        }
    }

    public void loadLoading() {
        type = 0;
        invalidate();
    }

    public void loadSuccess() {
        type = 1;
        startSuccessAnim();
    }

    public void loadFailure() {
        type = 2;
        startFailAnim();
    }

    public void loadWarning() {
        type = 3;
        startWarnAnim();
    }

    private void startSuccessAnim() {
        ValueAnimator success = ValueAnimator.ofFloat(0f, 1.0f);
        success.setDuration(500);
        success.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                successValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //组合动画,一先一后执行
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(success).after(circleAnimator).before(sleepAnimator);
//        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.start();
    }

    private void startFailAnim() {
        ValueAnimator failLeft = ValueAnimator.ofFloat(0f, 1.0f);
        failLeft.setDuration(250);
        failLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                failValueLeft = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        ValueAnimator failRight = ValueAnimator.ofFloat(0f, 1.0f);
        failRight.setDuration(250);
        failRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                failValueRight = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //组合动画,一先一后执行
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(failLeft).after(circleAnimator).before(failRight).before(sleepAnimator);
//        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.start();
    }

    private void startWarnAnim() {
        ValueAnimator warnTop = ValueAnimator.ofFloat(0f, 1.0f);
        warnTop.setDuration(150);
        warnTop.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                warnValueTop = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        ValueAnimator warnDown = ValueAnimator.ofFloat(0f, 1.0f);
        warnDown.setDuration(350);
        warnDown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                warnValueDown = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(warnTop).after(circleAnimator).before(warnDown).before(sleepAnimator);
//        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.start();
    }

    /**
     * dp转px
     */
    public static int dp2px(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * px转dp
     */
    public static float px2dp(Context context, int px) {
        return (px / context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 将sp值转换为px值
     */
    public static int sp2px(Context context, float sp) {
        return (int) (sp * context.getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }

    public interface FinishListener {
        void finish();
    }

    public void setListener(FinishListener listener) {
        this.listener = listener;
    }
}
