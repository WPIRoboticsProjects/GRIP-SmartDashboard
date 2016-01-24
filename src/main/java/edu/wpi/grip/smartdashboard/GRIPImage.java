package edu.wpi.grip.smartdashboard;

import javax.swing.*;
import java.awt.*;

/**
 * A Swing component that renders either an image or an error.  This is used inside of {@link GRIPExtension} to
 * show the live video feed from GRIP.
 */
public class GRIPImage extends JComponent {

    private Image image = null;
    private String error = null;

    public GRIPImage() {
        super();

        setPreferredSize(new Dimension(640, 480));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /**
     * Set the latest image to show and clear any error
     */
    public synchronized void setImage(Image image) {
        this.image = image;
        this.error = null;
        EventQueue.invokeLater(this::repaint);
    }

    /**
     * Set an error to show instead of an image
     */
    public synchronized void setError(String error) {
        this.error = error;
        this.image = null;
        EventQueue.invokeLater(this::repaint);
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        final Graphics2D g2d = ((Graphics2D) g);
        final int em = g2d.getFontMetrics().getHeight();

        if (image == null) {
            g2d.setColor(Color.PINK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.BLACK);
            g2d.drawString(error == null ? "No image available" : error, em / 2, em);
        } else {
            final double aspectRatio = (double) image.getHeight(null) / image.getWidth(null);
            int x = 0, y = 0, width = getWidth(), height = getHeight();

            // Preserve the image's aspect ratio.  If this component is too wide, make the image less wide and center
            // it horizontally.  If it's too tall, make the image shorter and center it vertically.
            if (width * aspectRatio > height) {
                width = (int) (getHeight() / aspectRatio);
                x = (getWidth() - width) / 2;
            } else {
                height = (int) (getWidth() * aspectRatio);
                y = (getHeight() - height) / 2;
            }

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, x, y, width, height, null);
        }
    }
}
