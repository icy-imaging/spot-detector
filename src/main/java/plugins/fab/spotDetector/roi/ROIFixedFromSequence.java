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

import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.main.GlobalSequenceListener;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI3D;
import icy.sequence.Sequence;
import jxl.write.WritableSheet;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class ROIFixedFromSequence extends ROIDetectionAbstract implements ActionListener {
    JLabel currentSequenceNameLabel = new JLabel();
    Color labelDefaultColor = null;
    JButton loadROIButton = new JButton("Click to load reference ROIs from focused Sequence.");

    public ROIFixedFromSequence() {
        roiListStored = new ArrayList<>();

        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("use same ROI for all sequences"));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Click on the button to copy the ROIs")));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("from the current focused sequence:")));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("** WARNING: existing ROIs")));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("in batch files will be replaced")));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("by the reference ROI**")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(currentSequenceNameLabel));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(loadROIButton));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));

        refreshCurrentSequenceLabel();

        loadROIButton.addActionListener(this);

        Icy.getMainInterface().addGlobalSequenceListener(new GlobalSequenceListener() {
            /**
             * {@inheritDoc}
             *
             * @param sequence {@inheritDoc}
             */
            @Override
            public void sequenceOpened(final Sequence sequence) {
                refreshCurrentSequenceLabel();
            }

            /**
             * {@inheritDoc}
             *
             * @param sequence {@inheritDoc}
             */
            @Override
            public void sequenceClosed(final Sequence sequence) {

            }
        });
    }

    private void refreshCurrentSequenceLabel() {
        final Sequence sequence = getActiveSequence();
        if (sequence == null) {
            currentSequenceNameLabel.setForeground(Color.red);
            currentSequenceNameLabel.setText("No sequence loaded");
        }
        else {
            currentSequenceNameLabel.setForeground(labelDefaultColor);
            currentSequenceNameLabel.setText(sequence.getName());
        }
    }

    @Override
    public void process(final GlobalDetectionToken gdt) {
        final ArrayList<ROI> roiArrayList;

        gdt.inputSequence.removeAllROI();
        for (final ROI roi : roiListStored) {
            gdt.inputSequence.addROI(roi);
        }

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
                        if (roi3d.contains(detectionSpot.getMassCenter().x, detectionSpot.getMassCenter().y, detectionSpot.getMassCenter().z)) {
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
            @SuppressWarnings("unchecked") // clone method returns only Object
            final ArrayList<DetectionSpot> value = (ArrayList<DetectionSpot>) gdt.detectionResult.clone();
            ROI2Detection.put(new ROI2DRectangle(gdt.inputSequence.getBounds2D()), value);
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

    ArrayList<ROI2D> roiListStored;

    @Override
    public void actionPerformed(final ActionEvent e) {
        final Sequence sequence = Icy.getMainInterface().getActiveSequence();
        if (sequence == null) {
            new FailedAnnounceFrame("No sequence loaded");
            return;
        }

        roiListStored = sequence.getROI2Ds();

        if (roiListStored.isEmpty()) {
            new FailedAnnounceFrame("No ROI found in the focused sequence. (" + sequence.getName() + ")");
            return;
        }

        loadROIButton.setText(roiListStored.size() + " ROI(s) Loaded.");
    }

    @Override
    public void saveXLS(final WritableSheet page, final GlobalDetectionToken gdt) throws InterruptedException {
        ROI_XLS_Common_Saver.saveXLS(page, gdt, "ROI From Sequence module");
    }
}
