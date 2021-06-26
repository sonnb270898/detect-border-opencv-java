package com.boxes.utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.boxes.ui.R;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.*;

public class ConvertFile {
    private static double[][] Matrix = {
            {831.17633057, 0., 337.5638728},
            {0., 823.71417236, 252.90510332},
            {0., 0., 1.}
    };

    private final static int[][] capture_solution = {{640,480}, {800,600},{1024,768}, {1280,1024}};
    private static int mCaptureWidth = capture_solution[0][0];
    private static int mCaptureHeight = capture_solution[0][1];

    public static Point mid_point(Point ptA, Point ptB){
        return new Point((ptA.x + ptB.x) / 2.0, (ptA.y + ptB.y) / 2.0);
    }

    public static Size estimateSize(List<Point> box) {
        double width = Math.sqrt(Math.abs(Math.pow(box.get(0).x,2) - Math.pow(box.get(1).x,2)));
        double height = Math.sqrt(Math.abs(Math.pow(box.get(0).y,2) - Math.pow(box.get(3).x,2)));
        return new Size(width,height);
    }

    public static Mat resizeImage(Mat img, int width, int height){
        Mat img_resized = new Mat();
        if (width!=-1 && height!=-1){
            Imgproc.resize(img, img_resized, new Size(width, height));
            return img_resized;
        }
        int img_width = img.width();
        int img_height = img.height();
        Imgproc.resize(img, img_resized, new Size(width, (int)(img_height*width/img_width)));
        return img_resized;
    }

    public static List<Point> orderPoint(List<Point> box) {
        Collections.sort(box, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                return Double.compare(p1.x, p2.x);
            }
        });
        if (box.get(0).y > box.get(1).y){
            Collections.swap(box,0,1);
            Collections.swap(box,1,3);
        }else{
            Collections.swap(box,1,2);
            Collections.swap(box,2,3);
        }
        return box;
    }

    public static void get_contour(Mat img, Mat img_cotour){
        //find contour
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //for (MatOfPoint contour: contours)
//            Imgproc.fillPoly(dest, Arrays.asList(contour), new Scalar(255, 255, 255));

//        Scalar green = new Scalar(81, 190, 0);
//        for (MatOfPoint contour: contours) {
//            RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
//            drawRotatedRect(dest, rotatedRect, green, 4);
//        }
//
//        public static void drawRotatedRect(Mat image, RotatedRect rotatedRect, Scalar color, int thickness) {
//            Point[] vertices = new Point[4];
//            rotatedRect.points(vertices);
//            MatOfPoint points = new MatOfPoint(vertices);
//            Imgproc.drawContours(image, Arrays.asList(points), -1, color, thickness);
//        }

        double maxVal = -1;
        int maxValIdx = -1;
        double minArea = 2000;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            //Imgproc.drawContours(img_cotour, contours, contourIdx, new Scalar(0,0,255), 1);
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));

            if (contourArea < minArea) continue;

            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }

            Imgproc.drawContours(img_cotour, contours, maxValIdx, new Scalar(0,0,255), 1);

            Bitmap bitmap_tmp2 = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_cotour, bitmap_tmp2);
            Log.e("tienld", "main: " + bitmap_tmp2);

            MatOfPoint2f approxContour2f = new MatOfPoint2f();
            contours.get(contourIdx).convertTo(approxContour2f, CvType.CV_32FC2);
            double peri = Imgproc.arcLength(approxContour2f, true);
            Imgproc.approxPolyDP(approxContour2f, approxContour2f,0.02 * peri, true);
            MatOfPoint matOfPoint = new MatOfPoint();
            approxContour2f.convertTo(matOfPoint, CvType.CV_8UC2);
            Rect rect = Imgproc.boundingRect(matOfPoint);

            //Imgproc.rectangle(img_cotour, new Point(rect.x, rect.y), new Point(rect.x+rect.width,rect.y+rect.height), new Scalar(255,0,0),2);
            RotatedRect rotate_rect_boxes = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(contourIdx).toArray()));
            MatOfPoint rect_boxes = new MatOfPoint();
