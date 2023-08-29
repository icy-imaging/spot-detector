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

import icy.gui.frame.IcyFrame;
import icy.system.thread.ThreadUtil;
import jxl.write.WritableSheet;

/**
 * @author Fabrice de Chaumont
 */
public class ExcelIcyFrame {

    /**
     * Display an XLS page
     * Work in progress
     * should be a XlsManager with a multi page support...
     * copy / paste and so on...
     *
     * @param name          name of the frame
     * @param writableSheet excel sheet to display
     */
    public ExcelIcyFrame(final String name, final WritableSheet writableSheet) {

        ThreadUtil.invokeLater(() -> {
            final IcyFrame frame = new IcyFrame(name);
            frame.getContentPane().add(new ExcelGrid(writableSheet));
            frame.pack();
            frame.setVisible(true);
        });
    }
}
