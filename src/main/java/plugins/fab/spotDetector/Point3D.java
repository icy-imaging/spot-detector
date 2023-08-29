/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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

import java.awt.geom.Point2D;

public class Point3D {

    @Override
    public String toString() {
        return "Point3D[" + x + "," + y + "," + z + "]";
    }

    public double x, y, z;

    public Point3D() {
    }

    public Point3D(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point3D(final double x, final double y) {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    public Point3D(final double[] coord) {
        this.x = coord[0];
        this.y = coord[1];
        if (coord.length > 2)
            this.z = coord[2];
        else
            this.z = 0;
    }

    public Point2D asPoint2D() {
        return new Point2D.Double(x, y);
    }

}
