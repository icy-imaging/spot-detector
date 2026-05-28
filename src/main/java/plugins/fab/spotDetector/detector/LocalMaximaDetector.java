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

import fr.icy.common.collection.array.Array1DUtil;
import fr.icy.model.image.IcyBufferedImage;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.DetectionSpot;
import plugins.fab.spotDetector.Point3D;

import java.util.ArrayList;
import java.util.Arrays;

//import quia.swip.util.CroppedArray;
//import quia.swip.util.MArray;
//import quia.swip.util.Spot;

/**
 * @author nicolas chenouard/thibault lagache
 */
public class LocalMaximaDetector {
    private final IcyBufferedImage image;// = null;
    // private IcyBufferedImage consideredMA;
    private final boolean[] consideredMA;
    private final int width;
    private final int height;
    private final double distance;

    private boolean[] consideredMax;
    public ArrayList<double[]> maxima;
    private ArrayList<double[]> maximaToSeparate;
    // private ArrayList<ArrayList<double[]>> tmpSeparatedMax;
    // int maxNumMax;
    int numMax;

    public LocalMaximaDetector(final @NotNull IcyBufferedImage image, final double distance) {
        this.image = image;
        numMax = 0;
        width = image.getWidth();
        height = image.getHeight();
        consideredMA = new boolean[width * height];
        Arrays.fill(consideredMA, false);
        this.distance = distance;
    }

    public LocalMaximaDetector(final IcyBufferedImage image) {
        this(image, 10);
    }

    public void detectLocalMaxima() {
        detectLocalMaxima(2);

        /*maxima = new ArrayList<>(50);
        double[] results = Array1DUtil.arrayToDoubleArray(image.getDataXY(0),
                image.isSignedDataType());


        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                if (consideredMA[j * width + i] == false) {
                    consideredMA[j * width + i] = true;
                    if (isMax18connex2D(results, i, j)) {
                        maxima.add(new double[]{i, j, 0});
                        numMax++;
                    }
                }
                else {
                    explore18connex2D(results, i, j);
                }
            }
        }

        maximaToSeparate = maxima;*/
    }

    public int getNumMax() {
        return maxima.size();
    }

