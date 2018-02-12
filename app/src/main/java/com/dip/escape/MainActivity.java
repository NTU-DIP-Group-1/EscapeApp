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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity  extends Activity implements SurfaceHolder.Callback {
    private ImageView mImageView;
    private SurfaceView mSurfaceView;
    private Bitmap mImageBitmap;
    private Button mTakePhotoButton;
    private TextView mHexTextView;
    private View mColorSeen;

    private SurfaceHolder mHolder;

    private Camera mCamera;
    private Camera.Parameters mParameters;
    private Camera.PictureCallback mCall;
    private Handler mHandler;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private int mWidth = 0;
    private int mHeight = 0;

    private Camera.Size mPhotoSize;

    boolean mStopHandler = false;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mCamera != null) {
                // do your stuff - don't create a new runnable here!
                mCamera.takePicture(null, null, mCall);

                if (!mStopHandler) {
                    mHandler.postDelayed(this, 10);
                }
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mPreferences = getApplicationContext().getSharedPreferences("pref", 0);
        mEditor = mPreferences.edit();

        mHexTextView = (TextView) findViewById(R.id.colorHex);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mTakePhotoButton = (Button) findViewById(R.id.takePicture);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mColorSeen = findViewById(R.id.color_seen);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mCall);
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder sv, int arg1, int arg2, int arg3) {
        mParameters = mCamera.getParameters();
        mCamera.setDisplayOrientation(90);
      //  setBestPictureResolution();

        mCamera.setParameters(mParameters);
        try {
            mCamera.setPreviewDisplay(sv);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mCamera.setParameters(mParameters);
        mCamera.startPreview();

        mCall = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                if (data != null) {
                    mImageBitmap = decodeBitmap(data);
                }
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

                    mImageView.setImageBitmap(tempBtm);
                    int color = tempBtm.getPixel(halfWidth,halfHeight);

                    String strColor = String.format("#%06X", 0xFFFFFF & color);
                    mHexTextView.setText(strColor);
                    mColorSeen.setBackgroundColor(color);
                }

            }
        };
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw the preview.
        // mCamera = Camera.open();
        mCamera = getCameraInstance();
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                mHandler.post(runnable);

            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }
        } else
            Toast.makeText(getApplicationContext(), "Camera is not available",
                    Toast.LENGTH_SHORT).show();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (mCamera != null) {
            // stop the preview
            mCamera.stopPreview();
            // release the camera
            mCamera.release();
        }
        // unbind the camera from this object
        if (mHandler != null)
            mHandler.removeCallbacks(runnable);
    }

    public static Bitmap decodeBitmap(byte[] data) {

        Bitmap bitmap = null;
        BitmapFactory.Options bfOptions = new BitmapFactory.Options();
        bfOptions.inDither = false; // Disable Dithering mode
        bfOptions.inPurgeable = true; // Tell to gc that whether it needs free
        // memory, the Bitmap can be cleared
        bfOptions.inInputShareable = true; // Which kind of reference will be
        // used to recover the Bitmap data
        // after being clear, when it will
        // be used in the future
        bfOptions.inTempStorage = new byte[32 * 1024];

        if (data != null)
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                    bfOptions);

        return bitmap;
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
//
//    private void setBestPictureResolution() {
//        // get biggest picture size
//        mWidth = mPreferences.getInt("Picture_Width", 0);
//        mHeight = mPreferences.getInt("Picture_height", 0);
//
//        if (mWidth == 0 | mHeight == 0) {
//            mPhotoSize = getBiggesttPictureSize(mParameters);
//            if (mPhotoSize != null)
//                mParameters
//                        .setPictureSize(mPhotoSize.width, mPhotoSize.height);
//            // save width and height in sharedprefrences
//            mWidth = mPhotoSize.width;
//            mHeight = mPhotoSize.height;
//            mEditor.putInt("Picture_Width", mWidth);
//            mEditor.putInt("Picture_height", mHeight);
//            mEditor.commit();
//
//        } else {
//            // if (pictureSize != null)
//            mParameters.setPictureSize(mWidth, mHeight);
//        }
//    }
//
//    private Camera.Size getBiggesttPictureSize(Camera.Parameters parameters) {
//        Camera.Size result = null;
//
//        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
//            if (result == null) {
//                result = size;
//            } else {
//                int resultArea = result.width * result.height;
//                int newArea = size.width * size.height;
//
//                if (newArea > resultArea) {
//                    result = size;
//                }
//            }
//        }
//
//        return (result);
//    }

    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
}
