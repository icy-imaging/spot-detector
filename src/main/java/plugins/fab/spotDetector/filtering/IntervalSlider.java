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
package plugins.fab.spotDetector.filtering;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JPanel;

/**
 * Creates a slider for int intervals.
 * 
 * @author Fabrice de Chaumont
 *
 */
public class IntervalSlider extends JPanel implements MouseListener , MouseMotionListener {

	private static final long serialVersionUID = 5897728258613197422L;
	int min;
	int max;
	int currentLowValue ;
	int currentHighValue ;
	enum DRAGGING { NOT_DRAGGING , LOW ,HIGH , ALL };
	DRAGGING draggingState = DRAGGING.NOT_DRAGGING;
	DRAGGING displayMoveState = DRAGGING.NOT_DRAGGING;
	
	public IntervalSlider( int min , int max ) {
				
		this.setPreferredSize( new Dimension( 0 , 20 ) );
		this.min = min;
		this.max = max;
		this.addMouseListener( this );
		this.addMouseMotionListener( this );
		
	}

	public IntervalSlider( int min , int max , int lowValue, int highValue ) {
		
		this( min , max );
		this.currentLowValue = lowValue;
		this.currentHighValue = highValue;
		
	}


	
	@Override
	protected void paintComponent(Graphics g) {

		super.paintComponent(g);

		g.setColor( Color.black );
		g.fillRect( 0 , 0 , getWidth(), getHeight() );

		double lowx = getXForValue( currentLowValue );
		double highx = getXForValue( currentHighValue );
		
		g.setColor( Color.gray );
		g.fill3DRect( (int)lowx-1 , 1 , (int)( highx-lowx )+1, getHeight()-2 , true );
		
		g.setColor( Color.lightGray );
		// middle dot
		g.fillOval( (int)(( highx+lowx ) /2 ) - 3 , 4, 10, 10 );

		// left edge
		{
			if ( draggingState == DRAGGING.LOW || displayMoveState == DRAGGING.LOW )
			{
				g.setColor( Color.yellow );
			}else
			{
				g.setColor( Color.lightGray );	
			}
			
			g.drawRect( (int)lowx-1 , 1 , 1, getHeight()-3 );
			g.drawRect( (int)lowx-1 , 1 , 5, 1 );
			g.drawRect( (int)lowx-1 , getHeight()-3 , 5, 1 );

		}

		// right edge
		{
			if ( draggingState == DRAGGING.HIGH || displayMoveState == DRAGGING.HIGH  )
			{
				g.setColor( Color.yellow );
			}else
			{
				g.setColor( Color.lightGray );	
			}
			
			g.drawRect( (int)highx-1 , 1 , 1, getHeight()-3 );
			g.drawRect( (int)highx-6 , 1 , 5, 1 );
			g.drawRect( (int)highx-6 , getHeight()-3 , 5, 1 );

		}

		if ( draggingState == DRAGGING.ALL || displayMoveState == DRAGGING.ALL )
		{
			g.setColor( Color.yellow );
			g.drawRect( (int)lowx-1 , 1 , 1, getHeight()-3 );
			g.drawRect( (int)lowx-1 , 1 , 5, 1 );
			g.drawRect( (int)lowx-1 , getHeight()-3 , 5, 1 );
			g.drawRect( (int)highx-1 , 1 , 1, getHeight()-3 );
			g.drawRect( (int)highx-6 , 1 , 5, 1 );
			g.drawRect( (int)highx-6 , getHeight()-3 , 5, 1 );
			g.fillOval( (int)(( highx+lowx ) /2 ) - 3 , 4, 10, 10 );

			
			//g.drawRect( (int)lowx-1 ,  getHeight()/2 , (int)( highx-lowx ), 1 );
		}

		
	}

	private double getXForValue( double value )
	{
		double pxVal = getWidth() / (double)(max - min) ;
		double result = (double)( value - min ) * pxVal;				
		return result;
	}
	
