package com.yaorugang.coco;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.WindowManager;


/**
 * Created by yaorugang on 4/22/2016.
 */
public class CocoGestureDetector implements Runnable
{
    private final static int COMMAND_LONG_PRESSED    = 0x11000F;

    private Context mContext = null;
    private OnGestureListener mGestureListener = null;

    private long MIN_LONG_PRESSED_TIME = 400;           // 启动 long pressed 门限时间，单位毫秒。
    private double MIN_MOVE_DISTANCE = 0;               // 最小有效滑动距离，如果小于此值，则认为滑动无效。实际数值在构造函数中进行初始化。
    private boolean mLongPressed = false;               // 是否长按
    private boolean mImmediateMove = false;             // 是否按下后立刻滑动
    private boolean mLongPressedMove = false;           // 是否长按后移动
    private boolean mStartTimerForPress = false;        // 启动按下计时，用来辅助判断是否长按
    private Thread mLongPressThread = null;             // 识别长按事件的线程
    private MotionEvent mDownMotionEvent;               // the first down motion event

    public CocoGestureDetector(Context context, CocoGestureDetector.OnGestureListener listener)
    {
        mContext = context;
        mGestureListener = listener;

        // 初始化最小有效滑动距离为屏幕宽度的1/10。
        Point point = new Point();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(point);
        MIN_MOVE_DISTANCE = point.x / 15;

        // 启动长按识别线程
        mLongPressThread = new Thread(this);
        mLongPressThread.start();
    }

    public void setLongPressInterval(long interval)
    {
        MIN_LONG_PRESSED_TIME = interval;
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        // 不支持多点触控，如果发现有多个点，直接返回，不执行任何操作。
        // 多点触控时需要特别处理每个点的index和pointer id, 这样才能精确识别每个点。
        // 此版本用getPointerCount()方法忽略多点触控，这样可以使一些默认的函数比如getX()得到的就是
        // 唯一的触点信息，以简化程序复杂度。
        if (event.getPointerCount() > 1)
            return true;

        boolean willProcessMsg = true;

        switch (event.getActionMasked())
        {
        // 只有第一个点按下时才触发 ACTION_DOWN，后续点按下时触发ACTION_POINTER_DOWN
        case MotionEvent.ACTION_DOWN:
            // copy the first on down motion event，以供后面使用。
            // 因为 onTouchEvent(MotionEvent event) 函数始终使用同一个MotionEvent对象，所以这里需要将
            // 其进行拷贝，保持其独立性。
            mDownMotionEvent = MotionEvent.obtain(event);

            mLongPressed = false;
            mImmediateMove = false;
            mLongPressedMove = false;

            // onDown函数决定按键是否有效，根据onDown的返回值来决定是否处理后续消息。
            willProcessMsg = mGestureListener.onDown(mDownMotionEvent);

            if (willProcessMsg)
                mStartTimerForPress = true; // 启动长按侦测。

            break;
        case MotionEvent.ACTION_MOVE:

            if (mImmediateMove)
            {
                mGestureListener.onMove(mDownMotionEvent, event, getMoveDirection(mDownMotionEvent, event));
            }
            else if (mLongPressedMove)
            {
                mGestureListener.onLongPressedMove(mDownMotionEvent, event, getMoveDirection(mDownMotionEvent, event));
            }
            else
            {
                if (computeMoveDistance(mDownMotionEvent, event) > MIN_MOVE_DISTANCE)
                {
                    if (mLongPressed)
                        mLongPressedMove = true;
                    else
                        mImmediateMove = true;
                }
            }


            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_CANCEL:

            mStartTimerForPress = false;

            if (mImmediateMove)
                willProcessMsg = mGestureListener.onSwipe(mDownMotionEvent, event, getMoveDirection(mDownMotionEvent, event));
            else if (mLongPressedMove)
                willProcessMsg = mGestureListener.onLongPressedSwipe(mDownMotionEvent, event, getMoveDirection(mDownMotionEvent, event));
            else if (mLongPressed)
                willProcessMsg = mGestureListener.onLongPressedTapUp(event);
            else
                willProcessMsg = mGestureListener.onSingleTapUp(event);


            break;
        }

        return willProcessMsg; // 返回true时，表示此ViewGroup拦截消息，MotionEvent对象不会向下传递到ViewGroup包含的控件中去。
    }

