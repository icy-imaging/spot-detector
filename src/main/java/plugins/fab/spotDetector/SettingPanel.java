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

package plugins.fab.spotDetector;

import icy.gui.util.GuiUtil;
import icy.network.NetworkUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Fabrice de Chaumont
 */
public class SettingPanel extends JPanel implements ActionListener {
    JButton jumpToHelpButton = new JButton("<html><center><br>Don't know how to use the spot detector ?<br><br>Click this button for online documentation<br><br></html>");
		
	/*
	welcome
	|
	Input :
	from an opened image - from a batch - from webcam
	|
	Channel selection
	|
	Detector: hard threshold - wavelets
					|				|
				detect negative / positive
					|				|					
					|				Size of object
					|				|
	|--------------------------------
	|
	ROI : from sequence
	|
	Post filtering
	|
	|
	Ouput
	|
	|
	Display
	
	
	*/

    public SettingPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new TitledBorder("Settings"));
        jumpToHelpButton.addActionListener(this);
        welcome();
    }

    private void welcome() {
        removeAll();
        add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(30)));
        add(GuiUtil.createLineBoxPanel(jumpToHelpButton));
        add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(30)));
        add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == jumpToHelpButton) {
            NetworkUtil.openBrowser("https://icy.bioimageanalysis.org/plugin/Spot_Detector");
        }
    }
}
