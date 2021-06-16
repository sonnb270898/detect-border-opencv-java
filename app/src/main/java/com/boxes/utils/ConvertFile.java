package com.boxes.utils;
import android.graphics.Bitmap;
import android.util.Log;

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
        // draw contour
        Imgproc.drawContours(img_cotour, contours, -1, new Scalar(0,255,0), 2);
        Imgproc.circle(img_cotour, new Point(320, 240), 10, new Scalar(0, 0, 255), -1);

        double a = (7.25 * 10637.37974683544 - Matrix[0][2]) / Matrix[0][0];
        double b = (7.25 * 10637.37974683544 - Matrix[1][2]) / Matrix[1][1];
        Imgproc.circle(img_cotour, new Point(320 - (int) a, 240 - (int) b), 10, new Scalar(0, 0, 255), -1);
        Imgproc.circle(img_cotour, new Point(320 + (int) a, 240 + (int) b), 10, new Scalar(0, 0, 255), -1);
        Imgproc.circle(img_cotour, new Point(320 - (int) a, 240 + (int) b), 10, new Scalar(0, 0, 255), -1);
        Imgproc.circle(img_cotour, new Point(320 + (int) a, 240 - (int) b), 10, new Scalar(0, 0, 255), -1);

        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
//            double minArea = gettrackbar;
            double minArea = 500;
            if (contourArea < minArea) continue;

//            double peri = Imgproc.arcLength(new MatOfPoint2f(contours.get(contourIdx)), true);
//            MatOfPoint2f approxContour2f = new MatOfPoint2f();
//            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(contourIdx)), approxContour2f,0.02 * peri, true);
//            Rect rect = Imgproc.boundingRect(contours.get(contourIdx));

            RotatedRect rotate_rect_boxes = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(contourIdx).toArray()));

            MatOfPoint rect_boxes = new MatOfPoint();
//            rect_boxes = (MatOfPoint)result.get("contours");
            Imgproc.boxPoints(rotate_rect_boxes, rect_boxes);

            rect_boxes.convertTo(rect_boxes, CvType.CV_32S);
            // get order_point
            List<Point> order_point = orderPoint(rect_boxes.toList().subList(0,4));
            rect_boxes.fromList(order_point);

            Size s = estimateSize(order_point);

            Point tltr = mid_point(order_point.get(0), order_point.get(1));
            Point blbr = mid_point(order_point.get(3), order_point.get(2));
            Point tlbl = mid_point(order_point.get(0), order_point.get(3));
            Point trbr = mid_point(order_point.get(1), order_point.get(2));

            double dr_W = s.width / 10637.37974683544;
            double dr_H = s.height / 12068.5;

            Imgproc.putText(img_cotour, String.format("%.1f cm",dr_H), new Point(tltr.x, tltr.y - 20), Core.FONT_HERSHEY_SIMPLEX, .7,
                    new Scalar (0, 255, 0), 2);
            Imgproc.putText(img_cotour, String.format("%.1f cm",dr_W), new Point(tlbl.x - 20, tlbl.y), Core.FONT_HERSHEY_SIMPLEX, .7,
                    new Scalar (0, 255, 0), 2);
        }
    }


    public static void main(Bitmap bitmap,Bitmap bitmap1) {

        //Reading the Image from the video
//        VideoCapture capture = new VideoCapture();
//        capture.open(0); cmamab abasd

        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap, frame);
        Mat KERNEL1 = Mat.ones(8,8,CvType.CV_8U);
        Mat KERNEL2 = Mat.ones(5,5,CvType.CV_8U);
        while (true)
        {
            //capture.read(frame);

            Mat img_contour = frame.clone();

            frame = resizeImage(frame, 640, 480);
            //blur image
            Mat blur_img = new Mat();
            Imgproc.GaussianBlur(frame, blur_img, new Size(7,7),0);
            //convert to gray image
            Mat gray_img = new Mat();
            Imgproc.cvtColor(blur_img, gray_img, Imgproc.COLOR_RGBA2GRAY);
            //get threshold
            int threshold1 = 120;
            int threshold2 = 255;
            //get Canny
            Mat canny_img = new Mat();
            Imgproc.Canny(gray_img, canny_img, threshold1, threshold2);

            Mat process_img = new Mat();
            Imgproc.dilate(canny_img, process_img, KERNEL1);
            Imgproc.erode(process_img, process_img, KERNEL2);
            get_contour(canny_img, img_contour);
            Bitmap bitmap2 = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_contour, bitmap2);
//            imgStack = stackImages(0.8, ([img, imgCanny, imgGray],
//                                     [imgDil, imgContour, imgErode]))
//            Utils.matToBitmap(img_contour, bitmap1);
//            Log.e("tienld", "main: " + bitmap );
        }
    }

}
