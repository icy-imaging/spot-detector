package plugins.fab.spotDetector.display;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.color.ColorSpace;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import plugins.fab.spotDetector.DetectionSpot;
import plugins.kernel.canvas.VtkCanvas;
import vtk.vtkActor;
import vtk.vtkPolyDataMapper;
import vtk.vtkRenderer;
import vtk.vtkSphereSource;
import icy.canvas.Canvas2D;
import icy.canvas.Canvas3D;
import icy.canvas.IcyCanvas;
import icy.gui.util.GuiUtil;
import icy.painter.Painter;
import icy.roi.ROI2D;
import icy.sequence.Sequence;

public class BasicDetectionPainter implements Painter {

	boolean projectDetection = false;
	boolean displayDetectionMark = true;

	boolean displayDetectionIndex = false;
	int fontLabelSize = 14;

	Color color = Color.red;
	HashMap<ROI2D, ArrayList<DetectionSpot>> detectionsHashMap;
	boolean displayROINumber = false;
	boolean displayDetectionNumberOverROI = false;

	final int FONT_SIZE = 16;

	@Override
	public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void mouseDrag(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		//boolean projectDetection ;//= projectDetectionMark.isSelected();

		if ( canvas instanceof Canvas2D )
		{


			int currentT = canvas.getT();
			int currentZ = canvas.getZ();

			if ( displayDetectionMark )
			{
				g.setFont( new Font( "Arial" , Font.BOLD, fontLabelSize ) );

				g.setColor( color );
				Ellipse2D ellipse = new Ellipse2D.Double( 0,0,0,0 );

				for ( ROI2D roi : detectionsHashMap.keySet() ){

					ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get( roi );

					for ( DetectionSpot detectionSpot : detectionArrayList )
					{
						// FIXME : profiter de la structure d'un groupe de point en hashmap pour trouver directement les points à afficher !

						if ( currentT != detectionSpot.getT() ) continue;
						if ( !projectDetection )
						{
							if ( currentZ != (int)detectionSpot.getMassCenter().z ) continue;
						}
						double x = detectionSpot.getMassCenter().x;
						double y = detectionSpot.getMassCenter().y;

						ellipse.setFrame( x-3, y-3, 7, 7 );

						g.draw( ellipse );

						if ( displayDetectionIndex )
						{
							g.drawString( "" + detectionArrayList.indexOf( detectionSpot ), (int)x , (int)y );
						}

					}
				}
			}

			// display detection Label
//			System.out.println("display detection Label " + displayDetectionIndex );
//			if ( displayDetectionIndex )
//			{
//				g.setFont( new Font( "Arial" , Font.BOLD, fontLabelSize ) );
//				g.setColor( color );
//
//				for ( ROI2D roi : detectionsHashMap.keySet() ){
//
//					ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get( roi );
//
//					for ( DetectionSpot detectionSpot : detectionArrayList )
//					{
//
//						if ( currentT != detectionSpot.getT() ) continue;
//						if ( !projectDetection )
//						{
//							if ( currentZ != (int)detectionSpot.getMassCenter().z ) continue;
//						}
//						double x = detectionSpot.getMassCenter().x;
//						double y = detectionSpot.getMassCenter().y;
//
//						g.drawString( "" + detectionArrayList.indexOf( detectionSpot ), (int)x , (int)y );
//System.out.println("writing");
//					}
//				}
//			}

			// draw text of ROI

			for ( ROI2D roi : sequence.getROI2Ds() )
			{
				if ( roi.getName().startsWith("spot") ) continue;

				ArrayList<DisplayTxt> displayTxtList = new ArrayList<BasicDetectionPainter.DisplayTxt>();

				int fontSize = (int)ROI2D.canvasToImageLogDeltaX(canvas, FONT_SIZE ) ;
				if ( fontSize < 1 ) fontSize = 1;
				Font font = new Font( "Arial" , Font.BOLD , fontSize );
				g.setFont( font );
				Rectangle2D oneLetterBound = GuiUtil.getStringBounds( g , font, "X" );
				float offsetY = 0;
				g.setColor( roi.getColor() );


				if ( displayROINumber )
				{

					String label = " ROI " + roi.getName() + " ";

					Rectangle2D labelBounds = GuiUtil.getStringBounds( g , font, label );

					displayTxtList.add( new DisplayTxt ( label, new Rectangle2D.Double(
							(float)(roi.getBounds2D().getCenterX() - labelBounds.getWidth()/2) ,
							(float)( roi.getBounds2D().getCenterY() + labelBounds.getHeight() ) ,
							labelBounds.getWidth(),
							labelBounds.getHeight()
							) ) );
					offsetY+=oneLetterBound.getHeight();
				}

				// display detection number
				if ( displayDetectionNumberOverROI )
				{
					ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get( roi );
					if ( detectionArrayList !=null )
					{
						int nbDetection = detectionArrayList.size();
						String detectionLabel = " nb detection: "+nbDetection+" ";
						Rectangle2D detectionLabelBounds = GuiUtil.getStringBounds( g , font, detectionLabel );

						displayTxtList.add( new DisplayTxt ( detectionLabel , new Rectangle2D.Double(
								(float)(roi.getBounds2D().getCenterX() - detectionLabelBounds.getWidth()/2),
								roi.getBounds2D().getCenterY() + offsetY + detectionLabelBounds.getHeight() ,
								detectionLabelBounds.getWidth(),
								detectionLabelBounds.getHeight() ) ) );

						offsetY+=oneLetterBound.getHeight();
					}
				}

				if ( roi.getColor().getRed() == 0 && roi.getColor().getGreen() == 0 && roi.getColor().getBlue()== 0 )
				{
					/*
					String removeLabel = " removed ";
					Rectangle2D removeLabelBounds = GuiUtil.getStringBounds( g , font, removeLabel );
					displayTxtList.add( new DisplayTxt ( removeLabel , new Rectangle2D.Double(
							(float)(roi.getBounds2D().getCenterX() - removeLabelBounds.getWidth()/2),
							roi.getBounds2D().getCenterY() + offsetY + removeLabelBounds.getHeight() ,
							removeLabelBounds.getWidth(),
							removeLabelBounds.getHeight() ) ) );

					offsetY+=oneLetterBound.getHeight();

					// fade out ROI.
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f ) );
					Color oldColor = g.getColor();
					g.setColor( Color.black );
					g.fill( (Shape) roi );
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f ) );
					g.setColor( oldColor );
					*/
					//						g.drawString( "removed" , (float)roi.getBounds2D().getCenterX() - xOffset , (float)roi.getBounds2D().getCenterY() +yOffset );
				}



				// display the background

				Rectangle2D blackBox = null;
				for ( DisplayTxt dt : displayTxtList )
				{
					if ( blackBox == null )
					{
						blackBox = new Rectangle2D.Double( dt.bounds.getX() , dt.bounds.getY() , dt.bounds.getWidth() , dt.bounds.getHeight() ) ;
					}else
					{
						blackBox = blackBox.createUnion( dt.bounds );
					}
				}

				if ( blackBox != null )
				{
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f ) );
					Color oldColor = g.getColor();
					float intensity = roi.getColor().getRed()/255f + roi.getColor().getGreen()/255f + roi.getColor().getBlue()/255f;
					intensity/=3f;
					intensity = 1-intensity;
					g.setColor( new Color( intensity , intensity , intensity ) );

					g.translate( 0 , -oneLetterBound.getHeight()*0.8 );
					g.fill( blackBox );
					g.translate( 0 , +oneLetterBound.getHeight()*0.8 );
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f ) );
					g.setColor( oldColor );
				}

				// display the text
				g.setColor( roi.getColor() );

				for ( DisplayTxt dt : displayTxtList )
				{
					g.drawString( dt.string , (float)dt.bounds.getX() , (float) dt.bounds.getY() );
				}

			}


		}

		if (canvas instanceof VtkCanvas )
		{
			// 3D canvas
			final VtkCanvas canvas3d = (VtkCanvas) canvas;
// FIXME : in case of several 3D viewer ?
			if (!initialized3D)
			{
				init3D(canvas3d.getRenderer());
				initialized3D = true;
			}
		}

	}
	private boolean initialized3D;

	class DisplayTxt
	{
		String string;
		Rectangle2D bounds;
		public DisplayTxt( String string , Rectangle2D bounds ) {
			this.string = string;
			this.bounds = bounds;
		}
	}

    // init vtk objects
    private void init3D(vtkRenderer renderer)
    {

//			g.setColor( colorChooserButton.getColor() );
//			Ellipse2D ellipse = new Ellipse2D.Double( 0,0,0,0 );

			for ( ROI2D roi : detectionsHashMap.keySet() ){

				ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get( roi );

				for ( DetectionSpot detectionSpot : detectionArrayList )
				{
					// FIXME : profiter de la structure d'un groupe de point en hashmap pour trouver directement les points à afficher !

//					if ( currentT != detectionSpot.getT() ) continue;
//					if ( currentZ != detectionSpot.getMassCenter().z ) continue;

					double x = detectionSpot.getMassCenter().x;
					double y = detectionSpot.getMassCenter().y;
					double z = detectionSpot.getMassCenter().z;
					double[] center = {x,y,z};

			        // source
			        final vtkSphereSource sphere = new vtkSphereSource();
			        sphere.SetCenter( center );
			        sphere.SetRadius(1);
			        sphere.SetThetaResolution(5);
			        sphere.SetPhiResolution(5);

			        // mapper
			        final vtkPolyDataMapper map = new vtkPolyDataMapper();
			        map.SetInputData(sphere.GetOutput());

			        // actor
			        final vtkActor aSphere = new vtkActor();
			        aSphere.SetMapper(map);
			        aSphere.GetProperty().SetColor(0, 1, 0);

			        renderer.AddActor(aSphere);
				}
			}



    }

}