//            rect_boxes = (MatOfPoint)result.get("contours");
            Imgproc.boxPoints(rotate_rect_boxes, rect_boxes);
            rect_boxes.convertTo(rect_boxes, CvType.CV_32S);
            // get order_point
            List<Point> order_point = orderPoint(rect_boxes.toList().subList(0,4));
            rect_boxes.fromList(order_point);

            Point tltr = mid_point(order_point.get(0), order_point.get(1));
            Point blbr = mid_point(order_point.get(3), order_point.get(2));
            Point tlbl = mid_point(order_point.get(0), order_point.get(3));
            Point trbr = mid_point(order_point.get(1), order_point.get(2));

            List<Point> tmp = new ArrayList<>();
            tmp.add(tltr);
            tmp.add(trbr);
            tmp.add(blbr);
            tmp.add(tlbl);

            Size s = estimateSize(tmp);

            double dr_W = s.width ;
            double dr_H = s.height ;

            Imgproc.putText(img_cotour, String.format("%.1f cm",dr_H), new Point(tltr.x, tltr.y - 20), Imgproc.FONT_HERSHEY_SIMPLEX, .7,
                    new Scalar (0, 255, 0), 2);
            Imgproc.putText(img_cotour, String.format("%.1f cm",dr_W), new Point(tlbl.x - 20, tlbl.y), Imgproc.FONT_HERSHEY_SIMPLEX, .7,
                    new Scalar (0, 255, 0), 2);
        }
    }

    public static Bitmap main(Context context, Bitmap bitmap) {
        //Reading the Image from the video
//        VideoCapture capture = new VideoCapture();
//        capture.open(0);
        Bitmap bitmap2 = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap, frame);
        Mat KERNEL1 = Mat.ones(8,8,CvType.CV_8U);
        Mat KERNEL2 = Mat.ones(5,5,CvType.CV_8U);
        Mat KERNEL3 = Mat.ones(3,3,CvType.CV_8U);

        Bitmap bgImg_bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.i093);
        Mat bgImg_mat = new Mat();
        Mat bgImg_mat_gray = new Mat();
        Utils.bitmapToMat(bgImg_bmp, bgImg_mat);
        bgImg_mat = resizeImage(bgImg_mat, 1280, 720);
        Imgproc.cvtColor(bgImg_mat, bgImg_mat_gray, Imgproc.COLOR_RGBA2GRAY);
        //blur image
        Mat bgImg_blur = new Mat();
        Imgproc.GaussianBlur(bgImg_mat_gray, bgImg_blur, new Size(7,7),0);
//        BackgroundSubtractorMOG2 backSub;
//        backSub = Video.createBackgroundSubtractorMOG2();
//        Mat fgMask = new Mat();

        while (true) {
            // capture.read(frame);
            frame = resizeImage(frame, 1280, 720);
            Mat img_contour = frame.clone();
            // convert to gray image
            Mat gray_img = new Mat();
            Imgproc.cvtColor(frame, gray_img, Imgproc.COLOR_RGBA2GRAY);
            Mat blur_img = new Mat();
            Mat diffImg = new Mat();
            Imgproc.GaussianBlur(gray_img, blur_img, new Size(7,7),0);
            // backSub.apply(blur_img , fgMask,0.0);
            Core.absdiff(bgImg_mat_gray, blur_img, diffImg);
            Mat thresh = new Mat();
            Imgproc.threshold(diffImg, thresh,0, 255,
                    Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);

//            Bitmap bitmap_tmp2 = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(thresh, bitmap_tmp2);
//            Log.e("tienld", "main: " + bitmap_tmp2);

            Imgproc.erode(thresh, thresh, KERNEL2);
            Mat se = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(7, 7));
            Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, se);

            int threshold1 = 85;
            int threshold2 = 255;
            //get Canny
            Mat canny_img = new Mat();
            Imgproc.Canny(thresh, canny_img, threshold1, threshold2);
            Imgproc.dilate(canny_img, canny_img, KERNEL2);
            Imgproc.morphologyEx(canny_img, canny_img, Imgproc.MORPH_CLOSE, se);
            Imgproc.morphologyEx(canny_img, canny_img, Imgproc.MORPH_CLOSE, se);
            Imgproc.morphologyEx(canny_img, canny_img, Imgproc.MORPH_CLOSE, se);

            Bitmap bitmap_tmp2 = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(canny_img, bitmap_tmp2);
            Log.e("tienld", "main: " + bitmap_tmp2);
            get_contour(canny_img, img_contour);
            Utils.matToBitmap(img_contour, bitmap2);
            Log.e("tienld", "main: " + bitmap2);
            break;
        }
        return bitmap2;
    }

}
