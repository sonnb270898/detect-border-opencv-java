package com.boxes.ui.camera;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.boxes.ui.R;
import com.boxes.utils.ConvertFile;
import com.boxes.uvc.bluetooth.BluetoothLeActivity;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.UiExecute;
import net.posprinter.utils.DataForSendToPrinterPos80;

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
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.UnsupportedEncodingException;
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

public final class DetectActivity extends BaseActivity implements CameraDialog.CameraDialogParent , IDataCallback{

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
    @BindView(R.id.tvSettingHeight)
    TextView tvSettingHeight;

    private static final String TAG = "tienld";
    private Rect rectRoi = null;
    private List<Integer> listPointInRoi = new ArrayList<>();
    private Mat startFrame=null;
    private static final boolean USE_SURFACE_ENCODER = false;
    private static final int PREVIEW_WIDTH = 1280; // 640 1280
    private static final int PREVIEW_HEIGHT = 720; //360 720
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
    private boolean isRequest;


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
        }else {
            ApplicationController.get().setFilters();
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
        public void onAttach(final UsbDevice device) {
            Log.e(TAG, "onAttach: ");
            mUSBMonitor.requestPermission(device);
        }

        @Override
        public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (mCameraHandler !=null){
                Log.e(TAG, "onConnect: ");
                mCameraHandler.open(ctrlBlock);
                startPreview();
            }

//            if (mCameraHandler != null) {
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    Log.e(TAG, "onConnect: ");
//                    mCameraHandler.open(ctrlBlock);
//                    startPreview();
//                    updateItems();
//                }).start();
//            }
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
            Log.d(TAG, "Device Detached");
            mCameraHandler.close();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    @Override
    public USBMonitor getUSBMonitor() {
        Log.e(TAG, "getUSBMonitor: ");
        return mUSBMonitor;
	}

    @Override
    public void onDialogResult(boolean canceled) {
        Log.e(TAG, "onDialogResult" +  canceled);
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

            bitmap = Bitmap.createBitmap(srcBitmap);

            Mat frame1 = new Mat();
            Utils.bitmapToMat(srcBitmap, frame1);
            int KERNEL_SIZE1 = 8;
            int KERNEL_SIZE2 = 5;
            int KERNEL_SIZE3 = 3;

//            Bitmap bgImg_bmp = BitmapFactory.decodeResource(getResources(), R.drawable.i093);
//            Mat bgImg_mat = new Mat();
//            Mat bgImg_mat_gray = new Mat();
//            Utils.bitmapToMat(bgImg_bmp, bgImg_mat);
//            bgImg_mat = ConvertFile.resizeImage(bgImg_mat, PREVIEW_WIDTH, PREVIEW_HEIGHT);
//            Imgproc.cvtColor(bgImg_mat, bgImg_mat_gray, Imgproc.COLOR_RGBA2GRAY);
//            //blur image
//            Mat bgImg_blur = new Mat();
//            Imgproc.GaussianBlur(bgImg_mat_gray, bgImg_blur, new Size(7,7),0);

            BackgroundSubtractor backSub;
            backSub = Video.createBackgroundSubtractorMOG2();
            Mat fgMask = new Mat();


            while (true) {
                // capture.read(frame);
                Mat frame2 = new Mat();
                Size scaleSize = new Size(PREVIEW_WIDTH,PREVIEW_HEIGHT);
                Imgproc.resize(frame1, frame2, scaleSize, 0, 0, Imgproc.INTER_CUBIC);
//                backSub.apply(frame2 , fgMask);
//                Mat fgMask1 = new Mat();
//                Imgproc.resize(fgMask, fgMask1, scaleSize, 0, 0, Imgproc.INTER_CUBIC);
//                Imgproc.threshold(fgMask1, fgMask1,127, 255,
//                        Imgproc.THRESH_BINARY);

//                Bitmap bitmap_tmp2 = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(fgMask, bitmap_tmp2);
//                Log.e("tienld", "main: " + bitmap_tmp2);

                Mat img_contour = frame2.clone();
                // convert to gray image
                Mat gray_img = new Mat();
                Mat blur_img = new Mat();
                Imgproc.GaussianBlur(frame2, blur_img, new Size(7,7),1);
                Imgproc.cvtColor(blur_img, gray_img, Imgproc.COLOR_BGR2GRAY);

                //Imgproc.medianBlur(gray_img, blur_img, 7);
                //Core.absdiff(bgImg_mat_gray, blur_img, diffImg);
                Mat cannyImg = new Mat();
//                Imgproc.threshold(blur_img, thresh,75, 255,
//                        Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
                int threshold1 = 25;
                int threshold2 = 255;
                Imgproc.Canny(gray_img, cannyImg, threshold1, threshold2);
//                Imgproc.adaptiveThreshold(blur_img, thresh, 75, Imgproc.ADAPTIVE_THRESH_MEAN_C,
//                        Imgproc.THRESH_BINARY, 11, 12);
                Mat dilated = new Mat();
                Mat eroded = new Mat();
                Mat opening = new Mat();
                Mat closing = new Mat();

                Mat element1 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,
                        new Size(2 * KERNEL_SIZE1 + 1, 2 * KERNEL_SIZE1 + 1),
                        new Point(KERNEL_SIZE1, KERNEL_SIZE1));
                Mat element2 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,
                        new Size(2 * KERNEL_SIZE2 + 1, 2 * KERNEL_SIZE2 + 1),
                        new Point(KERNEL_SIZE2, KERNEL_SIZE2));

                Mat element3 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,
                        new Size(2 * KERNEL_SIZE3 + 1, 2 * KERNEL_SIZE3 + 1),
                        new Point(KERNEL_SIZE3, KERNEL_SIZE3));

                Imgproc.dilate(cannyImg, dilated, element1);
                Imgproc.erode(dilated, eroded, element2);

                Imgproc.morphologyEx(eroded, opening,  Imgproc.MORPH_OPEN, element3);
                Imgproc.morphologyEx(opening, closing,  Imgproc.MORPH_CLOSE, element3);

