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

package plugins.fab.spotDetector.filtering;

import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.component.ComponentUtil;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class IntervalSliderValue extends JPanel implements ActionListener {
    IntervalSlider intervalSlider;
    JTextField minTextField = new JTextField("");
    JTextField maxTextField = new JTextField("");
    JLabel titleLabel = new JLabel("", JLabel.CENTER);

    public IntervalSliderValue(final int min, final int max, final int lowValue, final int highValue, final String title) {
        intervalSlider = new IntervalSlider(min, max, lowValue, highValue);
        intervalSlider.addActionListener(this);

        ComponentUtil.setFixedHeight(intervalSlider, 20);
        ComponentUtil.setFixedWidth(minTextField, 50);
        ComponentUtil.setFixedWidth(maxTextField, 50);

        titleLabel.setText(title);
        minTextField.setText("" + lowValue);
        maxTextField.setText("" + highValue);

        minTextField.addActionListener(this);
        maxTextField.addActionListener(this);

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(GuiUtil.createLineBoxPanel(titleLabel));
        this.add(GuiUtil.createLineBoxPanel(minTextField, intervalSlider, maxTextField));
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == intervalSlider) {
            minTextField.setText("" + intervalSlider.getCurrentLowValue());
            maxTextField.setText("" + intervalSlider.getCurrentHighValue());
        }

        if (e.getSource() == minTextField) {
            try {
                final int min = Integer.parseInt(minTextField.getText());
                intervalSlider.setCurrentLowValue(min);
            }
            catch (final Exception e1) {
                IcyLogger.error(this.getClass(), e1);
            }
        }

        if (e.getSource() == maxTextField) {
            try {
                final int max = Integer.parseInt(maxTextField.getText());
                intervalSlider.setCurrentHighValue(max);
            }
            catch (final Exception e1) {
                IcyLogger.error(this.getClass(), e1);
            }
        }

    }

    ArrayList<ActionListener> actionListenerList = new ArrayList<>();

    public void addActionListener(final ActionListener actionListener) {
        actionListenerList.add(actionListener);
    }

    public void removeActionListener(final ActionListener actionListener) {
        actionListenerList.remove(actionListener);
    }

    private void fireAction() {
        for (final ActionListener al : actionListenerList) {
            al.actionPerformed(new ActionEvent(this, 0, ""));
        }
    }

    public int getCurrentLowValue() {
        return intervalSlider.currentLowValue;
    }

    public int getCurrentHighValue() {
        return intervalSlider.currentHighValue;
    }
}
