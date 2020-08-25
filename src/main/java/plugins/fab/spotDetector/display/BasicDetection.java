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
package plugins.fab.spotDetector.display;

import icy.gui.component.button.ColorChooserButton;
import icy.gui.component.button.ColorChooserButton.ColorChangeListener;
import icy.gui.main.MainEvent;
import icy.gui.main.MainListener;
import icy.gui.util.ComponentUtil;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.painter.Painter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import jxl.write.WritableSheet;
import plugins.fab.spotDetector.GlobalDetectionToken;

public class BasicDetection extends DisplayDetectionAbstract implements ActionListener , ColorChangeListener , MainListener {

	
	JCheckBox displayROINumber = new JCheckBox("Display ROI number and name" , true );
	JCheckBox displayDetectionMark = new JCheckBox( "Display detection mark" , true );
	JCheckBox displayDetectionNumberOverROICheckBox = new JCheckBox( "Display number of detection of ROI" , true );
	ColorChooserButton colorChooserButton = new ColorChooserButton( Color.red );
	JCheckBox projectDetectionMark = new JCheckBox( "Project all detections of a same stack" , true );
	JCheckBox displayDetectionIndexCheckBox = new JCheckBox( "Display detection index" , false );
	JTextField fontSizeTextField = new JTextField( "14" );
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		refreshPainterFromInterface();
	}

	@Override
	public void colorChanged(ColorChooserButton source) {
		refreshPainterFromInterface();
	}


	public BasicDetection() {
		
		getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
    	getPanel().setBorder( new TitledBorder( "Basic detection painter" ) );

    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( displayDetectionMark , Box.createHorizontalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( displayDetectionNumberOverROICheckBox , Box.createHorizontalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( displayROINumber , Box.createHorizontalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Detection color: ") , colorChooserButton , Box.createHorizontalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( projectDetectionMark , Box.createHorizontalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( displayDetectionIndexCheckBox , Box.createHorizontalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Font size:" ), fontSizeTextField , Box.createHorizontalGlue() ) );    	
    	
    	ComponentUtil.setFixedHeight( fontSizeTextField , 20 );
    	displayDetectionNumberOverROICheckBox.addActionListener( this );
    	displayDetectionMark.addActionListener( this );
    	displayDetectionIndexCheckBox.addActionListener( this );
    	displayROINumber.addActionListener( this );
    	projectDetectionMark.addActionListener( this );
    	colorChooserButton.addColorChangeListener( this );
    	fontSizeTextField.addActionListener( this );
    	
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 50 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
    	
    	Icy.getMainInterface().addListener( this );
    	
	}



	@Override
	public void process(GlobalDetectionToken gdt) {

		// remove painter from an existing sequence
		
		for ( Painter painter: gdt.inputSequence.getPainters() )
		{
			if ( painter instanceof BasicDetectionPainter )
			{
				gdt.inputSequence.removePainter( painter );
			}
		}
		
		BasicDetectionPainter basicDetectionPainter = new BasicDetectionPainter();
		basicDetectionPainter.detectionsHashMap = gdt.roi2detection;
		
		gdt.inputSequence.addPainter( basicDetectionPainter );
		updatePainter( basicDetectionPainter );
		
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

	private void updatePainter(BasicDetectionPainter basicDetectionPainter) {

		basicDetectionPainter.color = colorChooserButton.getColor();
		basicDetectionPainter.displayDetectionMark = displayDetectionMark.isSelected();
		basicDetectionPainter.displayDetectionNumberOverROI = displayDetectionNumberOverROICheckBox.isSelected();
		basicDetectionPainter.displayROINumber = displayROINumber.isSelected();
		basicDetectionPainter.projectDetection = projectDetectionMark.isSelected();
		basicDetectionPainter.displayDetectionIndex = displayDetectionIndexCheckBox.isSelected();
		try{
			basicDetectionPainter.fontLabelSize = Integer.parseInt( fontSizeTextField.getText() );
		}catch ( Exception e)
		{
			basicDetectionPainter.fontLabelSize = 14;
		}
		
		for ( Sequence sequence : Icy.getMainInterface().getSequencesContaining( basicDetectionPainter ) )
		{
			sequence.painterChanged( basicDetectionPainter );
		}
	}

	@Override
	public void close() {

		// remove this painter on sequence containing the painters

		for ( Sequence sequence : Icy.getMainInterface().getSequences() )
		{
			for ( Painter painter: sequence.getPainters() )
			{
				if ( painter instanceof BasicDetectionPainter )
				{
					sequence.removePainter( painter );
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
	public void sequenceFocused(MainEvent event) {
		
		refreshDetectionInterfaceFromSequencePainter();
		
	}
	
	
	private void refreshPainterFromInterface() {
		
		Sequence focusedSequence = getFocusedSequence();
		if ( focusedSequence != null )
		{
			for ( Painter painter : focusedSequence.getPainters() )
			{
				if ( painter instanceof BasicDetectionPainter )
				{

					BasicDetectionPainter bdp = (BasicDetectionPainter) painter;
					updatePainter( bdp );
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
		
		Sequence focusedSequence = getFocusedSequence();
		if ( focusedSequence != null )
		{
			for ( Painter painter : focusedSequence.getPainters() )
			{
				if ( painter instanceof BasicDetectionPainter )
				{

					BasicDetectionPainter basicDetectionPainter = (BasicDetectionPainter) painter;
					colorChooserButton.setEnabled( false );
					colorChooserButton.setColor( basicDetectionPainter.color );
					colorChooserButton.setEnabled( true );

					displayDetectionMark.setEnabled( false );
					displayDetectionMark.setSelected( basicDetectionPainter.displayDetectionMark );
					displayDetectionMark.setEnabled( true );

					displayDetectionNumberOverROICheckBox.setEnabled( false );
					displayDetectionNumberOverROICheckBox.setSelected( basicDetectionPainter.displayDetectionNumberOverROI );
					displayDetectionNumberOverROICheckBox.setEnabled( true );

					displayROINumber.setEnabled( false );
					displayROINumber.setSelected( basicDetectionPainter.displayROINumber );
					displayROINumber.setEnabled( true );

					projectDetectionMark.setEnabled( false );
					projectDetectionMark.setSelected( basicDetectionPainter.projectDetection );
					projectDetectionMark.setEnabled( true );
				}
			}
		}
		

	}

	@Override
	public void pluginOpened(MainEvent event) {}
	@Override
	public void pluginClosed(MainEvent event) {}
	@Override
	public void viewerOpened(MainEvent event) {}
	@Override
	public void viewerFocused(MainEvent event) {}
	@Override
	public void viewerClosed(MainEvent event) {}
	@Override
	public void sequenceOpened(MainEvent event) {}
	
	@Override
	public void sequenceClosed(MainEvent event) {}
	@Override
	public void roiAdded(MainEvent event) {}
	@Override
	public void roiRemoved(MainEvent event) {}
	@Override
	public void painterAdded(MainEvent event) {}
	@Override
	public void painterRemoved(MainEvent event) {}

	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {
		// TODO Auto-generated method stub
		
	}


	
}
