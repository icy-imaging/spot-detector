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

package plugins.fab.spotDetector.display;

import org.bioimageanalysis.icy.gui.GraphicsUtil;
import org.bioimageanalysis.icy.gui.canvas.Canvas2D;
import org.bioimageanalysis.icy.gui.canvas.IcyCanvas;
import org.bioimageanalysis.icy.gui.canvas.VtkCanvas;
import org.bioimageanalysis.icy.model.overlay.Overlay;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.jetbrains.annotations.Contract;
import plugins.fab.spotDetector.DetectionSpot;
import vtk.vtkActor;
import vtk.vtkPolyDataMapper;
import vtk.vtkRenderer;
import vtk.vtkSphereSource;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

public class BasicDetectionPainter extends Overlay {
    boolean projectDetection = false;
    boolean displayDetectionMark = true;

    boolean displayDetectionIndex = false;
    int fontLabelSize = 14;

    Color color = Color.red;
    HashMap<ROI, ArrayList<DetectionSpot>> detectionsHashMap;
    boolean displayROINumber = false;
    boolean displayDetectionNumberOverROI = false;

    final int FONT_SIZE = 16;

    public BasicDetectionPainter() {
        super("BasicDetectionPainter");
    }

    @Override
    public void paint(final Graphics2D g, final Sequence sequence, final IcyCanvas canvas) {
        //boolean projectDetection ;//= projectDetectionMark.isSelected();

        if (canvas instanceof Canvas2D) {
            final int currentT = canvas.getPositionT();
            final int currentZ = canvas.getPositionZ();

            if (displayDetectionMark) {
                g.setFont(new Font("Arial", Font.BOLD, fontLabelSize));

                g.setColor(color);
                final Ellipse2D ellipse = new Ellipse2D.Double(0, 0, 0, 0);

                for (final ROI roi : detectionsHashMap.keySet()) {

                    final ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get(roi);

                    for (final DetectionSpot detectionSpot : detectionArrayList) {
                        // FIXME : take advantage of the structure of a group of points in hashmap to find directly the points to display!

                        if (currentT != detectionSpot.getT()) continue;
                        if (!projectDetection) {
                            if (currentZ != (int) detectionSpot.getMassCenter().z) continue;
                        }
                        final double x = detectionSpot.getMassCenter().x;
                        final double y = detectionSpot.getMassCenter().y;

                        ellipse.setFrame(x - 3, y - 3, 7, 7);

                        g.draw(ellipse);

                        if (displayDetectionIndex) {
                            g.drawString("" + detectionArrayList.indexOf(detectionSpot), (int) x, (int) y);
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

            for (final ROI roi : sequence.getROIs()) {
                if (roi.getName().startsWith("spot")) continue;

                final ArrayList<DisplayTxt> displayTxtList = new ArrayList<>();

                int fontSize = (int) canvas.canvasToImageLogDeltaX(FONT_SIZE);
                if (fontSize < 1) fontSize = 1;
                final Font font = new Font("Arial", Font.BOLD, fontSize);
                g.setFont(font);
                final Rectangle2D oneLetterBound = GraphicsUtil.getStringBounds(g, font, "X");
                float offsetY = 0;
                g.setColor(roi.getColor());

                if (displayROINumber) {

                    final String label = " ROI " + roi.getName() + " ";

                    final Rectangle2D labelBounds = GraphicsUtil.getStringBounds(g, font, label);

                    displayTxtList.add(new DisplayTxt(label, new Rectangle2D.Double(
                            (float) (roi.getBounds5D().getCenterX() - labelBounds.getWidth() / 2),
                            (float) (roi.getBounds5D().getCenterY() + labelBounds.getHeight()),
                            labelBounds.getWidth(),
                            labelBounds.getHeight()
                    )));
                    offsetY += (float) oneLetterBound.getHeight();
                }

                // display detection number
                if (displayDetectionNumberOverROI) {
                    final ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get(roi);
                    if (detectionArrayList != null) {
                        final int nbDetection = detectionArrayList.size();
                        final String detectionLabel = " nb detection: " + nbDetection + " ";
                        final Rectangle2D detectionLabelBounds = GraphicsUtil.getStringBounds(g, font, detectionLabel);

                        displayTxtList.add(new DisplayTxt(detectionLabel, new Rectangle2D.Double(
                                (float) (roi.getBounds5D().getCenterX() - detectionLabelBounds.getWidth() / 2),
                                roi.getBounds5D().getCenterY() + offsetY + detectionLabelBounds.getHeight(),
                                detectionLabelBounds.getWidth(),
                                detectionLabelBounds.getHeight())));

                        offsetY += (float) oneLetterBound.getHeight();
                    }
                }

                //if (roi.getColor().getRed() == 0 && roi.getColor().getGreen() == 0 && roi.getColor().getBlue() == 0) {
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
                //}


                // display the background

                Rectangle2D blackBox = null;
                for (final DisplayTxt dt : displayTxtList) {
                    if (blackBox == null) {
                        blackBox = new Rectangle2D.Double(dt.bounds.getX(), dt.bounds.getY(), dt.bounds.getWidth(), dt.bounds.getHeight());
                    }
                    else {
                        blackBox = blackBox.createUnion(dt.bounds);
                    }
                }

                if (blackBox != null) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    final Color oldColor = g.getColor();
                    float intensity = roi.getColor().getRed() / 255f + roi.getColor().getGreen() / 255f + roi.getColor().getBlue() / 255f;
                    intensity /= 3f;
                    intensity = 1 - intensity;
                    g.setColor(new Color(intensity, intensity, intensity));

                    g.translate(0, -oneLetterBound.getHeight() * 0.8);
                    g.fill(blackBox);
                    g.translate(0, oneLetterBound.getHeight() * 0.8);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g.setColor(oldColor);
                }

                // display the text
                g.setColor(roi.getColor());

                for (final DisplayTxt dt : displayTxtList) {
                    g.drawString(dt.string, (float) dt.bounds.getX(), (float) dt.bounds.getY());
                }
            }
        }

        if (canvas instanceof final VtkCanvas canvas3d) {
            // 3D canvas
            // FIXME : in case of several 3D viewer ?
            if (!initialized3D) {
                init3D(canvas3d.getRenderer());
                initialized3D = true;
            }
        }

    }

    private boolean initialized3D;

    static class DisplayTxt {
        String string;
        Rectangle2D bounds;

        @Contract(pure = true)
        public DisplayTxt(final String string, final Rectangle2D bounds) {
            this.string = string;
            this.bounds = bounds;
        }
    }

    // init vtk objects
    private void init3D(final vtkRenderer renderer) {
//			g.setColor( colorChooserButton.getColor() );
//			Ellipse2D ellipse = new Ellipse2D.Double( 0,0,0,0 );

        for (final ROI roi : detectionsHashMap.keySet()) {
            final ArrayList<DetectionSpot> detectionArrayList = detectionsHashMap.get(roi);

            for (final DetectionSpot detectionSpot : detectionArrayList) {
                // FIXME : take advantage of the structure of a group of points in hashmap to find directly the points to display!

//					if ( currentT != detectionSpot.getT() ) continue;
//					if ( currentZ != detectionSpot.getMassCenter().z ) continue;

                final double x = detectionSpot.getMassCenter().x;
                final double y = detectionSpot.getMassCenter().y;
                final double z = detectionSpot.getMassCenter().z;
                final double[] center = {x, y, z};

                // source
                final vtkSphereSource sphere = new vtkSphereSource();
                sphere.SetCenter(center);
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
