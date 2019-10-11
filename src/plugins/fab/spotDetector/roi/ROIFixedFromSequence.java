package plugins.fab.spotDetector.roi;

import icy.file.xls.XlsManager;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.main.MainAdapter;
import icy.gui.main.MainEvent;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI2DRectangle;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import jxl.write.WritableSheet;

import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;

public class ROIFixedFromSequence extends ROIDetectionAbstract implements ActionListener {

	JLabel currentSequenceNameLabel = new JLabel();	
	Color labelDefaultColor = null;
	JButton loadROIButton = new JButton("Click to load reference ROIs from focused Sequence.");
	
	public ROIFixedFromSequence()
	{
		roiListStored = new ArrayList<ROI2D>();
		
		getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
		getPanel().setBorder( new TitledBorder( "use same ROI for all sequences" ) );

		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Click on the button to copy the ROIs") ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("from the current focused sequence:") ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("** WARNING: existing ROIs") ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("in batch files will be replaced") ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("by the reference ROI**") ) );
		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
		getPanel().add( GuiUtil.createLineBoxPanel( currentSequenceNameLabel ) );
		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
		getPanel().add( GuiUtil.createLineBoxPanel( loadROIButton ) );
		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );

		refreshCurrentSequenceLabel();

		loadROIButton.addActionListener( this );
		
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
		
		ArrayList<ROI2D> roiArrayList;
		
		gdt.inputSequence.removeAllROI();		
		for ( ROI roi : roiListStored )
		{
			gdt.inputSequence.addROI(roi);
		}
		
		roiArrayList = gdt.inputSequence.getROI2Ds();

		// remove detection if they are in a black ROI

		ArrayList<DetectionSpot> cloneDetectionList = new ArrayList<DetectionSpot>(gdt.detectionResult);
		for ( ROI2D roi : roiArrayList )
		{			
			if ( roi.getColor().getRed() == 0 && roi.getColor().getGreen() == 0 && roi.getColor().getBlue() == 0) // roi is black
			{
				for ( DetectionSpot detectionSpot : cloneDetectionList )
				{
					if ( roi.contains( detectionSpot.getMassCenter().x , detectionSpot.getMassCenter().y ) )
					{
						gdt.detectionResult.remove( detectionSpot );
					}
				}
			}
		}
		
		// create a hashMap with the detections binded to ROI
		
		HashMap<ROI2D, ArrayList<DetectionSpot>> ROI2Detection = new HashMap<ROI2D, ArrayList<DetectionSpot>>();
				
		// create a fake ROI taking all the sequence if no existing ROI on sequence.
		if ( roiArrayList.size() == 0 )
		{
			ROI2Detection.put( 
					new ROI2DRectangle( new Point2D.Double(0,0),
							new Point2D.Double( gdt.inputSequence.getWidth() , 
									gdt.inputSequence.getHeight() ), 
									false ) ,
									(ArrayList<DetectionSpot>)gdt.detectionResult.clone() );
		}else
		{ // there is ROI is the sequence
			
			// setup hashMap
			for ( ROI2D roi : roiArrayList ){
				ROI2Detection.put( roi , new ArrayList<DetectionSpot>() );
			}
			
			// fill hashMap
			for ( ROI2D roi : roiArrayList ){
				for ( DetectionSpot spot : gdt.detectionResult )
				{
					if ( roi.contains( spot.getMassCenter().x , spot.getMassCenter().y ) )
					{
						ROI2Detection.get(roi).add( spot );
					}
				}
			}
		}
		
		// rebuild the detectionResult with what has been found in ROIs
		
		gdt.detectionResult.clear();
//		Set<Entry<ROI2D, ArrayList<DetectionSpot>>> roiList = ROI2Detection.keySet();
		for ( ROI2D roi : ROI2Detection.keySet() )
		{
			for ( DetectionSpot ds : ROI2Detection.get(roi) )
			{
				gdt.detectionResult.add( ds );
			}
		}
		
		
		// set detection with ROI
		
		gdt.roi2detection = ROI2Detection;
		
		
	}

	ArrayList<ROI2D> roiListStored; 

	@Override
	public void actionPerformed(ActionEvent e) {

		Sequence sequence = Icy.getMainInterface().getFocusedSequence();
		if ( sequence == null )
		{
			new FailedAnnounceFrame("No sequence loaded");
			return;
		}
		
		roiListStored = sequence.getROI2Ds();
		
		if ( roiListStored.size() == 0 )
		{
			new FailedAnnounceFrame("No ROI found in the focused sequence. ("+sequence.getName()+")");
			return;
		}
		
		loadROIButton.setText( "" + roiListStored.size() + " ROI(s) Loaded." );
		
		
	}


	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {


		ROI_XLS_Common_Saver.saveXLS( page, gdt , "ROI From Sequence module");
		
	}
}
