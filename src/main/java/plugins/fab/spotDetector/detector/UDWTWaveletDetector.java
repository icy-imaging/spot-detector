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
package plugins.fab.spotDetector.detector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import icy.gui.util.GuiUtil;
import icy.image.ImageUtil;
import icy.main.Icy;
import icy.resource.ResourceUtil;
import icy.roi.ROI;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.fab.spotDetector.detector.wavelets.UDWT.B3SplineUDWT;

public class UDWTWaveletDetector extends DetectorDetectionAbstract implements ActionListener
{
	ArrayList<UDWTScale> UDWTScaleArrayList = new ArrayList<UDWTScale>();
	UDWTWaveletCore waveletCore = new UDWTWaveletCore();
	
    public UDWTWaveletDetector()
    {
    	getPanel().setLayout( new BorderLayout() );
        getPanel().add( getConfigurationPanel() , BorderLayout.CENTER );
    }

	@Override
	public void process(GlobalDetectionToken gdt) throws InterruptedException {
		
		boolean detectNegative = detectNegativeRadio.isSelected();
		boolean computeWATwithROI = computeWATwithROICheckBox.isSelected();
		boolean force2DWaveletsFor2D = use2DWaveletsFor3DImages.isSelected();
		
		// copy ROI from input sequence to the computation sequence
		
		for ( ROI roi : gdt.inputSequence.getROIs() )
		{
			String name = roi.getName();

			ROI roiCopy = roi.getCopy();
			roi.setName( name ); // because of the bug when coppying roi.
			roiCopy.setName( name ); // else the name has " copy" in the end of it.

			gdt.inputComputationSequence.addROI( roiCopy );			
		}
		
		gdt.detectionResult = waveletCore.computeDetection( 
				displayBinaryImageCheckBox.isSelected(), UDWTScaleArrayList , 
				gdt.inputComputationSequence , detectNegative , computeWATwithROI , force2DWaveletsFor2D );
				
		if ( displayBinaryImageCheckBox.isSelected() )
		{
			Icy.getMainInterface().addSequence( waveletCore.getBinarySequence() );		
		}
		
	}

	JButton addScaleButton;
	JButton removeScaleButton;
	JPanel scalePanel;
	JScrollPane scrollPane;

