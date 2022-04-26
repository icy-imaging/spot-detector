package plugins.fab.spotDetector.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.roi.BooleanMask3D;
import icy.roi.ROI;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.point.Point5D;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.Point3D;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi3d.ROI3DArea;

public class DetectionToROI
{
    static class DetectionToROIConverter implements Callable<List<ROI>>
    {
        final List<DetectionSpot> spots;
        final int baseIndex;

        public DetectionToROIConverter(List<DetectionSpot> spots, int baseIndex)
        {
            super();

            this.spots = spots;
            this.baseIndex = baseIndex;
        }

        @Override
        public List<ROI> call() throws Exception
        {
            final List<ROI> result = new ArrayList<>(spots.size());

            for (int s = 0; s < spots.size(); s++)
            {
                final DetectionSpot spot = spots.get(s);
                final int size = spot.points.size();

                if (size == 0)
                    continue;

                icy.type.point.Point3D[] p3ds = new icy.type.point.Point3D[size];
                for (int p = 0; p < size; p++)
                {
                    Point3D pt = spot.points.get(p);
                    p3ds[p] = new icy.type.point.Point3D.Double(pt.x, pt.y, pt.z);
                }

                BooleanMask3D bm3d = new BooleanMask3D(p3ds);
                ROI3DArea roi3D = new ROI3DArea(bm3d);
                ROI roi;

                if (roi3D.getSizeZ() == 1) // check if the sizeZ is 1 and convert to ROI2D
                {
                    ROI2DArea roi2D = roi3D.getSlice((int) roi3D.getPosition().getZ());
                    roi2D.setZ(roi3D.getPosition().z);
                    roi = roi2D;
                }
                else
                    roi = roi3D;

                Point5D pos = roi.getPosition5D();
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

    public static ArrayList<ROI> convertDetectionToROI(ArrayList<DetectionSpot> detectionList)
            throws InterruptedException
    {
        Processor processor = new Processor(detectionList.size(), SystemUtil.getNumberOfCPUs() * 2);
        List<Future<List<ROI>>> tasks = new ArrayList<>();
        final int size = detectionList.size();
        final int groupsSize = 1000;

        for (int i = 0; i < detectionList.size(); i += groupsSize)
            tasks.add(processor
                    .submit(new DetectionToROIConverter(detectionList.subList(i, Math.min(i + groupsSize, size)), i)));

        ArrayList<ROI> result = new ArrayList<ROI>();
        for (Future<List<ROI>> task : tasks)
        {
            try
            {
                result.addAll(task.get());
            }
            catch (InterruptedException e)
            {
                break;
            }
            catch (ExecutionException e)
            {
                e.getCause().printStackTrace();
                break;
            }
        }

        return result;
    }
}
