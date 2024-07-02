/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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
import org.bioimageanalysis.icy.extension.plugin.abstract_.PluginActionable;
import org.bioimageanalysis.icy.extension.plugin.annotation_.IcyPluginIcon;
import org.bioimageanalysis.icy.extension.plugin.annotation_.IcyPluginName;
import org.bioimageanalysis.icy.gui.GuiUtil;
import org.bioimageanalysis.icy.gui.frame.IcyFrame;
import org.bioimageanalysis.icy.gui.frame.IcyFrameEvent;
import org.bioimageanalysis.icy.gui.frame.IcyFrameListener;
import org.bioimageanalysis.icy.gui.frame.progress.AnnounceFrame;
import org.bioimageanalysis.icy.gui.frame.progress.FailedAnnounceFrame;
import org.bioimageanalysis.icy.network.NetworkUtil;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.bioimageanalysis.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import plugins.fab.ROITagger.TagPainterManager;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Spot detector
 *
 * @author Fabrice de Chaumont
 */
@IcyPluginName("Spot Detector")
@IcyPluginIcon(path = "/spot-detector.png")
public class SpotDetector extends PluginActionable implements TreeSelectionListener, ActionListener, IcyFrameListener {
    IcyFrame icyFrame;
    private JTree tree;
    private final JPanel rightPanel = new JPanel();

    JPanel actionPanel = new JPanel();
    JButton startButton = new JButton("Start Detection");
    JButton helpButton = new JButton("Help");
    JPanel leftPanel = new JPanel();
    JPanel mainPanel = null;

    private final ArrayList<GeneralSpotDetectionPanel> spotModulePanelArrayList = new ArrayList<>();

    SettingPanel settingPanel = new SettingPanel();
    InputPanel inputPanel = new InputPanel(this);
    PreProcessPanel preProcessPanel = new PreProcessPanel(this);
    ROIPanel roiPanel = new ROIPanel(this);
    ColocalizationPanel colocalizationPanel = new ColocalizationPanel(this);
    OutPutPanel outputPanel = new OutPutPanel(this);
    DetectorPanel detectorPanel = new DetectorPanel(this);
    DisplayDetectionPanel displayDetectionPanel = new DisplayDetectionPanel(this);
    FilteringPanel filteringPanel = new FilteringPanel(this);

    public ArrayList<GeneralSpotDetectionPanel> getSpotModulePanelArrayList() {
        return new ArrayList<>(spotModulePanelArrayList);
    }

    private void buildDetectionModulePanelArrayList() {
        spotModulePanelArrayList.clear();
        spotModulePanelArrayList.add(inputPanel); // Select source
        spotModulePanelArrayList.add(preProcessPanel); // Select channel
        spotModulePanelArrayList.add(detectorPanel); // Detector
        // //spotModulePanelArrayList.add(new DeClusteringPanel(this)); // Desclusturing ( should be included in post processing )
        spotModulePanelArrayList.add(roiPanel); // Select roi / create ROI
        spotModulePanelArrayList.add(filteringPanel); // Geometric filtering
        // spotModulePanelArrayList.add(colocalizationPanel); // later on
        spotModulePanelArrayList.add(outputPanel); // image witness output and data
        spotModulePanelArrayList.add(displayDetectionPanel);
    }

    public void setSelection(final String selection) {
        // get all nodes
        final List<TreeNode> nodes = new ArrayList<>();
        final TreeNode root = (TreeNode) tree.getModel().getRoot();
        nodes.add(root);
        // appendAllChildrenToList(nodes, root, true);

        final java.util.Enumeration<? extends javax.swing.tree.TreeNode> children = root.children();

        // Enumeration children = parent.children();
        if (children != null) {
            while (children.hasMoreElements()) {
                final TreeNode node = children.nextElement();

                if (node.toString().compareTo(selection) == 0) {
                    final TreePath path = new TreePath(new Object[]{root, node});
                    tree.setSelectionPath(path);
                }
            }
        }
    }

