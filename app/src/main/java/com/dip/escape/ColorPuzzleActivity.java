package com.dip.escape;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

public class ColorPuzzleActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public interface NewColorReadListener {
        void onColorRead(DetectedColor color);
    }

    public  enum DetectedColor {
        PURPLE,
        PINK,
        BLUE,
        BLACK,
        WHITE,
        YELLOW,
        GREEN,
        ORANGE,
        RED,
        GREY;

        public int getValue() {
            return ordinal() + 1;
        }


        public static DetectedColor fromInteger(int x) {
            switch(x) {
                case 1:
                    return PURPLE;
                case 2:
                    return PINK;
                case 3:
                    return BLUE;
                case 4:
                    return BLACK;
                case 5:
                    return WHITE;
                case 6:
                    return YELLOW;
                case 7:
                    return GREEN;
                case 8:
                    return ORANGE;
                case 9:
                    return RED;
                case 10:
                    return GREY;
            }
            return null;
        }

    }

    private Button mTakePhotoButton;
    private TextView mHexTextView;
    private TextView mInstructions;

    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;
    private NewColorReadListener mListener;

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mTakePicture;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_color_puzzle);
        mTakePicture = false;

        mHexTextView = (TextView) findViewById(R.id.color);
        mTakePhotoButton = (Button) findViewById(R.id.takePicture);
        mInstructions = (TextView) findViewById(R.id.instructions);
        final AlertDialog diag = new AlertDialog.Builder(this)
                .setTitle("How to play?")
                .setView(R.layout.instruction_dialog_frag)
                .create();
        mInstructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                diag.show();
            }
        });

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        PhotonConnect.authenticate(this);

        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mTakePicture = true;
            }
        });


        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.block_game_fragment, new BlockGameFragment());
        ft.commit();
    }

    public synchronized void registerNewColorReadListener(NewColorReadListener listener) {
        mListener = listener;
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

                    DetectedColor c = getColorName(red,green,blue);
                    mListener.onColorRead(c);
                    mHexTextView.setText(c.name());
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

    public DetectedColor getColorName(int r, int g, int b) {
        if (r > 190 && g > 190 && b > 190) {
            return DetectedColor.WHITE;
        } else if (r < 45 && g < 45 && b < 45) {
            return DetectedColor.BLACK;
        } else if (Math.abs(r-g) < 25 && Math.abs(r - b) < 25 && Math.abs(b-g) < 25) {
            return DetectedColor.GREY;
        } else if ((b - r) > 90 && b > g) {
            return DetectedColor.BLUE;
        } else if ((g - r) > 25 && g > b) {
            return DetectedColor.GREEN;
        }  else if ((r - g) < 40 && g > b) {
            return DetectedColor.YELLOW;
        } else if ((r - g) >= 30 && (r - g) < 120 && r>b && g>b) {
            return DetectedColor.ORANGE;
        } else if ((r - b) > 100 && r > g) {
            return DetectedColor.RED;
        } else if ((r - g) > 30 && r > b) {
            return DetectedColor.PINK;
        } else if ((b - g) > 25 && b > r) {
            return DetectedColor.PURPLE;
        }
        return DetectedColor.GREY;
    }
}
