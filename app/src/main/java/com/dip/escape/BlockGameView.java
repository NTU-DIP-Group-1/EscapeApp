package com.dip.escape;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

import java.util.HashMap;

/**
 * Created by irynashvydchenko on 2018-02-19.
 */

public class BlockGameView extends SurfaceView implements Runnable {

    // This stores all the colors and maps them to a hex value that we want to display
    public HashMap<MainActivity.DetectedColor, String> colorValues;

    // thread is needded to not lag the UI
    private Thread mThread = null;

    // first integer indicates what direction the cube should move in
    // ie. 1 => vertical 2 => horizontal
    private int[][] gameBoard = new int[][]{{11,0,0,0,13,0,0},
                                            {11,0,22,22,13,24,24},
                                            {0,0,0,0,13,17,0},
                                            {0,0,15,26,26,17,18},
                                            {29,29,15,0,0,17,18},
                                            {0,0,0,0,0,0,18}};

    // a boolean that keeps track of whether the user is playing the game currently
    private volatile boolean mPlaying;

    // These variables are used to draw the game
    private Canvas mCanvas;
    private SurfaceHolder mHolder;
    private Paint mPaint;
    private Paint mArrowPaint;

    // these are the game play screen width and height
    private int mScreenWidth;
    private int mScreenHeight;

    // this is how often we wnat to refresh the screen that holds the game
    private long mNextFrameTime;
    private final long FRAMES_PER_SECOND = 10;

    // dynamically calculated block sizes, based on screen size
    private int mBlockSizeWidth;
    private int mBlockSizeHeight;

    // set number of blocks we want for width and height
    private final int NUM_BLOCKS_WIDE = 7;
    private final int NUM_BLOCKS_HIGH = 6;

    // this keeps track of whether we have drawn the exit arrow
    private boolean mArrowDrawn;

    // this is set to true if we are currently moving a block
    private boolean mCurrentlyMoving;

