package edu.wpi.grip.smartdashboard;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class GRIPExtension extends StaticWidget {

    public final static String NAME = "GRIP Output Viewer";

    private final static Logger logger = Logger.getLogger(GRIPExtension.class.getName());

    static {
        Logger.getGlobal().addHandler(new StreamHandler(new FileOutputStream(FileDescriptor.err), new SimpleFormatter()));
    }

    private final static int PORT = 1180;
    private final static byte[] MAGIC_NUMBERS = {0x01, 0x00, 0x00, 0x00};
    private final static int HW_COMPRESSION = -1;
    private final static int SIZE_640x480 = 0;

    public final IntegerProperty fpsProperty = new IntegerProperty(this, "FPS", 30);
    public final StringProperty addressProperty = new StringProperty(this, "GRIP Address", "localhost");

    private final GRIPImage gripImage = new GRIPImage();
    private final GRIPReportList gripReportList = new GRIPReportList();

    private boolean shutdownThread = false;
    private final Thread thread = new Thread(() -> {
        byte[] magic = new byte[4];
        byte[] imageBuffer = new byte[64 * 1024];

        // Loop until the widget is removed or SmartDashboard is closed.  The outer loop only completes an
        // iteration when the thread is interrupted or an exception happens.
        while (!shutdownThread) {
            try {
                try (Socket socket = new Socket(addressProperty.getValue(), PORT);
                     DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                    logger.info("Established connection to " + socket.getInetAddress());

                    // In the FRC dashboard protocol, the client (us) starts out by sending three 32-bit integers
                    // (FPS, compression level, and a size enum).  FPS is the only one actually recognized by
                    // GRIP.
                    outputStream.writeInt(fpsProperty.getValue());
                    outputStream.writeInt(HW_COMPRESSION);
                    outputStream.writeInt(SIZE_640x480);

                    while (!Thread.currentThread().isInterrupted()) {
                        // Each frame in the FRC dashboard image protocol starts with a 4 magic numbers.  If we
                        // don't get those four numbers, something's wrong.
                        inputStream.readFully(magic);
                        if (!Arrays.equals(magic, MAGIC_NUMBERS)) {
                            throw new IOException("Invalid stream (wrong magic numbers: " + Arrays.toString(magic) + ")");
                        }

                        // Next, the server sends a 32-bit number indicating the number of bytes in this frame,
                        // then the raw bytes.
                        int imageSize = inputStream.readInt();
                        imageBuffer = growIfNecessary(imageBuffer, imageSize);
                        inputStream.readFully(imageBuffer, 0, imageSize);

                        // Decode the image and redraw
                        gripImage.setImage(ImageIO.read(new ByteArrayInputStream(imageBuffer, 0, imageSize)));
                    }
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                    gripImage.setError(e.getMessage());
                } finally {
                    Thread.sleep(1000); // Wait a second before trying again
                }
            } catch (InterruptedException e) {
                // The main thread will interrupt the capture thread to indicate that properties have changed, or
                // possibly that the thread should shut down.
                logger.log(Level.INFO, "Capture thread interrupted");
            }
        }
    }, "Capture");

    @Override
    public void init() {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(1.0);
        splitPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        splitPane.add(gripImage, JSplitPane.LEFT);
        splitPane.add(gripReportList, JSplitPane.RIGHT);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        gripImage.setReportList(gripReportList);
        thread.start();
        gripReportList.start();
    }

    /**
     * Return an array big enough to hold least at least "capacity" elements.  If the supplied buffer is big enough,
     * it will be reused to avoid unnecessary allocations.
     */
    private static byte[] growIfNecessary(byte[] buffer, int capacity) {
        if (capacity > buffer.length) {
            int newCapacity = buffer.length;
            while (newCapacity < capacity) {
                newCapacity *= 1.5;
            }
            logger.info("Growing to " + newCapacity);
            return new byte[newCapacity];
        }

        return buffer;
    }

    @Override
    public void disconnect() {
        shutdownThread = true;
        thread.interrupt();
    }

    @Override
    public void propertyChanged(Property property) {
        thread.interrupt();
    }
}
