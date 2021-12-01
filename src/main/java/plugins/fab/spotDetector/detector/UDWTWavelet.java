package plugins.fab.spotDetector.detector;

import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.thread.ThreadUtil;

import java.util.ArrayList;

import plugins.fab.spotDetector.DetectionSpot;

public class UDWTWavelet {

	private ArrayList<DetectionSpot> detectionResult;
	
	public ArrayList<DetectionSpot> getDetectionResult() {
		return detectionResult;
	}
	

	
	public void detect( 
			Sequence inputSequence,
			boolean detectNegative,
			boolean useROIforWATComputation,
			final double[] scaleParameters
			) throws InterruptedException
	{
		
		UDWTWaveletCore waveletCore = new UDWTWaveletCore( );
		
		ArrayList<DetectionSpot> detectionList = new ArrayList<DetectionSpot>();

		Sequence inputComputationSequence = SequenceUtil.getCopy( inputSequence );

		// copy ROIs from inputSequence to inputComputationSequence		
		for ( ROI roi : inputSequence.getROIs() )
		{			
			
			inputComputationSequence.addROI( roi.getCopy() );
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
		
		final ArrayList<UDWTScale> UDWTScaleArrayList = new ArrayList<UDWTScale>();

//		System.out.println("Block debug.");

		ThreadUtil.invokeNow( new Runnable() {

			@Override
			public void run() {
				
				for ( int i = 0 ; i< scaleParameters.length ; i++ )
				{
					boolean scaleEnabled = true;
					if ( scaleParameters[i] == 0 ) scaleEnabled = false;
					
					UDWTScale scale = new UDWTScale(
							i+1 , scaleEnabled , scaleParameters[i] );
					UDWTScaleArrayList.add( scale );
				}
				
				
//				for ( int i = 0 ; i< 5 ; i++ )
//				{
//					//System.out.println( "Scale enabled #" +i +" : " + useScaleBoolean[i].getValue() );
//					UDWTScale scale = new UDWTScale( i+1 , useScaleBoolean[i].getValue() , scaleParameter[i].getValue() );
//					UDWTScaleArrayList.add( scale );
//				}
			}

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
				UDWTScaleArrayList ,
				inputComputationSequence ,
				detectNegative ,
				useROIforWATComputation );
				
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
