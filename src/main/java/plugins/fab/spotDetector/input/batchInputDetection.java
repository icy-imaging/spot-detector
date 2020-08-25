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
package plugins.fab.spotDetector.input;

import icy.file.FileUtil;
import icy.file.Loader;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;

import jxl.write.WritableSheet;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.fab.spotDetector.GlobalDetectionTokenListener;

public class batchInputDetection extends InputDetectionAbstract implements ActionListener, GlobalDetectionTokenListener
{

	JButton inputFolderButton = new JButton("No folder selected");
	JLabel fileToProcessLabel = new JLabel("no files to process");
	ArrayList<File> originalFileToProcessArrayList = new ArrayList<File>(); // never altered but by the user himself
	ArrayList<File> processFileToProcessArrayList = new ArrayList<File>(); // list used while processing

    public batchInputDetection()
    {

    	getPanel().setLayout( new BoxLayout( getPanel() , BoxLayout.PAGE_AXIS ) );
    	getPanel().setBorder( new TitledBorder( "Batch Input. Grab all file recursivly but does not enter in 'save' folders" ) );

    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( new JLabel("Current input folder:") ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( inputFolderButton ) );
    	inputFolderButton.addActionListener( this );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 10 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( fileToProcessLabel ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalStrut( 50 ) ) );
    	getPanel().add( GuiUtil.createLineBoxPanel( Box.createVerticalGlue() ) );
    }


	@Override
	public void detectionFinished() {

		if ( processFileToProcessArrayList.size() > 0 )
		{
			spotDetector.detectionModuleRequestRecompute( true , true );
		}else
		{
			// prepare list for next launch
			processFileToProcessArrayList = new ArrayList<File>( originalFileToProcessArrayList );
			progressFrame = null;
		}
	}

	ProgressFrame progressFrame = null ;

	@Override
	public void process(GlobalDetectionToken gdt) {

		Icy.getMainInterface().closeAllViewers();

		if ( progressFrame == null ) progressFrame = new ProgressFrame("");

		progressFrame.setMessage("Processing ... " + processFileToProcessArrayList.size() + " remaining.");

		gdt.addGlobalDetectionTokenListener( this );

		if ( processFileToProcessArrayList.size() > 0 )
		{
			String path = processFileToProcessArrayList.remove(0).getAbsolutePath();
			Sequence sequence = Loader.loadSequence(Loader.getSequenceFileImporter(path, true), path, 0, false);

			gdt.inputSequence = sequence;
		}

		if ( processFileToProcessArrayList.size() == 0 )
		{
			progressFrame.close();
		}
	}



	@Override
	public void actionPerformed(ActionEvent e) {

		if ( e.getSource() == inputFolderButton )
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogType(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	        Preferences preferences = Preferences.userRoot().node("icy/browser");
	        String browserDirectory = preferences.get("path", "");

			if(browserDirectory == "")
			    fileChooser.setCurrentDirectory(new File(browserDirectory));

			int returnValue = fileChooser.showDialog( null , "Root Directory of scan");
			if (returnValue == JFileChooser.APPROVE_OPTION)
			{
				String scanDirectory = (String)fileChooser.getSelectedFile().getAbsolutePath();
				inputFolderButton.setText( scanDirectory );
				preferences.put("path", fileChooser.getCurrentDirectory().getPath() );
				buildFileList(scanDirectory);
			}
		}

	}

	private void buildFileList( String scanDirectory ) {
		File mainDir = new File( scanDirectory );
		originalFileToProcessArrayList.clear();
		scanDir( mainDir );
		fileToProcessLabel.setText("Total number of files in batch:" + originalFileToProcessArrayList.size() );
	}

	public void scanDir( File directory )
	{
		//System.out.println("Getting into dir : " + directory.getAbsolutePath() );

		List<File> listImages = null;

		try
		{
			listImages = Arrays.asList( directory.listFiles() );

		if ( listImages == null || listImages.size() == 0 )
			{
			JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", 0);
			return;
			}
		}
		catch( Exception e )
		{
			JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", 0);
			return;
		}

		Collections.sort(listImages);

		for ( File file : listImages )
		{
			if ( file.isDirectory() )
			{
				String fileAsString = file.getAbsolutePath();
				if ( fileAsString.contains("save") )
				{
					//System.out.println("Skipping directory : " + file );
					continue; // on ignore les anciennes sauvegarde
				}

				scanDir( file );
				continue;
			}

			if( file.isHidden() ) continue; // evite de se taper des .DSStore chez mac par exemple.

			if ( FileUtil.getFileExtension( file.getName(), false ).toLowerCase().contains("xml") ) continue;

			//System.out.println("file to process: " + file );
			originalFileToProcessArrayList.add( file );

			// Close previous files
//			while ( getSequences().getSize() > 0 )
//			{
//				Sequence sequence = (Sequence) getSequences().get( 0 );
//				sequence.unload();
//			}
//			// Load file
//
//			sequence = new Sequence();
//
//			try
//			{
//				Loader loader = new Loader( sequence , new File[]{file});
//				loader.start();
//			try {
//				loader.join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			}
//			catch( Exception e1 )
//			{
//				System.err.println("ERROR WITH FILE : " + file.getAbsolutePath() );
//				return;
//			}
//
//			// Add ROI painter on sequence.
//			ROIexcludePainter = new ROIExcludePainter ( sequence );
//
//			// Analysis
//			StartComputeDetectionThread();
//			//computeWaveletOnChannelRed( file , new File( file.getAbsolutePath()+"_t.png" ) );
//
//			// Check si la detection est terminée.
//			System.out.print("Processing..");
//			while ( computeDetectionThread.isAlive() )
//			{
//				System.out.print(".");
//				try {
//					Thread.sleep( 500 );
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//
//			while ( !progressBar.getString( ).contains("Finished") )
//			{
//				try {
//					Thread.sleep( 500 );
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//
//			System.out.println("");
//
//			sequence = null;


		}

		// copy file in the process list
		processFileToProcessArrayList = new ArrayList<File>( originalFileToProcessArrayList );

	}


	@Override
	public void saveXLS(WritableSheet page, GlobalDetectionToken gdt) {
		// TODO Auto-generated method stub

	}





