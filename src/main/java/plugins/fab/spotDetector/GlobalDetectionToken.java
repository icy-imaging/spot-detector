/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

package plugins.fab.spotDetector;

import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.sequence.Sequence;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The global detection token is created at the begininning of the process and is then send from
 * one plugin to the next, so that they can fill the ROIs, detection and provide some instance of
 * class which be used
 * at saving or ouput.
 *
 * @author Fabrice de Chaumont
 */
public class GlobalDetectionToken {
    /**
     * The original sequence.
     * Should not be modified. Used to paint the final painter on it.
     */
    public Sequence inputSequence; // TODO: Should be changed to a list for sequencial work (i.e: live aquisition)

    /**
     * The sequence to compute
     * <p>
     * Could be changed by the preprocessor to provide the channel to look for.
     */
    public Sequence inputComputationSequence;

    public ArrayList<ROI> roiArrayList = new ArrayList<>();
    public ROISaver roiSaver = null;

    public ArrayList<DetectionSpot> detectionResult;

    /**
     * Result of detection by ROI
     * filled by the ROI Processor
     */
    public HashMap<ROI, ArrayList<DetectionSpot>> roi2detection;

    ArrayList<GlobalDetectionTokenListener> GlobalDetectionTokenListenerList = new ArrayList<>();

    public void addGlobalDetectionTokenListener(final GlobalDetectionTokenListener gdtl) {
        GlobalDetectionTokenListenerList.add(gdtl);
    }

    public void removeGlobalDetectionTokenListener(final GlobalDetectionTokenListener gdtl) {
        GlobalDetectionTokenListenerList.remove(gdtl);
    }

    public void fireDetectionFinished() {
        for (final GlobalDetectionTokenListener gdtl : GlobalDetectionTokenListenerList) {
            gdtl.detectionFinished();
        }
    }
}





