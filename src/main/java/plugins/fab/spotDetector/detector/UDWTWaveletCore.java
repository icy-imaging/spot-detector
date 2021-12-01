package plugins.fab.spotDetector.detector;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3i;

import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
import icy.type.TypeUtil;
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

    private void addConnectedComponentDetection(Sequence binarySequence, ArrayList<DetectionSpot> detectionList, int t,
            Sequence originalSequence)
    {
        Map<Integer, List<ConnectedComponent>> result = ConnectedComponents.extractConnectedComponents(binarySequence,
                0, ConnectedComponents.ExtractionType.BACKGROUND, 0, Integer.MAX_VALUE, null);

        detectionList.addAll(convert2detectionList(originalSequence, result, t));

        // LabelExtractor labelExtractor = new LabelExtractor();

        // List<ROI> roiList = labelExtractor.extractLabels( binarySequence, LabelExtractor.ExtractionType.ANY_LABEL_VS_BACKGROUND , 0 );
        //
        // for ( ROI roi : roiList )
        // {
        // Icy.getMainInterface().getActiveSequence().addROI( roi );
        // System.out.println( roi );
        // }

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

        // display UDWTScaleArrayList for debug

        // for ( UDWTScale scale : UDWTScaleArrayList )
        // {
        // System.out.println( "scale # " + UDWTScaleArrayList.indexOf( scale ) + " enabled : " + scale.isEnabled() + " threshold:" + scale.getThreshold() );
        // }

        //

        ArrayList<DetectionSpot> detectionList = new ArrayList<DetectionSpot>();

        int numScales = getNumberOfMaxEnabledScale();

        // System.out.println("Num scales enabled : " + numScales );

        Sequence inputSequence = sequenceIn;

        if (inputSequence == null)
            return detectionList;
        if (inputSequence.getAllImage().size() == 0)
            return detectionList;

        // Sequence binSequence = new Sequence();
        binarySequence = new Sequence();
        binarySequence.setName("Binary Sequence"); // Check if this name already exists and change it if needed.

        final List<ROI> rois = new ArrayList<>();
        final Rectangle bounds = sequenceIn.getBounds2D();

        // rois used for WAT computation ?
        if (useROIforWATcomputation)
        {
            rois.addAll(sequenceIn.getROIs());
            // by default we use Sequence bounds
            if (rois.isEmpty())
                rois.add(new ROI2DRectangle(bounds));
        }

        //
        // 2D Image
        //

        if (inputSequence.getSizeZ() == 1)
        {
            {
                B3SplineUDWT waveletTransform = new B3SplineUDWT();
                if (waveletTransform.isNumberOfScaleOkForImage2D(inputSequence.getSizeX(), inputSequence.getSizeY(),
                        getNumberOfMaxEnabledScale()) == false)
                {
                    System.err.println("Scale configuration error");
                    return detectionList;
                }
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++)
            {
                // check for interruption from time to time as this can be a long process
                if (Thread.interrupted())
                    throw new InterruptedException("SpotDetector.computeDetection(..) process interrupted.");

                // IcyBufferedImage image = inputSequence.getFirstImage();
                IcyBufferedImage image = inputSequence.getImage(t, 0);

                if (image != null)
                {
                    // build boolean mask for this frame
                    final boolean[] mask = buildBinaryMask(bounds, rois, t, 0);

                    // compute scales
                    final float dataIn[] = Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());
                    // decompose the image
                    B3SplineUDWT waveletTransform = new B3SplineUDWT();

                    float[][] scales;
                    try
                    {
                        scales = waveletTransform.b3WaveletScales2D(dataIn, image.getWidth(), image.getHeight(),
                                numScales);
                    }
                    catch (WaveletConfigException e1)
                    {
                        e1.printStackTrace();
                        return detectionList;
                    }
                    float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, numScales,
                            image.getWidth() * image.getHeight());
                    Sequence resultSequence = new Sequence();
                    resultSequence.setName("WaveletCoefficients");

                    // Apply threshold to coefficients but not last one ( residual )

                    // Filtering
                    for (int i = 0; i < coefficients.length - 1; i++)
                    {
                        if (detectNegative)
                        {
                            for (int ii = 0; ii < coefficients[i].length; ii++)
                            {
                                coefficients[i][ii] = -coefficients[i][ii];
                            }
                        }

                        filter_wat(coefficients[i], i, inputSequence.getWidth(), inputSequence.getHeight(), mask);
                    }

                    // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                    { // fill of 0 the residual image
                        float c[] = coefficients[coefficients.length - 1];
                        for (int i = 0; i < c.length; i++)
                        {
                            c[i] = 0;
                        }
                    }

                    // Icy.addSequence(resultSequence);
                    resultSequence.dataChanged();
                    // reconstruct the image from the wavelet coefficients
                    final IcyBufferedImage imageOut = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1,
                            TypeUtil.TYPE_BYTE);
                    float[] binaryDetectionResult = new float[image.getWidth() * image.getHeight()];

                    // waveletTransform.b3WaveletReconstruction2D(coefficients, coefficients[numScales], binaryDetectionResult, numScales,
                    // image.getWidth()*image.getHeight());
                    waveletTransform.b3SpotConstruction2D(coefficients, binaryDetectionResult, numScales,
                            image.getWidth() * image.getHeight(), UDWTScaleArrayList);

                    // Binarisation de la reconstruction.
                    {
                        for (int i = 0; i < binaryDetectionResult.length; i++)
                        {
                            if (binaryDetectionResult[i] != 0) // >
                            {
                                binaryDetectionResult[i] = 255;
                            }
                            else
                            {
                                binaryDetectionResult[i] = 0;
                            }
                        }
                    }

                    ArrayUtil.arrayToArray(binaryDetectionResult, imageOut.getDataXY(0), image.isSignedDataType());

                    Sequence binarySequenceForConnectedComponentExtraction = new Sequence();
                    binarySequenceForConnectedComponentExtraction.setImage(0, 0, imageOut);
                    addConnectedComponentDetection(binarySequenceForConnectedComponentExtraction, detectionList, t,
                            inputSequence);

                    if (computeBinaryDetection)
                    {
                        binarySequence.setImage(t, 0, imageOut);
                    }

                }
            }
        }

        //
        // 3D Image ( with the force2DWavelets for 3D enabled )
        //

        if (inputSequence.getSizeZ() > 1 && force2DWaveletsFor3D == true)
        {
            System.out.println("Entering force 2D wavelet for 3D");
            {
                B3SplineUDWT waveletTransform = new B3SplineUDWT();
                if (waveletTransform.isNumberOfScaleOkForImage2D(inputSequence.getSizeX(), inputSequence.getSizeY(),
                        getNumberOfMaxEnabledScale()) == false)
                {
                    System.err.println("Scale configuration error (your image is too small for this scale)");
                    return detectionList;
                }
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++)
            {
                Sequence binary3DOutput = new Sequence();

                for (int z = 0; z < sequenceIn.getSizeZ(); z++)
                {
                    // build boolean mask for this frame / slice
                    final boolean[] mask = buildBinaryMask(bounds, rois, t, z);

                    // IcyBufferedImage image = inputSequence.getFirstImage();
                    IcyBufferedImage image = inputSequence.getImage(t, z);

                    // compute scales

                    final float dataIn[] = Array1DUtil.arrayToFloatArray(image.getDataXY(0), image.isSignedDataType());
                    // decompose the image
                    B3SplineUDWT waveletTransform = new B3SplineUDWT();

                    float[][] scales;
                    try
                    {
                        scales = waveletTransform.b3WaveletScales2D(dataIn, image.getWidth(), image.getHeight(),
                                numScales);
                    }
                    catch (WaveletConfigException e1)
                    {
                        e1.printStackTrace();
                        return detectionList;
                    }
                    float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, numScales,
                            image.getWidth() * image.getHeight());
                    Sequence resultSequence = new Sequence();
                    resultSequence.setName("WaveletCoefficients");

                    // Apply threshold to coefficients but not last one ( residual )

                    // Filtering
                    for (int i = 0; i < coefficients.length - 1; i++)
                    {
                        if (detectNegative)
                        {
                            for (int ii = 0; ii < coefficients[i].length; ii++)
                            {
                                coefficients[i][ii] = -coefficients[i][ii];
                            }
                        }

                        filter_wat(coefficients[i], i, inputSequence.getWidth(), inputSequence.getHeight(), mask);

                    }

                    // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                    { // fill of 0 the residual image
                        float c[] = coefficients[coefficients.length - 1];
                        for (int i = 0; i < c.length; i++)
                        {
                            c[i] = 0;
                        }
                    }

                    // Icy.addSequence(resultSequence);
                    resultSequence.dataChanged();
                    // reconstruct the image from the wavelet coefficients
                    final IcyBufferedImage imageOut = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1,
                            TypeUtil.TYPE_BYTE);
                    float[] binaryDetectionResult = new float[image.getWidth() * image.getHeight()];

                    // waveletTransform.b3WaveletReconstruction2D(coefficients, coefficients[numScales], binaryDetectionResult, numScales,
                    // image.getWidth()*image.getHeight());
                    waveletTransform.b3SpotConstruction2D(coefficients, binaryDetectionResult, numScales,
                            image.getWidth() * image.getHeight(), UDWTScaleArrayList);

                    // Binarisation de la reconstruction.
                    {
                        for (int i = 0; i < binaryDetectionResult.length; i++)
                        {
                            if (binaryDetectionResult[i] != 0) // >
                            {
                                binaryDetectionResult[i] = 255;
                            }
                            else
                            {
                                binaryDetectionResult[i] = 0;
                            }
                        }
                    }

                    ArrayUtil.arrayToArray(binaryDetectionResult, imageOut.getDataXY(0), image.isSignedDataType());

                    // Sequence binarySequenceForConnectedComponentExtraction = new Sequence();
                    binary3DOutput.setImage(0, z, imageOut);
                    // binarySequenceForConnectedComponentExtraction.setImage(0, 0, imageOut);

                    if (computeBinaryDetection)
                    {
                        binarySequence.setImage(t, z, imageOut);
                    }

                }

                // Icy.getMainInterface().addSequence( binary3DOutput );
                addConnectedComponentDetection(binary3DOutput, detectionList, t, inputSequence);

            }

            // ******* FIN PARTIE 2D

            // {
            // B3SplineUDWT waveletTransform = new B3SplineUDWT();
            // if ( waveletTransform.isNumberOfScaleOkForImage3D(
            // inputSequence.getSizeX(),
            // inputSequence.getSizeY(), inputSequence.getSizeZ(),
            // getNumberOfMaxEnabledScale() ) == false )
            // {
            // int scale2 = waveletTransform.getMinSize( 2 );
            // int scale3 = waveletTransform.getMinSize( 3 );
            // int scale4 = waveletTransform.getMinSize( 4 );
            //
            // MessageDialog.showDialog( "<html><center>There is a problem with the scale configuration (but don't worry, read this):" +
            // "<br><br>To run in 3D, the wavelet algorithm needs a number of slices depending on the scale(s) you enabled:" +
            // "<br>"+
            // "<br>Scale 2 : "+ scale2 + " slices"+
            // "<br>Scale 3 : "+ scale3 + " slices"+
            // "<br>Scale 4 : "+ scale4 + " slices"+
            // "<br>"+
            // "<br>To use those parameters you need more Z in your stack.<br>Still, you can bypass this problem by selecting" +
            // "<br>the option <b>Force use of 2D Wavelets for 3D</b> in the detector panel" +
            // "<br>In this case, each 2D slices will be computed separately and results will be merged to create the resulting stack," +
            // "<br>(and you don't need to add more Z slices)," +
            // "</center></html>" , MessageDialog.WARNING_MESSAGE );
            //
            // return detectionList;
            // }
            // }
            //
            // for ( int t = 0 ; t < sequenceIn.getSizeT() ; t++ )
            // {
            // final float[][] dataIn = new float[inputSequence.getSizeZ()][];
            // for(int z=0; z<inputSequence.getSizeZ(); z++)
            // dataIn[z] = Array1DUtil.arrayToFloatArray(inputSequence.getDataXY(t, z, 0), inputSequence.isSignedDataType());
            // B3SplineUDWT waveletTransform = new B3SplineUDWT();
            //
            // float[][][] scales;
            // try {
            // scales = waveletTransform.b3WaveletScales3D(dataIn, inputSequence.getSizeX(), inputSequence.getSizeY(), inputSequence.getSizeZ(), numScales);
            // } catch (WaveletConfigException e1) {
            // e1.printStackTrace();
            // return detectionList;
            // }
            // float[][][] coefficients = waveletTransform.b3WaveletCoefficients3D(scales, dataIn, numScales, inputSequence.getSizeX()*inputSequence.getSizeY(),
            // inputSequence.getSizeZ());
            //
            // // Apply threshold to coefficients but not last one ( residual )
            //
            // // Filtering
            //
            // for ( int scale = 0 ; scale < coefficients.length-1 ; scale++ )
            // {
            // for ( int z = 0 ; z < coefficients[scale].length ; z++ )
            // {
            // if ( detectNegative )
            // {
            // for ( int ii = 0 ; ii < coefficients[scale][z].length ; ii++ )
            // {
            // coefficients[scale][z][ii]=-coefficients[scale][z][ii];
            // }
            // }
            //
            // filter_wat( coefficients[scale][z] , scale , inputSequence.getWidth() , inputSequence.getHeight() , mask );
            // }
            // }
            //
            // // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
            // { // fill of 0 the residual image
            // float c[][] = coefficients[coefficients.length-1];
            // for ( int z = 0 ; z < c.length ; z++ )
            // {
            // for ( int i = 0 ; i < c[z].length ; i++ )
            // {
            // c[z][i]=0;
            // }
            // }
            // }
            //
            // //reconstruct the image from the wavelet coefficients
            // //Sequence reconstructionSequence = new Sequence();
            // float[][] reconstruction = new float[inputSequence.getSizeZ()][inputSequence.getSizeX()*inputSequence.getSizeY()];
            // //waveletTransform.b3WaveletReconstruction3D(coefficients, coefficients[numScales], reconstruction, numScales,
            // inputSequence.getSizeX()*inputSequence.getSizeY(), inputSequence.getSizeZ());
            // waveletTransform.b3SpotConstruction3D(coefficients, reconstruction, numScales, inputSequence.getSizeX()*inputSequence.getSizeY(),
            // inputSequence.getSizeZ() , UDWTScaleArrayList );
            //
            // //waveletTransform.b3SpotConstruction2D( coefficients, binaryDetectionResult, numScales, image.getWidth()*image.getHeight() , UDWTScaleArrayList
            // );
            //
            // // Binarisation de la reconstruction.
            //
            // for ( int z = 0 ; z < reconstruction.length ; z++ )
            // {
            // for ( int i = 0 ; i < reconstruction[z].length ; i++ )
            // {
            //
            // if ( reconstruction[z][i] != 0 ) // > 0
            // {
            // reconstruction[z][i] = 255;
            // }else
            // {
            // reconstruction[z][i] = 0;
            // }
            // }
            // }
            //
            // VolumetricImage vOut = new VolumetricImage();
            // for (int z=0; z<inputSequence.getSizeZ(); z++)
            // {
            // final IcyBufferedImage imageOut = new IcyBufferedImage(inputSequence.getSizeX(), inputSequence.getSizeY(), 1, TypeUtil.TYPE_BYTE);
            // ArrayUtil.arrayToArray(reconstruction[z], imageOut.getDataXY(0), true);
            // vOut.setImage(z, imageOut);
            // }
            // Sequence binarySequenceForConnectedComponentExtraction = new Sequence();
            // binarySequenceForConnectedComponentExtraction.addVolumetricImage( 0 , vOut );
            // addConnectedComponentDetection( binarySequenceForConnectedComponentExtraction , detectionList , t , inputSequence );
            //
            //
            // if ( computeBinaryDetection )
            // {
            // binarySequence.addVolumetricImage(t, vOut);
            // }
            // }

        }

        //
        // 3D Image ( classic, without the force2DWavelets for 3D enabled )
        //

        if (inputSequence.getSizeZ() > 1 && force2DWaveletsFor3D == false)
        {
            {
                B3SplineUDWT waveletTransform = new B3SplineUDWT();
                if (waveletTransform.isNumberOfScaleOkForImage3D(inputSequence.getSizeX(), inputSequence.getSizeY(),
                        inputSequence.getSizeZ(), getNumberOfMaxEnabledScale()) == false)
                {
                    int scale2 = waveletTransform.getMinSize(2);
                    int scale3 = waveletTransform.getMinSize(3);
                    int scale4 = waveletTransform.getMinSize(4);

                    MessageDialog.showDialog(
                            "<html><center>There is a problem with the scale configuration (but don't worry, read this):"
                                    + "<br><br>To run in 3D, the wavelet algorithm needs a number of slices depending on the scale(s) you enabled:"
                                    + "<br>" + "<br>Scale 2 : " + scale2 + " slices" + "<br>Scale 3 : " + scale3
                                    + " slices" + "<br>Scale 4 : " + scale4 + " slices" + "<br>"
                                    + "<br>To use those parameters you need more Z in your stack.<br>Still, you can bypass this problem by selecting"
                                    + "<br>the option <b>Force use of 2D Wavelets for 3D</b> in the detector panel"
                                    + "<br>In this case, each 2D slices will be computed separately and results will be merged to create the resulting stack,"
                                    + "<br>(and you don't need to add more Z slices),"
                                    + "<br>The sequence file name is: " + inputSequence.getFilename()
                                    + "</center></html>",
                            MessageDialog.WARNING_MESSAGE);

                    return detectionList;
                }
            }

            for (int t = 0; t < sequenceIn.getSizeT(); t++)
            {
                final float[][] dataIn = new float[inputSequence.getSizeZ()][];
                for (int z = 0; z < inputSequence.getSizeZ(); z++)
                    dataIn[z] = Array1DUtil.arrayToFloatArray(inputSequence.getDataXY(t, z, 0),
                            inputSequence.isSignedDataType());
                B3SplineUDWT waveletTransform = new B3SplineUDWT();

                float[][][] scales;
                try
                {
                    scales = waveletTransform.b3WaveletScales3D(dataIn, inputSequence.getSizeX(),
                            inputSequence.getSizeY(), inputSequence.getSizeZ(), numScales);
                }
                catch (WaveletConfigException e1)
                {
                    e1.printStackTrace();
                    return detectionList;
                }
                float[][][] coefficients = waveletTransform.b3WaveletCoefficients3D(scales, dataIn, numScales,
                        inputSequence.getSizeX() * inputSequence.getSizeY(), inputSequence.getSizeZ());

                // Apply threshold to coefficients but not last one ( residual )

                // Filtering

                final boolean[][] masks = new boolean[inputSequence.getSizeZ()][];

                // build boolean masks for all slices
                for (int z = 0; z < masks.length; z++)
                    masks[z] = buildBinaryMask(bounds, rois, t, z);

                for (int scale = 0; scale < coefficients.length - 1; scale++)
                {
                    for (int z = 0; z < coefficients[scale].length; z++)
                    {
                        if (detectNegative)
                        {
                            for (int ii = 0; ii < coefficients[scale][z].length; ii++)
                            {
                                coefficients[scale][z][ii] = -coefficients[scale][z][ii];
                            }
                        }

                        filter_wat(coefficients[scale][z], scale, inputSequence.getWidth(), inputSequence.getHeight(),
                                masks[z]);
                    }
                }

                // TODO: Instead of putting it to residual image 0 , remove it from reconstruction
                { // fill of 0 the residual image
                    float c[][] = coefficients[coefficients.length - 1];
                    for (int z = 0; z < c.length; z++)
                    {
                        for (int i = 0; i < c[z].length; i++)
                        {
                            c[z][i] = 0;
                        }
                    }
                }

                // reconstruct the image from the wavelet coefficients
                // Sequence reconstructionSequence = new Sequence();
                float[][] reconstruction = new float[inputSequence.getSizeZ()][inputSequence.getSizeX()
                        * inputSequence.getSizeY()];
                // waveletTransform.b3WaveletReconstruction3D(coefficients, coefficients[numScales], reconstruction, numScales,
                // inputSequence.getSizeX()*inputSequence.getSizeY(), inputSequence.getSizeZ());
                waveletTransform.b3SpotConstruction3D(coefficients, reconstruction, numScales,
                        inputSequence.getSizeX() * inputSequence.getSizeY(), inputSequence.getSizeZ(),
                        UDWTScaleArrayList);

                // waveletTransform.b3SpotConstruction2D( coefficients, binaryDetectionResult, numScales, image.getWidth()*image.getHeight() ,
                // UDWTScaleArrayList );

                // Binarisation de la reconstruction.

                for (int z = 0; z < reconstruction.length; z++)
                {
                    for (int i = 0; i < reconstruction[z].length; i++)
                    {

                        if (reconstruction[z][i] != 0) // > 0
                        {
                            reconstruction[z][i] = 255;
                        }
                        else
                        {
                            reconstruction[z][i] = 0;
                        }
                    }
                }

                VolumetricImage vOut = new VolumetricImage();
                for (int z = 0; z < inputSequence.getSizeZ(); z++)
                {
                    final IcyBufferedImage imageOut = new IcyBufferedImage(inputSequence.getSizeX(),
                            inputSequence.getSizeY(), 1, TypeUtil.TYPE_BYTE);
                    ArrayUtil.arrayToArray(reconstruction[z], imageOut.getDataXY(0), true);
                    vOut.setImage(z, imageOut);
                }
                Sequence binarySequenceForConnectedComponentExtraction = new Sequence();
                binarySequenceForConnectedComponentExtraction.addVolumetricImage(0, vOut);
                addConnectedComponentDetection(binarySequenceForConnectedComponentExtraction, detectionList, t,
                        inputSequence);

                if (computeBinaryDetection)
                {
                    binarySequence.addVolumetricImage(t, vOut);
                }
            }

        }
        binarySequence.dataChanged();

        if (computeBinaryDetection)
        {
            // Icy.addSequence(binarySequence);
            // should now be retrive using getBinarySequence() directly from the caller.
        }

        return detectionList;

    }

    private boolean[] buildBinaryMask(Rectangle bounds, List<ROI> rois, int t, int z) throws InterruptedException
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

        ImageMathInfo imageCaract = avesigma(data, mask);

        double dcoeff[] = new double[getNumberOfMaxEnabledScale()]; // was fixed to 5.

        for (int i = 0; i < getNumberOfMaxEnabledScale(); i++)
        {
            dcoeff[i] = getScaleThreshold(i) / 100.0d;
        }
        double coeffThr = (lambdac[depth + 1] * imageCaract.mad) / dcoeff[depth];

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

    private class ImageMathInfo
    {
        double mad;
    }

    /**
     * @param m
     * @return
     */
    private ImageMathInfo avesigma(float[] image, boolean mask[])
    {
        ImageMathInfo ic = new ImageMathInfo();
        ic.mad = getMeanAverageDistance(image, mask);
        return ic;
    }

    /**
     * @return mean average distance
     */
    private float getMeanAverageDistance(float[] data, boolean mask[])
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

    private float getVar(float[] data)
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

    private float getMean(float[] data, boolean mask[])
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

    private ArrayList<DetectionSpot> convert2detectionList(Sequence originalSequence,
            Map<Integer, List<ConnectedComponent>> result, int t)
    {

        ArrayList<DetectionSpot> detectionList = new ArrayList<DetectionSpot>();

        for (int i : result.keySet())
        {
            List<ConnectedComponent> list = result.get(i);
            for (ConnectedComponent cc : list)
            {
                DetectionSpot detection = new DetectionSpot();

                for (Point3i point : cc.getPoints())
                {
                    detection.points.add(

                            new Point3D(point.x, point.y, point.z));
                }

                detection.setT(t);
                detection.computeMassCenter();
                computeMinMeanMaxIntensity(detection, originalSequence);
                detectionList.add(detection);

            }
        }

        return detectionList;

    }

    private void computeMinMeanMaxIntensity(DetectionSpot detection, Sequence originalSequence)
    {

        double minIntensity = Double.MAX_VALUE;
        double maxIntensity = Double.MIN_VALUE;
        double sumIntensity = 0;

        for (Point3D point : detection.points)
        {

            IcyBufferedImage image = originalSequence.getImage(detection.getT(), (int) point.z);
            double value = image.getData((int) point.x, (int) point.y, 0);

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
        {
            averageIntensity = sumIntensity / detection.points.size();
        }
        detection.meanIntensity = averageIntensity;

    }

}
