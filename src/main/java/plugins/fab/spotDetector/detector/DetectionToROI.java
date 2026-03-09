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

import org.bioimageanalysis.extension.kernel.roi.roi2d.ROI2DArea;
import org.bioimageanalysis.extension.kernel.roi.roi3d.ROI3DArea;
import org.bioimageanalysis.icy.common.geom.point.Point3D;
import org.bioimageanalysis.icy.common.geom.point.Point5D;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.roi.mask.BooleanMask3D;
import org.bioimageanalysis.icy.system.SystemUtil;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.bioimageanalysis.icy.system.thread.Processor;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.DetectionSpot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DetectionToROI {
    record DetectionToROIConverter(List<DetectionSpot> spots, int baseIndex) implements Callable<List<ROI>> {
        @Override
        public @NotNull List<ROI> call() {
            final List<ROI> result = new ArrayList<>(spots.size());

            for (int s = 0; s < spots.size(); s++) {
                final DetectionSpot spot = spots.get(s);
                final int size = spot.points.size();

                if (size == 0)
                    continue;

                final Point3D[] p3ds = new Point3D[size];
                for (int p = 0; p < size; p++) {
                    final plugins.fab.spotDetector.Point3D pt = spot.points.get(p);
                    p3ds[p] = new Point3D.Double(pt.x, pt.y, pt.z);
                }

                final BooleanMask3D bm3d = new BooleanMask3D(p3ds);
                final ROI3DArea roi3D = new ROI3DArea(bm3d);
                final ROI roi;

                if (roi3D.getSizeZ() == 1) // check if the sizeZ is 1 and convert to ROI2D
                {
                    final ROI2DArea roi2D = roi3D.getSlice((int) roi3D.getPosition().getZ());
                    roi2D.setZ(roi3D.getPosition().z);
                    roi = roi2D;
                }
                else
                    roi = roi3D;

                final Point5D pos = roi.getPosition5D();
                // adjust T for ROI
                pos.setT(spot.getT());
                roi.setPosition5D(pos);
                // set name
                roi.setName("spot #" + (baseIndex + s));

                // add the ROI to result
                result.add(roi);
            }

            return result;
        }
    }

    @SuppressWarnings("resource")
    public static @NotNull ArrayList<ROI> convertDetectionToROI(final @NotNull ArrayList<DetectionSpot> detectionList) {
        final Processor processor = new Processor(Math.max(256, detectionList.size()), SystemUtil.getNumberOfCPUs() * 2);
        final List<Future<List<ROI>>> tasks = new ArrayList<>();
        final int size = detectionList.size();
        final int groupsSize = 1000;

        for (int i = 0; i < detectionList.size(); i += groupsSize)
            tasks.add(processor.submit(new DetectionToROIConverter(detectionList.subList(i, Math.min(i + groupsSize, size)), i)));

        final ArrayList<ROI> result = new ArrayList<>();
        for (final Future<List<ROI>> task : tasks) {
            try {
                result.addAll(task.get());
            }
            catch (final InterruptedException e) {
                break;
            }
            catch (final ExecutionException e) {
                IcyLogger.error(DetectionToROI.class, e);
                break;
            }
        }

        return result;
    }
}
