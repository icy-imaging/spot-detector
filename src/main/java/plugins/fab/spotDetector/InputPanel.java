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
import plugins.fab.spotDetector.input.InputDetectionAbstract;
import plugins.fab.spotDetector.input.currentSequenceInputDetection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class InputPanel extends GeneralSpotDetectionPanel implements ActionListener {
    JComboBox<PluginDescriptor> inputChoice = new JComboBox<>();
    JPanel pluginPanel = new JPanel();

    /**
     * Plugin used for detection
     */
    InputDetectionAbstract inputDetection = null;

    public void process(final GlobalDetectionToken gdt) throws InterruptedException {
        inputDetection.process(gdt);
        //return inputDetection.getInputSequence();
    }

    public InputPanel(final SpotDetector spotDetector) {
        super(spotDetector);

        setTitle("Input");

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

        for (final PluginDescriptor pluginDescriptor : ExtensionLoader.getPlugins(InputDetectionAbstract.class, false, false)) {
            if (pluginDescriptor.isInstanceOf(InputDetectionAbstract.class))
                if (!pluginDescriptor.isAbstract()) {
                    inputChoice.addItem(pluginDescriptor);
                    // set default input plugin
                    if (pluginDescriptor.getClassName().equalsIgnoreCase(currentSequenceInputDetection.class.getName()))
                    {
                        inputChoice.setSelectedItem(pluginDescriptor);
                    }
                }
        }
    }

    /**
     * refresh interface and reinstanciate processor
     */
    private void refreshInterface() throws NullPointerException {
        //System.out.println("input choice");

        final PluginDescriptor pluginDescriptor = (PluginDescriptor) Objects.requireNonNull(inputChoice.getSelectedItem(), "No input plugin selected");
        //System.out.println(pluginDescriptor.getName());

        pluginPanel.removeAll();

        Plugin plugin = null;
        try {
            plugin = pluginDescriptor.getPluginClass().getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            IcyLogger.error(this.getClass(), e1);
        }

        inputDetection = (InputDetectionAbstract) Objects.requireNonNull(plugin, "No input plugin found");
        // FIXME : create a listener instead of this
        inputDetection.spotDetector = spotDetector;

        pluginPanel.add(inputDetection.getPanel(), BorderLayout.CENTER);
        pluginPanel.updateUI();
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == inputChoice) {
            refreshInterface();
            fireChange(this);
        }
    }
}
