package com.boxes.ui.camera;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.boxes.ApplicationController;
import com.boxes.callback.IDataCallback;
import com.boxes.service.BluetoothLeService;
import com.boxes.service.MyHandler;
import com.boxes.uvc.bluetooth.BluetoothLeActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.boxes.utils.BoundDetection.detectObject;
import static com.boxes.utils.BoundDetection.estimateSize;
import static com.boxes.utils.BoundDetection.orderPoint;

public final class DetectActivity extends BluetoothLeActivity implements CameraDialog.CameraDialogParent , IDataCallback{

    @BindView(R.id.imageButton)
    ImageButton mCameraButton;
    @BindView(R.id.button_call_disconnect)
    ImageButton disconnectButton;
    @BindView(R.id.selectRoi)
    ImageButton selectRoiButton;
    @BindView(R.id.imageView)
    ImageView mImageView;
    @BindView(R.id.cropImageView)
    ImageView mCropImageView;
    @BindView(R.id.obj_height)
    TextView objHeight;
    @BindView(R.id.obj_width)
    TextView objWidth;
    @BindView(R.id.obj_depth)
    TextView objDepth;
    @BindView(R.id.camera_view)
    View view;
    @BindView(R.id.tvWeight)
    TextView tvWeight;
    @BindView(R.id.tvSettingCam)
    TextView tvSettingCam;

    private static final String TAG = "tienld";
    private Rect rectRoi = null;
    private List<Integer> listPointInRoi = new ArrayList<>();
    private Mat startFrame=null;
    private static final boolean USE_SURFACE_ENCODER = false;
    private static final int PREVIEW_WIDTH = 1280; // 640
    private static final int PREVIEW_HEIGHT = 720; //480
    private static final int PREVIEW_MODE = 0; // YUV
    private USBMonitor mUSBMonitor;
    private UVCCameraHandler mCameraHandler;
    private CameraViewInterface mUVCCameraView;
    private final boolean isInCapturing = false;
    private final int[][] capture_solution = {{640,480}, {800,600},{1024,768}, {1280,1024}};
    private int mCaptureWidth = capture_solution[0][0];
    private int mCaptureHeight = capture_solution[0][1];
    private int heightImgView;
    private int widthImgView;
    private static boolean cropImgFlag;
    private int startX, startY;
    private Bitmap bitmap = null;
    private final Bitmap srcBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private MyHandler mHandler;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
        connectUSB();
        setUpCameraDetect();
    }

    private void setUpCameraDetect(){
        mCaptureWidth = capture_solution[0][0];
        mCaptureHeight = capture_solution[0][1];
        bitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.RGB_565);
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView, USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
    }

    private void connectUSB(){
        mHandler = new MyHandler(this,this);
        if (ApplicationController.get().connected){
            ApplicationController.get().usbService.setHandler(mHandler);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initView(){
        mUVCCameraView = (CameraViewInterface)view;
        mImageView.setOnTouchListener((v, event) -> {
            if(widthImgView != 0){
                float ratioImgWidth = heightImgView * PREVIEW_WIDTH/PREVIEW_HEIGHT;
                float imgBoundary = (widthImgView - ratioImgWidth)/2;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = (int) ((event.getX() - imgBoundary)/ratioImgWidth*PREVIEW_WIDTH);
                    startY = (int) (event.getY()/heightImgView * PREVIEW_HEIGHT);
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
            }
            return false;
        });

    }



    @Override
    protected void onStart() {
        super.onStart();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
        }
        mUSBMonitor.register();
		if (mUVCCameraView != null) {
  			mUVCCameraView.onResume();
		}
    }


    @Override
    protected void onStop() {
        mCameraHandler.close();
        mUSBMonitor.unregister();
        if (mUVCCameraView != null) mUVCCameraView.onPause();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mCameraHandler != null) {
            mCameraHandler.setPreviewCallback(null);
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        super.onDestroy();
    }

    private void startPreview() {
        if (mCameraHandler != null) {
            final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
            mCameraHandler.setPreviewCallback(mIFrameCallback);
            mCameraHandler.startPreview(new Surface(st));

        }
        updateItems();
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) { }

        @Override
        public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (mCameraHandler != null) {
                mCameraHandler.open(ctrlBlock);
                startPreview();
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (mCameraHandler != null) {
                queueEvent(() -> {
                    try{
                        mCameraHandler.setPreviewCallback(null);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    mCameraHandler.close();
                }, 0);
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(DetectActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
	}

    @Override
    public void onDialogResult(boolean canceled) {
        Log.v(TAG, "onDialogResult:canceled=" + canceled);
    }

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


    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            frame.clear();
            if(!isActive() || isInCapturing){
                return;
            }
            if(bitmap == null){
                Toast.makeText(DetectActivity.this, "Bitmap", Toast.LENGTH_SHORT).show();
                return;
            }

            srcBitmap.copyPixelsFromBuffer(frame);

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

                    objWidth.setText(Double.toString(Math.floor(s.width*0.1)));
                    objHeight.setText(Double.toString(Math.floor(s.height*0.1)));

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

    @Override
    public void receiveData(String data) {
        Log.e("tienld", "receiveData: " + data );
        objDepth.setText(data);
    }

    @Override
    public void getData(String var1, String var2, String var3, String var4) {
        tvWeight.setText(String.format("%s%s", var1, var4));
    }

    @Override
    public void getConnectionState(String var1, boolean var2) {

    }

    @Override
    public void getConnectionInfo(String var1, String var2) {

    }

    @Override
    public void getBluetoothLeService(BluetoothLeService var1) {

    }

    @OnClick({R.id.obj_height,R.id.button_call_disconnect,R.id.selectRoi,R.id.imageButton,R.id.tvSettingCam})
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_call_disconnect:
                finish();
                break;
            case R.id.selectRoi:
                if (listPointInRoi.size() != 0) {
                    listPointInRoi.clear();
                    rectRoi = null;
                    mCropImageView.setImageBitmap(null);
                }
                break;
            case R.id.imageButton:
                heightImgView = mImageView.getHeight();
                widthImgView = mImageView.getWidth();
                if ((mCameraHandler != null) && !mCameraHandler.isOpened()) {
                    CameraDialog.showDialog(DetectActivity.this);
                } else {
                    mCameraHandler.close();
                }
                break;
            case R.id.tvSettingCam:
                ApplicationController.get().usbService.write("0\r".getBytes());
                break;

        }
    }
}
