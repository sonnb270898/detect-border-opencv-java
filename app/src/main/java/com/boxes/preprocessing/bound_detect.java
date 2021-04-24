package com.boxes.preprocessing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.opencv.highgui.HighGui;

public class bound_detect {

//    public static final String file ="/home/son/Downloads/VID_20210415_141541.mp4";
    public static Mat resizeImage(Mat img, int width){
        Mat img_resized = new Mat();
        int img_width = img.width();
        int img_height = img.height();
        Imgproc.resize(img, img_resized, new Size(width, (int)(img_height*width/img_width)));
        return img_resized;
    }

    public static Map<String, Object> detectObject(Mat gray_img, double min_area) {

        Map<String, Object> result = new HashMap<String, Object>();
        //smooth image
        Imgproc.GaussianBlur(gray_img, gray_img, new Size(5,5), 0);
        //binary image
        Imgproc.threshold(gray_img, gray_img, 127, 255, Imgproc.THRESH_BINARY);
//        Imgproc.dilate(gray_img, gray_img, kernel, new Point(-1,-1), 4);
        //detect canny
        Mat canny_img = new Mat();
        Imgproc.Canny(gray_img, canny_img, 50, 255);
        //find contour
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(canny_img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        double maxVal = -1;
        int maxValIdx = -1;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (contourArea > min_area && maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        if (maxValIdx == -1){
            result.put("has_Obj", false);
            result.put("contours", null);
            return result;
        }
        result.put("has_Obj", true);
        result.put("contours", contours.get(maxValIdx));
        return result;
    }
    public static boolean detectMotion(Mat preFrame, Mat curFrame, float minArea){
        boolean flag = false;
        Mat diff = new Mat();
//        Mat kernel = Mat.ones(5, 5, CvType.CV_8U);
        Core.absdiff(preFrame, curFrame, diff);
        Imgproc.threshold(diff, diff, 25, 255, Imgproc.THRESH_BINARY);
//        Imgproc.dilate(diff, diff, kernel, new Point(-1,-1), 2);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(diff, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint c: contours) {
            if (Imgproc.contourArea(c) < minArea)
                    continue;
            flag = true;
        }
//        HighGui.imshow("diff", diff);
        return flag;
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
            if (box.get(1).y > box.get(2).y) Collections.swap(box,1,2);
        }else{
            Collections.swap(box,1,2);
            Collections.swap(box,2,3);
        }
        return box;
    }

    public static Size estimateSize(List<Point> box) {
        double width = Math.sqrt(Math.abs(Math.pow(box.get(0).x,2) - Math.pow(box.get(1).x,2)));
        double height = Math.sqrt(Math.abs(Math.pow(box.get(0).y,2) - Math.pow(box.get(3).x,2)));
        return new Size(width,height);
    }

    public static void main(String args[]) {
        //Loading the OpenCV core library
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME);
    }
}