	JRadioButton detectNegativeRadio = new JRadioButton("Detect dark spot over bright blackground");
	JRadioButton detectPositiveRadio = new JRadioButton("Detect bright spot over dark blackground");
	ButtonGroup buttonGroup = new ButtonGroup();	
	JCheckBox computeWATwithROICheckBox = new JCheckBox("compute Wat considering ROIs");
	JPanel examplePanel;
	JPanel positiveExamplePanel;
	JCheckBox displayBinaryImageCheckBox = new JCheckBox( "Display binary image (before filtering)" , false );
	JCheckBox use2DWaveletsFor3DImages = new JCheckBox( "Force use of 2D Wavelets for 3D" , false );
	
	
	JPanel getConfigurationPanel()
	{
		use2DWaveletsFor3DImages.setToolTipText("If enabled, each images of a 3D stack will be computed separatly by a 2D wavelet. This helps to bypass the scale size limitation due to a too small number of z in the image.");
		//use2DWaveletsFor3DImages.setEnabled( false );
		JPanel configurationPanel = new JPanel();
		configurationPanel.setLayout( new BorderLayout() );
		addScaleButton = new JButton("Add scale");
		addScaleButton.addActionListener( this );
		removeScaleButton = new JButton("Remove scale");
		removeScaleButton.addActionListener( this );
		
		buttonGroup.add( detectNegativeRadio );
		buttonGroup.add( detectPositiveRadio );
		detectNegativeRadio.setSelected( true );
		detectNegativeRadio.addActionListener( this );
		detectPositiveRadio.addActionListener( this );
		
		configurationPanel.add( GuiUtil.besidesPanel( removeScaleButton , addScaleButton ) , BorderLayout.SOUTH );
		
		scalePanel = new JPanel();
		scalePanel.setLayout( new BoxLayout( scalePanel , BoxLayout.PAGE_AXIS ) );
		scalePanel.setBorder( new TitledBorder( "Size of spots to detect: (scale and sensitivity)" ) );
		
		scrollPane = new JScrollPane( scalePanel );

		addScalePanel(false);
		addScalePanel(true);
		addScalePanel(false);

		examplePanel = new JPanel();
		examplePanel.setLayout( new BoxLayout( examplePanel , BoxLayout.PAGE_AXIS ) );
		examplePanel.setBorder( new TitledBorder( "Examples of input image that would use this setting:" ) );
		
		refreshExamplePanel();
		
		JPanel negativePositivePanel = new JPanel();
		negativePositivePanel.setLayout( new BoxLayout( negativePositivePanel , BoxLayout.PAGE_AXIS ) );

		negativePositivePanel.add( Box.createVerticalStrut( 5 ) );
		negativePositivePanel.add( GuiUtil.createLineBoxPanel( Box.createHorizontalGlue() , detectPositiveRadio , Box.createHorizontalGlue() ) );
		negativePositivePanel.add( GuiUtil.createLineBoxPanel( Box.createHorizontalGlue() , detectNegativeRadio , Box.createHorizontalGlue() ) );		
		negativePositivePanel.add( Box.createVerticalStrut( 5 ) );
		negativePositivePanel.add( GuiUtil.createLineBoxPanel( examplePanel ) );
		negativePositivePanel.add( GuiUtil.createLineBoxPanel( displayBinaryImageCheckBox ) );
		negativePositivePanel.add( GuiUtil.createLineBoxPanel( use2DWaveletsFor3DImages ) );		
		negativePositivePanel.add( GuiUtil.createLineBoxPanel( computeWATwithROICheckBox ) );
		negativePositivePanel.add( Box.createVerticalStrut( 10 ) );
		
		configurationPanel.add( negativePositivePanel , BorderLayout.NORTH );
		configurationPanel.add( scrollPane , BorderLayout.CENTER );
		
		return configurationPanel;
		
	}
	
	private void refreshExamplePanel() {
		
		examplePanel.removeAll();
		
		if ( detectNegativeRadio.isSelected() )
		{
			examplePanel.add( GuiUtil.createLineBoxPanel( 		
					Box.createHorizontalGlue(),
					getIcon("plugins/fab/spotDetector/detector/neg1.png"),
					Box.createHorizontalStrut( 5 ),
					getIcon("plugins/fab/spotDetector/detector/neg2.png"),
					Box.createHorizontalStrut( 5 ),
					getIcon("plugins/fab/spotDetector/detector/neg3.png"),
					Box.createHorizontalGlue()
			) );	
		}else
		{
			examplePanel.add( GuiUtil.createLineBoxPanel( 	
					Box.createHorizontalGlue(),
					getIcon("plugins/fab/spotDetector/detector/pos1.png"),
					Box.createHorizontalGlue()
			) );	
		}
		examplePanel.add( Box.createVerticalStrut( 5 ) );
		if ( examplePanel.getParent()!=null )
		{
		examplePanel.getParent().validate();
		}
	}


	private JLabel getIcon( String name )
	{
		ImageIcon icon = ResourceUtil.getImageIcon( ImageUtil.loadImage( getResourceAsStream( name ) ) );		
		JLabel label = new JLabel( icon );
		label.setSize( 64 , 64 );
		label.setPreferredSize( new Dimension( 64 , 64 ) );
		
		return label;
	}
	
	private void addScalePanel( boolean enabled ) {
				
		UDWTScale scale = new UDWTScale( UDWTScaleArrayList.size()+1 , enabled , 100 );
		UDWTScaleArrayList.add( scale );
		rebuildScalePanel();
	}

