package plugins.fab.spotDetector.filtering;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import icy.gui.component.ComponentUtil;
import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import jxl.write.WritableSheet;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.fab.spotDetector.Point3D;

public class deClusterFiltering extends FilteringDetectionAbstract {

	IntervalSliderValue intervalSize = new IntervalSliderValue( 0 , 300 , 0 , 300 , "Range of accepted objects (in pixels) (not used)" );
	JTextField spotRaySizeTextBox = new JTextField("4");

    public deClusterFiltering()
    {        
        getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
        
        ComponentUtil.setFixedHeight( intervalSize , 40 );
        
        getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
        getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("spot ray:") , spotRaySizeTextBox ) );
        getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
        getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 50 ) ) );
        getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
        
    }

	private double getSurfaceOfOneSpot( double spotRay ) {

		int bound = (int) Math.ceil(spotRay+1);
		int surface = 0;

		for ( int x = -bound ; x<= bound ; x++ )
		{
			for ( int y = -bound ; y<= bound ; y++ )
			{
				Point2D p = new Point2D.Double(x,y);
				if ( p.distance( 0 , 0 ) < spotRay )
				{
					surface++;
				}
			}			
		}
		
		return surface;
	}
    
	// two spots right next to each other offset by 1 pixel.
	private double getSurfaceOfTwoContiguousSpot( double spotRay ) {

		int bound = (int) Math.ceil(spotRay+1)+1;
		int surface = 0;

		for ( int x = -bound ; x<= bound ; x++ )
		{
			for ( int y = -bound ; y<= bound ; y++ )
			{
				Point2D p = new Point2D.Double(x,y);				
				if ( p.distance( 0 , 0 ) < spotRay 
						||
						p.distance( 1 , 0 ) < spotRay
						)
				{
					surface++;
				}
			}			
		}
		
		return surface;
	}
	
	private void fit(DetectionSpot detectionSpot,
			ArrayList<DetectionSpot> detectionList )
	{
		detectionSpot.points.get( 0 );
	}
	
	int nbSplitDetection = 0;
	
	private void filterTest(DetectionSpot detectionSpot,
			ArrayList<DetectionSpot> detectionList , GlobalDetectionToken gdt ) {
		
		
		//System.out.println("e");
		
//		if ( detectionSpot.points.size() < 2 ) // too small
//		{
//			//System.out.println("Remove detection");
//			detectionList.remove( detectionSpot );			
//			
//		}
		
		//if ( detectionSpot.points.size() > surfaceOfOneSpot ) // a redecouper.
		
		double max = 0;
		detectionSpot.getT();
		
		for ( Point3D p : detectionSpot.points )
		{
			double value = gdt.inputComputationSequence.getImage( detectionSpot.getT() , (int)p.z ).getData( (int) p.x , (int) p.y , 0 );
			if ( value > max ) max = value;
		}
			
		if (max >80 )
		{
			nbSplitDetection++;
			DetectionSpot newSpot = new DetectionSpot(  );
			newSpot.points.add( new Point3D( detectionSpot.getMassCenter().x +1 , detectionSpot.getMassCenter().y +1, detectionSpot.getMassCenter().z ) );
			newSpot.setT( detectionSpot.getT() );
			newSpot.computeMassCenter();
			detectionList.add( newSpot );
		}
//			
//		
//		{
//			// System.out.println("Split spot");			
//			// change existing detection
//			nbSplitDetection++;
//	
//			Point3D massCenter = new Point3D( detectionSpot.getMassCenter().x , detectionSpot.getMassCenter().x );		
//			
//			// shift mass center
//			
//			massCenter.x ++;
//			massCenter.y ++;
//			
//			// new point			
//			
//			DetectionSpot newSpot = new DetectionSpot(  );
//			for ( int x = -1 ; x<=1 ; x++ )
//				for ( int y = -1 ; y<=1 ; y++ )
//				{
//					newSpot.points.add( new Point3D( x+ massCenter.x ,y+ massCenter.x ) );
//				}
//			detectionList.add( newSpot );
//			
//		}
		
		
		
		
	}
	
// old
	private void filterTest2(DetectionSpot detectionSpot,
			ArrayList<DetectionSpot> detectionList , int surfaceOfOneSpot , int diffSurface ) {
		
		
		//System.out.println("e");
		
		if ( detectionSpot.points.size() < 2 ) // too small
		{
			//System.out.println("Remove detection");
			detectionList.remove( detectionSpot );			
			
		}
		
		if ( detectionSpot.points.size() > surfaceOfOneSpot ) // a redecouper.
		{
			// System.out.println("Split spot");			
			// change existing detection
			nbSplitDetection++;
	
			Point3D massCenter = new Point3D( detectionSpot.getMassCenter().x , detectionSpot.getMassCenter().x );
			
			detectionSpot.points.clear();
			for ( int x = -1 ; x<=1 ; x++ )
				for ( int y = -1 ; y<=1 ; y++ )
				{
					detectionSpot.points.add( new Point3D( x + massCenter.x , y + massCenter.x ) );
				}			
			
			// shift mass center
			
			massCenter.x ++;
			massCenter.y ++;
			
			// new point			
			
			DetectionSpot newSpot = new DetectionSpot(  );
			for ( int x = -1 ; x<=1 ; x++ )
				for ( int y = -1 ; y<=1 ; y++ )
				{
					newSpot.points.add( new Point3D( x+ massCenter.x ,y+ massCenter.x ) );
				}
			detectionList.add( newSpot );
			
		}
		
		
		
		
	}
	
	
	
	@Override
	public void process(GlobalDetectionToken gdt) {

		System.out.println("Cluster filtering...");
		
		double spotRay = Double.parseDouble( spotRaySizeTextBox.getText() );
		
		// compute surface of 1 spot:
		
		int surfaceOfOneSpot = (int)getSurfaceOfOneSpot( spotRay );
		int surfaceOfTwoContiguousSpot = (int)getSurfaceOfTwoContiguousSpot( spotRay );
		
		System.out.println("surface of one spot: " + surfaceOfOneSpot );
		System.out.println("surface of 2 glued spots: " + surfaceOfTwoContiguousSpot );
		System.out.println("difference between the 2:" + (surfaceOfTwoContiguousSpot-surfaceOfOneSpot) );
		

		int highValue = (int) surfaceOfOneSpot;
		int lowValue = 2 ;
		
		nbSplitDetection = 0;
		// Filter the roi dataset
		for ( ROI roi : gdt.roi2detection.keySet() )
		{
			ArrayList<DetectionSpot> detectionList = gdt.roi2detection.get( roi );
			ArrayList<DetectionSpot> detectionListCopy = new ArrayList<DetectionSpot> ( detectionList );
			
			for ( DetectionSpot detectionSpot : detectionListCopy )
			{
				filterTest( detectionSpot , detectionList , gdt );
			}
			
		}
		System.out.println("Nombre de split phase 1: " + nbSplitDetection );
		
		nbSplitDetection = 0;
		// filter the detection gdt.detectionResult dataset
		{
			ArrayList<DetectionSpot> detectionList = gdt.detectionResult;
			ArrayList<DetectionSpot> detectionListCopy = new ArrayList<DetectionSpot> ( detectionList );
			
			for ( DetectionSpot detectionSpot : detectionListCopy )
			{
				filterTest( detectionSpot , detectionList , gdt );
			}			
		}
		System.out.println("Nombre de split phase 2: " + nbSplitDetection );
	

	}

	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {
		// TODO Auto-generated method stub
		
	}


	
	
}
