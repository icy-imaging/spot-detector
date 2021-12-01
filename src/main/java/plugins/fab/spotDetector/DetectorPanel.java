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

import plugins.fab.spotDetector.detector.DetectorDetectionAbstract;

public class DetectorPanel extends GeneralSpotDetectionPanel implements ActionListener
{

	private static final long serialVersionUID = 2633014431215333797L;
	JComboBox inputChoice = new JComboBox();
    JPanel pluginPanel = new JPanel();
    DetectorDetectionAbstract detectorDetection = null;

    public DetectorPanel(SpotDetector spotDetector)
    {
        super(spotDetector);

        setTitle("Detector");
        pluginPanel.setLayout( new BorderLayout() );
        setLayout( new BorderLayout() );
        
        add(inputChoice, BorderLayout.NORTH);
        add(pluginPanel, BorderLayout.CENTER);

        buildInputChoice();
        inputChoice.addActionListener(this);
        refreshInterface();
    }

    public void process( GlobalDetectionToken gdt ) throws InterruptedException
    {
    	detectorDetection.process( gdt );
    }
    
    /**
     * Build the list of available processor founds in the plugin list
     */
    private void buildInputChoice()
    {

        inputChoice.removeAll();

        for (final PluginDescriptor pluginDescriptor : PluginLoader.getPlugins())
        {
            if (pluginDescriptor.isInstanceOf(DetectorDetectionAbstract.class))
            {            	
            	if ( !pluginDescriptor.isAbstract() )
            	{
            		//System.out.println( pluginDescriptor.getClassName() );
            		inputChoice.addItem(pluginDescriptor);
            		if ( pluginDescriptor.getClassName().contains("UDWT") ) // TODO : minor fix needed here
            		{
            			inputChoice.setSelectedItem( pluginDescriptor );
            		}
            	}
            }
        }

    }
    
    public void setDetectorPlugin( PluginDescriptor detectorPluginDescriptorClass )
    {
    	System.out.println("Detector set is " + detectorPluginDescriptorClass );
    	
    	for ( int i = 0 ; i < inputChoice.getModel().getSize() ; i++ )
    	{
    		PluginDescriptor pd= (PluginDescriptor) inputChoice.getModel().getElementAt( i );
    		System.out.println( "*"+pd.getClassName()+"*" );
    		System.out.println( "*"+detectorPluginDescriptorClass.getClassName()+"*" );
    		
    		if ( pd.getClassName().equals( detectorPluginDescriptorClass.getClassName() ) )
    		{
    			System.out.println("Match");
    			inputChoice.setSelectedItem( pd );
    			break;
    		}
    	}
    }

    private void refreshInterface()
    {
    	PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();
    	refreshInterface( pluginDescriptor );
    }
    
    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface( PluginDescriptor pluginDescriptor )
    {
    	
        //System.out.println("Detector panel : selected plugin is : " +pluginDescriptor.getClassName() );
        
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

        detectorDetection = (DetectorDetectionAbstract) plugin;

        pluginPanel.add( detectorDetection.getPanel() , BorderLayout.CENTER );
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
