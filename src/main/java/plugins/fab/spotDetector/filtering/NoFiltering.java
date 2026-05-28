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

package plugins.fab.spotDetector.filtering;

import org.apache.poi.ss.usermodel.Sheet;
import fr.icy.gui.GuiUtil;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;

/**
 * @author Fabrice de Chaumont
 */
public class NoFiltering extends FilteringDetectionAbstract {
    public NoFiltering() {
        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("No Filtering")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    @Override
    public void process(final GlobalDetectionToken gdt) {
        //
    }

    @Override
    public void saveXLS(final Sheet page, final GlobalDetectionToken gdt) {
        //
    }
}
