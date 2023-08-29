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

package plugins.fab.spotDetector.output;

import jxl.write.WritableSheet;
import plugins.fab.spotDetector.DetectionModuleAbstract;
import plugins.fab.spotDetector.GlobalDetectionToken;

public class OutPutDetectionAbstract extends DetectionModuleAbstract {

    @Override
    public void process(final GlobalDetectionToken gdt) {
    }

    @Override
    public void saveXLS(final WritableSheet page, final GlobalDetectionToken gdt) {
    }

}
