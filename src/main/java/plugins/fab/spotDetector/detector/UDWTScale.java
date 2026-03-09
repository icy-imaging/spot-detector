/*
 * Copyright (c) 2010-2026. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package plugins.fab.spotDetector.detector;

import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.component.ComponentUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;

public class UDWTScale implements CaretListener {
    JPanel scalePanel;
    JTextField sensitivityTextField;
    //	JLabel errorLabel ;
    JCheckBox enabledCheckBox;
    public int scaleNumber;

    public UDWTScale(final int scaleNumber, final boolean enabled, final double threshold) {
        this.scaleNumber = scaleNumber;

        enabledCheckBox = new JCheckBox("Scale " + scaleNumber, enabled);
        sensitivityTextField = new JTextField("" + threshold);
//		ComponentUtil.setFixedHeight( thresholdTextField , 22 );
//		errorLabel = new JLabel("");
//		errorLabel.setAlignmentX( JLabel.CENTER );
//		errorLabel.setForeground( Color.red );
        sensitivityTextField.addCaretListener(this);
//		JLabel thresholdLabel = new JLabel("Threshold: ");
        final JLabel descriptionLabel = new JLabel("");
//		ComponentUtil.setFixedWidth( thresholdTextField ,100 );
        ComponentUtil.setFixedSize(sensitivityTextField, new Dimension(60, 22));
        ComponentUtil.setFixedSize(descriptionLabel, new Dimension(80, 22));
        ComponentUtil.setFixedSize(enabledCheckBox, new Dimension(80, 22));

        switch (scaleNumber) {
            case 1:
                descriptionLabel.setText("~1 pixel");
                break;
            case 2:
                descriptionLabel.setText("~3 pixels");
                break;
            case 3:
                descriptionLabel.setText("~7 pixels");
                break;
            case 4:
                descriptionLabel.setText("~13 pixels");
                break;
            case 5:
                descriptionLabel.setText("~25 pixels");
                break;
        }

        scalePanel = new JPanel();
        scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.PAGE_AXIS));
        scalePanel.add(GuiUtil.createLineBoxPanel(
                enabledCheckBox, //thresholdLabel ,
                sensitivityTextField,
                Box.createHorizontalStrut(20),
                descriptionLabel, Box.createHorizontalGlue()
        ));


        //xLax.createVerticalGlue();

        //this.setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
//		this.add( GuiUtil.createLineBoxPanel( northPane ) );
//		this.add( GuiUtil.createLineBoxPanel( tmpPanel ) );
//		scalePanel.add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );

        //GuiUtil.createLineBoxPanel( enabledCheckBox , thresholdLabel , thresholdTextField , errorLabel );
        //scalePanel = GuiUtil.createLineBoxPanel( enabledCheckBox , thresholdLabel , thresholdTextField , errorLabel , Box.gu);
    }

    public JPanel getPanel() {
        return scalePanel;
    }

    public void setThreshold(final double threshold) {
        sensitivityTextField.setText("" + threshold);
    }

    // The threshold is now the sensitivity.
    public double getThreshold() {
        double threshold;

        try {
            threshold = Double.parseDouble(sensitivityTextField.getText());
//			if ( errorLabel.getText().length() > 0 )
//			{
////				errorLabel.setText("");
//			}
        }
        catch (final NumberFormatException nfe) {
            threshold = 0;
//			errorLabel.setText("value not recognized");				
        }

        return threshold;
    }

    @Override
    public void caretUpdate(final CaretEvent e) {
        getThreshold();
    }

    public boolean isEnabled() {
        return enabledCheckBox.isSelected();
    }

    public static @NotNull JPanel getHeaderPanel() {
        final JPanel headerPanel = new JPanel();

        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));

        final JLabel scaleLabel = new JLabel("Scale enabled");
        ComponentUtil.setFixedSize(scaleLabel, new Dimension(80, 22));
        final JLabel thresholdLabel = new JLabel("Sensitivity");
        ComponentUtil.setFixedSize(thresholdLabel, new Dimension(80, 22));
        final JLabel objectSizeLabel = new JLabel("Object size");
        ComponentUtil.setFixedSize(objectSizeLabel, new Dimension(80, 22));

        headerPanel.add(GuiUtil.createLineBoxPanel(scaleLabel, thresholdLabel, objectSizeLabel, Box.createHorizontalGlue()));

        return headerPanel;
    }
}