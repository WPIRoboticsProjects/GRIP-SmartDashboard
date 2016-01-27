package edu.wpi.grip.smartdashboard;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A JList that shows all of the subtables in the "GRIP/" NetworkTable.  This is used to display all of the reports
 * being published by GRIP.
 */
public class GRIPReportList extends JList<GRIPReportList.Report> {

    private final static Color[] COLORS = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN,
            Color.MAGENTA, Color.PINK, Color.ORANGE,};

    public class Report {
        String key;
        ITable table;
        Color color;
        boolean show = true;

        public Report(String key, ITable table, Color color) {
            this.key = key;
            this.table = table;
            this.color = color;
        }
    }

    /**
     * A ListCellRenderer that renders a checkbox with information from a report.  This includes the subtable that
     * the report is stored in, whether or not it it's shown, and what color it should be shown as.  This serves as
     * sort of a "key" for the visualization.
     */
    private class ReportCellRenderer extends JCheckBox implements ListCellRenderer<Report> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Report> l, Report report, int i, boolean s, boolean f) {
            setText(report.key);
            setSelected(report.show);
            setForeground(report.color);
            if (getGraphics() != null) {
                final int em = getGraphics().getFontMetrics().getHeight();
                setBorder(new EmptyBorder(em / 4, em / 2, em / 4, em));
            }
            return this;
        }
    }

    private final List<Report> reports = new ArrayList<>();
    private final DefaultListModel<Report> model = new DefaultListModel<>();

    public GRIPReportList() {
        super();

        setMinimumSize(new Dimension(200, 0));
        setPreferredSize(new Dimension(200, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        setModel(model);

        // When one of the elements is clicked, toggle whether or not that report is shown
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index != -1) {
                    Report report = getModel().getElementAt(index);
                    report.show = !report.show;
                }
            }
        });
    }

    /**
     * Start listening for NetworkTables values
     */
    public void start() {
        setCellRenderer(new ReportCellRenderer());

        NetworkTable.getTable("GRIP").addSubTableListener((table, key, v, i) -> {
            Report report = new Report(key, table.getSubTable(key), COLORS[(reports.size()) % COLORS.length]);
            model.addElement(report);
            reports.add(report);
            report.table.addTableListener((t, k, o, b) -> EventQueue.invokeLater(() -> getParent().repaint()));

            repaint();
        });
    }

    public List<Report> getReports() {
        return reports;
    }
}