	private double getValueForX( double x )
	{
		double pxVal = (double)(max - min) / getWidth() ;
		double result = x * pxVal + min;				
		return result;
	}
	
	public void setCurrentLowValue( int value )
	{
		if ( value <= currentHighValue )
		{
			currentLowValue = value;
			repaint();
			fireAction();
		}
	}	
	
	public void setCurrentHighValue( int value )
	{
		if ( value >= currentLowValue )
		{
			System.out.println( value );
			currentHighValue = value;
			repaint();
			fireAction();
		}
	}
	
	public int getCurrentLowValue()
	{
		repaint();
		return currentLowValue;
	}
	
	public int getCurrentHighValue()
	{
		repaint();
		return currentHighValue;
	}
	
	DRAGGING computeDraggingState ( MouseEvent e )
	{
		DRAGGING draggingResult = draggingState;
		
		if ( draggingResult == DRAGGING.NOT_DRAGGING )
		{
			double xVal = getValueForX( e.getX() );
			// decide wich bound to drag.

			if ( Math.abs( xVal-currentLowValue ) < Math.abs( xVal-currentHighValue ) )
			{
				draggingResult = DRAGGING.LOW;
			}else
			{
				draggingResult = DRAGGING.HIGH;
			}
			
			if ( xVal > currentLowValue && xVal < currentHighValue ) // click on the interval.
			{
				double centerValue = ( currentLowValue + currentHighValue ) / 2;
				double centerX = getXForValue( centerValue );
				double mouseX = e.getX();
				if ( Math.abs( centerX - mouseX ) < 10 )
				{
					draggingResult = DRAGGING.ALL;
				}
			}						
		}	
		
		return draggingResult;
	}

	private void updateBounds(MouseEvent e) {
		
		draggingState = computeDraggingState( e );
		
		double mouseX = e.getX();

		if ( draggingState == DRAGGING.LOW )
		{
			currentLowValue = (int) getValueForX( mouseX );			
			if ( currentLowValue > currentHighValue ) currentLowValue = currentHighValue;
		}
		if ( draggingState == DRAGGING.HIGH )
		{
			currentHighValue = (int) getValueForX( mouseX );			
			if ( currentHighValue < currentLowValue ) currentHighValue = currentLowValue;
		}
		if ( draggingState == DRAGGING.ALL )
		{
			double currentInterval = currentHighValue - currentLowValue;			
			double centerValue = (int) getValueForX( mouseX );
			currentLowValue = (int) (centerValue - currentInterval / 2) ;
			currentHighValue = (int) (centerValue + currentInterval /2) ;
			
			// ensure that the interval size will not decrease
			if ( currentHighValue > max ) currentLowValue -= currentHighValue - max;
			if ( currentLowValue < min ) currentHighValue += min - currentLowValue ;
			
		}

				
		if ( currentLowValue < min ) currentLowValue = min;
		if ( currentHighValue > max ) currentHighValue = max;		
		
		repaint();
		
		fireAction();
		
	}
	
	ArrayList<ActionListener> actionListenerList = new ArrayList<ActionListener>();
	
	public void addActionListener( ActionListener actionListener )
	{
		actionListenerList.add( actionListener );
	}
	public void removeActionListener( ActionListener actionListener )
	{
		actionListenerList.remove( actionListener );
	}
	private void fireAction() 
	{		
		for ( ActionListener al : actionListenerList )
		{
			al.actionPerformed( new ActionEvent( this , 0,"" ) );
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		updateBounds( e );
	}


	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		displayMoveState = DRAGGING.NOT_DRAGGING;
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		draggingState = DRAGGING.NOT_DRAGGING;
		updateBounds( e );
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		draggingState = DRAGGING.NOT_DRAGGING;
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
		draggingState = computeDraggingState( e );

		updateBounds( e );
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		displayMoveState = computeDraggingState( e );
		
		repaint();
	}
	
}
