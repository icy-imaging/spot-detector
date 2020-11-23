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
package plugins.fab.spotDetector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.network.NetworkUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.abstract_.PluginActionable;
import icy.system.thread.ThreadUtil;
import plugins.fab.ROITagger.TagPainterManager;

/**
 * Spot detector
 *
 * @author Fabrice de Chaumont
 */
public class SpotDetector extends PluginActionable implements TreeSelectionListener, ActionListener, IcyFrameListener
{

    IcyFrame icyFrame;
    private JTree tree;
    private JPanel rightPanel = new JPanel();

    JPanel actionPanel = new JPanel();
    JButton startButton = new JButton("Start Detection");
    JButton helpButton = new JButton("Help");
    JPanel leftPanel = new JPanel();
    JPanel mainPanel = null;

    private ArrayList<GeneralSpotDetectionPanel> spotModulePanelArrayList = new ArrayList<GeneralSpotDetectionPanel>();

    SettingPanel settingPanel = new SettingPanel();
    InputPanel inputPanel = new InputPanel(this);
    PreProcessPanel preProcessPanel = new PreProcessPanel(this);
    ROIPanel roiPanel = new ROIPanel(this);
    ColocalizationPanel colocalizationPanel = new ColocalizationPanel(this);
    OutPutPanel outputPanel = new OutPutPanel(this);
    DetectorPanel detectorPanel = new DetectorPanel(this);
    DisplayDetectionPanel displayDetectionPanel = new DisplayDetectionPanel(this);
    FilteringPanel filteringPanel = new FilteringPanel(this);

    public ArrayList<GeneralSpotDetectionPanel> getSpotModulePanelArrayList()
    {
        return new ArrayList<GeneralSpotDetectionPanel>(spotModulePanelArrayList);
    }

    private void buildDetectionModulePanelArrayList()
    {
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

    public void setSelection(String selection)
    {

        // get all nodes

        List<TreeNode> nodes = new ArrayList<TreeNode>();
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        nodes.add(root);
        // appendAllChildrenToList(nodes, root, true);

        Enumeration<TreeNode> children = root.children();

        // Enumeration children = parent.children();
        if (children != null)
        {
            while (children.hasMoreElements())
            {
                TreeNode node = children.nextElement();

                if (node.toString().compareTo(selection) == 0)
                {
                    TreePath path = new TreePath(new Object[] {root, node});
                    tree.setSelectionPath(path);
                }
            }
        }

    }

    public void valueChanged(TreeSelectionEvent e)
    {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        rightPanel.removeAll();

        if (node == rootWizzard)
        {
            // System.out.println("wizard");
            rightPanel.add(settingPanel, BorderLayout.CENTER);
            // rightPanel.validate();
            rightPanel.updateUI();
            // rightScrollPane.validate();
            // rightPanel.repaint();
        }

        for (GeneralSpotDetectionPanel pp : spotModulePanelArrayList)
        {
            if (pp.getNode() == node)
            {
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

    public void checkAndSetBasicPreferences()
    {
        buildDetectionModulePanelArrayList();
    }

    DefaultMutableTreeNode rootWizzard = null;
    JScrollPane rightScrollPane = null;

    /**
     * Build interface
     */
    @Override
    public void run()
    {
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

        icyFrame = GuiUtil.generateTitleFrame("Spot Detector", mainPanel, new Dimension(600, 120), true, true, true,
                true);

        buildDetectionModulePanelArrayList();

        // build menu.

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        rootWizzard = new DefaultMutableTreeNode("Wizard");
        DefaultMutableTreeNode rootSettings = new DefaultMutableTreeNode("Settings");
        for (GeneralSpotDetectionPanel pp : spotModulePanelArrayList)
        {
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
        JScrollPane treeScrollPane = new JScrollPane(tree);

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

        icyFrame.addToMainDesktopPane();
        icyFrame.setSize(new Dimension(512, 480));
        // icyFrame.pack();
        icyFrame.center();
        icyFrame.toFront();

        new TagPainterManager();

        icyFrame.addFrameListener(this);
    }

    ComputationThread computationThread = null;

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == startButton)
        {
            startComputation(true, true);
        }

        if (e.getSource() == helpButton)
        {
            NetworkUtil.openBrowser(getDescriptor().getWeb());
        }

    }

    /**
     * @param performSaveOperation
     * @param waitForComputationThreadAndLaunch
     */
    private void startComputation(boolean performSaveOperation, boolean waitForComputationThreadAndLaunch)
    {
        if (computationThread != null && computationThread.busy)
        {
            return;
        }

        computationThread = new ComputationThread();
        computationThread.performSaveOperation = performSaveOperation;
        computationThread.start();
    }

    /**
     * Request recomputation of detection
     * 
     * @param performSaveOperation
     *        perform save operation
     * @param waitForComputationThreadAndLaunch
     *        if true, guarantee that this operation will be performed.
     */
    public void detectionModuleRequestRecompute(boolean performSaveOperation, boolean waitForComputationThreadAndLaunch)
    {
        // System.out.println("************************************** Module request recomputation : " + waitForComputationThreadAndLaunch );
        startComputation(performSaveOperation, waitForComputationThreadAndLaunch);
    }

    class ComputationThread extends Thread
    {
        public boolean performSaveOperation;
        GlobalDetectionToken gdt;
        boolean busy = true;

        @Override
        public void run()
        {

            AnnounceFrame annonceFrame = new AnnounceFrame("Computation started");

            ThreadUtil.invokeNow(new Runnable()
            {
                @Override
                public void run()
                {
                    startButton.setEnabled(false);
                }
            });

            GlobalDetectionToken gdt = new GlobalDetectionToken();

            try
            {
                inputPanel.process(gdt);

                if (gdt.inputSequence == null)
                {
                    new FailedAnnounceFrame("No input sequence found");
                    return;
                }

                preProcessPanel.process(gdt);

                detectorPanel.process(gdt);

                roiPanel.process(gdt);

                filteringPanel.process(gdt);

                displayDetectionPanel.process(gdt);

                // // Creates Output. Should be changed to let all piece create their own results
                if (performSaveOperation)
                {
                    outputPanel.process(gdt);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                annonceFrame.close();
                ThreadUtil.invokeNow(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        startButton.setEnabled(true);
                    }
                });
                busy = false;
                gdt.fireDetectionFinished();
            }
        }
    }

    public void fireChange(InputPanel inputPanel)
    {

        // System.out.println("Fire change");

        // reBuildOutPutPanel();

    }

    @Override
    public void icyFrameClosed(IcyFrameEvent e)
    {

        // will remove painter from the display detection Panel
        displayDetectionPanel.close();

    }

    @Override
    public void icyFrameActivated(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameClosing(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameDeactivated(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameDeiconified(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameExternalized(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameIconified(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameInternalized(IcyFrameEvent e)
    {
    }

    @Override
    public void icyFrameOpened(IcyFrameEvent e)
    {
    }

    public void setDetector(PluginDescriptor detectorPluginDescriptorClass)
    {

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
