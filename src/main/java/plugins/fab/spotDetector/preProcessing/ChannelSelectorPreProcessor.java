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

package plugins.fab.spotDetector.preProcessing;

import icy.gui.util.ComponentUtil;
import icy.gui.util.GuiUtil;
import icy.sequence.SequenceUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
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
    public void process(final GlobalDetectionToken gdt) throws InterruptedException {
        gdt.inputComputationSequence = SequenceUtil.extractChannel(gdt.inputSequence, selectedChannel);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == channelSelectionComboBox) {
            selectedChannel = channelSelectionComboBox.getSelectedIndex();
        }
    }

    @Override
    public void saveXLS(final WritableSheet page, final GlobalDetectionToken gdt) {
        int row = page.getRows();
        XLSUtil.setCellString(page, 0, row, "Pre processor :");
        //xlsManager.setLabel( 0 , row , "Pre processor :" );
        XLSUtil.setCellString(page, 1, row, "Channel selector");
        //xlsManager.setLabel( 1 , row , "Channel selector" );
        row++;
        XLSUtil.setCellString(page, 0, row, "Channel Selected:");
        //xlsManager.setLabel( 0 , row+1 , "Channel Selected:" );
        XLSUtil.setCellNumber(page, 0, row, selectedChannel);
        //xlsManager.setNumber( 1 , row+1 , selectedChannel );
    }
}
