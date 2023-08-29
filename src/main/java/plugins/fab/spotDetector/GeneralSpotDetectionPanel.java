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

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

/**
 * @author Fabrice de Chaumont
 */
public abstract class GeneralSpotDetectionPanel extends JPanel {
    private String title = "no title";
    private DefaultMutableTreeNode node = null;

    SpotDetector spotDetector;

    public GeneralSpotDetectionPanel(final SpotDetector spotDetector) {
        this.spotDetector = spotDetector;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(""));
        node = new DefaultMutableTreeNode(getTitle());
    }

    public void fireChange(final InputPanel inputPanel) {
        spotDetector.fireChange(inputPanel);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
        this.setBorder(BorderFactory.createTitledBorder(title));
        node = new DefaultMutableTreeNode(getTitle());
    }

    public DefaultMutableTreeNode getNode() {
        return node;
    }

}
