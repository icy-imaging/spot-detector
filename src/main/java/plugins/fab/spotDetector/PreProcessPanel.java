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
import icy.sequence.Sequence;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import plugins.fab.spotDetector.preProcessing.PreProcessingAbstract;

public class PreProcessPanel extends GeneralSpotDetectionPanel implements ActionListener
{
	
	private static final long serialVersionUID = -7715619663968178337L;
	JComboBox preProcessorChoice = new JComboBox();
    JPanel pluginPanel = new JPanel();
    /**
     * Plugin used for pre processing
     */
    PreProcessingAbstract preProcessorDetection = null;

    public void process( GlobalDetectionToken gdt )
    {
    	preProcessorDetection.process( gdt ) ;
        //gdt.inputSequence;
    }

    public PreProcessPanel(SpotDetector spotDetector)
    {

        super(spotDetector);

        setTitle("Pre Processing");
        
        pluginPanel.setLayout( new BorderLayout() );

        add(preProcessorChoice, BorderLayout.NORTH);
        add(pluginPanel, BorderLayout.CENTER);

        buildInputChoice();
        preProcessorChoice.addActionListener(this);
        refreshInterface();

    }

    /**
     * Build the list of available processor founds in the plugin list
     */
    private void buildInputChoice()
    {

        preProcessorChoice.removeAll();

        for (final PluginDescriptor pluginDescriptor : PluginLoader.getPlugins())
        {
            if (pluginDescriptor.isInstanceOf( PreProcessingAbstract.class))
            	if ( !pluginDescriptor.isAbstract() )
            {
                preProcessorChoice.addItem(pluginDescriptor);
            }
        }
        
        preProcessorChoice.addActionListener( this );

    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface()
    {
        //System.out.println("input choice");

        PluginDescriptor pluginDescriptor = (PluginDescriptor) preProcessorChoice.getSelectedItem();
        //System.out.println(pluginDescriptor.getName());

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

        preProcessorDetection = (PreProcessingAbstract) plugin;

        pluginPanel.add( preProcessorDetection.getPanel() , BorderLayout.CENTER );
        pluginPanel.updateUI();

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

//        if (e.getSource() == inputChoice)
        {
            refreshInterface();
//            fireChange(this);
        }

    }

}
