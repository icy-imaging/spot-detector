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

import icy.gui.util.GuiUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import plugins.fab.spotDetector.roi.ROIDetectionAbstract;

public class ROIPanel extends GeneralSpotDetectionPanel implements ActionListener
{

    private static final long serialVersionUID = -8556671504886645654L;

    JComboBox inputChoice = new JComboBox();
    JPanel pluginPanel = new JPanel();
    ROIDetectionAbstract roiDetection = null;

    public ROIPanel(SpotDetector spotDetector)
    {
        super(spotDetector);
        setTitle("Region of Interest");

        pluginPanel.setLayout( new BorderLayout() );
        
        add(inputChoice, BorderLayout.NORTH);
        add(pluginPanel, BorderLayout.CENTER);

        buildInputChoice();
        inputChoice.addActionListener(this);
        refreshInterface();
    }

    /**
     * Build the list of available processor founds in the plugin list
     */
    private void buildInputChoice()
    {

        inputChoice.removeAll();

        for (final PluginDescriptor pluginDescriptor : PluginLoader.getPlugins())
        {
            if (pluginDescriptor.isInstanceOf(ROIDetectionAbstract.class))
            	if ( !pluginDescriptor.isAbstract() )
            {
                inputChoice.addItem(pluginDescriptor);
                if ( pluginDescriptor.getClassName().contains( "FromSequence") ) // TODO: need little fix here
                {
                	inputChoice.setSelectedItem( pluginDescriptor );
                }
            }
        }

    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface()
    {
        PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();

        pluginPanel.removeAll();

        Plugin plugin = null;
        try
        {
            plugin = pluginDescriptor.getPluginClass().newInstance();
        }
        catch (InstantiationException e1)
        {
            e1.printStackTrace();
        }
        catch (IllegalAccessException e1)
        {
            e1.printStackTrace();
        }

        roiDetection = (ROIDetectionAbstract) plugin;

        pluginPanel.add( roiDetection.getPanel() , BorderLayout.CENTER );
        pluginPanel.updateUI();

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        if (e.getSource() == inputChoice)
        {
            refreshInterface();
        }

    }

    /*
     * FIXME : peut etre pas la bonne facon de faire
     * comment obtenir le meme effet que le pattern special que l'on colle dans une image ?
     */
    public ArrayList<ROI> getROIs()
    {
        // TODO Auto-generated method stub
        return null;
    }

	public void process(GlobalDetectionToken gdt) {
		
		roiDetection.process( gdt );
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	

}
