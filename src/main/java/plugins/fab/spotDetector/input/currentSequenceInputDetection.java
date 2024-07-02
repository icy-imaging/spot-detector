/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

package plugins.fab.spotDetector.input;

import org.apache.poi.ss.usermodel.Sheet;
import org.bioimageanalysis.icy.Icy;
import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.listener.GlobalSequenceListener;
import org.bioimageanalysis.icy.io.xls.XLSXUtil;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class currentSequenceInputDetection extends InputDetectionAbstract {
    JLabel currentSequenceNameLabel = new JLabel();
    Color labelDefaultColor;

    public currentSequenceInputDetection() {
        labelDefaultColor = currentSequenceNameLabel.getForeground();
        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("Select channel to use to create ROI"));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Current sequence input:")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(currentSequenceNameLabel));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
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
        gdt.inputSequence = getActiveSequence();
    }

    @Override
    public void saveXLS(final @NotNull Sheet page, final @NotNull GlobalDetectionToken gdt) {
        int row = page.getLastRowNum();
        XLSXUtil.setCellString(page, 0, row, "Input Module:");
        //xlsManager.setLabel( 0 , row , "Input Module: " );
        XLSXUtil.setCellString(page, 1, row, "Current Sequence Input");
        //xlsManager.setLabel( 1 , row , "Current Sequence Input" );
        row++;
        XLSXUtil.setCellString(page, 0, row, "File name:");
        //xlsManager.setLabel( 0 , row+1 , "File Name:" );
        XLSXUtil.setCellString(page, 1, row, gdt.inputSequence.getFilename());
        //xlsManager.setLabel( 1 , row+1 , gdt.inputSequence.getFilename() );
    }
}
