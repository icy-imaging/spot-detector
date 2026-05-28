/*
 * Copyright (c) 2010-2026. Institut Pasteur.
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

package plugins.fab.spotDetector.detector;

import fr.icy.model.roi.ROI;
import fr.icy.model.sequence.Sequence;
import fr.icy.model.sequence.SequenceUtil;
import fr.icy.system.thread.ThreadUtil;
import plugins.fab.spotDetector.DetectionSpot;

import java.util.ArrayList;

public class UDWTWavelet {
    private ArrayList<DetectionSpot> detectionResult;

    public ArrayList<DetectionSpot> getDetectionResult() {
        return detectionResult;
    }

    public void detect(
            final Sequence inputSequence,
            final boolean detectNegative,
            final boolean useROIforWATComputation,
            final double[] scaleParameters
    ) throws InterruptedException {

        final UDWTWaveletCore waveletCore = new UDWTWaveletCore();

        //ArrayList<DetectionSpot> detectionList = new ArrayList<>();

        final Sequence inputComputationSequence = SequenceUtil.getCopy(inputSequence);

        // copy ROIs from inputSequence to inputComputationSequence
        for (final ROI roi : inputSequence.getROIs()) {

            inputComputationSequence.addROI(roi.getCopy());
        }

//		if ( inputSequenceROIVar.getValue() !=null )
//		{
//			for ( ROI2D roi : inputSequenceROIVar.getValue().getROI2Ds() )
//			{
//				System.out.println("roi:"+roi);
//				String name = roi.getName();
//
//				ROI2D roiCopy = (ROI2D) roi.getCopy();
//				roi.setName( name ); // because of the bug when coppying roi.
//				roiCopy.setName( name ); // else the name has " copy" in the end of it.
//
//				inputComputationSequence.addROI( roiCopy );
//			}
//		}
//		
        //MessageDialog.showDialog(message, messageType)

        // create UDWTScale Parameter list.

        final ArrayList<UDWTScale> UDWTScaleArrayList = new ArrayList<>();

//		System.out.println("Block debug.");

        ThreadUtil.invokeNow(() -> {

            for (int i = 0; i < scaleParameters.length; i++) {
                final boolean scaleEnabled = scaleParameters[i] != 0;

                final UDWTScale scale = new UDWTScale(
                        i + 1, scaleEnabled, scaleParameters[i]);
                UDWTScaleArrayList.add(scale);
            }

//				for ( int i = 0 ; i< 5 ; i++ )
//				{
//					//System.out.println( "Scale enabled #" +i +" : " + useScaleBoolean[i].getValue() );
//					UDWTScale scale = new UDWTScale( i+1 , useScaleBoolean[i].getValue() , scaleParameter[i].getValue() );
//					UDWTScaleArrayList.add( scale );
//				}
        });

        //inputSequence.getImage( 0 , 0 ).getData(x, y, c)
//		ROI2DRectangle roi = new ROI2DRectangle( 
//				new Point2D.Double( 2 , 2 ),
//				new Point2D.Double( 2 , 2 ),
//				false
//				);


        // Perform detection.

        //ArrayList<DetectionSpot> detectionResult;
        this.detectionResult = waveletCore.computeDetection(
                true,
                UDWTScaleArrayList,
                inputComputationSequence,
                detectNegative,
                useROIforWATComputation
        );

        // set outputs.

        // binary output: propagate ROIs.
//		{
//			Sequence binarySequence = waveletCore.getBinarySequence();
//			
//			if ( inputSequenceROIVar.getValue() !=null )
//			{
//				for ( ROI2D roi : inputSequenceROIVar.getValue().getROI2Ds() )
//				{
//					System.out.println("roi:"+roi);
//					String name = roi.getName();
//
//					ROI2D roiCopy = (ROI2D) roi.getCopy();
//					roi.setName( name ); // because of the bug when coppying roi.
//					roiCopy.setName( name ); // else the name has " copy" in the end of it.
//
//					binarySequence.addROI( roiCopy );
//				}
//			}
//
//			binarySpotSequence.setValue( binarySequence );
//		}
    }
}