//                Bitmap bitmap_tmp2 = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(cannyImg, bitmap_tmp2);
//                Log.e("tienld", "main: " + bitmap_tmp2);

                ConvertFile.get_contour(closing, img_contour);
                Utils.matToBitmap(img_contour, bitmap);
                Log.e("tienld", "main: " + bitmap);
                break;
            }


            //bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);

            //Mat currentFrame = new Mat();
            //Mat crop_gray_img = new Mat();
            //Utils.bitmapToMat(srcBitmap, currentFrame);


//            if (startFrame == null){
//                startFrame = crop_gray_img.clone();
//            }
//            Map<String, Object> result;
//            boolean motionFlag=false;
//            if(rectRoi != null) {
//
//                Mat crop_currentFrame = currentFrame.submat(rectRoi);
//                Imgproc.cvtColor(crop_currentFrame, crop_gray_img, Imgproc.COLOR_RGB2GRAY);
//
//                if (cropImgFlag) {
//                    Bitmap bmp = Bitmap.createBitmap(crop_currentFrame.cols(), crop_currentFrame.rows(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(crop_currentFrame, bmp);
//                    mCropImageView.setImageBitmap(bmp);
//                    cropImgFlag = false;
//                }
//                result = detectObject(crop_gray_img, 100);
//
//                if ((boolean) result.get("has_Obj") && !motionFlag) {
//
//                    RotatedRect rotate_rect_boxes = Imgproc.minAreaRect(new MatOfPoint2f(((MatOfPoint) result.get("contours")).toArray()));
//
//                    MatOfPoint rect_boxes = new MatOfPoint();
//                    Imgproc.boxPoints(rotate_rect_boxes, rect_boxes);
//
//                    rect_boxes.convertTo(rect_boxes, CvType.CV_32S);
//                    // get order_point
//                    List<Point> order_point = orderPoint(rect_boxes.toList().subList(0, 4), rectRoi);
//                    rect_boxes.fromList(order_point);
//                    //estimate size
//                    Size s = estimateSize(order_point);
//                    Point mid_width = new Point((order_point.get(0).x + order_point.get(1).x) / 2 - 50, (order_point.get(0).y + order_point.get(1).y) / 2);
//                    Point mid_height = new Point((order_point.get(1).x + order_point.get(2).x) / 2, (order_point.get(1).y + order_point.get(2).y) / 2);
//
//                    objWidth.setText(Double.toString(Math.floor(s.width*0.1)));
//                    objHeight.setText(Double.toString(Math.floor(s.height*0.1)));
//
//                    Imgproc.line(currentFrame, order_point.get(0), order_point.get(1), new Scalar(0, 255, 0), 3);
//                    Imgproc.line(currentFrame, order_point.get(1), order_point.get(2), new Scalar(0, 255, 0), 3);
//                    Imgproc.line(currentFrame, order_point.get(2), order_point.get(3), new Scalar(0, 255, 0), 3);
//                    Imgproc.line(currentFrame, order_point.get(3), order_point.get(0), new Scalar(0, 255, 0), 3);
//
//
//                    Imgproc.putText(currentFrame, String.format("width %.1f cm", s.width * 0.1), mid_width, Core.FONT_HERSHEY_SIMPLEX, 0.65, new Scalar(0, 0, 255), 2);
//                    Imgproc.putText(currentFrame, String.format("height %.1f cm", s.height * 0.1), mid_height, Core.FONT_HERSHEY_SIMPLEX, 0.65, new Scalar(0, 0, 255), 2);
//                }
//            }
//            startFrame = crop_gray_img.clone();
            mImageView.post(mUpdateImageTask);
        }
    };

    private final Runnable mUpdateImageTask = new Runnable() {
        @Override
        public void run() {
            mImageView.setImageBitmap(bitmap);
        }
    };

    @Override
    public void receiveData(String data) {
        Log.e("tienld", "receiveData: " + data );
        objDepth.setText(data);
    }

