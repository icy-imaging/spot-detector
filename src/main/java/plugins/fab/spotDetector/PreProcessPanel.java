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
import plugins.fab.spotDetector.preProcessing.PreProcessingAbstract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

public class PreProcessPanel extends GeneralSpotDetectionPanel implements ActionListener {
    JComboBox<PluginDescriptor> preProcessorChoice = new JComboBox<>();
    JPanel pluginPanel = new JPanel();

    /**
     * Plugin used for pre processing
     */
    PreProcessingAbstract preProcessorDetection = null;

    public void process(final GlobalDetectionToken gdt) throws InterruptedException {
        preProcessorDetection.process(gdt);
        //gdt.inputSequence;
    }

    public PreProcessPanel(final SpotDetector spotDetector) {
        super(spotDetector);

        setTitle("Pre Processing");

        pluginPanel.setLayout(new BorderLayout());

        add(preProcessorChoice, BorderLayout.NORTH);
        add(pluginPanel, BorderLayout.CENTER);

        buildInputChoice();
        preProcessorChoice.addActionListener(this);
        refreshInterface();
    }

    /**
     * Build the list of available processor founds in the plugin list
     */
    private void buildInputChoice() {
        preProcessorChoice.removeAll();

        for (final PluginDescriptor pluginDescriptor : PluginLoader.getPlugins()) {
            if (pluginDescriptor.isInstanceOf(PreProcessingAbstract.class))
                if (!pluginDescriptor.isAbstract()) {
                    preProcessorChoice.addItem(pluginDescriptor);
                }
        }

        preProcessorChoice.addActionListener(this);
    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface() {
        //System.out.println("input choice");

        final PluginDescriptor pluginDescriptor = (PluginDescriptor) preProcessorChoice.getSelectedItem();
        //System.out.println(pluginDescriptor.getName());

        pluginPanel.removeAll();

        Plugin plugin = null;
        try {
            assert pluginDescriptor != null;
            plugin = pluginDescriptor.getPluginClass().getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            IcyExceptionHandler.showErrorMessage(e1, true);
        }

        preProcessorDetection = (PreProcessingAbstract) plugin;

        assert preProcessorDetection != null;
        pluginPanel.add(preProcessorDetection.getPanel(), BorderLayout.CENTER);
        pluginPanel.updateUI();

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
//        if (e.getSource() == inputChoice)
        {
            refreshInterface();
//            fireChange(this);
        }
    }
}
