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

import fr.icy.extension.kernel.roi.roi2d.ROI2DRectangle;
import fr.icy.common.collection.array.Array1DUtil;
import fr.icy.common.collection.array.ArrayUtil;
import fr.icy.common.type.DataType;
import fr.icy.gui.dialog.MessageDialog;
import fr.icy.model.image.IcyBufferedImage;
import fr.icy.model.roi.ROI;
import fr.icy.model.roi.mask.BooleanMask2D;
import fr.icy.model.sequence.Sequence;
import fr.icy.model.sequence.VolumetricImage;
import fr.icy.system.SystemUtil;
import fr.icy.system.thread.Processor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugins.adufour.connectedcomponents.ConnectedComponent;
import plugins.adufour.connectedcomponents.ConnectedComponents;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.Point3D;
import plugins.fab.spotDetector.detector.wavelets.UDWT.B3SplineUDWT;
import plugins.fab.spotDetector.detector.wavelets.UDWT.WaveletConfigException;

import javax.swing.*;
import javax.vecmath.Point3i;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDWTWaveletCore {
    private static final Logger LOGGER = Logger.getLogger(UDWTWaveletCore.class.getName());

    public class UDWTWaveletCore2DProcessor implements Callable<List<DetectionSpot>> {
        final Sequence sequence;
        final int frame;
        final int numScales;
        final boolean negative;
        final boolean useROIWat;
        final boolean doBinary;

        @Contract(pure = true)
        public UDWTWaveletCore2DProcessor(final Sequence in, final int t, final int ns, final boolean neg, final boolean useROIWat, final boolean doBinary) {
            super();

            sequence = in;
            frame = t;
            numScales = ns;
            negative = neg;
            this.useROIWat = useROIWat;
            this.doBinary = doBinary;
        }

        @Override
        public List<DetectionSpot> call() throws Exception {
            final List<DetectionSpot> detectionList = new ArrayList<>();
            final List<ROI> rois = new ArrayList<>();
            final Rectangle bounds = sequence.getBounds2D();

            // rois used for WAT computation ?
            if (useROIWat) {
                rois.addAll(sequence.getROIs());
                // by default we use Sequence bounds
                if (rois.isEmpty())
                    rois.add(new ROI2DRectangle(bounds));
            }

            // IcyBufferedImage image = sequenceIn.getFirstImage();
            final IcyBufferedImage image = sequence.getImage(frame, 0);

            if (image != null) {
                // build boolean mask for this frame
                final boolean[] mask = buildBinaryMask(bounds, rois, frame, 0);
                // compute scales
                final float[] dataIn = Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());
                // decompose the image
                final B3SplineUDWT waveletTransform = new B3SplineUDWT();

                final float[][] scales;
                try {
                    scales = waveletTransform.b3WaveletScales2D(dataIn, image.getWidth(), image.getHeight(), numScales);
                }
                catch (final WaveletConfigException e1) {
                    if (LOGGER.isLoggable(Level.SEVERE))
                        LOGGER.log(Level.SEVERE, "The wavelet is incorrect for the given image dimension.", e1);
                    return detectionList;
                }
                final float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, numScales, image.getWidth() * image.getHeight());

                // Apply threshold to coefficients but not last one ( residual )

                // Filtering
                for (int i = 0; i < coefficients.length - 1; i++) {
                    if (negative) {
                        for (int ii = 0; ii < coefficients[i].length; ii++)
                            coefficients[i][ii] = -coefficients[i][ii];
                    }

                    filter_wat(coefficients[i], i, sequence.getWidth(), sequence.getHeight(), mask);
                }

                // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                // fill of 0 the residual image
                Arrays.fill(coefficients[coefficients.length - 1], 0f);

                // reconstruct the image from the wavelet coefficients
                final IcyBufferedImage imageOut = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1, DataType.UBYTE);
                final float[] binaryDetectionResult = new float[image.getWidth() * image.getHeight()];

                // waveletTransform.b3WaveletReconstruction2D(coefficients, coefficients[numScales], binaryDetectionResult, numScales,
                // image.getWidth()*image.getHeight());
                waveletTransform.b3SpotConstruction2D(coefficients, binaryDetectionResult, numScales,
                        image.getWidth() * image.getHeight(), UDWTScaleArrayList);

                // Binarisation de la reconstruction.
                for (int i = 0; i < binaryDetectionResult.length; i++)
                    binaryDetectionResult[i] = (binaryDetectionResult[i] != 0) ? 255 : 0;

                ArrayUtil.arrayToArray(binaryDetectionResult, imageOut.getDataXY(0), image.isSignedDataType());

                final Sequence seq = new Sequence(imageOut);

                addConnectedComponentDetection(seq.getVolumetricImage(0), detectionList, frame, sequence);

                if (doBinary)
                    binarySequence.setImage(frame, 0, imageOut);
            }

            return detectionList;
        }
    }

    public class UDWTWaveletCore2D3DProcessor implements Callable<List<DetectionSpot>> {
        final Sequence sequence;
        final int frame;
        final int numScales;
        final boolean negative;
        final boolean useROIWat;
        final boolean doBinary;

        @Contract(pure = true)
        public UDWTWaveletCore2D3DProcessor(final Sequence in, final int t, final int ns, final boolean neg, final boolean useROIWat, final boolean doBinary) {
            super();

            sequence = in;
            frame = t;
            numScales = ns;
            negative = neg;
            this.useROIWat = useROIWat;
            this.doBinary = doBinary;
        }

        @SuppressWarnings("resource")
        @Override
        public List<DetectionSpot> call() throws Exception {
            final List<DetectionSpot> detectionList = new ArrayList<>();
            final List<ROI> rois = new ArrayList<>();
            final Rectangle bounds = sequence.getBounds2D();

            // rois used for WAT computation ?
            if (useROIWat) {
                rois.addAll(sequence.getROIs());
                // by default we use Sequence bounds
                if (rois.isEmpty())
                    rois.add(new ROI2DRectangle(bounds));
            }

            final Sequence binary3DOutput = new Sequence();

            for (int z = 0; z < sequence.getSizeZ(); z++) {
                // build boolean mask for this frame / slice
                final boolean[] mask = buildBinaryMask(bounds, rois, frame, z);
                // IcyBufferedImage image = sequence.getFirstImage();
                final IcyBufferedImage image = sequence.getImage(frame, z);
                // compute scales
                final float[] dataIn = Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());
                // decompose the image
                final B3SplineUDWT waveletTransform = new B3SplineUDWT();

                final float[][] scales;
                try {
                    scales = waveletTransform.b3WaveletScales2D(dataIn, image.getWidth(), image.getHeight(), numScales);
                }
                catch (final WaveletConfigException e1) {
                    if (LOGGER.isLoggable(Level.SEVERE))
                        LOGGER.log(Level.SEVERE, "The wavelet is incorrect for the given image dimension.", e1);
                    return detectionList;
                }
                final float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, numScales, image.getWidth() * image.getHeight());

                // Apply threshold to coefficients but not last one ( residual )
                // Filtering
                for (int i = 0; i < coefficients.length - 1; i++) {
                    if (negative) {
                        for (int ii = 0; ii < coefficients[i].length; ii++)
                            coefficients[i][ii] = -coefficients[i][ii];
                    }

                    filter_wat(coefficients[i], i, sequence.getWidth(), sequence.getHeight(), mask);

                }

                // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                // fill of 0 the residual image
                Arrays.fill(coefficients[coefficients.length - 1], 0f);

                // reconstruct the image from the wavelet coefficients
                final IcyBufferedImage imageOut = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1, DataType.UBYTE);
                final float[] binaryDetectionResult = new float[image.getWidth() * image.getHeight()];

                waveletTransform.b3SpotConstruction2D(coefficients, binaryDetectionResult, numScales, image.getWidth() * image.getHeight(), UDWTScaleArrayList);

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

    public class UDWTWaveletCore3DProcessor implements Callable<List<DetectionSpot>> {
        final Sequence sequence;
        final int frame;
        final int numScales;
        final boolean negative;
        final boolean useROIWat;
        final boolean doBinary;

        @Contract(pure = true)
        public UDWTWaveletCore3DProcessor(final Sequence in, final int t, final int ns, final boolean neg, final boolean useROIWat, final boolean doBinary) {
            super();

            sequence = in;
            frame = t;
            numScales = ns;
            negative = neg;
            this.useROIWat = useROIWat;
            this.doBinary = doBinary;
        }

        @Override
        public List<DetectionSpot> call() throws Exception {
            final List<DetectionSpot> detectionList = new ArrayList<>();
            final List<ROI> rois = new ArrayList<>();
            final Rectangle bounds = sequence.getBounds2D();

            // rois used for WAT computation ?
            if (useROIWat) {
                rois.addAll(sequence.getROIs());
                // by default we use Sequence bounds
                if (rois.isEmpty())
                    rois.add(new ROI2DRectangle(bounds));
            }

            final float[][] dataIn = new float[sequence.getSizeZ()][];
            for (int z = 0; z < sequence.getSizeZ(); z++)
                dataIn[z] = Array1DUtil.arrayToFloatArray(sequence.getDataXY(frame, z, 0), sequence.isSignedDataType());
            final B3SplineUDWT waveletTransform = new B3SplineUDWT();

            final float[][][] scales;
            try {
                scales = waveletTransform.b3WaveletScales3D(dataIn, sequence.getSizeX(), sequence.getSizeY(), sequence.getSizeZ(), numScales);
            }
            catch (final WaveletConfigException e1) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "The wavelet is incorrect for the given image dimension.", e1);
                return detectionList;
            }
            final float[][][] coefficients = waveletTransform.b3WaveletCoefficients3D(scales, dataIn, numScales, sequence.getSizeX() * sequence.getSizeY(), sequence.getSizeZ());

            // Apply threshold to coefficients but not last one ( residual )

            // Filtering

            final boolean[][] masks = new boolean[sequence.getSizeZ()][];

            // build boolean masks for all slices
            for (int z = 0; z < masks.length; z++)
                masks[z] = buildBinaryMask(bounds, rois, frame, z);

            for (int scale = 0; scale < coefficients.length - 1; scale++) {
                for (int z = 0; z < coefficients[scale].length; z++) {
                    if (negative) {
                        for (int ii = 0; ii < coefficients[scale][z].length; ii++)
                            coefficients[scale][z][ii] = -coefficients[scale][z][ii];
                    }

                    filter_wat(coefficients[scale][z], scale, sequence.getWidth(), sequence.getHeight(), masks[z]);
                }
            }

            // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
            // fill of 0 the residual image
            final float[][] c = coefficients[coefficients.length - 1];
            for (final float[] floats : c) Arrays.fill(floats, 0f);

            // reconstruct the image from the wavelet coefficients
            final float[][] reconstruction = new float[sequence.getSizeZ()][sequence.getSizeX() * sequence.getSizeY()];
            waveletTransform.b3SpotConstruction3D(coefficients, reconstruction, numScales, sequence.getSizeX() * sequence.getSizeY(), sequence.getSizeZ(), UDWTScaleArrayList);

            // Binarisation de la reconstruction.
            for (final float[] binaryDetectionResult : reconstruction) {
                for (int i = 0; i < binaryDetectionResult.length; i++)
                    binaryDetectionResult[i] = (binaryDetectionResult[i] != 0) ? 255 : 0;
            }

            final VolumetricImage vOut = new VolumetricImage();
            for (int z = 0; z < sequence.getSizeZ(); z++) {
                final IcyBufferedImage imageOut = new IcyBufferedImage(sequence.getSizeX(), sequence.getSizeY(), 1, DataType.UBYTE);
                ArrayUtil.arrayToArray(reconstruction[z], imageOut.getDataXY(0), true);
                vOut.setImage(z, imageOut);
            }

            addConnectedComponentDetection(vOut, detectionList, frame, sequence);

            if (doBinary)
                binarySequence.addVolumetricImage(frame, vOut);

            return detectionList;
        }
    }

    int getNumberOfMaxEnabledScale() {
        int maxScale = 0;
        for (final UDWTScale scale : UDWTScaleArrayList) {
            if (scale.isEnabled()) {
                if (scale.scaleNumber > maxScale)
                    maxScale = scale.scaleNumber;
            }
        }
        return maxScale;

    }

    @Contract(pure = true)
    private int getNumberOfScale() {
        return UDWTScaleArrayList.size();
    }

    private double getScaleThreshold(final int scale) {
        return UDWTScaleArrayList.get(scale).getThreshold();
    }

    private boolean isScaleEnabled(final int scale) {
        return UDWTScaleArrayList.get(scale).isEnabled();
    }

    ArrayList<UDWTScale> UDWTScaleArrayList = null;

    private void addConnectedComponentDetection(final @NotNull VolumetricImage vOut, final List<DetectionSpot> detectionList, final int frame, final Sequence originalSequence) {
        final IcyBufferedImage img = vOut.getFirstImage();
        final VolumetricImage workVol = new VolumetricImage();

        for (int z = 0; z < vOut.getSize(); z++)
            workVol.setImage(z, new IcyBufferedImage(img.getSizeX(), img.getSizeY(), 1, DataType.UINT));

        final List<ConnectedComponent> result = ConnectedComponents.extractConnectedComponents(vOut,
                0,
                ConnectedComponents.ExtractionType.BACKGROUND,
                false,
                false,
                false,
                0,
                Integer.MAX_VALUE,
                workVol
        );

        detectionList.addAll(convert2detectionList(originalSequence, result, frame));
    }

    Sequence binarySequence;

    /**
     * @return the binary image from spot detection computation
     */
    public Sequence getBinarySequence() {
        return binarySequence;
    }

    /**
     * Compute the spot detection on the given input {@link Sequence}
     *
     * @param computeBinaryDetection  compute the binary detection image
     * @param UDWTScaleArrayList      detection settings (scale levels and sensibility)
     * @param sequenceIn              input {@link Sequence}
     * @param detectNegative          detect dark over bright instead of bright over dark
     * @param useROIforWATcomputation only compute WAT with pixel in ROIs.
     * @return the list detection
     * @throws InterruptedException
     */
    public ArrayList<DetectionSpot> computeDetection(
            final boolean computeBinaryDetection, // was displayBinaryDetection. Caller should be changed accordinaly
            final ArrayList<UDWTScale> UDWTScaleArrayList,
            final Sequence sequenceIn,
            final boolean detectNegative,
            final boolean useROIforWATcomputation
    ) throws InterruptedException {
        return computeDetection(computeBinaryDetection, UDWTScaleArrayList, sequenceIn, detectNegative, useROIforWATcomputation, false);
    }

    /**
     * Compute the spot detection on the given input {@link Sequence}
     *
     * @param computeBinaryDetection  compute the binary detection image
     * @param UDWTScaleArrayList      detection settings (scale levels and sensibility)
     * @param sequenceIn              input {@link Sequence}
     * @param detectNegative          detect dark over bright instead of bright over dark
     * @param useROIforWATcomputation only compute WAT with pixel in ROIs.
     * @param force2DWaveletsFor3D    force 2D wavelet detection for 3D image
     * @return the list detection
     * @throws InterruptedException
     */
    @SuppressWarnings("resource")
    public ArrayList<DetectionSpot> computeDetection(
            final boolean computeBinaryDetection, // was displayBinaryDetection. Caller should be changed accordinaly
            final ArrayList<UDWTScale> UDWTScaleArrayList,
            final Sequence sequenceIn,
            final boolean detectNegative,
            final boolean useROIforWATcomputation,
            final boolean force2DWaveletsFor3D
    ) throws InterruptedException {
        this.UDWTScaleArrayList = UDWTScaleArrayList;

        final ArrayList<DetectionSpot> detectionList = new ArrayList<>();

        final int numScales = getNumberOfMaxEnabledScale();

        if (sequenceIn == null)
            return detectionList;
        if (sequenceIn.getAllImage().isEmpty())
            return detectionList;

        // Sequence binSequence = new Sequence();
        binarySequence = new Sequence();
        binarySequence.setName("Binary Sequence"); // Check if this name already exists and change it if needed.

        // final Processor processor = new Processor(1);
        final Processor processor = new Processor(Math.max(256, sequenceIn.getSizeT()), SystemUtil.getNumberOfCPUs() * 2);
        final List<Future<?>> tasks = new ArrayList<>();

        //
        // 2D Image
        //
        if (sequenceIn.getSizeZ() == 1) {
            if (!B3SplineUDWT.isNumberOfScaleOkForImage2D(sequenceIn.getSizeX(), sequenceIn.getSizeY(), getNumberOfMaxEnabledScale())) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "Scale configuration error.");
                return detectionList;
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++) {
                // 2D
                tasks.add(processor.submit(new UDWTWaveletCore2DProcessor(sequenceIn, t, numScales, detectNegative, useROIforWATcomputation, computeBinaryDetection)));
            }
        }

        //
        // 3D Image ( with the force2DWavelets for 3D enabled )
        //
        if (sequenceIn.getSizeZ() > 1 && force2DWaveletsFor3D) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(Level.INFO, "Entering force 2D wavelet for 3D.");

            if (!B3SplineUDWT.isNumberOfScaleOkForImage2D(sequenceIn.getSizeX(), sequenceIn.getSizeY(), getNumberOfMaxEnabledScale())) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "Scale configuration error (your image is too small for this scale)");
                return detectionList;
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++) {
                tasks.add(processor.submit(new UDWTWaveletCore2D3DProcessor(
                        sequenceIn,
                        t,
                        numScales,
                        detectNegative,
                        useROIforWATcomputation,
                        computeBinaryDetection
                )));
            }
        }

        // ******* FIN PARTIE 2D

        //
        // 3D Image ( classic, without the force2DWavelets for 3D enabled )
        //
        if (sequenceIn.getSizeZ() > 1 && !force2DWaveletsFor3D) {
            if (!B3SplineUDWT.isNumberOfScaleOkForImage3D(sequenceIn.getSizeX(), sequenceIn.getSizeY(), sequenceIn.getSizeZ(), getNumberOfMaxEnabledScale())) {
                final int scale2 = B3SplineUDWT.getMinSize(2);
                final int scale3 = B3SplineUDWT.getMinSize(3);
                final int scale4 = B3SplineUDWT.getMinSize(4);

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
                        JOptionPane.WARNING_MESSAGE);

                return detectionList;
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++) {
                tasks.add(processor.submit(new UDWTWaveletCore3DProcessor(
                        sequenceIn,
                        t,
                        numScales,
                        detectNegative,
                        useROIforWATcomputation,
                        computeBinaryDetection
                )));
            }
        }

        binarySequence.beginUpdate();
        try {
            boolean exception = false;
            for (final Future<?> task : tasks) {
                try {
                    @SuppressWarnings("unchecked") final List<DetectionSpot> spots = (List<DetectionSpot>) task.get();
                    if ((spots != null) && !spots.isEmpty())
                        detectionList.addAll(spots);
                }
                catch (final InterruptedException ex) {
                    // stop now
                    processor.removeAllWaitingTasks();
                    processor.shutdownNow();

                    // re-throw it
                    throw new InterruptedException("SpotDetector.computeDetection(..) process interrupted.");
                }
                catch (final ExecutionException e) {
                    // display it only once
                    if (!exception) {
                        if (LOGGER.isLoggable(Level.SEVERE))
                            LOGGER.log(Level.SEVERE, "An error occurred during the computation.", e);
                        exception = true;
                    }
                }
            }
        }
        finally {
            binarySequence.dataChanged();
            binarySequence.endUpdate();
        }

        //if (computeBinaryDetection) {
        // Icy.addSequence(binarySequence);
        // should now be retrive using getBinarySequence() directly from the caller.
        //}

        return detectionList;
    }

    static boolean @Nullable [] buildBinaryMask(final Rectangle bounds, final @NotNull List<ROI> rois, final int t, final int z) throws InterruptedException {
        if (rois.isEmpty())
            return null;

        final BooleanMask2D result = new BooleanMask2D(bounds, new boolean[bounds.width * bounds.height]);

        // merge all masks from wanted position
        for (final ROI roi : rois)
            result.add(roi.getBooleanMask2D(z, t, -1, true));

        return result.mask;
    }

    /**
     * Filters coefficients with the lambda computation
     * mask[] is true, the pixel is considered in computation of the wat.
     * if mask is null, no mask considered, all pixel used.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void filter_wat(final float[] data, final int depth, final int width, final int height, final boolean[] mask) {
        if (!isScaleEnabled(depth)) // if the scale is not selected, fill of 0. could be optimized by not rebuilding it.
        {
            Arrays.fill(data, 0);
            return;
        }

        // Init lambda value
        final double[] lambdac = new double[getNumberOfScale() + 2];

        for (int i = 0; i < getNumberOfScale() + 2; i++) {
            // ( 1 << (2*i) ) ) gives 1 , 4 , 16 , 64 , 256 , 1024 ...
            lambdac[i] = Math.sqrt(2 * Math.log((double) (width * height) / (1 << (2 * i))));
        }

        final float mad = avesigma(data, mask);
        final double[] dcoeff = new double[getNumberOfMaxEnabledScale()]; // was fixed to 5.

        for (int i = 0; i < getNumberOfMaxEnabledScale(); i++) {
            dcoeff[i] = getScaleThreshold(i) / 100.0d;
        }
        final double coeffThr = (lambdac[depth + 1] * mad) / dcoeff[depth];

        for (int i = 0; i < data.length; i++) {
            if (data[i] >= coeffThr) {
                // data[ i ] = 255 ;
            }
            else {
                data[i] = 0;
            }
        }
    }

    private static float avesigma(final float[] image, final boolean[] mask) {
        return getMeanAverageDistance(image, mask);
    }

    /**
     * @return mean average distance
     */
    private static float getMeanAverageDistance(final float[] data, final boolean[] mask) {
        final float mean = getMean(data, mask);
        float a = 0;
        float s;

        if (mask == null) {
            for (final float datum : data) {
                s = datum - mean;
                a = a + Math.abs(s);
            }

            if (data.length > 0)
                return a / data.length;
        }
        else {
            float nbValue = 0;
            for (int i = 0; i < data.length; i++) {
                if (mask[i]) {
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

    @Contract(pure = true)
    private static float getVar(final float @NotNull [] data) {
        if (data.length <= 1)
            return 0;

        float sum = 0;
        float sum2 = 0;
        float val;

        for (final float datum : data) {
            val = datum;
            sum2 += val * val;
            sum += val;
        }

        return (sum2 - sum * sum / data.length) / (data.length - 1);
    }

    @Contract(pure = true)
    private static float getMean(final float[] data, final boolean[] mask) {
        float mean = 0;
        float sum = 0;

        if (mask == null) {
            for (final float datum : data) {
                sum += datum;
            }
            if (data.length > 0)
                mean = sum / data.length;
        }
        else {
            float nbValue = 0;
            for (int i = 0; i < data.length; i++) {
                if (mask[i]) {
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

    private @NotNull List<DetectionSpot> convert2detectionList(final Sequence originalSequence, final @NotNull List<ConnectedComponent> components, final int t) {
        final List<DetectionSpot> detectionList = new ArrayList<>();

        for (final ConnectedComponent cc : components) {
            final DetectionSpot detection = new DetectionSpot();

            for (final Point3i point : cc.getIterablePoints())
                detection.points.add(new Point3D(point.x, point.y, point.z));

            detection.setT(t);
            detection.computeMassCenter();
            computeMinMeanMaxIntensity(detection, originalSequence);
            detectionList.add(detection);
        }

        return detectionList;
    }

    private void computeMinMeanMaxIntensity(final @NotNull DetectionSpot detection, final @NotNull Sequence originalSequence) {
        double minIntensity = Double.MAX_VALUE;
        double maxIntensity = Double.MIN_VALUE;
        double sumIntensity = 0;

        final boolean signed = originalSequence.isSignedDataType();
        final int w = originalSequence.getSizeX();
        int z = -1;
        Object data = null;

        for (final Point3D point : detection.points) {
            if ((z != (int) point.z)) {
                z = (int) point.z;
                data = originalSequence.getDataXY(detection.getT(), (int) point.z, 0);
            }

            final double value = Array1DUtil.getValueAsDouble(Objects.requireNonNull(data), ((int) point.y * w) + (int) point.x, signed);

            if (value < minIntensity)
                minIntensity = value;
            if (value > maxIntensity)
                maxIntensity = value;

            sumIntensity += value;
        }

        detection.maxIntensity = maxIntensity;
        detection.minIntensity = minIntensity;

        double averageIntensity = 0;
        if (!detection.points.isEmpty())
            averageIntensity = sumIntensity / detection.points.size();

        detection.meanIntensity = averageIntensity;
    }
}
