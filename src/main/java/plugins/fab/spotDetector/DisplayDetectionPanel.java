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
import fr.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.display.DisplayDetectionAbstract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class DisplayDetectionPanel extends GeneralSpotDetectionPanel implements ActionListener {
    JComboBox<PluginDescriptor> inputChoice = new JComboBox<>();
    JPanel pluginPanel = new JPanel();

    /**
     * Plugin used for detection
     */
    DisplayDetectionAbstract displayDetectionPlugin = null;

    public void process(final GlobalDetectionToken gdt) {
        displayDetectionPlugin.process(gdt);
    }

    public DisplayDetectionPanel(final SpotDetector spotDetector) {
        super(spotDetector);

        setTitle("Display");

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

        for (final PluginDescriptor pluginDescriptor : ExtensionLoader.getPlugins(DisplayDetectionAbstract.class, false, false)) {
            if (pluginDescriptor.isInstanceOf(DisplayDetectionAbstract.class))
                if (!pluginDescriptor.isAbstract()) {
                    inputChoice.addItem(pluginDescriptor);

                    // set default input plugin
                    if (pluginDescriptor.getName().contains("currentSequenceInputDetection")) // TODO: minor fix here
                    {
                        inputChoice.setSelectedItem(pluginDescriptor);
                    }
                }
        }
    }

    /**
     * Close the panel ( first created to remove the painter of the detection )
     */
    public void close() {
        if (displayDetectionPlugin != null) {
            displayDetectionPlugin.close();
        }
    }

    /**
     * refresh interface and re-instantiate processor
     */
    private void refreshInterface() {
        IcyLogger.debug(this.getClass(), "display detection choice");

        final PluginDescriptor pluginDescriptor = (PluginDescriptor) inputChoice.getSelectedItem();

        pluginPanel.removeAll();

        Plugin plugin = null;
        try {
            plugin = Objects.requireNonNull(pluginDescriptor).getPluginClass().getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e1) {
            IcyLogger.error(this.getClass(), e1);
        }

        displayDetectionPlugin = (DisplayDetectionAbstract) plugin;

        pluginPanel.add(Objects.requireNonNull(displayDetectionPlugin).getPanel(), BorderLayout.CENTER);
        pluginPanel.updateUI();
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == inputChoice) {
            refreshInterface();
        }
    }
}