    @Override
    public void valueChanged(final TreeSelectionEvent e) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        rightPanel.removeAll();

        if (node == rootWizzard) {
            // System.out.println("wizard");
            rightPanel.add(settingPanel, BorderLayout.CENTER);
            // rightPanel.validate();
            rightPanel.updateUI();
            // rightScrollPane.validate();
            // rightPanel.repaint();
        }

        for (final GeneralSpotDetectionPanel pp : spotModulePanelArrayList) {
            if (pp.getNode() == node) {
                // for (GeneralSpotDetectionPanel preferencePanel : spotModulePanelArrayList)
                // {
                // rightPanel.add(preferencePanel);
                // preferencePanel.setVisible(false);
                // }
                // pp.setVisible(true);
                rightPanel.add(pp, BorderLayout.CENTER);
                pp.setVisible(true);
                rightPanel.updateUI();
                // rightScrollPane.validate();
                // rightPanel.validate();
                // rightPanel.repaint();
            }
        }
    }

    public void checkAndSetBasicPreferences() {
        buildDetectionModulePanelArrayList();
    }

    DefaultMutableTreeNode rootWizzard = null;
    JScrollPane rightScrollPane = null;

    /**
     * Build interface
     */
    @Override
    public void run() {
        // new ToolTipFrame(
        // "<html>"+
        // "<br>Due to a lot of messages in the forum :)," +
        // "<br>the plugin has slightly evolved since last version:"+
        // "<br>"+
        // "<br><b>Important changes:</b>"+
        // "<br><li>Export settings: export as an image, binary image, excel are now disabled by default."+
        // "<br><li>Bloc: detection as ROIs are now exported."+
        // "</html>"
        // );

        mainPanel = GuiUtil.generatePanel();

        icyFrame = GuiUtil.generateTitleFrame(
                "Spot Detector",
                mainPanel,
                new Dimension(600, 120),
                true,
                true,
                true,
                true
        );

        buildDetectionModulePanelArrayList();

        // build menu.

        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        rootWizzard = new DefaultMutableTreeNode("Wizard");
        final DefaultMutableTreeNode rootSettings = new DefaultMutableTreeNode("Settings");
        for (final GeneralSpotDetectionPanel pp : spotModulePanelArrayList) {
            rootSettings.add(pp.getNode());
        }

        // root.add( rootWizzard );
        root.add(rootSettings);

        tree = new JTree(root);

        tree.setRootVisible(false);

        for (int row = 0; row < tree.getRowCount(); row++) // Expand tree
        {
            tree.expandRow(row);
        }

        tree.addTreeSelectionListener(this);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setOpaque(true);
        final JScrollPane treeScrollPane = new JScrollPane(tree);

        mainPanel.setLayout(new BorderLayout());

        // treeScrollPane.setPreferredSize(new Dimension(200, 100));
        treeScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        actionPanel.setLayout(new BorderLayout());

        actionPanel.add(startButton, BorderLayout.EAST);
        actionPanel.add(helpButton, BorderLayout.WEST);

        helpButton.addActionListener(this);

        startButton.addActionListener(this);

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        // leftPanel.add(actionPanel, BorderLayout.SOUTH);

        rightScrollPane = new JScrollPane(rightPanel);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        // mainPanel.add(rightPanel, BorderLayout.CENTER);
        mainPanel.add(rightScrollPane, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        // mainPanel.add(GuiUtil.besidesPanel(new Help("ICY Preferences").getHelpButton("Help")), BorderLayout.SOUTH);

        rightPanel.setLayout(new BorderLayout());
        rightPanel.removeAll();
        // rightPanel.setLayout( new BorderLayout() );

        // for (GeneralSpotDetectionPanel preferencePanel : spotModulePanelArrayList)
        // {
        // //rightPanel.add( preferencePanel , BorderLayout.CENTER );
        // rightPanel.add( preferencePanel);
        // preferencePanel.setVisible(false);
        // }

        // reBuildOutPutPanel();

        // spotModulePanelArrayList.get(0).setVisible(true);
        rightPanel.removeAll();
        rightPanel.add(settingPanel);
        rightPanel.revalidate();
        rightScrollPane.revalidate();

        icyFrame.addToDesktopPane();
        icyFrame.setPreferredSize(new Dimension(800, 480));
        icyFrame.setSize(icyFrame.getPreferredSize());
        icyFrame.setMinimumSize(icyFrame.getPreferredSize());
        icyFrame.setResizable(true);
        icyFrame.pack();
        icyFrame.center();
        icyFrame.toFront();

        new TagPainterManager();

        icyFrame.addFrameListener(this);
    }

    ComputationThread computationThread = null;

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == startButton) {
            startComputation(true, true);
        }

        if (e.getSource() == helpButton) {
            NetworkUtil.openBrowser(getDescriptor().getWeb());
        }
    }

    private void startComputation(final boolean performSaveOperation, final boolean waitForComputationThreadAndLaunch) {
        if (computationThread != null && computationThread.busy) {
            return;
        }

        computationThread = new ComputationThread();
        computationThread.performSaveOperation = performSaveOperation;
        computationThread.start();
    }

    /**
     * Request recomputation of detection
     *
     * @param performSaveOperation              perform save operation
     * @param waitForComputationThreadAndLaunch if true, guarantee that this operation will be performed.
     */
    public void detectionModuleRequestRecompute(final boolean performSaveOperation, final boolean waitForComputationThreadAndLaunch) {
        // System.out.println("************************************** Module request recomputation : " + waitForComputationThreadAndLaunch );
        startComputation(performSaveOperation, waitForComputationThreadAndLaunch);
    }

    class ComputationThread extends Thread {
        public boolean performSaveOperation;
        GlobalDetectionToken gdt;
        boolean busy = true;

        @Override
        public void run() {
            final AnnounceFrame annonceFrame = new AnnounceFrame("Computation started");

            ThreadUtil.invokeNow(() -> startButton.setEnabled(false));

            final GlobalDetectionToken gdt = new GlobalDetectionToken();

            try {
                inputPanel.process(gdt);

                if (gdt.inputSequence == null) {
                    new FailedAnnounceFrame("No input sequence found");
                    return;
                }

                preProcessPanel.process(gdt);

                detectorPanel.process(gdt);

                roiPanel.process(gdt);

                filteringPanel.process(gdt);

                displayDetectionPanel.process(gdt);

                // // Creates Output. Should be changed to let all piece create their own results
                if (performSaveOperation) {
                    outputPanel.process(gdt);
                }
            }
            catch (final InterruptedException e) {
                new FailedAnnounceFrame(e.getMessage());
            }
            catch (final Exception e) {
                IcyLogger.error(this.getClass(), e);
            }
            finally {
                annonceFrame.close();
                ThreadUtil.invokeNow(() -> startButton.setEnabled(true));
                busy = false;
                gdt.fireDetectionFinished();
            }
        }
    }

    public void fireChange(final InputPanel inputPanel) {
        // System.out.println("Fire change");

        // reBuildOutPutPanel();
    }

    @Override
    public void icyFrameClosed(final IcyFrameEvent e) {
        // will remove painter from the display detection Panel
        displayDetectionPanel.close();
    }

    @Override
    public void icyFrameActivated(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameClosing(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameDeactivated(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameDeiconified(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameExternalized(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameIconified(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameInternalized(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameOpened(final IcyFrameEvent e) {
    }

    public void setDetector(final PluginDescriptor detectorPluginDescriptorClass) {
        detectorPanel.setDetectorPlugin(detectorPluginDescriptorClass);
    }

    // /**
    // * Build the outPutPanel
    // */
    // private void reBuildOutPutPanel()
    // {
    //
    // outputPanel.refresh();
    //
    // }
}
