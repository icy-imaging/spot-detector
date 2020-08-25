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
package plugins.fab.spotDetector.roi;

import icy.file.xls.XlsManager;
import icy.gui.main.MainAdapter;
import icy.gui.main.MainEvent;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.roi.ROI2D;
import icy.roi.ROI2DRectangle;
import icy.sequence.Sequence;
import icy.util.XMLUtil;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import jxl.write.WritableSheet;

import ome.xml.model.ROI;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;

public class ROIFromSequence extends ROIDetectionAbstract
{
	JLabel currentSequenceNameLabel = new JLabel();
	Color labelDefaultColor = null;

	public ROIFromSequence()
	{
		//getPanel().add(new JLabel("from sequence"));

		//getPanel().add(new JLabel("Existing ROIs are used to perform the computation"));

		getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
		getPanel().setBorder( new TitledBorder( "use ROI in current sequence" ) );

		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Current sequence input:") ) );
		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
		getPanel().add( GuiUtil.createLineBoxPanel( currentSequenceNameLabel ) );
		getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 30 ) ) );
		getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Tip: Remove detection areas with black rois") ) );
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

		ArrayList<ROI2D> roiArrayList;

		roiArrayList = gdt.inputSequence.getROI2Ds();

		ArrayList<ROI2D> roiArrayListCopy = new ArrayList<ROI2D>( roiArrayList );
		for ( ROI2D roi : roiArrayListCopy ){
			if ( roi.getName().startsWith("spot")) roiArrayList.remove( roi );
		}

//		if ( roi.getName().startsWith("spot") ) continue;


		// remove detection if they are in a black ROI

		ArrayList<DetectionSpot> cloneDetectionList = new ArrayList<DetectionSpot>(gdt.detectionResult);
		for ( ROI2D roi : roiArrayList )
		{
			if ( roi.getColor().getRed() == 0 && roi.getColor().getGreen() == 0 && roi.getColor().getBlue() == 0) // roi is black
			{
				for ( DetectionSpot detectionSpot : cloneDetectionList )
				{
					if ( roi.contains( detectionSpot.getMassCenter().x , detectionSpot.getMassCenter().y, detectionSpot.getMassCenter().z,
							detectionSpot.getT() , -1 ) )
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
					if ( roi.contains( spot.getMassCenter().x , spot.getMassCenter().y, spot.getMassCenter().z,
							spot.getT() , -1 ) )
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

	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {

		ROI_XLS_Common_Saver.saveXLS( page , gdt , "ROI From Sequence module");

	}



}





























