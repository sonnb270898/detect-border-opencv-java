import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;

import java.util.*;

public class bound_detect {

    public static final String file ="/home/son/Downloads/VID_20210415_141541.mp4";
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

        if (contours.size() == 0){
            result.put("has_Obj", false);
            result.put("contours", null);
            return result;
        }
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
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
        HighGui.imshow("diff", diff);
        return flag;
    }

    public static void main(String args[]) {
        //Loading the OpenCV core library
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME);

//        //Instantiating the Imagecodecs class
//        Imgcodecs imageCodecs = new Imgcodecs();

        //Reading the Image from the video
        VideoCapture capture = new VideoCapture();
        capture.open(2);

        Mat s_frame = new Mat();
        Mat frame = new Mat();

        capture.read(s_frame);
        s_frame = resizeImage(s_frame, 640);
        Imgproc.cvtColor(s_frame, s_frame, Imgproc.COLOR_RGB2GRAY);

        while (true)
        {
            capture.read(frame);
            if (!capture.grab()) {
                System.exit(0);
            }

            frame = resizeImage(frame, 640);
            //convert to gray image
            Mat gray_img = new Mat();
            Imgproc.cvtColor(frame, gray_img, Imgproc.COLOR_RGB2GRAY);

            Map<String, Object> result= detectObject(gray_img, 200);
            if ((boolean)result.get("has_Obj") && !detectMotion(s_frame, gray_img, 200)) {
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            RotatedRect rotate_rect_boxes = Imgproc.minAreaRect(new MatOfPoint2f(((MatOfPoint)result.get("contours")).toArray()));

            MatOfPoint rect_boxes = new MatOfPoint();
//            rect_boxes = (MatOfPoint)result.get("contours");
            Imgproc.boxPoints(rotate_rect_boxes, rect_boxes);

            rect_boxes.convertTo(rect_boxes, CvType.CV_32S);
            rect_boxes.fromList(rect_boxes.toList().subList(0,4));
            contours.add(rect_boxes);
            Imgproc.drawContours(frame, contours, 0, new Scalar(0,255,0), 2);
        }
            s_frame = gray_img.clone();
            HighGui.imshow( "window Name", frame );
            int c = HighGui.waitKey( 1 );
            if (c == 27) {
                System.exit(0);
            }
        }
    }
}
