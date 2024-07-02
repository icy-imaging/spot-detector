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

import org.apache.poi.ss.usermodel.Sheet;
import org.bioimageanalysis.icy.gui.frame.IcyFrame;
import org.bioimageanalysis.icy.system.thread.ThreadUtil;

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
     * @param sheet excel sheet to display
     */
    public ExcelIcyFrame(final String name, final Sheet sheet) {
        ThreadUtil.invokeLater(() -> {
            final IcyFrame frame = new IcyFrame(name);
            frame.getContentPane().add(new ExcelGrid(sheet));
            frame.pack();
            frame.setVisible(true);
        });
    }
}