    public BlockGameView(Context context) {
        super(context);

        initColorVals();
        mPlaying = false;
        mHolder = getHolder();
        mPaint = new Paint();
        mArrowPaint = new Paint();
        mArrowDrawn = false;
        mCurrentlyMoving = false;

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mScreenHeight = getHeight();
                mScreenWidth = getWidth() - 100;

                mBlockSizeWidth = mScreenWidth / NUM_BLOCKS_WIDE;
                mBlockSizeHeight = mScreenHeight / NUM_BLOCKS_HIGH;

                startGame();
                mPlaying = true;
            }
        });
    }

    public void initColorVals() {
        colorValues = new HashMap<MainActivity.DetectedColor, String>();
        colorValues.put(MainActivity.DetectedColor.RED, "#FF0000");
        colorValues.put(MainActivity.DetectedColor.WHITE, "#FFFFFF");
        colorValues.put(MainActivity.DetectedColor.BLUE, "#0051ff");
        colorValues.put(MainActivity.DetectedColor.GREEN, "#1bcc00");
        colorValues.put(MainActivity.DetectedColor.PURPLE, "#8227f6");
        colorValues.put(MainActivity.DetectedColor.PINK, "#ff2beb");
        colorValues.put(MainActivity.DetectedColor.ORANGE, "#ffa700");
        colorValues.put(MainActivity.DetectedColor.YELLOW, "#fdff00");
        colorValues.put(MainActivity.DetectedColor.BLACK, "#000000");
    }

    public void run() {
        while (mPlaying) {
            if(checkForUpdate()) {
                drawGame();
            }
        }
    }

    public void startGame() {
        drawGame();
    }

    public boolean checkForUpdate() {
        if(mNextFrameTime <= System.currentTimeMillis()){
            mNextFrameTime =System.currentTimeMillis() + 1000 / FRAMES_PER_SECOND;
            return true;
        }
        return false;
    }

    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
        } catch (Exception e) {
        }
    }

    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void drawGame() {
        if (mHolder.getSurface().isValid()) {
            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.argb(255, 255, 204, 153));

            for (int i = 0; i < NUM_BLOCKS_HIGH; i++) {
                for (int j = 0; j < NUM_BLOCKS_WIDE ; j++ ) {
                    int colorInt = gameBoard[i][j] %10;
                    if (colorInt > 0) {

                        if (colorInt == MainActivity.DetectedColor.RED.getValue() && !mArrowDrawn) {
                            drawArrow(i, j);
                        }

                        mPaint.setColor(Color.parseColor(colorValues.get(MainActivity.DetectedColor.fromInteger(colorInt))));

                        mCanvas.drawRect( j * mBlockSizeWidth,
                                i * mBlockSizeHeight,
                                (j + 1) * mBlockSizeWidth,
                                (1 + i) * mBlockSizeHeight,
                                mPaint);
                    }
                }
            }

            mArrowDrawn = false;
            mHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    public void drawArrow(int i, int j) {
        mArrowPaint.setStyle(Paint.Style.STROKE);
        mArrowPaint.setStrokeWidth(4);
        mArrowPaint.setColor(Color.RED);
        Path path = new Path();
        path.moveTo(40, 0);
        path.lineTo(0, 20);
        path.moveTo(40, 0);
        path.lineTo(0, -20);
        path.moveTo(0, 20);
        path.lineTo(0, -20);
        path.moveTo(0, 0);
        path.lineTo(-40, 0);

        path.close();
        path.offset(((NUM_BLOCKS_WIDE)*mBlockSizeWidth +60), (i)*mBlockSizeHeight + 60);
        mCanvas.drawPath(path, mArrowPaint);
        mArrowDrawn = true;
    }

    public void executeMoveBlock(MainActivity.DetectedColor color) {
        boolean moved = false;
        if (colorValues.get(color) != null) {
            int colorToMove = color.getValue();
            for (int i = 0; i < NUM_BLOCKS_HIGH; i++) {
                for (int j = 0; j < NUM_BLOCKS_WIDE; j++) {
                    int colorInt = gameBoard[i][j] %10;
                    if (colorInt == colorToMove) {
                        int direction = gameBoard[i][j]/10;
                        if (direction == 1) {
                            int top = -1, bot = -1;
                            int count = i-1;
                            while (count >= 0){
                                if (gameBoard[count][j]%10 == colorToMove) {
                                    count --;
                                } else {
                                    top = count;
                                    break;
                                }
                            }
                            count = i+1;

                            while (count < NUM_BLOCKS_HIGH){
                                if (gameBoard[count][j]%10 == colorToMove) {
                                    count ++;
                                } else {
                                    bot = count;
                                    break;
                                }
                            }
                            int max = bot >= 0 ? bot : NUM_BLOCKS_HIGH;
                            int lengthBlock = max - (top+1);
                            if (top > -1 && gameBoard[top][j]%10 == 0) {
                                int emptyBlock = top;
                                while (emptyBlock >= 0 && gameBoard[emptyBlock][j]%10 == 0) {
                                    emptyBlock--;
                                }


                                for (int k = top + 1; k < max; k ++) {
                                    gameBoard[k][j] = 0;
                                }
                                for (int k = lengthBlock-1; k >= 0; k--) {
                                    gameBoard[(emptyBlock+1+k)][j] = colorToMove + 10;
                                }
                                moved = true;
                                break;
                            } else if (bot < NUM_BLOCKS_HIGH && gameBoard[bot][j]%10 == 0) {
                                int emptyBlock = bot;
                                while (emptyBlock < NUM_BLOCKS_HIGH && gameBoard[emptyBlock][j]%10 == 0) {
                                    emptyBlock++;
                                }
                                for (int k = top + 1; k < bot; k ++) {
                                    gameBoard[k][j] = 0;
                                }
                                for (int k = lengthBlock-1; k >= 0; k--) {
                                    gameBoard[(emptyBlock-1-k)][j] = colorToMove + 10;

                                }
                                moved = true;
                                break;
                            }
                        } else {
                            int left = -1, right = -1;
                            int count = j-1;
                            while (count >= 0){
                                if (gameBoard[i][count]%10 == colorToMove) {
                                    count --;
                                } else {
                                    left = count;

                                    break;
                                }
                            }
                            count = j+1;

                            while (count < NUM_BLOCKS_WIDE){
                                if (gameBoard[i][count]%10 == colorToMove) {
                                    count ++;
                                } else {
                                    right = count;
                                    break;
                                }
                            }
                            int max = right >= 0 ? right : NUM_BLOCKS_WIDE;

                            int lengthBlock = max - (left+1);
                            if (left > -1 && gameBoard[i][left]%10 == 0) {
                                int emptyBlock = left;
                                while (emptyBlock >= 0 && gameBoard[i][emptyBlock]%10 == 0) {
                                    emptyBlock--;
                                }

                                for (int k = left + 1; k < max; k ++) {
                                    gameBoard[i][k] = 0;
                                }
                                for (int k = lengthBlock-1; k >= 0; k--) {
                                    gameBoard[i][(emptyBlock+1+k)] = colorToMove + 20;
                                }
                                moved = true;
                                break;
                            } else if (right < NUM_BLOCKS_WIDE && gameBoard[i][right]%10 == 0) {
                                int emptyBlock = right;
                                while (emptyBlock < NUM_BLOCKS_WIDE && gameBoard[i][emptyBlock]%10 == 0) {
                                    emptyBlock++;
                                }
                                for (int k = left + 1; k < right; k ++) {
                                    gameBoard[i][k] = 0;
                                }
                                for (int k = lengthBlock-1; k >= 0; k--) {
                                    gameBoard[i][(emptyBlock-1-k)] = colorToMove + 20;
                                }
                                moved = true;
                                break;
                            }
                        }
                    }
                }
                if (moved) {
                    break;
                }
            }
        }
        mCurrentlyMoving = false;
    }

    public void moveBlock(MainActivity.DetectedColor color) {
        if (!mCurrentlyMoving) {
            mCurrentlyMoving = true;
            executeMoveBlock(color);
        }
    }

}