    /**
     * 实现Runnable接口的线程函数，此线程函数随着程序运行一直保持独立线程运行，它用来识别长按事件。
     */
    @Override
    public void run()
    {
        while (true)
        {
            if (mStartTimerForPress && !mLongPressed && !mImmediateMove)
            {
                if (getPressedTime() > MIN_LONG_PRESSED_TIME)
                {
                    Message msg = new Message();
                    msg.what = COMMAND_LONG_PRESSED;
                    handler.sendMessage(msg);
                }
            }

            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("<CocoGestureDetector.run(): Unexpected interrupted on thread!");
            }
        }
    }

    private Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case COMMAND_LONG_PRESSED:
                // 在执行onLongPressed()函数之前必须再次判断是否处于按下状态。因为长按消息是子线程通过handler进行
                // 触发的，而handler处理消息的时间不确定，这就可能导致一种情况：长按侦测线程已经向handler发送了长按
                // 事件消息，而在handler处理长按消息之前，手指就离开屏幕了，这时候handler如果还继续执行长按按下消息的话，
                // 后续将没有一个与之对应的长按抬起操作，程序将出现异常。所以在这种情况下，handler应该首先判断用户是否
                // 依旧处于按下状态，如果是，则进行长按按下消息响应。
                // 同理，也必须要判断是否 immediate move被激活，因为很有可能长按消息被触发但在handler处理长按消息之前，
                // 用户移动了手指从而触发了immediate move，这样handler就必须忽略长按消息。
                if (mStartTimerForPress && !mImmediateMove)
                {
                    mLongPressed = true;
                    mGestureListener.onLongPressed(mDownMotionEvent);
                }
                break;
            }

            super.handleMessage(msg);
        }
    };

    private long getPressedTime()
    {
        return SystemClock.uptimeMillis() - mDownMotionEvent.getEventTime();
    }

    private double computeMoveDistance(MotionEvent e1, MotionEvent e2)
    {
        float distanceX = e2.getX() - e1.getX();
        float distanceY = e2.getY() - e1.getY();

        return Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
    }

    private int getMoveDirection(MotionEvent e1, MotionEvent e2)
    {
        // 计算滑动的横向距离。
        float distanceX = e2.getX() - e1.getX();
        // 计算滑动的纵向距离。以为屏幕的坐标原点(0, 0)是从左上角开始的，而后面我们在计算角度时采用标准数学坐标
        // 系，既向上移动为正值，向下移动为负值，所以这里需要使用距离的相反数。
        float distanceY = -(e2.getY() - e1.getY());


        // 计算正切值时，分母不可以为0。
        if (0 == distanceX)
            distanceX = 1;
        if (0 == distanceY)
            distanceY = 1;

        double tangent = distanceY / distanceX; // 计算正切值
        double degree = Math.toDegrees(Math.atan(tangent)); // 将正切值转换为角度

        // 整个圆被划分为12个方向区域，每个区域占据30度，通过正切值计算出来的角度范围为-pi/2到pi/2，也就是说这一步
        // 的方向判断只能限定在Y轴的右侧部分
        int nDirection = 0;
        if (degree < -75)
            nDirection = 0;
        else if (degree < -45)
            nDirection = 1;
        else if (degree < -15)
            nDirection = 2;
        else if (degree < 15)
            nDirection = 3;
        else if (degree < 45)
            nDirection = 4;
        else if (degree < 75)
            nDirection = 5;
        else if (degree < 90)
            nDirection = 6;

        // 再根据滑动的X轴方向，来具体确定实际的滑动方向。
        if (distanceX < 0)
            nDirection = (nDirection + 6) % 12;

        return nDirection;
    }

    public interface OnGestureListener
    {
        /**
         * @param e the down motion event
         * @return true if the event is consumed, else false
         */
        boolean onDown(MotionEvent e);

        /**
         * 手指按下不动超过400毫秒（默认）即触发此事件，可以通过调用setLongPressInterval(long interval)函数更改这个阙值。
         * @param e the initial on down motion event that started the longpress
         */
        void onLongPressed(MotionEvent e);

        /**
         * 手指按下立即开始滑动，将连续触发此事件。
         * @param e1 the first down motion event that started the moving
         * @param e2 the move motion event that triggered the current onMove
         * @param direction 移动的方向。圆周共分12个方向，每个方向范围为30度。垂直向下也就是与Y轴夹角左右各15度的
         *                  范围为方向0，逆时针每30度递增1，直到方向11结束。
         * @return true if the event is consumed, else false
         */
        void onMove(MotionEvent e1, MotionEvent e2, int direction);

        /**
         * 长按以后开始移动，将会连续触发此事件。
         * @param e1 the first down motion event that started the moving
         * @param e2 the move motion event that triggered the current onMove
         * @param direction 移动的方向。圆周共分12个方向，每个方向范围为30度。垂直向下也就是与Y轴夹角左右各15度的
         *                  范围为方向0，逆时针每30度递增1，直到方向11结束。
         * @return true if the event is consumed, else false
         */
        void onLongPressedMove(MotionEvent e1, MotionEvent e2, int direction);

        /**
         * 手指按下后立即开始滑动，当手指抬起时触发此事件。
         * @param e1 the first down motion event that started the swiping
         * @param e2 the up motion event that triggered the swiping
         * @param direction 移动的方向。圆周共分12个方向，每个方向范围为30度。垂直向下也就是与Y轴夹角左右各15度的
         *                  范围为方向0，逆时针每30度递增1，直到方向11结束。
         * @return true if the event is consumed, else false
         */
        boolean onSwipe(MotionEvent e1, MotionEvent e2, int direction);

        /**
         * 长按以后再开始滑动，手指抬起时触发此事件。
         * @param e1 the first down motion event that started the long pressed swiping
         * @param e2 the up motion event that triggered the long pressed swiping
         * @param direction 移动的方向。圆周共分12个方向，每个方向范围为30度。垂直向下也就是与Y轴夹角左右各15度的
         *                  范围为方向0，逆时针每30度递增1，直到方向11结束。
         * @return true if the event is consumed, else false
         */
        boolean onLongPressedSwipe(MotionEvent e1, MotionEvent e2, int direction);

        /**
         * 手指快速单击事件。
         * @param e the up motion event that completed the single tap
         * @return true if the event is consumed, else false
         */
        boolean onSingleTapUp(MotionEvent e);

        /**
         * 长按以后没有移动，直接抬起将会触发此事件。
         * @param e the up motion event that completed the long pressed tap
         * @return true if the event is consumed, else false
         */
        boolean onLongPressedTapUp(MotionEvent e);

    }
}
