/*
 * Copyright 2010, 2011 Institut Pasteur.
 * 
 * This file is part of ICY.
 * 
 * ICY is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ICY is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ICY. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.spotDetector.input;

import icy.file.xls.XlsManager;
import icy.gui.main.MainAdapter;
import icy.gui.main.MainEvent;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.util.XLSUtil;

import java.awt.Color;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import jxl.write.WritableSheet;

import plugins.fab.spotDetector.GlobalDetectionToken;

public class currentSequenceInputDetection extends InputDetectionAbstract
{
	JLabel currentSequenceNameLabel = new JLabel();	
	Color labelDefaultColor = null;
	
    public currentSequenceInputDetection()
    {  	    	
    	
    	labelDefaultColor = currentSequenceNameLabel.getForeground();
    	getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
    	getPanel().setBorder( new TitledBorder( "Select channel to use to " +
    			"create ROI" ) );
    	
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Current sequence input:") ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( currentSequenceNameLabel ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 50 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
    	
    	refreshCurrentSequenceLabel();
    	
    	Icy.getMainInterface().addListener( new MainAdapter() {
    	@Override
    		public void sequenceFocused(MainEvent event) {
    			refreshCurrentSequenceLabel();
    		}	
		});

    }

	private void refreshCurrentSequenceLabel() {
		Sequence sequence = getFocusedSequence();
		if ( sequence == null )
		{
			currentSequenceNameLabel.setForeground( Color.red );
			currentSequenceNameLabel.setText( "No sequence loaded" );
		}else
		{
			currentSequenceNameLabel.setForeground( labelDefaultColor );
			currentSequenceNameLabel.setText( sequence.getName() );
		}
	}

	@Override
	public void process(GlobalDetectionToken gdt) {
		gdt.inputSequence = getActiveSequence();
	}

	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {

		int row = page.getRows();
		XLSUtil.setCellString( page , 0, row , "Input Module:" );
		//xlsManager.setLabel( 0 , row , "Input Module: " );
		XLSUtil.setCellString( page , 1, row , "Current Sequence Input" );
		//xlsManager.setLabel( 1 , row , "Current Sequence Input" );
		row++;
		XLSUtil.setCellString( page , 0, row , "File name:" );
		//xlsManager.setLabel( 0 , row+1 , "File Name:" );
		XLSUtil.setCellString( page , 1, row , gdt.inputSequence.getFilename() );
		//xlsManager.setLabel( 1 , row+1 , gdt.inputSequence.getFilename() );
		
		
	}



}
