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

import fr.icy.shared.logging.CustomLevel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Creates a slider for int intervals.
 *
 * @author Fabrice de Chaumont
 */
public class IntervalSlider extends JPanel implements MouseListener, MouseMotionListener {
    private static final Logger LOGGER = Logger.getLogger(IntervalSlider.class.getName());

    int min;
    int max;
    int currentLowValue;
    int currentHighValue;

    enum DRAGGING {NOT_DRAGGING, LOW, HIGH, ALL}

    DRAGGING draggingState = DRAGGING.NOT_DRAGGING;
    DRAGGING displayMoveState = DRAGGING.NOT_DRAGGING;

    public IntervalSlider(final int min, final int max) {
        this.setPreferredSize(new Dimension(0, 20));
        this.min = min;
        this.max = max;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    public IntervalSlider(final int min, final int max, final int lowValue, final int highValue) {
        this(min, max);
        this.currentLowValue = lowValue;
        this.currentHighValue = highValue;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());

        final double lowx = getXForValue(currentLowValue);
        final double highx = getXForValue(currentHighValue);

        g.setColor(Color.gray);
        g.fill3DRect((int) lowx - 1, 1, (int) (highx - lowx) + 1, getHeight() - 2, true);

        g.setColor(Color.lightGray);
        // middle dot
        g.fillOval((int) ((highx + lowx) / 2) - 3, 4, 10, 10);

        // left edge
        {
            if (draggingState == DRAGGING.LOW || displayMoveState == DRAGGING.LOW) {
                g.setColor(Color.yellow);
            }
            else {
                g.setColor(Color.lightGray);
            }

            g.drawRect((int) lowx - 1, 1, 1, getHeight() - 3);
            g.drawRect((int) lowx - 1, 1, 5, 1);
            g.drawRect((int) lowx - 1, getHeight() - 3, 5, 1);
        }

        // right edge
        {
            if (draggingState == DRAGGING.HIGH || displayMoveState == DRAGGING.HIGH) {
                g.setColor(Color.yellow);
            }
            else {
                g.setColor(Color.lightGray);
            }

            g.drawRect((int) highx - 1, 1, 1, getHeight() - 3);
            g.drawRect((int) highx - 6, 1, 5, 1);
            g.drawRect((int) highx - 6, getHeight() - 3, 5, 1);
        }

        if (draggingState == DRAGGING.ALL || displayMoveState == DRAGGING.ALL) {
            g.setColor(Color.yellow);
            g.drawRect((int) lowx - 1, 1, 1, getHeight() - 3);
            g.drawRect((int) lowx - 1, 1, 5, 1);
            g.drawRect((int) lowx - 1, getHeight() - 3, 5, 1);
            g.drawRect((int) highx - 1, 1, 1, getHeight() - 3);
            g.drawRect((int) highx - 6, 1, 5, 1);
            g.drawRect((int) highx - 6, getHeight() - 3, 5, 1);
            g.fillOval((int) ((highx + lowx) / 2) - 3, 4, 10, 10);

            //g.drawRect( (int)lowx-1 ,  getHeight()/2 , (int)( highx-lowx ), 1 );
        }
    }

    private double getXForValue(final double value) {
        final double pxVal = getWidth() / (double) (max - min);
        return (value - min) * pxVal;
    }

    private double getValueForX(final double x) {
        final double pxVal = (double) (max - min) / getWidth();
        return x * pxVal + min;
    }

    public void setCurrentLowValue(final int value) {
        if (value <= currentHighValue) {
            currentLowValue = value;
            repaint();
            fireAction();
        }
    }

    public void setCurrentHighValue(final int value) {
        if (value >= currentLowValue) {
            if (LOGGER.isLoggable(CustomLevel.DEBUG)) {
                LOGGER.log(CustomLevel.DEBUG, "Current value: " + value);
            }
            currentHighValue = value;
            repaint();
            fireAction();
        }
    }

    public int getCurrentLowValue() {
        repaint();
        return currentLowValue;
    }

    public int getCurrentHighValue() {
        repaint();
        return currentHighValue;
    }

    DRAGGING computeDraggingState(final MouseEvent e) {
        DRAGGING draggingResult = draggingState;

        if (draggingResult == DRAGGING.NOT_DRAGGING) {
            final double xVal = getValueForX(e.getX());
            // decide wich bound to drag.

            if (Math.abs(xVal - currentLowValue) < Math.abs(xVal - currentHighValue)) {
                draggingResult = DRAGGING.LOW;
            }
            else {
                draggingResult = DRAGGING.HIGH;
            }

            if (xVal > currentLowValue && xVal < currentHighValue) // click on the interval.
            {
                final double centerValue = (double) (currentLowValue + currentHighValue) / 2;
                final double centerX = getXForValue(centerValue);
                final double mouseX = e.getX();
                if (Math.abs(centerX - mouseX) < 10) {
                    draggingResult = DRAGGING.ALL;
                }
            }
        }

        return draggingResult;
    }

    private void updateBounds(final MouseEvent e) {
        draggingState = computeDraggingState(e);

        final double mouseX = e.getX();

        if (draggingState == DRAGGING.LOW) {
            currentLowValue = (int) getValueForX(mouseX);
            if (currentLowValue > currentHighValue) currentLowValue = currentHighValue;
        }
        if (draggingState == DRAGGING.HIGH) {
            currentHighValue = (int) getValueForX(mouseX);
            if (currentHighValue < currentLowValue) currentHighValue = currentLowValue;
        }
        if (draggingState == DRAGGING.ALL) {
            final double currentInterval = currentHighValue - currentLowValue;
            final double centerValue = (int) getValueForX(mouseX);
            currentLowValue = (int) (centerValue - currentInterval / 2);
            currentHighValue = (int) (centerValue + currentInterval / 2);

            // ensure that the interval size will not decrease
            if (currentHighValue > max) currentLowValue -= currentHighValue - max;
            if (currentLowValue < min) currentHighValue += min - currentLowValue;
        }

        if (currentLowValue < min) currentLowValue = min;
        if (currentHighValue > max) currentHighValue = max;

        repaint();

        fireAction();
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

    @Override
    public void mouseClicked(final MouseEvent e) {
        updateBounds(e);
    }


    @Override
    public void mouseEntered(final MouseEvent e) {
        //
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        displayMoveState = DRAGGING.NOT_DRAGGING;
        repaint();
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        draggingState = DRAGGING.NOT_DRAGGING;
        updateBounds(e);
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        draggingState = DRAGGING.NOT_DRAGGING;
        repaint();
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        draggingState = computeDraggingState(e);
        updateBounds(e);
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        displayMoveState = computeDraggingState(e);
        repaint();
    }
}
