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

import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import plugins.fab.spotDetector.display.DisplayDetectionAbstract;

public class DisplayDetectionPanel extends GeneralSpotDetectionPanel implements ActionListener
{

	private static final long serialVersionUID = 8859908731727831128L;

	JComboBox inputChoice = new JComboBox();
	JPanel pluginPanel = new JPanel();
	/**
	 * Plugin used for detection
	 */
	DisplayDetectionAbstract displayDetectionPlugin = null;

	public void process( GlobalDetectionToken gdt )
	{
		displayDetectionPlugin.process( gdt );
	}

	public DisplayDetectionPanel(SpotDetector spotDetector)
	{

		super(spotDetector);

		setTitle("Display");

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
			if (pluginDescriptor.isInstanceOf(DisplayDetectionAbstract.class))
				if ( !pluginDescriptor.isAbstract() )
				{
					inputChoice.addItem(pluginDescriptor);

					// set default input plugin
					if ( pluginDescriptor.getName().contains( "currentSequenceInputDetection" ) ) // TODO: minor fix here
					{
						inputChoice.setSelectedItem( pluginDescriptor ) ;
					}
				}
		}



	}

	/**
	 * Close the panel ( first created to remove the painter of the detection )
	 */
	public void close()
	{
		if ( displayDetectionPlugin!=null )
		{
			displayDetectionPlugin.close();
		}
		
	}
	
	/**
	 * refresh interface and re-instantiate processor
	 */
	private void refreshInterface()
	{
		//System.out.println("display detection choice");

		PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();
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

		displayDetectionPlugin = (DisplayDetectionAbstract) plugin;

		pluginPanel.add( displayDetectionPlugin.getPanel() , BorderLayout.CENTER );
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
}
