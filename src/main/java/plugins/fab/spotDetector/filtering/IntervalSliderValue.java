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

import icy.gui.component.ComponentUtil;
import icy.gui.util.GuiUtil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class IntervalSliderValue extends JPanel implements ActionListener {

	private static final long serialVersionUID = -2102078017554017686L;
	IntervalSlider intervalSlider = null;
	JTextField minTextField = new JTextField("" );
	JTextField maxTextField= new JTextField("" );
	JLabel titleLabel= new JLabel("" , JLabel.CENTER );
	
	public IntervalSliderValue( int min , int max , int lowValue, int highValue , String title ) {		
		
		intervalSlider = new IntervalSlider( min , max ,lowValue , highValue );		
		intervalSlider.addActionListener( this );
		
		ComponentUtil.setFixedHeight( intervalSlider , 20 );
		ComponentUtil.setFixedWidth( minTextField , 50 );
		ComponentUtil.setFixedWidth( maxTextField , 50 );
		
		titleLabel.setText( title );
		minTextField.setText( ""+lowValue );
		maxTextField.setText( ""+highValue );
		
		minTextField.addActionListener( this );
		maxTextField.addActionListener( this );

		this.setLayout( new BoxLayout( this , BoxLayout.PAGE_AXIS ) );
		this.add( GuiUtil.createLineBoxPanel( titleLabel ));
		this.add( GuiUtil.createLineBoxPanel( minTextField , intervalSlider , maxTextField ));
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == intervalSlider )
		{
			minTextField.setText( ""+ intervalSlider.getCurrentLowValue() );
			maxTextField.setText( ""+ intervalSlider.getCurrentHighValue() );
		}
		
		if ( e.getSource() == minTextField )
		{
			try
			{
				int min = Integer.parseInt( minTextField.getText() );
				intervalSlider.setCurrentLowValue( min );
			}
			catch (Exception e1) {
				e1.printStackTrace();
			}	
		}
		
		if ( e.getSource() == maxTextField )
		{
			try
			{
				int max = Integer.parseInt( maxTextField.getText() );
				intervalSlider.setCurrentHighValue( max );
			}
			catch (Exception e1) {
				e1.printStackTrace();
			}	
		}
		
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

	public int getCurrentLowValue()
	{
		return intervalSlider.currentLowValue;
	}
	
	public int getCurrentHighValue()
	{
		return intervalSlider.currentHighValue;
	}
	
}
