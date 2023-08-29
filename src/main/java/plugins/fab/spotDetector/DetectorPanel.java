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

import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;
import icy.system.IcyExceptionHandler;
import plugins.fab.spotDetector.detector.DetectorDetectionAbstract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

public class DetectorPanel extends GeneralSpotDetectionPanel implements ActionListener {
    JComboBox<PluginDescriptor> inputChoice = new JComboBox<>();
    JPanel pluginPanel = new JPanel();
    DetectorDetectionAbstract detectorDetection = null;

    public DetectorPanel(final SpotDetector spotDetector) {
        super(spotDetector);

        setTitle("Detector");
        pluginPanel.setLayout(new BorderLayout());
        setLayout(new BorderLayout());

        add(inputChoice, BorderLayout.NORTH);
        add(pluginPanel, BorderLayout.CENTER);

        buildInputChoice();
        inputChoice.addActionListener(this);
        refreshInterface();
    }

    public void process(final GlobalDetectionToken gdt) throws InterruptedException {
        detectorDetection.process(gdt);
    }

    /**
     * Build the list of available processor founds in the plugin list
     */
    private void buildInputChoice() {

        inputChoice.removeAll();

        for (final PluginDescriptor pluginDescriptor : PluginLoader.getPlugins()) {
            if (pluginDescriptor.isInstanceOf(DetectorDetectionAbstract.class)) {
                if (!pluginDescriptor.isAbstract()) {
                    //System.out.println( pluginDescriptor.getClassName() );
                    inputChoice.addItem(pluginDescriptor);
                    if (pluginDescriptor.getClassName().contains("UDWT")) // TODO : minor fix needed here
                    {
                        inputChoice.setSelectedItem(pluginDescriptor);
                    }
                }
            }
        }

    }

    public void setDetectorPlugin(final PluginDescriptor detectorPluginDescriptorClass) {
        System.out.println("Detector set is " + detectorPluginDescriptorClass);

        for (int i = 0; i < inputChoice.getModel().getSize(); i++) {
            final PluginDescriptor pd = inputChoice.getModel().getElementAt(i);
            System.out.println("*" + pd.getClassName() + "*");
            System.out.println("*" + detectorPluginDescriptorClass.getClassName() + "*");

            if (pd.getClassName().equals(detectorPluginDescriptorClass.getClassName())) {
                System.out.println("Match");
                inputChoice.setSelectedItem(pd);
                break;
            }
        }
    }

    private void refreshInterface() {
        final PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();
        assert pluginDescriptor != null;
        refreshInterface(pluginDescriptor);
    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface(final PluginDescriptor pluginDescriptor) {

        //System.out.println("Detector panel : selected plugin is : " +pluginDescriptor.getClassName() );

        pluginPanel.removeAll();

        Plugin plugin = null;
        try {
            plugin = pluginDescriptor.getPluginClass().getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            IcyExceptionHandler.showErrorMessage(e1, true);
        }

        detectorDetection = (DetectorDetectionAbstract) plugin;

        assert detectorDetection != null;
        pluginPanel.add(detectorDetection.getPanel(), BorderLayout.CENTER);
        pluginPanel.updateUI();

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == inputChoice) {
            refreshInterface();
        }
    }

}
