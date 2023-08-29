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

package plugins.fab.spotDetector.display;

import icy.gui.component.button.ColorChooserButton;
import icy.gui.component.button.ColorChooserButton.ColorChangeListener;
import icy.gui.main.GlobalSequenceListener;
import icy.gui.util.ComponentUtil;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import jxl.write.WritableSheet;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BasicDetection extends DisplayDetectionAbstract implements ActionListener, ColorChangeListener, GlobalSequenceListener {

    JCheckBox displayROINumber = new JCheckBox("Display ROI number and name", true);
    JCheckBox displayDetectionMark = new JCheckBox("Display detection mark", true);
    JCheckBox displayDetectionNumberOverROICheckBox = new JCheckBox("Display number of detection of ROI", true);
    ColorChooserButton colorChooserButton = new ColorChooserButton(Color.red);
    JCheckBox projectDetectionMark = new JCheckBox("Project all detections of a same stack", true);
    JCheckBox displayDetectionIndexCheckBox = new JCheckBox("Display detection index", false);
    JTextField fontSizeTextField = new JTextField("14");


    @Override
    public void actionPerformed(final ActionEvent e) {
        refreshPainterFromInterface();
    }

    @Override
    public void colorChanged(final ColorChooserButton source) {
        refreshPainterFromInterface();
    }


    public BasicDetection() {

        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("Basic detection painter"));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(displayDetectionMark, Box.createHorizontalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(displayDetectionNumberOverROICheckBox, Box.createHorizontalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(displayROINumber, Box.createHorizontalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Detection color: "), colorChooserButton, Box.createHorizontalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(projectDetectionMark, Box.createHorizontalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(displayDetectionIndexCheckBox, Box.createHorizontalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Font size:"), fontSizeTextField, Box.createHorizontalGlue()));

        ComponentUtil.setFixedHeight(fontSizeTextField, 20);
        displayDetectionNumberOverROICheckBox.addActionListener(this);
        displayDetectionMark.addActionListener(this);
        displayDetectionIndexCheckBox.addActionListener(this);
        displayROINumber.addActionListener(this);
        projectDetectionMark.addActionListener(this);
        colorChooserButton.addColorChangeListener(this);
        fontSizeTextField.addActionListener(this);

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));

        Icy.getMainInterface().addGlobalSequenceListener(this);

    }


    @Override
    public void process(final GlobalDetectionToken gdt) {

        // remove painter from an existing sequence

        for (final Overlay painter : gdt.inputSequence.getOverlays()) {
            if (painter instanceof BasicDetectionPainter) {
                gdt.inputSequence.removeOverlay(painter);
            }
        }

        final BasicDetectionPainter basicDetectionPainter = new BasicDetectionPainter();
        basicDetectionPainter.detectionsHashMap = gdt.roi2detection;

        gdt.inputSequence.addOverlay(basicDetectionPainter);
        updatePainter(basicDetectionPainter);

//		detectionsHashMap = gdt.roi2detection; 
//
//		gdt.inputSequence.removePainter( this );
//		gdt.inputSequence.addPainter( this );
//		
//		if ( gdt.binarySequence != null )
//		{
//			//System.out.println("adding test painter");
//			gdt.binarySequence.removePainter( this );
//			gdt.binarySequence.addPainter( this );
//			
//		}

        //System.out.println("painter called with : " + gdt.roi2detection );

    }

    private void updatePainter(final BasicDetectionPainter basicDetectionPainter) {

        basicDetectionPainter.color = colorChooserButton.getColor();
        basicDetectionPainter.displayDetectionMark = displayDetectionMark.isSelected();
        basicDetectionPainter.displayDetectionNumberOverROI = displayDetectionNumberOverROICheckBox.isSelected();
        basicDetectionPainter.displayROINumber = displayROINumber.isSelected();
        basicDetectionPainter.projectDetection = projectDetectionMark.isSelected();
        basicDetectionPainter.displayDetectionIndex = displayDetectionIndexCheckBox.isSelected();
        try {
            basicDetectionPainter.fontLabelSize = Integer.parseInt(fontSizeTextField.getText());
        }
        catch (final Exception e) {
            basicDetectionPainter.fontLabelSize = 14;
        }

        for (final Sequence sequence : Icy.getMainInterface().getSequencesContaining(basicDetectionPainter)) {
            sequence.overlayChanged(basicDetectionPainter);
        }
    }

    @Override
    public void close() {

        // remove this painter on sequence containing the painters

        for (final Sequence sequence : Icy.getMainInterface().getSequences()) {
            for (final Overlay painter : sequence.getOverlays()) {
                if (painter instanceof BasicDetectionPainter) {
                    sequence.removeOverlay(painter);
                }
            }
        }

//		ArrayList<Sequence> sequenceList = Icy.getMainInterface().getSequencesContaining( this );
//		for ( Sequence sequence : sequenceList )
//		{
//			sequence.removePainter( this );
//		}

    }


    @Override
    public void sequenceOpened(final Sequence sequence) {
        refreshDetectionInterfaceFromSequencePainter();
    }

    @Override
    public void sequenceClosed(final Sequence sequence) {

    }

    private void refreshPainterFromInterface() {
        final Sequence focusedSequence = getActiveSequence();
        if (focusedSequence != null) {
            for (final Overlay painter : focusedSequence.getOverlays()) {
                if (painter instanceof BasicDetectionPainter) {

                    final BasicDetectionPainter bdp = (BasicDetectionPainter) painter;
                    updatePainter(bdp);
                }
            }
        }

        // FIXME
        // should only look for proper painters
        //ArrayList<Sequence> sequenceList = Icy.getMainInterface().getSequencesContaining(  );
//		ArrayList<Sequence> sequenceList = Icy.getMainInterface().getSequences();
//		for ( Sequence sequence : sequenceList )
//		{
//			sequence.painterChanged( null );
//			//sequence.painterChanged( this );
//		}	
    }

    private void refreshDetectionInterfaceFromSequencePainter() {

        final Sequence focusedSequence = getActiveSequence();
        if (focusedSequence != null) {
            for (final Overlay painter : focusedSequence.getOverlays()) {
                if (painter instanceof BasicDetectionPainter) {

                    final BasicDetectionPainter basicDetectionPainter = (BasicDetectionPainter) painter;
                    colorChooserButton.setEnabled(false);
                    colorChooserButton.setColor(basicDetectionPainter.color);
                    colorChooserButton.setEnabled(true);

                    displayDetectionMark.setEnabled(false);
                    displayDetectionMark.setSelected(basicDetectionPainter.displayDetectionMark);
                    displayDetectionMark.setEnabled(true);

                    displayDetectionNumberOverROICheckBox.setEnabled(false);
                    displayDetectionNumberOverROICheckBox.setSelected(basicDetectionPainter.displayDetectionNumberOverROI);
                    displayDetectionNumberOverROICheckBox.setEnabled(true);

                    displayROINumber.setEnabled(false);
                    displayROINumber.setSelected(basicDetectionPainter.displayROINumber);
                    displayROINumber.setEnabled(true);

                    projectDetectionMark.setEnabled(false);
                    projectDetectionMark.setSelected(basicDetectionPainter.projectDetection);
                    projectDetectionMark.setEnabled(true);
                }
            }
        }
    }

    @Override
    public void saveXLS(final WritableSheet page, final GlobalDetectionToken gdt) {
        // TODO Auto-generated method stub
    }
}
