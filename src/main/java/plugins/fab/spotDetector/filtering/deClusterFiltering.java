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

package plugins.fab.spotDetector.filtering;

import org.apache.poi.ss.usermodel.Sheet;
import fr.icy.gui.GuiUtil;
import fr.icy.gui.component.ComponentUtil;
import fr.icy.model.roi.ROI;
import fr.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.fab.spotDetector.Point3D;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class deClusterFiltering extends FilteringDetectionAbstract {
    IntervalSliderValue intervalSize = new IntervalSliderValue(0, 300, 0, 300, "Range of accepted objects (in pixels) (not used)");
    JTextField spotRaySizeTextBox = new JTextField("4");

    public deClusterFiltering() {
        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));

        ComponentUtil.setFixedHeight(intervalSize, 40);

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("spot ray:"), spotRaySizeTextBox));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    private double getSurfaceOfOneSpot(final double spotRay) {
        final int bound = (int) Math.ceil(spotRay + 1);
        int surface = 0;

        for (int x = -bound; x <= bound; x++) {
            for (int y = -bound; y <= bound; y++) {
                final Point2D p = new Point2D.Double(x, y);
                if (p.distance(0, 0) < spotRay) {
                    surface++;
                }
            }
        }

        return surface;
    }

    // two spots right next to each other offset by 1 pixel.
    private double getSurfaceOfTwoContiguousSpot(final double spotRay) {
        final int bound = (int) Math.ceil(spotRay + 1) + 1;
        int surface = 0;

        for (int x = -bound; x <= bound; x++) {
            for (int y = -bound; y <= bound; y++) {
                final Point2D p = new Point2D.Double(x, y);
                if (p.distance(0, 0) < spotRay
                        ||
                        p.distance(1, 0) < spotRay
                ) {
                    surface++;
                }
            }
        }

        return surface;
    }

    // TODO: 29/08/2023 Never used, should be deleted
    @Contract(pure = true)
    private void fit(final @NotNull DetectionSpot detectionSpot, final ArrayList<DetectionSpot> detectionList) {
        detectionSpot.points.get(0);
    }

    int nbSplitDetection = 0;

    private void filterTest(final @NotNull DetectionSpot detectionSpot, final ArrayList<DetectionSpot> detectionList, final GlobalDetectionToken gdt) {
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

        for (final Point3D p : detectionSpot.points) {
            final double value = gdt.inputComputationSequence.getImage(detectionSpot.getT(), (int) p.z).getData((int) p.x, (int) p.y, 0);
            if (value > max) max = value;
        }

        if (max > 80) {
            nbSplitDetection++;
            final DetectionSpot newSpot = new DetectionSpot();
            newSpot.points.add(new Point3D(detectionSpot.getMassCenter().x + 1, detectionSpot.getMassCenter().y + 1, detectionSpot.getMassCenter().z));
            newSpot.setT(detectionSpot.getT());
            newSpot.computeMassCenter();
            detectionList.add(newSpot);
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

    @Override
    public void process(final @NotNull GlobalDetectionToken gdt) {
        IcyLogger.info(this.getClass(), "Cluster filtering...");

        final double spotRay = Double.parseDouble(spotRaySizeTextBox.getText());

        // compute surface of 1 spot:

        final int surfaceOfOneSpot = (int) getSurfaceOfOneSpot(spotRay);
        final int surfaceOfTwoContiguousSpot = (int) getSurfaceOfTwoContiguousSpot(spotRay);

        IcyLogger.info(this.getClass(), "surface of one spot: " + surfaceOfOneSpot);
        IcyLogger.info(this.getClass(), "surface of 2 glued spots: " + surfaceOfTwoContiguousSpot);
        IcyLogger.info(this.getClass(), "difference between the 2:" + (surfaceOfTwoContiguousSpot - surfaceOfOneSpot));


        final int highValue = surfaceOfOneSpot;
        final int lowValue = 2;

        nbSplitDetection = 0;
        // Filter the roi dataset
        for (final ROI roi : gdt.roi2detection.keySet()) {
            final ArrayList<DetectionSpot> detectionList = gdt.roi2detection.get(roi);
            final ArrayList<DetectionSpot> detectionListCopy = new ArrayList<>(detectionList);

            for (final DetectionSpot detectionSpot : detectionListCopy) {
                filterTest(detectionSpot, detectionList, gdt);
            }

        }
        IcyLogger.info(this.getClass(), "Number of split phase 1: " + nbSplitDetection);

        nbSplitDetection = 0;
        // filter the detection gdt.detectionResult dataset
        {
            final ArrayList<DetectionSpot> detectionList = gdt.detectionResult;
            final ArrayList<DetectionSpot> detectionListCopy = new ArrayList<>(detectionList);

            for (final DetectionSpot detectionSpot : detectionListCopy) {
                filterTest(detectionSpot, detectionList, gdt);
            }
        }
        IcyLogger.info(this.getClass(), "Number of split phase 2: " + nbSplitDetection);
    }

    @Override
    public void saveXLS(final Sheet page, final GlobalDetectionToken gdt) {
        //
    }
}
