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

import java.awt.geom.Point2D;
import java.io.Serializable;

public class Point3D
{

    @Override
    public String toString()
    {

        return "Point3D[" + x + "," + y + "," + z + "]";
    }

    public double x, y, z;

    public Point3D()
    {
    }

    public Point3D(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point3D(double x, double y)
    {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    public Point3D(double[] coord)
    {
        this.x = coord[0];
        this.y = coord[1];
        if (coord.length > 2)
            this.z = coord[2];
        else
            this.z = 0;
    }

    public Point2D asPoint2D()
    {
        return new Point2D.Double(x, y);
    }

}