	private void rebuildScalePanel()
	{
		scalePanel.removeAll();
		scalePanel.add( UDWTScale.getHeaderPanel() );		
		for ( UDWTScale b3Scale : UDWTScaleArrayList )
		{
			scalePanel.add( b3Scale.getPanel() );
		}
		scalePanel.add( Box.createVerticalGlue() );
		scalePanel.repaint();
		scrollPane.validate();

	}
	
	private void removeScalePanel() {
		
		if ( UDWTScaleArrayList.size() == 0 ) return;
		UDWTScaleArrayList.remove( UDWTScaleArrayList.size()-1 );
		rebuildScalePanel();
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == detectNegativeRadio || e.getSource() == detectPositiveRadio )
		{
			refreshExamplePanel();	
		}
		
		if ( e.getSource() == addScaleButton )
		{
			addScalePanel( true );	
		}

		if ( e.getSource() == removeScaleButton )
		{
			removeScalePanel();
		}
	}

	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {
		
		int row = page.getRows();
		XLSUtil.setCellString( page , 0, row , "Detector:" );		
		XLSUtil.setCellString( page , 1, row , "UDWT Wavelet Detector" );
		//xlsManager.setLabel( 0 , row , "Detector: " );
		//xlsManager.setLabel( 1 , row , "UDWT Wavelet Detector" );
		row++;
		XLSUtil.setCellString( page , 0, row , "Parameters:" );
		//xlsManager.setLabel( 0 , row , "Parameters:" );
		row++;
		
		for ( UDWTScale b3Scale : UDWTScaleArrayList )
		{
			XLSUtil.setCellString( page, 0 , row , "Scale " + b3Scale.scaleNumber );
			//xlsManager.setLabel( 0 , row , "Scale " + b3Scale.scaleNumber  );
			if ( b3Scale.isEnabled() )
			{
				XLSUtil.setCellString( page, 1 , row , "Enabled" );
				//xlsManager.setLabel( 1 , row , "Enabled" );
			}else
			{
				XLSUtil.setCellString( page, 1 , row , "Disabled" );
				//xlsManager.setLabel( 1 , row , "Disabled" );
			}
			XLSUtil.setCellString( page, 2 , row , "Threshold:" );
			//xlsManager.setLabel( 2 , row , "Threshold:" );
			XLSUtil.setCellNumber( page, 3 , row , b3Scale.getThreshold() );
			//xlsManager.setNumber( 3 , row , b3Scale.getThreshold() );
			
			row++;
		}

		B3SplineUDWT waveletTransform = new B3SplineUDWT();
		if ( waveletTransform.isNumberOfScaleOkForImage3D( gdt.inputSequence.getSizeX(),
				gdt.inputSequence.getSizeY(), gdt.inputSequence.getSizeZ(), waveletCore.getNumberOfMaxEnabledScale() ) )
		{
			XLSUtil.setCellString( page, 0 , row , "ERROR : Scale configuration error" );
			//xlsManager.setLabel( 0 , row , "ERROR : Scale configuration error" );
			row++;
			int minSize = waveletTransform.getMinSize( waveletCore.getNumberOfMaxEnabledScale() );
			XLSUtil.setCellString( page, 0 , row , "Scales selected needs an image of size " + minSize + "x"+ minSize + "x"+ minSize + " image" );
			//xlsManager.setLabel( 0 , row , "Scales selected needs an image of size " + minSize + "x"+ minSize + "x"+ minSize + " image" );
			row++;
			XLSUtil.setCellString( page, 0 , row , "Input Image size is: " + gdt.inputSequence.getSizeX() + "x"+ gdt.inputSequence.getSizeY() + "x"+ gdt.inputSequence.getSizeZ() + " image" );
			//xlsManager.setLabel( 0 , row , "Input Image size is: " + gdt.inputSequence.getSizeX() + "x"+ gdt.inputSequence.getSizeY() + "x"+ gdt.inputSequence.getSizeZ() + " image" );
		}
		
	}




	
}
