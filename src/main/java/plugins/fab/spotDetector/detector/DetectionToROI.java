package plugins.fab.spotDetector.detector;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.type.point.Point5D;

import java.util.ArrayList;

import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.Point3D;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi3d.ROI3DArea;

public class DetectionToROI {

	public static ArrayList<ROI> convertDetectionToROI(ArrayList<DetectionSpot> detectionList )
	{
		ArrayList<ROI> roiList = new ArrayList<ROI>();
		
		for ( DetectionSpot spot : detectionList )
		{
			ROI roi = null;
			ROI3DArea roi3D = new ROI3DArea();
			
			if ( spot.points.size() == 0 ) continue;
			
			roi3D.beginUpdate();
			try
			{
				for ( Point3D p : spot.points )
				{

					roi3D.addPoint( (int)p.x , (int)p.y, (int)p.z );
				}
			}finally{
				roi3D.endUpdate();
			}
			
			if ( roi3D.getSizeZ() == 1 ) // check if the sizeZ is 1 and convert to ROI2D
			{
				int z = (int)roi3D.getPosition().getZ();
				BooleanMask2D mask = roi3D.getBooleanMask2D(z, true );				
				ROI2DArea roi2D = new ROI2DArea( mask );
				roi2D.setZ( roi3D.getPosition().z );				
				roi = roi2D;
			}else
			{
				roi = roi3D;
			}
			
			roiList.add( roi );
			// set t for ROI.
			roi.setPosition5D( new Point5D.Double(  
					roi.getPosition5D().getX(),
					roi.getPosition5D().getY(),
					roi.getPosition5D().getZ(),
					spot.getT(),
					roi.getPosition5D().getC() ) );
			roi.setName("spot #"+detectionList.indexOf( spot ));
		}		
		
		return roiList;
	}
	
}
