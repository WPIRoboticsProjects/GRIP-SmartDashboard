package edu.wpi.grip.smartdashboard;

import edu.wpi.first.wpilibj.tables.ITable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Arrays;

/**
 * A Swing component that renders either an image or an error.  This is used inside of {@link GRIPExtension} to
 * show the live video feed from GRIP.
 */
public class GRIPImage extends JComponent {

    private Image image = null;
    private String error = null;
    private GRIPReportList reportList = null;

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

    public synchronized void setReportList(GRIPReportList reportList) {
        this.reportList = reportList;
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

            // Scale anything drawn after this point so it lines up with the image
            double scale = (double) width / image.getWidth(null);
            AffineTransform transform = g2d.getTransform();
            transform.translate(x, y);
            transform.scale(scale, scale);
            g2d.setTransform(transform);
            g2d.setStroke(new BasicStroke((float) (2 / scale)));

            synchronized (reportList) {
                for (GRIPReportList.Report report : reportList.getReports()) {
                    renderReport(g2d, report);
                }
            }
        }
    }

    /**
     * Draw a single GRIP report based on the values stored in a subtable.
     * <p>
     * This method looks for groups of subtable values that look like either a line, blob, or contour report, and draws
     * the appropriate visuals.  The color and visibility of the drawing is based on the {@link GRIPReportList.Report}
     * fields.
     */
    private void renderReport(Graphics2D g2d, GRIPReportList.Report report) {
        // Do nothing if the report is set to not show
        if (!report.show) {
            return;
        }

        ITable table = report.table;
        g2d.setColor(report.color);
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (table.getKeys().containsAll(Arrays.asList("x1", "x2", "y1", "y2"))) {
            // If the subtable has four equal-length number arrays called x1, y1, x2, and y2, then draw a line for
            // each element in the arrays
            double[] x1 = table.getNumberArray("x1");
            double[] x2 = table.getNumberArray("x2");
            double[] y1 = table.getNumberArray("y1");
            double[] y2 = table.getNumberArray("y2");

            if (x1.length == x2.length && x1.length == y1.length && x1.length == y2.length) {
                for (int i = 0; i < x1.length; i++) {
                    g2d.drawLine((int) x1[i], (int) y1[i], (int) x2[i], (int) y2[i]);
                }
            }
        } else if (table.getKeys().containsAll(Arrays.asList("x", "y", "size"))) {
            // If the subtable has three equal-length arrays called x, y, and size, draw a circle for each element
            double[] x = table.getNumberArray("x");
            double[] y = table.getNumberArray("y");
            double[] size = table.getNumberArray("size");

            if (x.length == y.length) {
                for (int i = 0; i < x.length; i++) {
                    g2d.drawOval((int) (x[i] - size[i] / 2), (int) (y[i] - size[i] / 2), (int) size[i], (int) size[i]);
                    g2d.drawLine((int) (x[i] - 8), (int) y[i], (int) (x[i] + 8), (int) y[i]);
                    g2d.drawLine((int) x[i], (int) (y[i] - 8), (int) x[i], (int) (y[i] + 8));
                }
            }
        } else if (table.getKeys().containsAll(Arrays.asList("centerX", "centerY", "width", "height"))) {
            // If the subtable has x, y, width, and height, draw rectangles.  This really means GRIP is publishing
            // contours, but it doesn't publish the full contour data.
            double x[] = table.getNumberArray("centerX");
            double y[] = table.getNumberArray("centerY");
            double width[] = table.getNumberArray("width");
            double height[] = table.getNumberArray("height");

            if (x.length == y.length && x.length == width.length && x.length == height.length) {
                for (int i = 0; i < x.length; i++) {
                    g2d.drawRect((int) (x[i] - width[i] / 2), (int) (y[i] - height[i] / 2), (int) width[i], (int) height[i]);
                    g2d.drawLine((int) (x[i] - 8), (int) y[i], (int) (x[i] + 8), (int) y[i]);
                    g2d.drawLine((int) x[i], (int) (y[i] - 8), (int) x[i], (int) (y[i] + 8));
                }
            }
        }
    }
}
