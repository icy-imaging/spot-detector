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

package plugins.fab.spotDetector.roi;

import org.apache.poi.ss.usermodel.Sheet;
import org.bioimageanalysis.extension.kernel.roi.roi2d.ROI2DRectangle;
import org.bioimageanalysis.icy.Icy;
import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.listener.GlobalSequenceListener;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ROIFromSequence extends ROIDetectionAbstract {
    JLabel currentSequenceNameLabel = new JLabel();
    Color labelDefaultColor = null;

    public ROIFromSequence() {
        // getPanel().add(new JLabel("from sequence"));

        // getPanel().add(new JLabel("Existing ROIs are used to perform the computation"));

        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("use ROI in current sequence"));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Current sequence input:")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(currentSequenceNameLabel));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(30)));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Tip: Remove detection areas with black rois")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));

        refreshCurrentSequenceLabel();

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
                //
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
    public void process(final @NotNull GlobalDetectionToken gdt) {
        final ArrayList<ROI> roiArrayList;

        roiArrayList = gdt.inputSequence.getROIs();

        final ArrayList<ROI> roiArrayListCopy = new ArrayList<>(roiArrayList);
        for (final ROI roi : roiArrayListCopy) {
            if (roi.getName().startsWith("spot"))
                roiArrayList.remove(roi);
        }

        // if ( roi.getName().startsWith("spot") ) continue;

        // remove detection if they are in a black ROI

        final ArrayList<DetectionSpot> cloneDetectionList = new ArrayList<>(gdt.detectionResult);
        for (final ROI roi : roiArrayList) {
            if (roi.getColor().getRed() == 0 && roi.getColor().getGreen() == 0 && roi.getColor().getBlue() == 0) // roi is black
            {
                for (final DetectionSpot detectionSpot : cloneDetectionList) {
                    if (roi.contains(
                            detectionSpot.getMassCenter().x,
                            detectionSpot.getMassCenter().y,
                            detectionSpot.getMassCenter().z,
                            detectionSpot.getT(),
                            -1
                    )) {
                        gdt.detectionResult.remove(detectionSpot);
                    }
                }
            }
        }

        // create a hashMap with the detections binded to ROI

        final HashMap<ROI, ArrayList<DetectionSpot>> ROI2Detection = new HashMap<>();

        // create a fake ROI taking all the sequence if no existing ROI on sequence.
        if (roiArrayList.isEmpty()) {
            @SuppressWarnings("unchecked") final // clone method return only Object
            ArrayList<DetectionSpot> value = (ArrayList<DetectionSpot>) gdt.detectionResult.clone();
            ROI2Detection.put(new ROI2DRectangle(gdt.inputSequence.getBounds2D()), value);
        }
        else { // there is ROI is the sequence
            // setup hashMap
            for (final ROI roi : roiArrayList)
                ROI2Detection.put(roi, new ArrayList<>());

            // fill hashMap
            for (final ROI roi : roiArrayList) {
                for (final DetectionSpot spot : gdt.detectionResult) {
                    if (roi.contains(spot.getMassCenter().x, spot.getMassCenter().y, spot.getMassCenter().z,
                            spot.getT(), -1)) {
                        ROI2Detection.get(roi).add(spot);
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
    public void saveXLS(final Sheet page, final GlobalDetectionToken gdt) throws InterruptedException {
        ROI_XLS_Common_Saver.saveXLS(page, gdt, "ROI From Sequence module");
    }
}