    public void detectLocalMaxima(final double minDist) {
        maxima = new ArrayList<>(50);
        final double[] results = Array1DUtil.arrayToDoubleArray(image.getDataXY(0), image.isSignedDataType());

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                if (!consideredMA[j * width + i]) {
                    consideredMA[j * width + i] = true;
                    if (isMax18connex2D(results, i, j)) {
                        maxima.add(new double[]{i, j, 0});
                        numMax++;
                    }
                }
                else {
                    explore18connex2D(results, i, j);
                }
            }
        }

        @SuppressWarnings("unchecked") final ArrayList<double[]> maximaCopy = (ArrayList<double[]>) maxima.clone();
        final double dist2 = minDist * minDist;
        for (int i = 0; i < maxima.size(); i++) {
            final double[] coord1 = maximaCopy.get(i); //i1,j1,z1=0
            for (int j = i + 1; j < maxima.size(); j++) {
                final double[] coord2 = maximaCopy.get(j);//i2,j2,z2=0
                if (Math.pow(coord1[0] - coord2[0], 2) + Math.pow(coord1[1] - coord2[1], 2) < dist2) {
                    if (results[(int) Math.round(coord1[1]) * width + (int) Math.round(coord1[0]) + 1] >
                            results[(int) Math.round(coord2[1]) * width + (int) Math.round(coord2[0]) + 1]) {
                        maxima.remove(coord2);
                    }
                    else {
                        maxima.remove(coord1);
                    }
                }
            }
        }
        maximaToSeparate = maxima;
    }

    public ArrayList<DetectionSpot> getMaximaAsSpots(final int t) {

        final ArrayList<DetectionSpot> spots = new ArrayList<>(maxima.size());
        for (final double[] max : maxima) {
            final DetectionSpot spot = new DetectionSpot();
            final ArrayList<Point3D> points = new ArrayList<>();
            final Point3D point = new Point3D(max);
            points.add(point);
            spot.points = points;
            //spot.setMassCenter(point);
            spot.computeMassCenter();
            spot.setT(t);
            spots.add(spot);

        }
        return spots;
    }

    protected boolean isMax18connex2D(final double @NotNull [] results, final int i, final int j) {
        final double val = results[j * width + i];// ]image.getDataAsDouble(i, j,
        // 0);// filteredArray.get(i, j);
        boolean max = true;
        if (i < width - 1) {
            if (val <= results[j * width + i + 1])
                max = false;
            else
                consideredMA[j * width + (i + 1)] = true;
            if (j < height - 1) {
                if (val <= results[(j + 1) * width + i + 1])
                    max = false;
                else
                    consideredMA[(j + 1) * width + (i + 1)] = true;
            }
        }
        if (j < height - 1) {
            if (val <= results[(j + 1) * width + i])
                max = false;
            else
                consideredMA[(j + 1) * width + i] = true;
            if (i > 0) {
                if (val <= results[(j + 1) * width + i - 1])
                    max = false;
                else
                    consideredMA[(j + 1) * width + i - 1] = true;
            }
        }
        return max;
    }

    protected void explore18connex2D(final double @NotNull [] results, final int i, final int j) {
        final double val = results[j * width + i];
        if (i < width - 1) {
            if (val >= results[j * width + i + 1])
                consideredMA[j * width + (i + 1)] = true;
            if (j < height - 1) {
                if (val >= results[(j + 1) * width + (i + 1)])
                    consideredMA[(j + 1) * width + (i + 1)] = true;
            }
        }
        if (j < height - 1) {
            if (val >= results[(j + 1) * width + i])
                consideredMA[(j + 1) * width + i] = true;
            if (i > 0) {
                if (val >= results[(j + 1) * width + i - 1])
                    consideredMA[(j + 1) * width + i - 1] = true;
            }
        }
    }

    // methods for maxima separation

    // public void initDistance(Vector<GaussProfile2D[]> profiles)
    // {
    // maxSigX = 0;
    // maxSigY = 0;
    // Iterator it = profiles.iterator();
    // while (it.hasNext())
    // {
    // GaussProfile2D[] profile = (GaussProfile2D[])it.next();
    // if (profile[1].get_sigx() > maxSigX)
    // maxSigX = (double)profile[1].get_sigx();
    // if (profile[1].get_sigy() > maxSigY)
    // maxSigX = (double)profile[1].get_sigy();
    // }
    // k = 3;
    // }

    /*
     * private boolean areTooNear(double[] center1, double[] center2, double
     * maxDist) { // if ((Math.abs(center1[0] - center2[0]) < k*maxSigX) &&
     * (Math.abs(center1[1] - center2[1]) < k*maxSigY)) // return true; // if
     * (Math.sqrt(Math.pow(center1[0] - center2[0], 2) + Math.pow(center1[1] -
     * center2[1], 2))< maxDist) // return true;
     *
     * // if (Math.sqrt(Math.pow(center1[0] - center2[0], 2) +
     * Math.pow(center1[1] - center2[1], 2))< maxDist) // return true; // return
     * false; //TODO: use L2 norm or normalized distance for differential
     * resolution double sum2 = 0; for (int i = 0; i< center1.length; i++) { //
     * if(Math.abs(center1[i] - center2[i]) > maxDist) // return false;
     * sum2+=(center1[i] - center2[i])*(center1[i] - center2[i]); } if (sum2 >
     * maxDist*maxDist) return false; return true; } public void
     * setMaxima(ArrayList<double[]> maxima) { this.maxima = maxima; } }
     */
	/*
	 

	/*
	 * protected boolean isMax6connex(int i, int j, int k) { double val =
	 * filteredArray.get(i, j, k); boolean max = true; if (i< width-1) { if (val
	 * < filteredArray.get(i+1, j, k)) max = false; else consideredMA.set(i+1,
	 * j, k, 1); } if (j<height-1) { if (val < filteredArray.get(i, j+1, k)) max
	 * = false; else consideredMA.set(i, j+1, k, 1); } if (k<depth-1) { if (val
	 * < filteredArray.get(i, j, k+1)) max = false; else consideredMA.set(i, j,
	 * k+1, 1); } return max; }
	 * 
	 * protected void explore6connex(int i, int j, int k) { double val =
	 * filteredArray.get(i, j, k); if (i< width-1) { if (val >
	 * filteredArray.get(i+1, j, k)) consideredMA.set(i+1, j, k, 1); } if
	 * (j<height-1) { if (val > filteredArray.get(i, j+1, k))
	 * consideredMA.set(i, j+1, k, 1); } if (k<depth-1) { if (val >
	 * filteredArray.get(i, j, k+1)) consideredMA.set(i, j, k+1, 1); } }
	 * 
	 * public void filterMaximaInArray(double thresholdValue) { for (Iterator it
	 * = maximaToSeparate.iterator(); it.hasNext();) { double[] position =
	 * (double[]) it.next(); int[] p2 = new int[position.length]; for (int i =
	 * 0; i < position.length; i++) p2[i] = (int) position[i]; if (array.get(p2)
	 * < thresholdValue) it.remove(); } }
	 * 
	 * public void filterMaximaInFilteredArray(double thresholdValue) { for
	 * (Iterator it = maximaToSeparate.iterator(); it.hasNext();) { double[]
	 * position = (double[]) it.next(); int[] p2 = new int[position.length]; for
	 * (int i = 0; i < position.length; i++) p2[i] = (int) position[i]; if
	 * (filteredArray.get(p2) < thresholdValue) it.remove(); } numMax =
	 * maximaToSeparate.size(); }
	 */

    /*
     * protected boolean isMax18connex(int i, int j, int k) { double val =
     * filteredArray.get(i, j, k); boolean max = true;
     *
     * if (i< width-1) { if (val <= filteredArray.get(i+1, j, k)) max = false;
     * else consideredMA.set(i+1, j, k, 1); if (j<height-1) { if (val <=
     * filteredArray.get(i+1, j+1, k)) max = false; else consideredMA.set(i+1,
     * j+1, k, 1); if (k<depth-1) { if (val <= filteredArray.get(i+1, j+1, k+1))
     * max = false; else consideredMA.set(i+1, j+1, k+1, 1); } } if (k<depth-1)
     * { if (val <= filteredArray.get(i+1, j, k+1)) max = false; else
     * consideredMA.set(i+1, j, k+1, 1); } } if (j<height-1) { if (val <=
     * filteredArray.get(i, j+1, k)) max = false; else consideredMA.set(i, j+1,
     * k, 1); if (k<depth-1) { if (val <= filteredArray.get(i, j+1, k+1)) max =
     * false; else consideredMA.set(i, j+1, k+1, 1); } } if (k<depth-1) { if
     * (val <= filteredArray.get(i, j, k+1)) max = false; else
     * consideredMA.set(i, j, k+1, 1); } return max; }
     */
    // des cas echappent! se reporter a la methode 2D
    /*
     * protected void explore18connex(int i, int j, int k) {
     *
     * // double val = filteredArray.get(i, j); // if (i< width-1) // { // if
     * (val >= filteredArray.get(i+1, j)) consideredMA.set(i+1, j, 1); // if
     * (j<height-1) // { // if (val >= filteredArray.get(i+1, j+1))
     * consideredMA.set(i+1, j+1, 1); // } // } // if (j<height-1) // { // if
     * (val >= filteredArray.get(i, j+1)) consideredMA.set(i, j+1, 1); // if
     * (i>0) // { // if (val >= filteredArray.get(i-1, j+1))
     * consideredMA.set(i-1, j+1, 1); // } // }
     *
     * double val = filteredArray.get(i, j, k); if (i< width-1) { if (val >=
     * filteredArray.get(i+1, j, k)) consideredMA.set(i+1, j, k, 1); if
     * (j<height-1) { if (val >= filteredArray.get(i+1, j+1, k))
     * consideredMA.set(i+1, j+1, k, 1); if (k<depth-1) { if (val >=
     * filteredArray.get(i+1, j+1, k+1))consideredMA.set(i+1, j+1, k+1, 1); } }
     * if (k<depth-1) { if (val >= filteredArray.get(i+1, j, k+1))
     * consideredMA.set(i+1, j, k+1, 1); } } if (j<height-1) { if (val >=
     * filteredArray.get(i, j+1, k)) consideredMA.set(i, j+1, k, 1); if
     * (k<depth-1) { if (val >= filteredArray.get(i, j+1, k+1))
     * consideredMA.set(i, j+1, k+1, 1); } } if (k<depth-1) { if (val >=
     * array.get(i, j, k+1)) consideredMA.set(i, j, k+1, 1); } }
     */

    /*
     * public LocalMaximaDetector (MArray array, MArray filteredArray, int
     * connexity) { this.array = array; this.filteredArray = filteredArray;
     * numMax = 0; this.connexity = connexity; width = array.getDimSize(0);
     * height = array.getDimSize(1); depth = array.getDimSize(2); consideredMA =
     * new MArray(MArray.SHORT, width, height, depth); consideredMA.setConst(0);
     * }
     */

    /*
     * private void testNeighborhood(double[] m1, int cntM, int pbId, double
     * maxDist) { for (int cntM2 = cntM+1; cntM2 < maximaToSeparate.size();
     * cntM2++) { if (!consideredMax[cntM2]) { double[] m2 =
     * maximaToSeparate.get(cntM2); if (areTooNear(m1, m2, maxDist)) {
     *
     * tmpSeparatedMax.get(pbId).add(new
     * double[]{(double)m2[0],(double)m2[1],(double)m2[2]});
     * consideredMax[cntM2] = true; testNeighborhood(m2, cntM2, pbId, maxDist);
     * } } } }
     */

    /*
     * public void separateWindowMaxima() { if (maxima==null)
     * detectLocalMaxima(); maximaToSeparate = maxima; windows =
     * separateMaxima(7); //TODO: find a best way to choose maxDist }
     */

    /*
     * public void separateWindowMaxima(double maxDist) { if (maxima==null)
     * detectLocalMaxima(); maximaToSeparate = maxima; windows =
     * separateMaxima(maxDist); }
     */

    // be careful windows can be overlapping, even if they have no maxima in
    // common
    /*
     * private ArrayList<MaximaWindow> separateMaxima(double maxDist) {
     * tmpSeparatedMax = new ArrayList<ArrayList<double[]>>(); consideredMax =
     * new boolean[maximaToSeparate.size()]; for (int i =0;
     * i<consideredMax.length; i++) consideredMax[i] = false; int pbId = 0; for
     * (int cntM = 0; cntM < maximaToSeparate.size(); cntM++) { if
     * (!consideredMax[cntM]) { ArrayList<double[]> vect = new
     * ArrayList<double[]>(1); double[] m = maximaToSeparate.get(cntM);
     * vect.add(m); tmpSeparatedMax.add(pbId, vect); testNeighborhood(m, cntM,
     * pbId, maxDist); pbId++; } }
     */

    /*
     * tmpWindows = new ArrayList<MaximaWindow>(tmpSeparatedMax.size()); for
     * (int i = 0; i< tmpSeparatedMax.size(); i++) {
     *
     * ArrayList<double[]> maxima = tmpSeparatedMax.get(i); double minX =
     * maxima.get(0)[0]; double maxX = maxima.get(0)[0]; double minY =
     * maxima.get(0)[1]; double maxY = maxima.get(0)[1]; for (int j = 1; j <
     * maxima.size(); j++) { double[] pos = maxima.get(j); if (pos[0] < minX)
     * minX = pos[0]; else if (pos[0] > maxX) maxX = pos[0]; if (pos[1] < minY)
     * minY = pos[1]; else if (pos[1] > maxY) maxY = pos[1]; } int gap =(int)
     * Math.floor(maxDist/2)+1; if (array.getDimSize(2)<=1)//dimension ==2 {
     * CroppedArray croppedArray = CroppedArray.croppedArrayFactory(array, new
     * int[]{(int)minX-gap, (int)minY-gap }, new int[] {(int)maxX + gap,
     * (int)maxY + gap}); ArrayList<double[]> newMaxima = new
     * ArrayList<double[]>(maxima.size()); ArrayList<double[]> oldMaxima = new
     * ArrayList<double[]>(maxima.size()); for (int j = 0; j < maxima.size();
     * j++) { oldMaxima.add(j,maxima.get(j));
     * newMaxima.add(croppedArray.convertCoordinatesFromOriginalToCrop(new
     * double[]{maxima.get(j)[0], maxima.get(j)[1]})); } tmpWindows.add(i, new
     * MaximaWindow(croppedArray, newMaxima, oldMaxima)); } else //dim ==3 {
     * double minZ = maxima.get(0)[2]; double maxZ = maxima.get(0)[2]; for (int
     * j = 1; j < maxima.size(); j++) { double[] pos = maxima.get(j); if (pos[2]
     * < minZ) minZ = pos[2]; else if (pos[2] > maxZ) maxZ = pos[2]; }
     * CroppedArray croppedArray = CroppedArray.croppedArrayFactory(array, new
     * int[]{(int)minX-gap, (int)minY-gap, (int) minZ - gap}, new int[]
     * {(int)maxX + gap, (int)maxY + gap, (int) maxZ + gap});
     * ArrayList<double[]> newMaxima = new ArrayList<double[]>(maxima.size());
     * ArrayList<double[]> oldMaxima = new ArrayList<double[]>(maxima.size());
     * for (int j = 0; j < maxima.size(); j++) { oldMaxima.add(j,maxima.get(j));
     * newMaxima
     * .add(croppedArray.convertCoordinatesFromOriginalToCrop(maxima.get(j))); }
     * tmpWindows.add(i, new MaximaWindow(croppedArray, newMaxima, oldMaxima));
     * } } return tmpWindows; }
     *
     * /*public ArrayList<MaximaWindow> getMaximaWindows() {return windows;}
     *
     * public ArrayList<MaximaWindow> getSubMaximaWindows() {return
     * subMaximaWindows;}
     */

    /*
     * private void subdivideWindows(double maxDist, MaximaWindow window) {
     * maximaToSeparate = window.getMaxima(); ArrayList<MaximaWindow> resWindows
     * = separateMaxima(maxDist); for (Iterator it = resWindows.iterator();
     * it.hasNext();) { MaximaWindow mw = (MaximaWindow)it.next(); if
     * (mw.getNumMax() > maxNumMax) { subdivideWindows((double)(maxDist-0.5),
     * mw); } else subMaximaWindows.add(mw); } }
     */

    /*
     * public void subdivideWindows(int maxNum) { maxNumMax = maxNum; double
     * maxDist = 10; subMaximaWindows = new ArrayList<MaximaWindow>(); for
     * (Iterator it = windows.iterator(); it.hasNext();) { MaximaWindow mw =
     * (MaximaWindow)it.next(); if (mw.getNumMax() > maxNumMax) {
     * subdivideWindows((double)(maxDist-0.5), mw); } else
     * subMaximaWindows.add(mw); } } }}
     */
}
