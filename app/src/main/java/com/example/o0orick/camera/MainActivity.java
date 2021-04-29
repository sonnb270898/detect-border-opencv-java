package com.example.o0orick.camera;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.utils.ViewAnimationHelper;
import com.serenegiant.widget.CameraViewInterface;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.boxes.preprocessing.bound_detect.detectMotion;
import static com.boxes.preprocessing.bound_detect.detectObject;
import static com.boxes.preprocessing.bound_detect.estimateSize;
import static com.boxes.preprocessing.bound_detect.orderPoint;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "MainActivity";

    private Rect rectRoi = null;
    private List<Integer> listPointInRoi = new ArrayList<>();

    private boolean touchFlag = true;

    Mat startFrame=null;
	private final Object mSync = new Object();


    private static final boolean USE_SURFACE_ENCODER = false;

    private static final int PREVIEW_WIDTH = 640; // 640

    private static final int PREVIEW_HEIGHT = 480; //480

    private static final int PREVIEW_MODE = 0; // YUV

    protected static final int SETTINGS_HIDE_DELAY_MS = 2500;

     //for accessing USB
    private USBMonitor mUSBMonitor;

     // Handler to execute camera related methods sequentially on private thread
    private UVCCameraHandler mCameraHandler;

    //for camera preview display
    private CameraViewInterface mUVCCameraView;

    //for open&start / stop&close camera preview
    private ImageButton mCameraButton;
    private ImageButton disconnectButton;
    private ImageButton selectRoiButton;
    private ImageView mImageView;
    private ImageView mCropImageView;

    private TextView objHeight;
    private TextView objWidth;
    private TextView objDepth;

    private boolean isScaling = false;
    private boolean isInCapturing = false;

    private int[][] capture_solution = {{640,480}, {800,600},{1024,768}, {1280,1024}};
    private int mCaptureWidth = capture_solution[0][0];
    private int mCaptureHeight = capture_solution[0][1];

    private int heightImgView;
    private int widthImgView;
    private static boolean cropImgFlag;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate:");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        mCameraButton = findViewById(R.id.imageButton);
        mCameraButton.setOnClickListener(mOnClickListener);

        mImageView = (ImageView)findViewById(R.id.imageView);

        mCropImageView = (ImageView) findViewById(R.id.cropImageView);

        //textView for size of object
        objWidth = (TextView) findViewById(R.id.obj_width);
        objHeight = (TextView) findViewById(R.id.obj_height);
        objDepth = (TextView) findViewById(R.id.obj_depth);

        mCaptureWidth = capture_solution[0][0];
        mCaptureHeight = capture_solution[0][1];
        bitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.RGB_565);

        final View view = findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface)view;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);


        disconnectButton = findViewById(R.id.button_call_disconnect);

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                onDestroy();
                finish();
            }
        });


        // get Region of Interest
        selectRoiButton = findViewById(R.id.selectRoi);
        selectRoiButton.setOnClickListener(new OnClickListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onClick(View v) {
            if (listPointInRoi.size() != 0){
                listPointInRoi.clear();
                rectRoi = null;
                mCropImageView.setImageBitmap(null);
            }
            if(widthImgView != 0){
                    mImageView.setOnTouchListener(new View.OnTouchListener() {
                        int startX, startY;
                        float ratioImgWidth = heightImgView*640/480, imgBoundary = (widthImgView - ratioImgWidth)/2;
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                startX = (int) ((event.getX() - imgBoundary)/ratioImgWidth*640);
                                startY = (int) (event.getY()/heightImgView * 480);
                                if(event.getX() >= imgBoundary && event.getX() <= (imgBoundary + ratioImgWidth)){
                                    listPointInRoi.add(startX);
                                    listPointInRoi.add(startY);
                                }
                            }

                            if (listPointInRoi.size() == 4){
                                rectRoi = new Rect(listPointInRoi.get(0), listPointInRoi.get(1),
                                        Math.abs(listPointInRoi.get(2) - listPointInRoi.get(0)),
                                        Math.abs(listPointInRoi.get(3) - listPointInRoi.get(1)));
                                mImageView.setOnTouchListener(null);
                                Log.w("xxxxxxx",String.format("%d,%d,%d,%d",rectRoi.x,rectRoi.y,rectRoi.width,rectRoi.height));
                                cropImgFlag = true;
                            }
                            Log.w("xxxxx", listPointInRoi.toString());
                            return false;
                        }

                    });
                }
            }
        });


        synchronized (mSync) {
	        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
	        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
	                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
		}
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart:");
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

		synchronized (mSync) {
        	mUSBMonitor.register();
		}
		if (mUVCCameraView != null) {
  			mUVCCameraView.onResume();
		}
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop:");
        synchronized (mSync) {
    		mCameraHandler.close();	// #close include #stopRecording and #stopPreview
			mUSBMonitor.unregister();
        }
		 if (mUVCCameraView != null)
			mUVCCameraView.onPause();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {
            if (mCameraHandler != null) {
                mCameraHandler.setPreviewCallback(null); //zhf
                mCameraHandler.release();
                mCameraHandler = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        super.onDestroy();
    }

    /**
     * event handler when click camera / capture button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                heightImgView = mImageView.getHeight();
                widthImgView = mImageView.getWidth();
                if ((mCameraHandler != null) && !mCameraHandler.isOpened()) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    mCameraHandler.close();
                }
            }
        }
    };

    private void startPreview() {
		synchronized (mSync) {
			if (mCameraHandler != null) {
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();

				mCameraHandler.setPreviewCallback(mIFrameCallback);
                mCameraHandler.startPreview(new Surface(st));

			}
		}
        updateItems();
    }

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");
            synchronized (mSync) {
                if (mCameraHandler != null) {
	                mCameraHandler.open(ctrlBlock);
	                startPreview();
	                updateItems();
				}
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            synchronized (mSync) {
                if (mCameraHandler != null) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                // maybe throw java.lang.IllegalStateException: already released
                                mCameraHandler.setPreviewCallback(null); //zhf
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                            mCameraHandler.close();
                        }
                    }, 0);
				}
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
		synchronized (mSync) {
			return mUSBMonitor;
		}
	}

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
    }

    //================================================================================
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }
    //================================================================================

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
            mImageView.setVisibility(visible_active);
        }
    };

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    private Bitmap bitmap = null;//Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565);
    private final Bitmap srcBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private String WarnText;

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            frame.clear();
            if(!isActive() || isInCapturing){
                return;
            }
            if(bitmap == null){
                Toast.makeText(MainActivity.this, "Bitmap", Toast.LENGTH_SHORT).show();
                return;
            }

            synchronized (bitmap) {
                srcBitmap.copyPixelsFromBuffer(frame);
                WarnText = "";

                if(bitmap.getWidth() != mCaptureWidth || bitmap.getHeight() != mCaptureHeight){
                    bitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.ARGB_8888);
                }

                bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);

                Mat currentFrame = new Mat();
                Mat crop_gray_img = new Mat();
                Utils.bitmapToMat(srcBitmap, currentFrame);


                if (startFrame == null){
                    startFrame = crop_gray_img.clone();
                }
                Map<String, Object> result;
                boolean motionFlag=false;
                if(rectRoi != null) {

                    Mat crop_currentFrame = currentFrame.submat(rectRoi);
                    Imgproc.cvtColor(crop_currentFrame, crop_gray_img, Imgproc.COLOR_RGB2GRAY);

                    if (cropImgFlag) {
                        Bitmap bmp = Bitmap.createBitmap(crop_currentFrame.cols(), crop_currentFrame.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(crop_currentFrame, bmp);
                        mCropImageView.setImageBitmap(bmp);
                        cropImgFlag = false;
                    }
                    result = detectObject(crop_gray_img, 100);
//                    motionFlag = detectMotion(crop_startFrame, crop_gray_img, 100);

                    if ((boolean) result.get("has_Obj") && !motionFlag) {

                        RotatedRect rotate_rect_boxes = Imgproc.minAreaRect(new MatOfPoint2f(((MatOfPoint) result.get("contours")).toArray()));

                        MatOfPoint rect_boxes = new MatOfPoint();
                        Imgproc.boxPoints(rotate_rect_boxes, rect_boxes);

                        rect_boxes.convertTo(rect_boxes, CvType.CV_32S);
                        // get order_point
                        List<Point> order_point = orderPoint(rect_boxes.toList().subList(0, 4), rectRoi);
                        rect_boxes.fromList(order_point);
                        //estimate size
                        Size s = estimateSize(order_point);
                        Point mid_width = new Point((order_point.get(0).x + order_point.get(1).x) / 2 - 50, (order_point.get(0).y + order_point.get(1).y) / 2);
                        Point mid_height = new Point((order_point.get(1).x + order_point.get(2).x) / 2, (order_point.get(1).y + order_point.get(2).y) / 2);

                        objWidth.setText(Double.toString(s.width*0.1));
                        objHeight.setText(Double.toString(s.width*0.1));
//                    objWidth.setText(Double.toString(s.width));
//
                        Imgproc.line(currentFrame, order_point.get(0), order_point.get(1), new Scalar(0, 255, 0), 3);
                        Imgproc.line(currentFrame, order_point.get(1), order_point.get(2), new Scalar(0, 255, 0), 3);
                        Imgproc.line(currentFrame, order_point.get(2), order_point.get(3), new Scalar(0, 255, 0), 3);
                        Imgproc.line(currentFrame, order_point.get(3), order_point.get(0), new Scalar(0, 255, 0), 3);


                        Imgproc.putText(currentFrame, String.format("width %.1f cm", s.width * 0.1), mid_width, Core.FONT_HERSHEY_SIMPLEX, 0.65, new Scalar(0, 0, 255), 2);
                        Imgproc.putText(currentFrame, String.format("height %.1f cm", s.height * 0.1), mid_height, Core.FONT_HERSHEY_SIMPLEX, 0.65, new Scalar(0, 0, 255), 2);
                    }
                }
                startFrame = crop_gray_img.clone();
                Utils.matToBitmap(currentFrame, bitmap);

            }
            mImageView.post(mUpdateImageTask);
        }
    };

    private final Runnable mUpdateImageTask = new Runnable() {
        @Override
        public void run() {
            synchronized (bitmap) {
                mImageView.setImageBitmap(bitmap);
            }
        }
    };









}