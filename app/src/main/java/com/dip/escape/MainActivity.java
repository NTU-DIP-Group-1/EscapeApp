package com.dip.escape;


import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.IOException;

public class MainActivity  extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private ImageView mImageView;
    private SurfaceView mSurfaceView;
    private Bitmap mImageBitmap;
    private Button mTakePhotoButton;
    private TextView mHexTextView;
    private View mColorSeen;

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
                    String strColor = String.format("#%06X", 0xFFFFFF & color);
                    mHexTextView.setText(strColor);
                    mColorSeen.setBackgroundColor(color);
                }
            });


        }

    }

    public Bitmap rotateImage(Bitmap src, float degree) {
        // create new matrix object
        Matrix matrix = new Matrix();
        // setup rotation degree
        matrix.postRotate(degree);
        // return new bitmap rotated using matrix
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(),
                matrix, true);
    }

    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

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
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
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

        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
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

        return mRgba; // This function must return
    }

}
