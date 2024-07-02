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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
    @Contract(pure = true)
    public DetectionSpot() {
        points = new ArrayList<>();
        massCenter = null;
    }

    /**
     *
     */
    int t;

    /**
     * Set time position
     *
     * @param t time position
     */
    public void setT(final int t) {
        this.t = t;
    }

    /**
     * @return mass center position
     */
    public Point3D getMassCenter() {
        if (massCenter == null)
            computeMassCenter();
        return massCenter;
    }

    /**
     * @return time position
     */
    public int getT() {
        return t;
    }

    /**
     * Compute mass center position of the spot
     */
    public void computeMassCenter() {
        massCenter = new Point3D();
        if (points.isEmpty())
            return;

        double x = 0;
        double y = 0;
        double z = 0;
        for (final Point3D p : points) {
            x += p.x;
            y += p.y;
            z += p.z;
        }
        x /= points.size();
        y /= points.size();
        z /= points.size();

        massCenter.x = x;
        massCenter.y = y;
        massCenter.z = z;
    }

    /**
     * Set mass center position
     *
     * @param point3d position to set
     */
    public void setMassCenter(final @NotNull Point3D point3d) {
        massCenter.x = point3d.x;
        massCenter.y = point3d.y;
        massCenter.z = point3d.z;
    }
}
