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

package plugins.fab.spotDetector.input;

import org.apache.poi.ss.usermodel.Sheet;
import fr.icy.Icy;
import fr.icy.gui.GuiUtil;
import fr.icy.gui.frame.progress.ProgressFrame;
import fr.icy.io.FileUtil;
import fr.icy.io.Loader;
import fr.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.GlobalDetectionToken;
import plugins.fab.spotDetector.GlobalDetectionTokenListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

public class batchInputDetection extends InputDetectionAbstract implements ActionListener, GlobalDetectionTokenListener {
    JButton inputFolderButton = new JButton("No folder selected");
    JLabel fileToProcessLabel = new JLabel("no files to process");
    ArrayList<File> originalFileToProcessArrayList = new ArrayList<>(); // never altered but by the user himself
    ArrayList<File> processFileToProcessArrayList = new ArrayList<>(); // list used while processing

    public batchInputDetection() {
        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("Batch Input. Grab all file recursivly but does not enter in 'save' folders"));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("Current input folder:")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(inputFolderButton));
        inputFolderButton.addActionListener(this);
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(10)));
        getPanel().add(GuiUtil.createLineBoxPanel(fileToProcessLabel));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalStrut(50)));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    @Override
    public void detectionFinished() {
        if (!processFileToProcessArrayList.isEmpty()) {
            spotDetector.detectionModuleRequestRecompute(true, true);
        }
        else {
            // prepare list for next launch
            processFileToProcessArrayList = new ArrayList<>(originalFileToProcessArrayList);
            progressFrame = null;
        }
    }

    ProgressFrame progressFrame = null;

    @Override
    public void process(final GlobalDetectionToken gdt) {
        Icy.getMainInterface().closeAllViewers();

        if (progressFrame == null) progressFrame = new ProgressFrame("");

        progressFrame.setMessage("Processing ... " + processFileToProcessArrayList.size() + " remaining.");

        gdt.addGlobalDetectionTokenListener(this);

        if (!processFileToProcessArrayList.isEmpty()) {
            final String path = processFileToProcessArrayList.remove(0).getAbsolutePath();
            gdt.inputSequence = Loader.loadSequence(Loader.getSequenceFileImporter(path, true), path, 0, false);
        }

        if (processFileToProcessArrayList.isEmpty()) {
            progressFrame.close();
        }
    }


    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == inputFolderButton) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            final Preferences preferences = Preferences.userRoot().node("icy/browser");
            final String browserDirectory = preferences.get("path", "");

            if (browserDirectory.isEmpty())
                fileChooser.setCurrentDirectory(new File(browserDirectory));

            final int returnValue = fileChooser.showDialog(null, "Root Directory of scan");
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                final String scanDirectory = fileChooser.getSelectedFile().getAbsolutePath();
                inputFolderButton.setText(scanDirectory);
                preferences.put("path", fileChooser.getCurrentDirectory().getPath());
                buildFileList(scanDirectory);
            }
        }
    }

    private void buildFileList(final String scanDirectory) {
        final File mainDir = new File(scanDirectory);
        originalFileToProcessArrayList.clear();
        scanDir(mainDir);
        fileToProcessLabel.setText("Total number of files in batch:" + originalFileToProcessArrayList.size());
    }

    public void scanDir(final @NotNull File directory) {
        IcyLogger.debug(this.getClass(), "Scanning directory: " + directory.getAbsolutePath());

        final List<File> listImages;

        try {
            final File[] files = directory.listFiles();
            if (files == null) {
                JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            listImages = Arrays.asList(files);

            if (listImages.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        catch (final Exception e) {
            JOptionPane.showMessageDialog(null, "No file ! Try selecting the parent directory of this folder", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Collections.sort(listImages);

        for (final File file : listImages) {
            if (file.isDirectory()) {
                final String fileAsString = file.getAbsolutePath();
                if (fileAsString.contains("save")) {
                    //System.out.println("Skipping directory : " + file );
                    continue; // on ignore les anciennes sauvegarde
                }

                scanDir(file);
                continue;
            }

            if (file.isHidden()) continue; // evite de se taper des .DSStore chez mac par exemple.

            if (FileUtil.getFileExtension(file.getName(), false).toLowerCase().contains("xml")) continue;

            //System.out.println("file to process: " + file );
            originalFileToProcessArrayList.add(file);

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
//			// Check if detection is done.
//			System.out.print("Processing..");
//			while ( computeDetectionThread.isAlive() )
//			{
//				System.out.print(".");
//				try {
//					Thread.sleep( 500 );
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//
//			while ( !progressBar.getString( ).contains("Finished") )
//			{
//				try {
//					Thread.sleep( 500 );
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//
//			System.out.println("");
//
//			sequence = null;


        }

        // copy file in the process list
        processFileToProcessArrayList = new ArrayList<>(originalFileToProcessArrayList);
    }


    @Override
    public void saveXLS(final Sheet page, final GlobalDetectionToken gdt) {
        //
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
//				// Check if detection is done.
//				System.out.print("Processing..");
//				while ( computeDetectionThread.isAlive() )
//				{
//					System.out.print(".");
//					try {
//						Thread.sleep( 500 );
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//
//				while ( !progressBar.getString( ).contains("Finished") )
//				{
//					try {
//						Thread.sleep( 500 );
//					} catch (InterruptedException e) {
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
