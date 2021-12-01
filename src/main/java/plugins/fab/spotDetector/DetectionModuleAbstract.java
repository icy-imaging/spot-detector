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

import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginLibrary;

import javax.swing.JPanel;

import jxl.write.WritableSheet;

public abstract class DetectionModuleAbstract extends Plugin implements PluginLibrary
{

    JPanel mainPanel = new JPanel();

    public JPanel getPanel()
    {
        return mainPanel;
    }

    public abstract void process(GlobalDetectionToken gdt) throws InterruptedException;
    public abstract void saveXLS(WritableSheet page, GlobalDetectionToken gdt) throws InterruptedException;
}
