package plugins.fab.spotDetector.detector;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;

import javax.vecmath.Point3i;

public class LocalMax
{
	
	public static ArrayList<Point3i> localMax(
			Sequence in, Sequence out, int t, int c, double threshold, int halfSizeX, int halfSizeY, int halfSizeZ)
	{
		int w = in.getSizeX();
		int h = in.getSizeY();
		int d = in.getSizeZ();
		
		Object array_z_xy = in.getDataXYZ(t, c);
		DataType dataType = in.getDataType_();
		
		int i, j, k, nx, ny, nz;
		
		ArrayList<Point3i> localmax = new ArrayList<Point3i>();
		byte[] outputSlice = null;
		
		for (k = 0; k < d; k++)
		{
			int xyOffset = 0;
			Object array_xy = Array.get(array_z_xy, k);
			
			if (out != null) outputSlice = new byte[w * h];
			
			for (j = 0; j < h; j++)
			{
				mainLoop: for (i = 0; i < w; i++ ) //, xyOffset++)
				{
					double value = Array1DUtil.getValue(array_xy, xyOffset, dataType);
					
					xyOffset = j * w + i;
					
					if (value <= threshold) continue mainLoop;
					
					for (nz = -halfSizeZ; nz <= halfSizeZ; nz++)
						for (ny = -halfSizeY; ny <= halfSizeY; ny++)
							neighborhood: for (nx = -halfSizeX; nx <= halfSizeX; nx++)
							{
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
			
			if (out != null) out.setImage(t, k, new IcyBufferedImage(w, h, new byte[][] { outputSlice }));
		}
		return localmax;
	}

}
