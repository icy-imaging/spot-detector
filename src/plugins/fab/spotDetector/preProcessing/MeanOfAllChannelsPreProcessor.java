package plugins.fab.spotDetector.preProcessing;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.util.XLSUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import jxl.write.WritableSheet;
import plugins.fab.spotDetector.GlobalDetectionToken;

public class MeanOfAllChannelsPreProcessor extends PreProcessingAbstract {

	public MeanOfAllChannelsPreProcessor() {

		getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
    	getPanel().setBorder( new TitledBorder( "Mean (Average) of all channels" ) );    	
    	
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("The input creates the average of all channels") ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("and keep z and t image location") ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );    	
   				
	}

	@Override
	public void process(GlobalDetectionToken gdt) {
		
		gdt.inputComputationSequence = new Sequence();
		
		for ( int t = 0 ; t < gdt.inputSequence.getSizeT() ; t++ )
		for ( int z = 0 ; z < gdt.inputSequence.getSizeZ(t) ; z ++ )
		{		
			IcyBufferedImage image = gdt.inputSequence.getImage( t , z );
			IcyBufferedImage averageImage = new IcyBufferedImage( image.getWidth() , image.getHeight() , 1 , DataType.DOUBLE );
			double[] result = new double[ image.getSizeX() * image.getSizeY() ]; 
			
			double [][] imageArray = new double[ image.getSizeC() ][];
					
			for ( int c = 0 ; c < image.getSizeC() ; c++ )
			{
				imageArray[c] = Array1DUtil.arrayToDoubleArray( image.getDataCopyXY( 0 ) , image.isSignedDataType() );				
			}
			
			for ( int i = 0 ; i < image.getSizeX() * image.getSizeY()  ; i++ )
			{
				result[i] = 0;
				for ( int c = 0 ; c < image.getSizeC() ; c++ )
				{
					result[i] += imageArray[c][i];				
				}
				result[i] /= (double)image.getSizeC();
			}
			
			averageImage.setDataXYAsDouble( 0 , result );			
			
			gdt.inputComputationSequence.setImage( t, z, averageImage );
			
			//Icy.addSequence( gdt.inputComputationSequence );			
		}
		
		
	}

	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {

		int row = page.getRows();
		XLSUtil.setCellString( page, 0, row, "Pre processor :" );
		XLSUtil.setCellString( page, 1, row, "Average of all channels :" );
//		xlsManager.setLabel( 0 , row , "Pre processor :" );
//		xlsManager.setLabel( 1 , row , "Average of all channels" );
		
	}
	
}
