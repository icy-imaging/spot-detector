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
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.filtering.FilteringDetectionAbstract;
import plugins.fab.spotDetector.filtering.NoFiltering;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class FilteringPanel extends GeneralSpotDetectionPanel implements ActionListener {
    JComboBox<PluginDescriptor> inputChoice = new JComboBox<>();
    JPanel pluginPanel = new JPanel();
    FilteringDetectionAbstract filteringDetection = null;

    public FilteringPanel(final SpotDetector spotDetector) {
        super(spotDetector);
        setTitle("Filtering");

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
            if (pluginDescriptor.isInstanceOf(FilteringDetectionAbstract.class))
                if (!pluginDescriptor.isAbstract()) {
                    inputChoice.addItem(pluginDescriptor);
                    if (pluginDescriptor.isInstanceOf(NoFiltering.class)) {
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

        filteringDetection = (FilteringDetectionAbstract) plugin;

        pluginPanel.add(Objects.requireNonNull(filteringDetection).getPanel(), BorderLayout.CENTER);

        pluginPanel.updateUI();
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == inputChoice) {
            refreshInterface();
        }
    }

    public void process(final GlobalDetectionToken gdt) throws InterruptedException {
        filteringDetection.process(gdt);
    }
}
