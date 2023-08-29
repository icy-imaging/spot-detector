/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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

package plugins.fab.spotDetector.roi;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.ComponentUtil;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI3D;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import jxl.write.WritableSheet;
import plugins.adufour.connectedcomponents.ConnectedComponent;
import plugins.adufour.connectedcomponents.ConnectedComponents;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ROICreateFromChannel extends ROIDetectionAbstract implements ActionListener, CaretListener {
    JComboBox<String> channelSelectionComboBox;
    int selectedChannel;
    JButton testSettingsButton = new JButton("Test those settings on current sequence (current ROI will be replaced)");
    JTextField thresholdTextField = new JTextField("50");
    JLabel thresholdErrorTextField = new JLabel("Invalid value");
    JCheckBox enableMultipleROI = new JCheckBox("Enable multiple ROIs", true);
    JTextField dilateROITextField = new JTextField("0");
    JTextField minimumSizeOfROITextField = new JTextField("100");

    public ROICreateFromChannel() {
        selectedChannel = 0;
        channelSelectionComboBox = new JComboBox<>();
        thresholdErrorTextField.setForeground(Color.red);

        channelSelectionComboBox.addItem("Channel 0");
        channelSelectionComboBox.addItem("Channel 1");
        channelSelectionComboBox.addItem("Channel 2");
        channelSelectionComboBox.addItem("Channel 3");
        channelSelectionComboBox.addItem("Channel 4");
        channelSelectionComboBox.addItem("Channel 5");
        channelSelectionComboBox.addItem("Channel 6");

        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("Create an ROI from a specific channel in a 2D image."));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Current sequence input:")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        ComponentUtil.setFixedHeight(channelSelectionComboBox, 20);

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createHorizontalStrut(50), channelSelectionComboBox, Box.createHorizontalStrut(50)));

        ComponentUtil.setFixedHeight(dilateROITextField, 20);
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createHorizontalStrut(50), new JLabel("Dilate ROIs:"), dilateROITextField, Box.createHorizontalStrut(50)));

        getPanel().add(GuiUtil.createLineBoxPanel(testSettingsButton));
        testSettingsButton.addActionListener(this);
        thresholdErrorTextField.setVisible(false);
        ComponentUtil.setFixedHeight(thresholdTextField, 20);
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Threshold: "), thresholdTextField, thresholdErrorTextField));
        getPanel().add(GuiUtil.createLineBoxPanel(enableMultipleROI));
        ComponentUtil.setFixedHeight(minimumSizeOfROITextField, 20);
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Minimum size of ROI(for multiple ROIs): "), minimumSizeOfROITextField));
        thresholdTextField.addCaretListener(this);

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(30)));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Tip: Remove detection areas with black rois")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    @Override
    public void process(final GlobalDetectionToken gdt) {
        createChannel(gdt.inputSequence);
        // gdt.roiArrayList.addAll( gdt.inputSequence.getROI2Ds() );

        final ArrayList<ROI> roiArrayList;
        roiArrayList = gdt.inputSequence.getROIs();

        // remove detection if they are in a black ROI

        final ArrayList<DetectionSpot> cloneDetectionList = new ArrayList<>(gdt.detectionResult);
        for (final ROI roi : roiArrayList) {
            if (roi.getColor().getRed() == 0 && roi.getColor().getGreen() == 0 && roi.getColor().getBlue() == 0) // roi is black
            {
                if (roi instanceof ROI2D) {
                    final ROI2D roi2d = (ROI2D) roi;

                    for (final DetectionSpot detectionSpot : cloneDetectionList) {
                        if (roi2d.contains(detectionSpot.getMassCenter().x, detectionSpot.getMassCenter().y)) {
                            gdt.detectionResult.remove(detectionSpot);
                        }
                    }
                }
                else if (roi instanceof ROI3D) {
                    final ROI3D roi3d = (ROI3D) roi;

                    for (final DetectionSpot detectionSpot : cloneDetectionList) {
                        if (roi3d.contains(detectionSpot.getMassCenter().x, detectionSpot.getMassCenter().y,
                                detectionSpot.getMassCenter().z)) {
                            gdt.detectionResult.remove(detectionSpot);
                        }
                    }
                }
            }
        }

        // create a hashMap with the detections binded to ROI

        final HashMap<ROI, ArrayList<DetectionSpot>> ROI2Detection = new HashMap<>();

        // create a fake ROI taking all the sequence if no existing ROI on sequence.
        if (roiArrayList.isEmpty()) {
            ROI2Detection.put(new ROI2DRectangle(gdt.inputSequence.getBounds2D()),
                    new ArrayList<>(gdt.detectionResult));
        }
        else { // there is ROI is the sequence

            // setup hashMap
            for (final ROI roi : roiArrayList)
                ROI2Detection.put(roi, new ArrayList<>());

            // fill hashMap
            for (final ROI roi : roiArrayList) {
                if (roi instanceof ROI2D) {
                    final ROI2D roi2d = (ROI2D) roi;

                    for (final DetectionSpot spot : gdt.detectionResult) {
                        if (roi2d.contains(spot.getMassCenter().x, spot.getMassCenter().y)) {
                            ROI2Detection.get(roi).add(spot);
                        }
                    }
                }
                else if (roi instanceof ROI3D) {
                    final ROI3D roi3d = (ROI3D) roi;

                    for (final DetectionSpot spot : gdt.detectionResult) {
                        if (roi3d.contains(spot.getMassCenter().x, spot.getMassCenter().y, spot.getMassCenter().z)) {
                            ROI2Detection.get(roi).add(spot);
                        }
                    }
                }
            }
        }

        // rebuild the detectionResult with what has been found in ROIs

        gdt.detectionResult.clear();
        // Set<Entry<ROI2D, ArrayList<DetectionSpot>>> roiList = ROI2Detection.keySet();
        for (final ROI roi : ROI2Detection.keySet()) {
            gdt.detectionResult.addAll(ROI2Detection.get(roi));
        }

        // set detection with ROI

        gdt.roi2detection = ROI2Detection;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == testSettingsButton) {
            ThreadUtil.bgRun(() -> createChannel(getActiveSequence()));
        }
    }

    private void createChannel(final Sequence sequence) {
        if (sequence == null)
            return;
        try {
            // Sequence sequence = getFocusedSequence();
            final double value = Double.parseDouble(thresholdTextField.getText());
            final IcyBufferedImage image = sequence.getImage(0, 0);
            final double[] data = Array1DUtil.arrayToDoubleArray(image.getDataXY(channelSelectionComboBox.getSelectedIndex()), image.isSignedDataType());

            final boolean[] booleanROI = new boolean[data.length];
            final byte[] byteROI = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                booleanROI[i] = (data[i] >= value);
                if (data[i] > value) {
                    byteROI[i] = (byte) 255;
                }
                else {
                    byteROI[i] = 0;
                }
            }

            if (enableMultipleROI.isSelected()) {
                sequence.removeAllROI();
                final IcyBufferedImage binaryImage = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1, DataType.BYTE);
                binaryImage.setDataXYAsByte(0, byteROI);
                final Sequence binarySequence = new Sequence(binaryImage);

                final Map<Integer, List<ConnectedComponent>> result = ConnectedComponents.extractConnectedComponents(
                        binarySequence,
                        0,
                        ConnectedComponents.ExtractionType.BACKGROUND,
                        0,
                        Integer.MAX_VALUE,
                        null
                );

                final double minSizeROI = Double.parseDouble(minimumSizeOfROITextField.getText());
                // only consider time point 0.
                {
                    final List<ConnectedComponent> ccList = result.get(0);
                    final AnnounceFrame af = new AnnounceFrame("Creating ROIs...");
                    for (final ConnectedComponent cc : ccList) {
                        if (cc.getPoints().length < minSizeROI)
                            continue;
                        final boolean[] connectedMask = new boolean[data.length];
                        for (final Point3i point : cc.getPoints()) // put points in mask
                        {
                            connectedMask[point.y * image.getWidth() + point.x] = true;
                        }

                        dilateMask(connectedMask, sequence);

                        // create the corresponding ROI
                        final ROI2DArea roi2DArea = new ROI2DArea();
                        roi2DArea.setAsBooleanMask(new Rectangle(0, 0, sequence.getWidth(), sequence.getHeight()), connectedMask);
                        roi2DArea.setName("Area " + ccList.indexOf(cc));
                        sequence.addROI(roi2DArea);

                    }
                    af.close();
                }

            }

            if (!enableMultipleROI.isSelected()) {
                dilateMask(booleanROI, sequence);
                sequence.removeAllROI();
                final ROI2DArea roi2DArea = new ROI2DArea();
                roi2DArea.setAsBooleanMask(new Rectangle(0, 0, sequence.getWidth(), sequence.getHeight()), booleanROI);
                sequence.addROI(roi2DArea);
            }

        }
        catch (final NumberFormatException e1) {
            new AnnounceFrame("ROI Create channel : Invalid threshold or dilatation argument");
        }
    }

    private void dilateMask(final boolean[] booleanROI, final Sequence sequence) {
        final int nbDilateIteration = Integer.parseInt(dilateROITextField.getText());
        final int width = sequence.getWidth();
        final int height = sequence.getHeight();

        final boolean[] dilated = new boolean[booleanROI.length];

        for (int i = 0; i < nbDilateIteration; i++) {
            // dilate
            {
                int index = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // if ( booleanROI[ y*width + x ] )
                        if (booleanROI[index]) {
                            put9pix(dilated, x, y, width, height);
                        }
                        index++;
                    }
                }
            }

            // copy dilated in original
            final int arraySize = booleanROI.length;
            System.arraycopy(dilated, 0, booleanROI, 0, arraySize);
        }
    }

    private void put9pix(final boolean[] dilated, final int x, final int y, final int width, final int height) {
        for (int yy = y - 1; yy < y + 2; yy++) {
            for (int xx = x - 1; xx < x + 2; xx++) {
                if ((xx >= 0) && (yy >= 0) && (xx < width) && (yy < height))
                    dilated[yy * width + xx] = true;
            }
        }
    }

    @Override
    public void caretUpdate(final CaretEvent e) {
        try {
            Double.parseDouble(thresholdTextField.getText()); // to test if value is correct
            thresholdErrorTextField.setVisible(false);
            testSettingsButton.setEnabled(true);
        }
        catch (final NumberFormatException e1) {
            thresholdErrorTextField.setVisible(true);
            testSettingsButton.setEnabled(false);
        }
    }

    @Override
    public void saveXLS(final WritableSheet page, final GlobalDetectionToken gdt) throws InterruptedException {
        ROI_XLS_Common_Saver.saveXLS(page, gdt, "ROI From channel module");
    }

}
