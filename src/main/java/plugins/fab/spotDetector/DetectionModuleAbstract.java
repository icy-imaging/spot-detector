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

package plugins.fab.spotDetector;

import org.apache.poi.ss.usermodel.Sheet;
import fr.icy.extension.plugin.abstract_.Plugin;

import javax.swing.*;

public abstract class DetectionModuleAbstract extends Plugin {
    JPanel mainPanel = new JPanel();

    public JPanel getPanel() {
        return mainPanel;
    }

    public abstract void process(GlobalDetectionToken gdt) throws InterruptedException;

    public abstract void saveXLS(Sheet page, GlobalDetectionToken gdt) throws InterruptedException;
}
