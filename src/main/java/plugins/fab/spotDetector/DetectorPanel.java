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

package plugins.fab.spotDetector;

import fr.icy.extension.ExtensionLoader;
import fr.icy.extension.plugin.PluginDescriptor;
import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.shared.logging.CustomLevel;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.detector.DetectorDetectionAbstract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DetectorPanel extends GeneralSpotDetectionPanel implements ActionListener {
    private static final Logger LOGGER = Logger.getLogger(DetectorPanel.class.getName());

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

        for (final PluginDescriptor pluginDescriptor : ExtensionLoader.getPlugins(DetectorDetectionAbstract.class, false, false)) {
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
        if (LOGGER.isLoggable(CustomLevel.DEBUG)) {
            LOGGER.log(CustomLevel.DEBUG, "Detector set is " + detectorPluginDescriptorClass);
        }

        for (int i = 0; i < inputChoice.getModel().getSize(); i++) {
            final PluginDescriptor pd = inputChoice.getModel().getElementAt(i);
            if (LOGGER.isLoggable(CustomLevel.DEBUG)) {
                LOGGER.log(CustomLevel.DEBUG, "*" + pd.getClassName() + "*");
                LOGGER.log(CustomLevel.DEBUG, "*" + detectorPluginDescriptorClass.getClassName() + "*");
            }

            if (pd.getClassName().equals(detectorPluginDescriptorClass.getClassName())) {
                if (LOGGER.isLoggable(CustomLevel.DEBUG)) {
                    LOGGER.log(CustomLevel.DEBUG, "Match");
                }
                inputChoice.setSelectedItem(pd);
                break;
            }
        }
    }

    private void refreshInterface() {
        final PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();
        refreshInterface(Objects.requireNonNull(pluginDescriptor));
    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface(final @NotNull PluginDescriptor pluginDescriptor) {
        if (LOGGER.isLoggable(CustomLevel.DEBUG)) {
            LOGGER.log(CustomLevel.DEBUG, "Detector panel : selected plugin is : " + pluginDescriptor.getClassName());
        }

        pluginPanel.removeAll();

        Plugin plugin = null;
        try {
            plugin = pluginDescriptor.getPluginClass().getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error while instantiating plugin", e1);
            }
        }

        detectorDetection = (DetectorDetectionAbstract) plugin;

        pluginPanel.add(Objects.requireNonNull(detectorDetection).getPanel(), BorderLayout.CENTER);
        pluginPanel.updateUI();
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == inputChoice) {
            refreshInterface();
        }
    }
}
