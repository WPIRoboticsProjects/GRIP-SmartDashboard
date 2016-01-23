package edu.wpi.grip.smartdashboard;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.Property;

import javax.swing.*;

public class GRIPExtension extends StaticWidget {

    public static final String NAME = "GRIP Output Viewer";

    @Override
    public void init() {
        add(new JButton("Hi"));
    }

    @Override
    public void propertyChanged(Property property) {

    }
}
