package plugins.fab.spotDetector.detector;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.vecmath.Point3i;

import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.collection.array.ArrayUtil;
import plugins.adufour.connectedcomponents.ConnectedComponent;
import plugins.adufour.connectedcomponents.ConnectedComponents;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.Point3D;
import plugins.fab.spotDetector.detector.wavelets.UDWT.B3SplineUDWT;
import plugins.fab.spotDetector.detector.wavelets.UDWT.WaveletConfigException;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class UDWTWaveletCore
{
    public class UDWTWaveletCore2DProcessor implements Callable<List<DetectionSpot>>
    {
        final Sequence sequence;
        final int frame;
        final int numScales;
        final boolean negative;
        final boolean useROIWat;
        final boolean doBinary;

        public UDWTWaveletCore2DProcessor(Sequence in, int t, int ns, boolean neg, boolean useROIWat, boolean doBinary)
        {
            super();

            sequence = in;
            frame = t;
            numScales = ns;
            negative = neg;
            this.useROIWat = useROIWat;
            this.doBinary = doBinary;
        }

        @Override
        public List<DetectionSpot> call() throws Exception
        {
            final List<DetectionSpot> detectionList = new ArrayList<>();
            final List<ROI> rois = new ArrayList<>();
            final Rectangle bounds = sequence.getBounds2D();

            // rois used for WAT computation ?
            if (useROIWat)
            {
                rois.addAll(sequence.getROIs());
                // by default we use Sequence bounds
                if (rois.isEmpty())
                    rois.add(new ROI2DRectangle(bounds));
            }

            // IcyBufferedImage image = sequenceIn.getFirstImage();
            IcyBufferedImage image = sequence.getImage(frame, 0);

            if (image != null)
            {
                // build boolean mask for this frame
                final boolean[] mask = buildBinaryMask(bounds, rois, frame, 0);
                // compute scales
                final float dataIn[] = Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());
                // decompose the image
                B3SplineUDWT waveletTransform = new B3SplineUDWT();

                float[][] scales;
                try
                {
                    scales = waveletTransform.b3WaveletScales2D(dataIn, image.getWidth(), image.getHeight(), numScales);
                }
                catch (WaveletConfigException e1)
                {
                    e1.printStackTrace();
                    return detectionList;
                }
                float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, numScales,
                        image.getWidth() * image.getHeight());

                // Apply threshold to coefficients but not last one ( residual )

                // Filtering
                for (int i = 0; i < coefficients.length - 1; i++)
                {
                    if (negative)
                    {
                        for (int ii = 0; ii < coefficients[i].length; ii++)
                            coefficients[i][ii] = -coefficients[i][ii];
                    }

                    filter_wat(coefficients[i], i, sequence.getWidth(), sequence.getHeight(), mask);
                }

                // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                // fill of 0 the residual image
                Arrays.fill(coefficients[coefficients.length - 1], 0f);

                // reconstruct the image from the wavelet coefficients
                final IcyBufferedImage imageOut = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1,
                        DataType.UBYTE);
                float[] binaryDetectionResult = new float[image.getWidth() * image.getHeight()];

                // waveletTransform.b3WaveletReconstruction2D(coefficients, coefficients[numScales], binaryDetectionResult, numScales,
                // image.getWidth()*image.getHeight());
                waveletTransform.b3SpotConstruction2D(coefficients, binaryDetectionResult, numScales,
                        image.getWidth() * image.getHeight(), UDWTScaleArrayList);

                // Binarisation de la reconstruction.
                for (int i = 0; i < binaryDetectionResult.length; i++)
                    binaryDetectionResult[i] = (binaryDetectionResult[i] != 0) ? 255 : 0;

                ArrayUtil.arrayToArray(binaryDetectionResult, imageOut.getDataXY(0), image.isSignedDataType());

                Sequence seq = new Sequence(imageOut);

                addConnectedComponentDetection(seq.getVolumetricImage(0), detectionList, frame, sequence);

                if (doBinary)
                    binarySequence.setImage(frame, 0, imageOut);
            }

            return detectionList;
        }
    }

    public class UDWTWaveletCore2D3DProcessor implements Callable<List<DetectionSpot>>
    {
        final Sequence sequence;
        final int frame;
        final int numScales;
        final boolean negative;
        final boolean useROIWat;
        final boolean doBinary;

        public UDWTWaveletCore2D3DProcessor(Sequence in, int t, int ns, boolean neg, boolean useROIWat,
                boolean doBinary)
        {
            super();

            sequence = in;
            frame = t;
            numScales = ns;
            negative = neg;
            this.useROIWat = useROIWat;
            this.doBinary = doBinary;
        }

        @Override
        public List<DetectionSpot> call() throws Exception
        {
            final List<DetectionSpot> detectionList = new ArrayList<>();
            final List<ROI> rois = new ArrayList<>();
            final Rectangle bounds = sequence.getBounds2D();

            // rois used for WAT computation ?
            if (useROIWat)
            {
                rois.addAll(sequence.getROIs());
                // by default we use Sequence bounds
                if (rois.isEmpty())
                    rois.add(new ROI2DRectangle(bounds));
            }

            Sequence binary3DOutput = new Sequence();

            for (int z = 0; z < sequence.getSizeZ(); z++)
            {
                // build boolean mask for this frame / slice
                final boolean[] mask = buildBinaryMask(bounds, rois, frame, z);
                // IcyBufferedImage image = sequence.getFirstImage();
                IcyBufferedImage image = sequence.getImage(frame, z);
                // compute scales
                final float dataIn[] = Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());
                // decompose the image
                B3SplineUDWT waveletTransform = new B3SplineUDWT();

                float[][] scales;
                try
                {
                    scales = waveletTransform.b3WaveletScales2D(dataIn, image.getWidth(), image.getHeight(), numScales);
                }
                catch (WaveletConfigException e1)
                {
                    e1.printStackTrace();
                    return detectionList;
                }
                float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, numScales,
                        image.getWidth() * image.getHeight());

                // Apply threshold to coefficients but not last one ( residual )
                // Filtering
                for (int i = 0; i < coefficients.length - 1; i++)
                {
                    if (negative)
                    {
                        for (int ii = 0; ii < coefficients[i].length; ii++)
                            coefficients[i][ii] = -coefficients[i][ii];
                    }

                    filter_wat(coefficients[i], i, sequence.getWidth(), sequence.getHeight(), mask);

                }

                // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                // fill of 0 the residual image
                Arrays.fill(coefficients[coefficients.length - 1], 0f);

                // reconstruct the image from the wavelet coefficients
                final IcyBufferedImage imageOut = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1,
                        DataType.UBYTE);
                float[] binaryDetectionResult = new float[image.getWidth() * image.getHeight()];

                waveletTransform.b3SpotConstruction2D(coefficients, binaryDetectionResult, numScales,
                        image.getWidth() * image.getHeight(), UDWTScaleArrayList);

                // Binarisation de la reconstruction.
                for (int i = 0; i < binaryDetectionResult.length; i++)
                    binaryDetectionResult[i] = (binaryDetectionResult[i] != 0) ? 255 : 0;

                ArrayUtil.arrayToArray(binaryDetectionResult, imageOut.getDataXY(0), image.isSignedDataType());
                binary3DOutput.setImage(0, z, imageOut);

                if (doBinary)
                    binarySequence.setImage(frame, z, imageOut);
            }

            addConnectedComponentDetection(binary3DOutput.getVolumetricImage(0), detectionList, frame, sequence);

            return detectionList;
        }
    }

    public class UDWTWaveletCore3DProcessor implements Callable<List<DetectionSpot>>
    {
        final Sequence sequence;
        final int frame;
        final int numScales;
        final boolean negative;
        final boolean useROIWat;
        final boolean doBinary;

        public UDWTWaveletCore3DProcessor(Sequence in, int t, int ns, boolean neg, boolean useROIWat, boolean doBinary)
        {
            super();

            sequence = in;
            frame = t;
            numScales = ns;
            negative = neg;
            this.useROIWat = useROIWat;
            this.doBinary = doBinary;
        }

        @Override
        public List<DetectionSpot> call() throws Exception
        {
            final List<DetectionSpot> detectionList = new ArrayList<>();
            final List<ROI> rois = new ArrayList<>();
            final Rectangle bounds = sequence.getBounds2D();

            // rois used for WAT computation ?
            if (useROIWat)
            {
                rois.addAll(sequence.getROIs());
                // by default we use Sequence bounds
                if (rois.isEmpty())
                    rois.add(new ROI2DRectangle(bounds));
            }

            final float[][] dataIn = new float[sequence.getSizeZ()][];
            for (int z = 0; z < sequence.getSizeZ(); z++)
                dataIn[z] = Array1DUtil.arrayToFloatArray(sequence.getDataXY(frame, z, 0), sequence.isSignedDataType());
            B3SplineUDWT waveletTransform = new B3SplineUDWT();

            float[][][] scales;
            try
            {
                scales = waveletTransform.b3WaveletScales3D(dataIn, sequence.getSizeX(), sequence.getSizeY(),
                        sequence.getSizeZ(), numScales);
            }
            catch (WaveletConfigException e1)
            {
                e1.printStackTrace();
                return detectionList;
            }
            float[][][] coefficients = waveletTransform.b3WaveletCoefficients3D(scales, dataIn, numScales,
                    sequence.getSizeX() * sequence.getSizeY(), sequence.getSizeZ());

            // Apply threshold to coefficients but not last one ( residual )

            // Filtering

            final boolean[][] masks = new boolean[sequence.getSizeZ()][];

            // build boolean masks for all slices
            for (int z = 0; z < masks.length; z++)
                masks[z] = buildBinaryMask(bounds, rois, frame, z);

            for (int scale = 0; scale < coefficients.length - 1; scale++)
            {
                for (int z = 0; z < coefficients[scale].length; z++)
                {
                    if (negative)
                    {
                        for (int ii = 0; ii < coefficients[scale][z].length; ii++)
                            coefficients[scale][z][ii] = -coefficients[scale][z][ii];
                    }

                    filter_wat(coefficients[scale][z], scale, sequence.getWidth(), sequence.getHeight(), masks[z]);
                }
            }

            // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
            // fill of 0 the residual image
            float c[][] = coefficients[coefficients.length - 1];
            for (int z = 0; z < c.length; z++)
                Arrays.fill(c[z], 0f);

            // reconstruct the image from the wavelet coefficients
            float[][] reconstruction = new float[sequence.getSizeZ()][sequence.getSizeX() * sequence.getSizeY()];
            waveletTransform.b3SpotConstruction3D(coefficients, reconstruction, numScales,
                    sequence.getSizeX() * sequence.getSizeY(), sequence.getSizeZ(), UDWTScaleArrayList);

            // Binarisation de la reconstruction.
            for (int z = 0; z < reconstruction.length; z++)
            {
                float[] binaryDetectionResult = reconstruction[z];

                for (int i = 0; i < binaryDetectionResult.length; i++)
                    binaryDetectionResult[i] = (binaryDetectionResult[i] != 0) ? 255 : 0;
            }

            VolumetricImage vOut = new VolumetricImage();
            for (int z = 0; z < sequence.getSizeZ(); z++)
            {
                final IcyBufferedImage imageOut = new IcyBufferedImage(sequence.getSizeX(), sequence.getSizeY(), 1,
                        DataType.UBYTE);
                ArrayUtil.arrayToArray(reconstruction[z], imageOut.getDataXY(0), true);
                vOut.setImage(z, imageOut);
            }

            addConnectedComponentDetection(vOut, detectionList, frame, sequence);

            if (doBinary)
                binarySequence.addVolumetricImage(frame, vOut);

            return detectionList;
        }
    }

    int getNumberOfMaxEnabledScale()
    {
        int maxScale = 0;
        for (UDWTScale scale : UDWTScaleArrayList)
        {
            if (scale.isEnabled())
            {
                if (scale.scaleNumber > maxScale)
                    maxScale = scale.scaleNumber;
            }
        }
        return maxScale;

    }

    private int getNumberOfScale()
    {
        return UDWTScaleArrayList.size();
    }

    private double getScaleThreshold(int scale)
    {
        return UDWTScaleArrayList.get(scale).getThreshold();
    }

    private boolean isScaleEnabled(int scale)
    {
        return UDWTScaleArrayList.get(scale).isEnabled();
    }

    ArrayList<UDWTScale> UDWTScaleArrayList = null;

    private void addConnectedComponentDetection(VolumetricImage vOut, List<DetectionSpot> detectionList, int frame,
            Sequence originalSequence)
    {
        IcyBufferedImage img = vOut.getFirstImage();
        VolumetricImage workVol = new VolumetricImage();

        for (int z = 0; z < vOut.getSize(); z++)
            workVol.setImage(z, new IcyBufferedImage(img.getSizeX(), img.getSizeY(), 1, DataType.UINT));

        List<ConnectedComponent> result = ConnectedComponents.extractConnectedComponents(vOut, 0,
                ConnectedComponents.ExtractionType.BACKGROUND, false, false, false, 0, Integer.MAX_VALUE, workVol);

        detectionList.addAll(convert2detectionList(originalSequence, result, frame));
    }

    Sequence binarySequence;

    /**
     * @return the binary image from spot detection computation
     */
    public Sequence getBinarySequence()
    {
        return binarySequence;
    }

    /**
     * Compute the spot detection on the given input {@link Sequence}
     * 
     * @param computeBinaryDetection
     *        compute the binary detection image
     * @param UDWTScaleArrayList
     *        detection settings (scale levels and sensibility)
     * @param sequenceIn
     *        input {@link Sequence}
     * @param detectNegative
     *        detect dark over bright instead of bright over dark
     * @param useROIforWATcomputation
     *        only compute WAT with pixel in ROIs.
     * @return the list detection
     * @throws InterruptedException
     */
    public ArrayList<DetectionSpot> computeDetection(boolean computeBinaryDetection, // was displayBinaryDetection. Caller should be changed accordinaly
            ArrayList<UDWTScale> UDWTScaleArrayList, Sequence sequenceIn, boolean detectNegative,
            boolean useROIforWATcomputation) throws InterruptedException
    {

        return computeDetection(computeBinaryDetection, UDWTScaleArrayList, sequenceIn, detectNegative,
                useROIforWATcomputation, false);
    }

    /**
     * Compute the spot detection on the given input {@link Sequence}
     * 
     * @param computeBinaryDetection
     *        compute the binary detection image
     * @param UDWTScaleArrayList
     *        detection settings (scale levels and sensibility)
     * @param sequenceIn
     *        input {@link Sequence}
     * @param detectNegative
     *        detect dark over bright instead of bright over dark
     * @param useROIforWATcomputation
     *        only compute WAT with pixel in ROIs.
     * @param force2DWaveletsFor3D
     *        force 2D wavelet detection for 3D image
     * @return the list detection
     * @throws InterruptedException
     */
    public ArrayList<DetectionSpot> computeDetection(boolean computeBinaryDetection, // was displayBinaryDetection. Caller should be changed accordinaly
            ArrayList<UDWTScale> UDWTScaleArrayList, Sequence sequenceIn, boolean detectNegative,
            boolean useROIforWATcomputation, boolean force2DWaveletsFor3D) throws InterruptedException
    {
        this.UDWTScaleArrayList = UDWTScaleArrayList;

        ArrayList<DetectionSpot> detectionList = new ArrayList<DetectionSpot>();

        int numScales = getNumberOfMaxEnabledScale();

        if (sequenceIn == null)
            return detectionList;
        if (sequenceIn.getAllImage().size() == 0)
            return detectionList;

        // Sequence binSequence = new Sequence();
        binarySequence = new Sequence();
        binarySequence.setName("Binary Sequence"); // Check if this name already exists and change it if needed.

        // final Processor processor = new Processor(1);
        final Processor processor = new Processor(Math.max(256, sequenceIn.getSizeT()), SystemUtil.getNumberOfCPUs() * 2);
        final List<Future<?>> tasks = new ArrayList<Future<?>>();

        //
        // 2D Image
        //
        if (sequenceIn.getSizeZ() == 1)
        {
            if (B3SplineUDWT.isNumberOfScaleOkForImage2D(sequenceIn.getSizeX(), sequenceIn.getSizeY(),
                    getNumberOfMaxEnabledScale()) == false)
            {
                System.err.println("Scale configuration error");
                return detectionList;
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++)
            {
                // 2D
                tasks.add(processor.submit(new UDWTWaveletCore2DProcessor(sequenceIn, t, numScales, detectNegative,
                        useROIforWATcomputation, computeBinaryDetection)));
            }
        }

        //
        // 3D Image ( with the force2DWavelets for 3D enabled )
        //
        if (sequenceIn.getSizeZ() > 1 && force2DWaveletsFor3D == true)
        {
            System.out.println("Entering force 2D wavelet for 3D");

            if (B3SplineUDWT.isNumberOfScaleOkForImage2D(sequenceIn.getSizeX(), sequenceIn.getSizeY(),
                    getNumberOfMaxEnabledScale()) == false)
            {
                System.err.println("Scale configuration error (your image is too small for this scale)");
                return detectionList;
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++)
            {
                tasks.add(processor.submit(new UDWTWaveletCore2D3DProcessor(sequenceIn, t, numScales, detectNegative,
                        useROIforWATcomputation, computeBinaryDetection)));
            }
        }

        // ******* FIN PARTIE 2D

        //
        // 3D Image ( classic, without the force2DWavelets for 3D enabled )
        //
        if (sequenceIn.getSizeZ() > 1 && force2DWaveletsFor3D == false)
        {
            if (B3SplineUDWT.isNumberOfScaleOkForImage3D(sequenceIn.getSizeX(), sequenceIn.getSizeY(),
                    sequenceIn.getSizeZ(), getNumberOfMaxEnabledScale()) == false)
            {
                int scale2 = B3SplineUDWT.getMinSize(2);
                int scale3 = B3SplineUDWT.getMinSize(3);
                int scale4 = B3SplineUDWT.getMinSize(4);

                MessageDialog.showDialog(
                        "<html><center>There is a problem with the scale configuration (but don't worry, read this):"
                                + "<br><br>To run in 3D, the wavelet algorithm needs a number of slices depending on the scale(s) you enabled:"
                                + "<br>" + "<br>Scale 2 : " + scale2 + " slices" + "<br>Scale 3 : " + scale3 + " slices"
                                + "<br>Scale 4 : " + scale4 + " slices" + "<br>"
                                + "<br>To use those parameters you need more Z in your stack.<br>Still, you can bypass this problem by selecting"
                                + "<br>the option <b>Force use of 2D Wavelets for 3D</b> in the detector panel"
                                + "<br>In this case, each 2D slices will be computed separately and results will be merged to create the resulting stack,"
                                + "<br>(and you don't need to add more Z slices)," + "<br>The sequence file name is: "
                                + sequenceIn.getFilename() + "</center></html>",
                        MessageDialog.WARNING_MESSAGE);

                return detectionList;
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++)
            {
                tasks.add(processor.submit(new UDWTWaveletCore3DProcessor(sequenceIn, t, numScales, detectNegative,
                        useROIforWATcomputation, computeBinaryDetection)));
            }
        }

        binarySequence.beginUpdate();
        try
        {
            boolean exception = false;
            for (Future<?> task : tasks)
            {
                try
                {
                    final List<DetectionSpot> spots = (List<DetectionSpot>) task.get();
                    if ((spots != null) && !spots.isEmpty())
                        detectionList.addAll(spots);
                }
                catch (InterruptedException ex)
                {
                    // stop now
                    processor.removeAllWaitingTasks();
                    processor.shutdownNow();

                    // re-throw it
                    throw new InterruptedException("SpotDetector.computeDetection(..) process interrupted.");
                }
                catch (ExecutionException e)
                {
                    // display it only once
                    if (!exception)
                    {
                        e.getCause().printStackTrace();
                        exception = true;
                    }
                }
            }
        }
        finally
        {
            binarySequence.dataChanged();
            binarySequence.endUpdate();
        }

        if (computeBinaryDetection)
        {
            // Icy.addSequence(binarySequence);
            // should now be retrive using getBinarySequence() directly from the caller.
        }

        return detectionList;
    }

    static boolean[] buildBinaryMask(Rectangle bounds, List<ROI> rois, int t, int z) throws InterruptedException
    {
        if (rois.isEmpty())
            return null;

        final BooleanMask2D result = new BooleanMask2D(bounds, new boolean[bounds.width * bounds.height]);

        // merge all masks from wanted position
        for (ROI roi : rois)
            result.add(roi.getBooleanMask2D(z, t, -1, true));

        return result.mask;
    }

    /**
     * Filters coefficients with the lambda computation
     * mask[] is true, the pixel is considered in computation of the wat.
     * if mask is null, no mask considered, all pixel used.
     */
    private void filter_wat(float[] data, int depth, int width, int height, boolean[] mask)
    {

        if (!isScaleEnabled(depth)) // if the scale is not selected, fill of 0. could be optimized by not rebuilding it.
        {
            for (int i = 0; i < data.length; i++)
            {
                data[i] = 0;
            }
            return;
        }

        // Init lambda value
        double lambdac[] = new double[getNumberOfScale() + 2];

        for (int i = 0; i < getNumberOfScale() + 2; i++)
        {
            // ( 1 << (2*i) ) ) gives 1 , 4 , 16 , 64 , 256 , 1024 ...
            lambdac[i] = Math.sqrt(2 * Math.log(width * height / (1 << (2 * i))));
        }

        float mad = avesigma(data, mask);
        double dcoeff[] = new double[getNumberOfMaxEnabledScale()]; // was fixed to 5.

        for (int i = 0; i < getNumberOfMaxEnabledScale(); i++)
        {
            dcoeff[i] = getScaleThreshold(i) / 100.0d;
        }
        double coeffThr = (lambdac[depth + 1] * mad) / dcoeff[depth];

        for (int i = 0; i < data.length; i++)
        {
            if (data[i] >= coeffThr)
            {
                // data[ i ] = 255 ;
            }
            else
            {
                data[i] = 0;
            }
        }
    }

    /**
     * @param m
     * @return
     */
    private static float avesigma(float[] image, boolean mask[])
    {
        return getMeanAverageDistance(image, mask);
    }

    /**
     * @return mean average distance
     */
    private static float getMeanAverageDistance(float[] data, boolean mask[])
    {
        float mean = getMean(data, mask);
        float a = 0;
        float s;

        if (mask == null)
        {
            for (int i = 0; i < data.length; i++)
            {
                s = data[i] - mean;
                a = a + Math.abs(s);
            }

            if (data.length > 0)
                return a / data.length;
        }
        else
        {
            float nbValue = 0;
            for (int i = 0; i < data.length; i++)
            {
                if (mask[i])
                {
                    s = data[i] - mean;
                    a = a + Math.abs(s);
                    nbValue++;
                }
            }

            if (nbValue > 0)
                return a / nbValue;
        }

        return 0;
    }

    private static float getVar(float[] data)
    {
        if (data.length <= 1)
            return 0;

        float sum = 0;
        float sum2 = 0;
        float val;

        for (int i = 0; i < data.length; i++)
        {
            val = data[i];
            sum2 += val * val;
            sum += val;
        }

        return (sum2 - sum * sum / data.length) / (data.length - 1);
    }

    private static float getMean(float[] data, boolean mask[])
    {
        float mean = 0;
        float sum = 0;

        if (mask == null)
        {
            for (int i = 0; i < data.length; i++)
            {
                sum += data[i];
            }
            if (data.length > 0)
                mean = sum / data.length;
        }
        else
        {
            float nbValue = 0;
            for (int i = 0; i < data.length; i++)
            {
                if (mask[i])
                {
                    sum += data[i];
                    nbValue++;
                }
            }
            if (nbValue > 0)
                mean = sum / nbValue;
        }

        return mean;
    }

    // FIXME : change the structure of 4D detections into a detection pool for faster access to all the detections of a time or a time and a z

    private List<DetectionSpot> convert2detectionList(Sequence originalSequence, List<ConnectedComponent> components,
            int t)
    {
        List<DetectionSpot> detectionList = new ArrayList<DetectionSpot>();

        for (ConnectedComponent cc : components)
        {
            DetectionSpot detection = new DetectionSpot();

            for (Point3i point : cc.getIterablePoints())
                detection.points.add(new Point3D(point.x, point.y, point.z));

            detection.setT(t);
            detection.computeMassCenter();
            computeMinMeanMaxIntensity(detection, originalSequence);
            detectionList.add(detection);
        }

        return detectionList;
    }

    private void computeMinMeanMaxIntensity(DetectionSpot detection, Sequence originalSequence)
    {
        double minIntensity = Double.MAX_VALUE;
        double maxIntensity = Double.MIN_VALUE;
        double sumIntensity = 0;

        boolean signed = originalSequence.isSignedDataType();
        int w = originalSequence.getSizeX();
        int z = -1;
        Object data = null;
        
        for (Point3D point : detection.points)
        {
            if ((z != (int)point.z))
            {
                z = (int)point.z;
                data = originalSequence.getDataXY(detection.getT(), (int) point.z, 0);
            }
                
            double value = Array1DUtil.getValue(data, ((int) point.y * w) +  (int) point.x, signed);

            if (value < minIntensity)
                minIntensity = value;
            if (value > maxIntensity)
                maxIntensity = value;

            sumIntensity += value;
        }

        detection.maxIntensity = maxIntensity;
        detection.minIntensity = minIntensity;

        double averageIntensity = 0;
        if (detection.points.size() > 0)
            averageIntensity = sumIntensity / detection.points.size();

        detection.meanIntensity = averageIntensity;
    }
}
