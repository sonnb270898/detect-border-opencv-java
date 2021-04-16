import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

public class bound_detect extends JPanel {

    public static final String file ="/home/son/Downloads/VID_20210415_141541.mp4";
    BufferedImage image;
    public static Mat resize_img(Mat img, float ratio){
        Mat img_resized = new Mat();
        int width = img.width();
        int height = img.height();
        Imgproc.resize(img, img_resized, new Size((int)(width*ratio), (int)(height*ratio)));
        return img_resized;
    }

//    public static BufferedImage MatToBufferedImage(Mat frame) {
//        //Mat() to BufferedImage
//        int type = 0;
//        if (frame.channels() == 1) {
//            type = BufferedImage.TYPE_BYTE_GRAY;
//        } else if (frame.channels() == 3) {
//            type = BufferedImage.TYPE_3BYTE_BGR;
//        }
//        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
//        WritableRaster raster = image.getRaster();
//        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
//        byte[] data = dataBuffer.getData();
//        frame.get(0, 0, data);
//
//        return image;
//    }

    public static void main(String args[]) {
        //Loading the OpenCV core library
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME);

        //Instantiating the Imagecodecs class
        Imgcodecs imageCodecs = new Imgcodecs();
//
//        //Reading the Image from the video
        VideoCapture capture = new VideoCapture();
        capture.open(file);

        // get width, height
        Mat s_frame = new Mat();
        capture.read(s_frame);
        s_frame = resize_img(s_frame, 0.5f);
        int width = s_frame.width();
        int height = s_frame.height();


        //Instantiate JFrame
        JFrame jframe = new JFrame("Title");
        jframe.setSize(width, height);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel vidpanel = new JLabel();
        jframe.setContentPane(vidpanel);
        jframe.setVisible(true);

        Mat frame2 = new Mat();
        while (capture.isOpened())
        {
            capture.read(frame2);
            if (!capture.grab()) {
                jframe.dispose();
                break;
            }
            frame2 = resize_img(frame2, 0.5f);
            //Encoding the image
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame2, matOfByte);

            //Storing the encoded Mat in a byte array
            byte[] byteArray = matOfByte.toArray();

            //Preparing the Buffered Image
            InputStream in = new ByteArrayInputStream(byteArray);
            BufferedImage bufImage = null;
            try {
                bufImage = ImageIO.read(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ImageIcon image = new ImageIcon(bufImage);
            vidpanel.setIcon(image);
            vidpanel.repaint();
        }

    }
}
