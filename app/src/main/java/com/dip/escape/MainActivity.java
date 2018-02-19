package com.dip.escape;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity  extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private Button mTakePhotoButton;
    private TextView mHexTextView;
    private View mColorSeen;

    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mTakePicture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mTakePicture = false;

        mHexTextView = (TextView) findViewById(R.id.colorHex);
        mTakePhotoButton = (Button) findViewById(R.id.takePicture);
        mColorSeen = findViewById(R.id.color_seen);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               mTakePicture = true;
            }
        });
    }

    public void captureColor(Bitmap mImageBitmap) {
        if (mImageBitmap != null) {
            Bitmap tempBtm = rotateImage(mImageBitmap, 90).copy(Bitmap.Config.ARGB_8888, true);
            Canvas cnvs=new Canvas(tempBtm);

            Paint paint=new Paint();
            paint.setStyle(Paint.Style.STROKE);
            DisplayMetrics dm = getResources().getDisplayMetrics() ;
            float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm);
            paint.setStrokeWidth(strokeWidth);

            paint.setColor(Color.RED);
            cnvs.drawBitmap(tempBtm, 0, 0, null);
            int halfHeight = tempBtm.getHeight()/2;
            int halfWidth = tempBtm.getWidth()/2;
            cnvs.drawRect(halfWidth - 50, halfHeight - 50,halfWidth + 50,halfHeight + 50 , paint);

            final int color = tempBtm.getPixel(halfWidth,halfHeight);

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);

                   // String strColor = String.format("#%06X", 0xFFFFFF & color);
                    mHexTextView.setText(getColorName(red,green,blue));
                    mColorSeen.setBackgroundColor(color);
                }
            });
        }
    }

    public Bitmap rotateImage(Bitmap src, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(),
                matrix, true);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        int halfHeight = mRgba.height()/2;
        int halfWidth = mRgba.width()/2;

        Core.rectangle(mRgba,
                new Point(halfWidth - 20, halfHeight - 20),
                new Point(halfWidth + 20, halfHeight + 20),
                new Scalar(255, 0, 0, 0), 2);

        if (mTakePicture) {
            Bitmap imageBitmap = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, imageBitmap);

            captureColor(imageBitmap);
            mTakePicture = false;
        }

        return mRgba;
    }

    public String getColorName(int r, int g, int b) {
        if (r > 215 && g > 215 && b > 215) {
            return "White";
        } else if (r < 15 && g < 15 && b < 15) {
            return "Black";
        } else if (Math.abs(r-g) < 20 && Math.abs(r - b) < 20 && Math.abs(b-g) < 20 ) {
            return "Grey";
        } else if ((b - r) > 90 && b > g) {
            return "Blue";
        } else if ((g - r) > 25 && g > b) {
            return "Green";
        }  else if ((r - g) < 40 && g > b) {
            return "Yellow";
        } else if ((r - g) >= 30 && (r - g) < 120 && r>b && g>b) {
            return "Orange";
        } else if ((r - b) > 100 && r > g) {
            return "Red";
        } else if ((r - g) > 30 && r > b) {
            return "Pink";
        } else if ((b - g) > 25 && b > r) {
            return "Purple";
        }
        return "unknown";
    }
}
