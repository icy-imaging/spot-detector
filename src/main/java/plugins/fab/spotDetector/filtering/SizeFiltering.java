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

package plugins.fab.spotDetector.filtering;

import org.apache.poi.ss.usermodel.Sheet;
import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.component.ComponentUtil;
import org.bioimageanalysis.icy.io.xls.XLSXUtil;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;
import java.util.ArrayList;

public class SizeFiltering extends FilteringDetectionAbstract {
    JTextField minValueTextField = new JTextField("0");
    JTextField maxValueTextField = new JTextField("3000");

    public SizeFiltering() {
        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));

        ComponentUtil.setFixedHeight(minValueTextField, 22);
        ComponentUtil.setFixedHeight(maxValueTextField, 22);

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Range of accepted objects (in pixels)")));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Min size: "), minValueTextField));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Max size: "), maxValueTextField));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    @Override
    public void process(final @NotNull GlobalDetectionToken gdt) {
        //System.out.println("Size filtering...");

        // Filter the roi dataset
        for (final ROI roi : gdt.roi2detection.keySet()) {
            final ArrayList<DetectionSpot> detection = gdt.roi2detection.get(roi);
            final ArrayList<DetectionSpot> detectionCopy = new ArrayList<>(detection);
            final int highValue = Integer.parseInt(maxValueTextField.getText());
            final int lowValue = Integer.parseInt(minValueTextField.getText());
            for (final DetectionSpot detectionSpot : detectionCopy) {
                if (!(detectionSpot.points.size() <= highValue && detectionSpot.points.size() >= lowValue)) {
                    detection.remove(detectionSpot);
                }
            }
        }

        // filter the detection gdt.detectionResult dataset
        {
            final ArrayList<DetectionSpot> detection = gdt.detectionResult;
            final ArrayList<DetectionSpot> detectionCopy = new ArrayList<>(detection);
            final int highValue = Integer.parseInt(maxValueTextField.getText());
            final int lowValue = Integer.parseInt(minValueTextField.getText());
            for (final DetectionSpot detectionSpot : detectionCopy) {
                if (!(detectionSpot.points.size() <= highValue && detectionSpot.points.size() >= lowValue)) {
                    detection.remove(detectionSpot);
                }
            }
        }
    }


    @Override
    public void saveXLS(final @NotNull Sheet page, final GlobalDetectionToken gdt) {
        int row = page.getLastRowNum();
        XLSXUtil.setCellString(page, 0, row, "Filtering: Simple Filtering");
        //xlsManager.setLabel( 0 , row , "Filtering: Simple Filtering");
        row++;
        XLSXUtil.setCellString(page, 0, row, "Detection Size range:");
        //xlsManager.setLabel( 0 , row , "Detection Size range:");
        XLSXUtil.setCellString(page, 1, row, "Min:");
        //xlsManager.setLabel( 1 , row , "Min:");
        XLSXUtil.setCellNumber(page, 2, row, Integer.parseInt(minValueTextField.getText()));
        //xlsManager.setNumber( 2 , row , Integer.parseInt( minValueTextField.getText() ) );
        XLSXUtil.setCellString(page, 3, row, "Max:");
        //xlsManager.setLabel( 3 , row , "Max:");
        XLSXUtil.setCellNumber(page, 4, row, Integer.parseInt(maxValueTextField.getText()));
        //xlsManager.setNumber( 4 , row , Integer.parseInt( maxValueTextField.getText() ) );
    }
}
