/*
 * Copyright 2010, 2011 Institut Pasteur.
 * 
 * This file is part of ICY.
 * 
 * ICY is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ICY is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ICY. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.spotDetector;

import java.util.ArrayList;
import java.util.HashMap;

import icy.roi.ROI;
import icy.sequence.Sequence;


/**
 * The global detection token is created at the begininning of the process and is then send from
 * one plugin to the next, so that they can fill the ROIs, detection and provide some instance of
 * class which be used
 * at saving or ouput.
 * 
 * @author Fabrice de Chaumont
 */
public class GlobalDetectionToken
{
	/** 
	 * The original sequence. 
	 * Should not be modified. Used to paint the final painter on it.
	 * 
	 * */
	public Sequence inputSequence; // TODO: Should be changed to a list for sequencial work (i.e: live aquisition)
	
	/** 
	 * The sequence to compute
	 * 
	 * Could be changed by the preprocessor to provide the channel to look for.
	 * 
	 * */
	public Sequence inputComputationSequence;
	
	public ArrayList<ROI> roiArrayList = new ArrayList<ROI>();
	public ROISaver roiSaver = null;

	public ArrayList<DetectionSpot> detectionResult;

	/**
	 * Result of detection by ROI
	 * filled by the ROI Processor
	 */
	public HashMap<ROI, ArrayList<DetectionSpot>> roi2detection;

	// FIXME: should be removed. debug only
	public Sequence binarySequence;
    
	ArrayList<GlobalDetectionTokenListener> GlobalDetectionTokenListenerList = new ArrayList<GlobalDetectionTokenListener>(); 
	
	public void addGlobalDetectionTokenListener( GlobalDetectionTokenListener gdtl )
	{
		GlobalDetectionTokenListenerList.add( gdtl );
	}
    
	public void removeGlobalDetectionTokenListener( GlobalDetectionTokenListener gdtl )
	{
		GlobalDetectionTokenListenerList.remove( gdtl );
	}

	public void fireDetectionFinished()
	{
		for ( GlobalDetectionTokenListener gdtl : GlobalDetectionTokenListenerList )
		{
			gdtl.detectionFinished();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}





