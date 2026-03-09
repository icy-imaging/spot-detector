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

import org.bioimageanalysis.icy.extension.plugin.PluginDescriptor;
import org.bioimageanalysis.icy.extension.plugin.PluginLoader;
import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.roi.ROIDetectionAbstract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Objects;

public class ROIPanel extends GeneralSpotDetectionPanel implements ActionListener {
    JComboBox<PluginDescriptor> inputChoice = new JComboBox<>();
    JPanel pluginPanel = new JPanel();
    ROIDetectionAbstract roiDetection = null;

    public ROIPanel(final SpotDetector spotDetector) {
        super(spotDetector);
        setTitle("Region of Interest");

        pluginPanel.setLayout(new BorderLayout());

        add(inputChoice, BorderLayout.NORTH);
        add(pluginPanel, BorderLayout.CENTER);

        buildInputChoice();
        inputChoice.addActionListener(this);
        refreshInterface();
    }

    /**
     * Build the list of available processor founds in the plugin list
     */
    private void buildInputChoice() {
        inputChoice.removeAll();

        for (final PluginDescriptor pluginDescriptor : PluginLoader.getPlugins()) {
            if (pluginDescriptor.isInstanceOf(ROIDetectionAbstract.class))
                if (!pluginDescriptor.isAbstract()) {
                    inputChoice.addItem(pluginDescriptor);
                    if (pluginDescriptor.getClassName().contains("FromSequence")) // TODO: need little fix here
                    {
                        inputChoice.setSelectedItem(pluginDescriptor);
                    }
                }
        }
    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface() {
        final PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();

        pluginPanel.removeAll();

        Plugin plugin = null;
        try {
            plugin = Objects.requireNonNull(pluginDescriptor).getPluginClass().getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            IcyLogger.error(this.getClass(), e1);
        }

        roiDetection = (ROIDetectionAbstract) plugin;

        pluginPanel.add(Objects.requireNonNull(roiDetection).getPanel(), BorderLayout.CENTER);
        pluginPanel.updateUI();
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == inputChoice) {
            refreshInterface();
        }
    }

    /*
     * FIXME : peut etre pas la bonne facon de faire
     * comment obtenir le meme effet que le pattern special que l'on colle dans une image ?
     */
    public ArrayList<ROI> getROIs() {
        return null;
    }

    public void process(final GlobalDetectionToken gdt) {
        roiDetection.process(gdt);
    }
}
