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


public class DetectionSpot {

	/**
	 * mass center of detection
	 */
	Point3D massCenter;
	
    /**
     * Point contained in detection
     */
    public ArrayList<Point3D> points;
    /**
     * Mean value of pixel in GrayLevel of the pixel in the group.
     */
    public double meanIntensity = 0;
    /**
     * Min value of pixel in GrayLevel of the pixel in the group.
     */
    public double minIntensity;
    /**
     * Max value of pixel in GrayLevel of the pixel in the group.
     */
    public double maxIntensity;

    /**
     * 
     */
    public DetectionSpot()
    {
        points = new ArrayList<Point3D>();
        massCenter = null;
    }

    /**
     * 
     */
    int t;

    /**
     * 
     * @param t
     */
	public void setT(int t) {
		this.t = t;	
	}

	public Point3D getMassCenter() {
		
		if ( massCenter == null ) computeMassCenter();
		return massCenter;
	}

	public int getT() {		
		return t;
	}

	public void computeMassCenter() {
		
		massCenter = new Point3D();
		if ( points.size() == 0 ) return;
		
		double x=0;
		double y=0;
		double z=0;
		for ( Point3D p : points )
		{
			x+= p.x;
			y+= p.y;
			z+= p.z;			
		}
		x /= points.size();
		y /= points.size();
		z /= points.size();
		
		massCenter.x = x;
		massCenter.y = y;
		massCenter.z = z;
		
		
		
	}

	public void setMassCenter(Point3D point3d) {
		massCenter.x = point3d.x;
		massCenter.y = point3d.y;
		massCenter.z = point3d.z;
	}

	
	
	
}
