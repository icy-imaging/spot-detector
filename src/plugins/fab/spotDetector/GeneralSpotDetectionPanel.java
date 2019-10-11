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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * @author Fabrice de Chaumont
 *
 */
public abstract class GeneralSpotDetectionPanel extends JPanel
{

    private static final long serialVersionUID = 4602116758638991276L;
    private String title = "no title";
    private DefaultMutableTreeNode node = null;

    SpotDetector spotDetector = null;

    public GeneralSpotDetectionPanel(SpotDetector spotDetector)
    {
        this.spotDetector = spotDetector;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(""));        
        node = new DefaultMutableTreeNode(getTitle());
    }

    public void fireChange(InputPanel inputPanel)
    {
        spotDetector.fireChange(inputPanel);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
        this.setBorder(BorderFactory.createTitledBorder(title));
        node = new DefaultMutableTreeNode(getTitle());
    }

    public DefaultMutableTreeNode getNode()
    {
        return node;
    }

}