//	ScannerFileThread scanThread = null;
//
//	public class ScannerFileThread extends Thread
//	{
//
//		@Override
//		public void run() {
//
//			startBatch.setEnabled( false );
//			setBatchFolderButton.setEnabled( false );
//
//			if ( scanDirectory == "" ) return;
//
//			File mainDir = new File( scanDirectory );
//			scanDir( mainDir );
//
//			setBatchFolderButton.setEnabled( true );
//			startBatch.setEnabled( true );
//
//		}
//
//		public void scanDir( File directory )
//		{
//			System.out.println("Getting into dir : " + directory.getAbsolutePath() );
//
//			List<File> listImages = null;
//
//			try
//			{
//				listImages = Arrays.asList( directory.listFiles() );
//
//			if ( listImages == null || listImages.size() == 0 )
//				{
//				JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", 0);
//				return;
//				}
//			}
//			catch( Exception e )
//			{
//				JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", 0);
//				return;
//			}
//
//			Collections.sort(listImages);
//
//			for ( File file : listImages )
//			{
//				if ( file.isDirectory() )
//				{
//					String fileAsString = file.getAbsolutePath();
//					if ( fileAsString.contains("save") )
//						{
//						System.out.println("Skipping directory : " + file );
//						continue; // on ignore les anciennes sauvegarde
//						}
//
//					scanDir( file );
//					continue;
//				}
//
//				if( file.isHidden() ) continue; // evite de se taper des .DSStore chez mac par exemple.
//
//				System.out.println("current file: " + file );
//				// Close previous files
//				while ( getSequences().getSize() > 0 )
//				{
//					Sequence sequence = (Sequence) getSequences().get( 0 );
//					sequence.unload();
//				}
//				// Load file
//
//				sequence = new Sequence();
//
//				try
//				{
//					Loader loader = new Loader( sequence , new File[]{file});
//					loader.start();
//				try {
//					loader.join();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				}
//				catch( Exception e1 )
//				{
//					System.err.println("ERROR WITH FILE : " + file.getAbsolutePath() );
//					return;
//				}
//
//				// Add ROI painter on sequence.
//				ROIexcludePainter = new ROIExcludePainter ( sequence );
//
//				// Analysis
//				StartComputeDetectionThread();
//				//computeWaveletOnChannelRed( file , new File( file.getAbsolutePath()+"_t.png" ) );
//
//				// Check si la detection est terminée.
//				System.out.print("Processing..");
//				while ( computeDetectionThread.isAlive() )
//				{
//					System.out.print(".");
//					try {
//						Thread.sleep( 500 );
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//
//				while ( !progressBar.getString( ).contains("Finished") )
//				{
//					try {
//						Thread.sleep( 500 );
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//
//				System.out.println("");
//
//				sequence = null;
//
//
//			}
//
//		}
//
//		boolean stop;
//
//		public void setStop() {
//			this.stop = true;
//		}
//
//	}





}
