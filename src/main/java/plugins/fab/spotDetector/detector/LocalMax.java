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

import org.bioimageanalysis.icy.common.collection.array.Array1DUtil;
import org.bioimageanalysis.icy.common.type.DataType;
import org.bioimageanalysis.icy.model.image.IcyBufferedImage;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;

import javax.vecmath.Point3i;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class LocalMax {
    public static @NotNull ArrayList<Point3i> localMax(
            final @NotNull Sequence in,
            final Sequence out,
            final int t,
            final int c,
            final double threshold,
            final int halfSizeX,
            final int halfSizeY,
            final int halfSizeZ
    ) {
        final int w = in.getSizeX();
        final int h = in.getSizeY();
        final int d = in.getSizeZ();

        final Object array_z_xy = in.getDataXYZ(t, c);
        final DataType dataType = in.getDataType();

        int i, j, k, nx, ny, nz;

        final ArrayList<Point3i> localmax = new ArrayList<>();
        byte[] outputSlice = null;

        for (k = 0; k < d; k++) {
            int xyOffset = 0;
            final Object array_xy = Array.get(array_z_xy, k);

            if (out != null) outputSlice = new byte[w * h];

            for (j = 0; j < h; j++) {
                mainLoop:
                for (i = 0; i < w; i++) //, xyOffset++)
                {
                    final double value = Array1DUtil.getValue(array_xy, xyOffset, dataType);

                    xyOffset = j * w + i;

                    if (value <= threshold) continue mainLoop;

                    for (nz = -halfSizeZ; nz <= halfSizeZ; nz++)
                        for (ny = -halfSizeY; ny <= halfSizeY; ny++)
                            neighborhood:for (nx = -halfSizeX; nx <= halfSizeX; nx++) {
                                // check that pixel (i+nx,j+ny,k+nz) is inside the image

                                if (
                                        i + nx < 0 ||
                                                i + nx >= w ||
                                                j + ny < 0 ||
                                                j + ny >= h ||
                                                k + nz < 0 ||
                                                k + nz >= d) continue neighborhood;

                                if (value <
                                        Array1DUtil.getValue(
                                                Array.get(array_z_xy, k + nz),
                                                xyOffset + nx + ny * w, dataType)
                                ) continue mainLoop;

//								Array.get(array_z_xy, nz),
//								xyOffset + nx + ny * w, dataType)

                            }

                    // code runs here => (i,j,k) is a local maximum
                    localmax.add(new Point3i(i, j, k));
                    if (out != null) outputSlice[xyOffset] = (byte) 0xff;
                }
            }

            if (out != null) out.setImage(t, k, new IcyBufferedImage(w, h, new byte[][]{outputSlice}));
        }
        return localmax;
    }
}
