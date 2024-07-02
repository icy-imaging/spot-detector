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

package plugins.fab.spotDetector.preProcessing;

import org.apache.poi.ss.usermodel.Sheet;
import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.component.ComponentUtil;
import org.bioimageanalysis.icy.io.xls.XLSXUtil;
import org.bioimageanalysis.icy.model.sequence.SequenceUtil;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChannelSelectorPreProcessor extends PreProcessingAbstract implements ActionListener {
    JComboBox<String> channelSelectionComboBox;
    int selectedChannel;

    public ChannelSelectorPreProcessor() {
        selectedChannel = 0;
        channelSelectionComboBox = new JComboBox<>();

        channelSelectionComboBox.addItem("Channel 0");
        channelSelectionComboBox.addItem("Channel 1");
        channelSelectionComboBox.addItem("Channel 2");
        channelSelectionComboBox.addItem("Channel 3");
        channelSelectionComboBox.addItem("Channel 4");
        channelSelectionComboBox.addItem("Channel 5");
        channelSelectionComboBox.addItem("Channel 6");

        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("Channel Selector"));
        ComponentUtil.setFixedHeight(channelSelectionComboBox, 20);

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Select channel:")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createHorizontalStrut(50), channelSelectionComboBox, Box.createHorizontalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));

        channelSelectionComboBox.addActionListener(this);
    }

    @Override
    public void process(final @NotNull GlobalDetectionToken gdt) throws InterruptedException {
        gdt.inputComputationSequence = SequenceUtil.extractChannel(gdt.inputSequence, selectedChannel);
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == channelSelectionComboBox) {
            selectedChannel = channelSelectionComboBox.getSelectedIndex();
        }
    }

    @Override
    public void saveXLS(final @NotNull Sheet page, final GlobalDetectionToken gdt) {
        int row = page.getLastRowNum();
        XLSXUtil.setCellString(page, 0, row, "Pre processor :");
        //xlsManager.setLabel( 0 , row , "Pre processor :" );
        XLSXUtil.setCellString(page, 1, row, "Channel selector");
        //xlsManager.setLabel( 1 , row , "Channel selector" );
        row++;
        XLSXUtil.setCellString(page, 0, row, "Channel Selected:");
        //xlsManager.setLabel( 0 , row+1 , "Channel Selected:" );
        XLSXUtil.setCellNumber(page, 0, row, selectedChannel);
        //xlsManager.setNumber( 1 , row+1 , selectedChannel );
    }
}