//    @Override
//    public void getData(String var1, String var2, String var3, String var4) {
//        tvWeight.setText(String.format("%s%s", var1, var4));
//    }
//
//    @Override
//    public void getConnectionState(String var1, boolean var2) {
//
//    }
//
//    @Override
//    public void getConnectionInfo(String var1, String var2) {
//
//    }
//
//    @Override
//    public void getBluetoothLeService(BluetoothLeService var1) {
//
//    }

    @OnClick({R.id.obj_height,R.id.button_call_disconnect,R.id.selectRoi,R.id.imageButton,R.id.tvSettingCam,R.id.tvSettingHeight})
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_call_disconnect:
                finish();
                break;
            case R.id.selectRoi:
                heightImgView = mImageView.getHeight();
                widthImgView = mImageView.getWidth();
                if (listPointInRoi.size() != 0) {
                    listPointInRoi.clear();
                    rectRoi = null;
                    mCropImageView.setImageBitmap(null);
                }
                break;
            case R.id.imageButton:
                if ((mCameraHandler != null) && !mCameraHandler.isOpened()) {
                    CameraDialog.showDialog(DetectActivity.this);
                    //mUSBMonitor.requestPermission(mUSBMonitor.getDevices().next());
                } else {
                    if (mCameraHandler != null) {
                        mCameraHandler.close();
                    }
                }
                break;
            case R.id.tvSettingCam:
                ApplicationController.get().usbService.write("0\r".getBytes());
                break;
            case R.id.tvSettingHeight:
                if (ApplicationController.get().connectedXprinter){
                    Log.e(TAG, "connectedXprinter: ");
                    ApplicationController.get().binder.writeDataByYouself(new UiExecute() {
                        @Override
                        public void onsucess() {
                            Log.e(TAG, "onsucess: ");
                        }

                        @Override
                        public void onfailed() {
                            Log.e(TAG, "onfailed: ");
                        }
                    }, () -> {
                        List<byte[]> list = new ArrayList<>();
                        String str = "Welcome to use the impact and thermal printer manufactured by professional POS receipt printer company!asasdasd asd asd asdasd asd asdasd asd asdasdasdas dasdadasdasd asdasdasd asdasd asd asd asd asd asd asd a ds";
                        byte[] data1 = strTobytes(str);
                        list.add(data1);
                        list.add(DataForSendToPrinterPos80.printQRcode(4,4,"asdhashdahsd asdahsdhasd"));
                        list.add(DataForSendToPrinterPos80.printByPagemodel());
                        return list;
                    });
                }
                break;

        }
    }

    public static byte[] strTobytes(String str){
        byte[] b=null,data=null;
        try {
            b = str.getBytes("utf-8");
            data=new String(b,"utf-8").getBytes("gbk");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return data;
    }
}
