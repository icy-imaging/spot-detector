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

import loci.formats.FormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import fr.icy.extension.kernel.roi.descriptor.measure.ROIInteriorDescriptor;
import fr.icy.Icy;
import fr.icy.common.type.DataType;
import fr.icy.gui.GuiUtil;
import fr.icy.gui.canvas.Canvas2D;
import fr.icy.gui.frame.progress.FailedAnnounceFrame;
import fr.icy.gui.viewer.Viewer;
import fr.icy.io.FileUtil;
import fr.icy.io.Saver;
import fr.icy.io.xls.XLSXUtil;
import fr.icy.io.xml.XMLUtil;
import fr.icy.model.image.IcyBufferedImage;
import fr.icy.model.roi.ROI;
import fr.icy.model.sequence.Sequence;
import fr.icy.model.sequence.SequenceUtil;
import fr.icy.model.swimmingPool.SwimmingObject;
import fr.icy.system.logging.IcyLogger;
import fr.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import plugins.fab.spotDetector.detector.DetectionToROI;
import plugins.nchenouard.spot.DetectionResult;
import plugins.nchenouard.spot.Spot;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class OutPutPanel extends GeneralSpotDetectionPanel {
    JCheckBox exportXLSCheckBox;
    JCheckBox exportXMLCheckBox;
    JCheckBox exportOriginalImageWithROIAndDetection;
    JCheckBox exportBinaryImage;
    JCheckBox exportSwimmingPoolCheckBox;
    JCheckBox exportToROIcheckBox;
    JCheckBox removePreviousSpotROIcheckBox;
    XLSPanel XLSPanel;
    XMLPanel XMLPanel;

    public OutPutPanel(final SpotDetector spotDetector) {
        super(spotDetector);
        setTitle("Output");

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // exportSwimmingPoolCheckBox = new JCheckBox("Export to SwimmingPool" , true );
        // exportOriginalImageWithROIAndDetection = new JCheckBox("Export original image with ROIs and detection" , true );
        // exportBinaryImage = new JCheckBox("Export binary image" , true );
        // exportToROIcheckBox = new JCheckBox("Export to ROI (experimental as the ROI rendering can be slow)" , false );
        // removePreviousSpotROIcheckBox = new JCheckBox("Remove previous spots rendered as ROI" , true );

        exportToROIcheckBox = new JCheckBox("Export to ROI", false);
        exportSwimmingPoolCheckBox = new JCheckBox("Export to SwimmingPool", false);
        exportOriginalImageWithROIAndDetection = new JCheckBox("Export original image with ROIs and detection", false);
        exportBinaryImage = new JCheckBox("Export binary image", false);
        removePreviousSpotROIcheckBox = new JCheckBox("Remove previous spots rendered as ROI", true);

        XLSPanel = new XLSPanel();
        XMLPanel = new XMLPanel();

        add(XLSPanel);
        add(XMLPanel);

        add(GuiUtil.createLineBoxPanel(exportToROIcheckBox, Box.createHorizontalGlue()));
        add(GuiUtil.createLineBoxPanel(removePreviousSpotROIcheckBox, Box.createHorizontalGlue()));
        add(GuiUtil.createLineBoxPanel(exportOriginalImageWithROIAndDetection, Box.createHorizontalGlue()));
        add(GuiUtil.createLineBoxPanel(exportBinaryImage, Box.createHorizontalGlue()));
        add(GuiUtil.createLineBoxPanel(exportSwimmingPoolCheckBox, Box.createHorizontalGlue()));
        add(Box.createVerticalGlue());

        // add( Box.createVerticalGlue() );
    }

    static class XMLPanel extends JPanel implements ActionListener {
        JPanel detailPanel = new JPanel();

        JButton specifyFile = new JButton("no file selected");
        JTextArea appendLabel = new JTextArea("The XML data will be appended to this file. If the file does not exist, it will be created.");

        File XMLAppendfile = null;
        JCheckBox enableXMLCheckBox = new JCheckBox("Enable XML export", false);

        public XMLPanel() {
            this.setBorder(new TitledBorder("XML output settings:"));
            this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

            final JPanel specificFilePanel = new JPanel();
            specificFilePanel.add(GuiUtil.createLineBoxPanel(enableXMLCheckBox, Box.createHorizontalGlue()));
            specificFilePanel.setLayout(new BoxLayout(specificFilePanel, BoxLayout.PAGE_AXIS));
            enableXMLCheckBox.addActionListener(this);
            appendLabel.setLineWrap(true);
            // appendLabel.setColumns( 15 );
            appendLabel.setWrapStyleWord(true);
            specificFilePanel.add(appendLabel);
            // ComponentUtil.setFixedWidth( specificFilePanel , 400 );
            specificFilePanel.add(GuiUtil.createLineBoxPanel(specifyFile), Box.createHorizontalGlue());

            specifyFile.addActionListener(this);
            this.add(specificFilePanel);

            refreshEnableStatePanel();
        }

        public boolean isSpecificExportFileToXLSSelected() {
            return enableXMLCheckBox.isSelected();
        }

        public File getSelectedFile() {
            return XMLAppendfile;
        }

        public boolean isExportToXMLSelected() {
            return enableXMLCheckBox.isSelected();
        }

        /**
         * refresh enable / not enable components
         */
        void refreshEnableStatePanel() {
            appendLabel.setEnabled(enableXMLCheckBox.isSelected());
            specifyFile.setEnabled(enableXMLCheckBox.isSelected());
        }

        @Override
        public void actionPerformed(final @NotNull ActionEvent e) {

            refreshEnableStatePanel();

            if (e.getSource() == specifyFile) {

                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                final int returnValue = fileChooser.showDialog(null, "XML Export");
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    XMLAppendfile = fileChooser.getSelectedFile();
                    specifyFile.setText(XMLAppendfile.getAbsolutePath());

                    XMLAppendfile = new File(FileUtil.setExtension(XMLAppendfile.getAbsolutePath(), ".xml"));
                }

            }

        }

    }

    static class XLSPanel extends JPanel implements ActionListener {
        JPanel detailPanel = new JPanel();

        JTextArea automaticLabel = new JTextArea("The XLS file will be saved in the 'save' folder of the original image. The file name will be 'originalfilename.xls'. If an XLS file already exists, it will be replaced.");
        JButton specifyFile = new JButton("no file selected");
        JTextArea appendLabel = new JTextArea("The XLS data will be appended to this file. If the file does not exist, it will be created. Each image will consist in one page in the XLS file. If it becomes slow consider using XML. Watch how to use it on the online documentation.");

        File XLSAppendfile = null;
        JCheckBox automaticAppendCheckBox = new JCheckBox("Append data to existing files.", false);
        JCheckBox enableAutomaticCheckBox = new JCheckBox("Enable Automatic", false);
        JCheckBox enableSpecificFileCheckBox = new JCheckBox("Enable Specific file", false);
        JCheckBox exportImageInExcelFileFor2D = new JCheckBox("Export Image (ROI and detections) in the XLS file (only for 2D images)", false);

        public XLSPanel() {
            this.setBorder(new TitledBorder("Excel output settings:"));
            this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

            final JPanel automaticPanel = new JPanel();
            automaticPanel.setBorder(new TitledBorder("Automatic XLS file naming"));
            automaticPanel.setLayout(new BoxLayout(automaticPanel, BoxLayout.PAGE_AXIS));
            automaticPanel.add(GuiUtil.createLineBoxPanel(enableAutomaticCheckBox, Box.createHorizontalGlue()));
            enableAutomaticCheckBox.addActionListener(this);
            automaticLabel.setLineWrap(true);
            automaticLabel.setWrapStyleWord(true);
            // automaticLabel.setColumns( 15 );
            automaticPanel.add(automaticLabel);
            automaticPanel.add(GuiUtil.createLineBoxPanel(automaticAppendCheckBox, Box.createHorizontalGlue()));
            this.add(automaticPanel);

            final JPanel specificFilePanel = new JPanel();
            specificFilePanel.setBorder(new TitledBorder("Append all data to a single file"));
            specificFilePanel.add(GuiUtil.createLineBoxPanel(enableSpecificFileCheckBox, Box.createHorizontalGlue()));
            specificFilePanel.setLayout(new BoxLayout(specificFilePanel, BoxLayout.PAGE_AXIS));
            enableSpecificFileCheckBox.addActionListener(this);
            appendLabel.setLineWrap(true);
            appendLabel.setWrapStyleWord(true);
            // appendLabel.setColumns( 15 );
            specificFilePanel.add(appendLabel);
            specificFilePanel.add(GuiUtil.createLineBoxPanel(specifyFile), Box.createHorizontalGlue());
            specifyFile.addActionListener(this);
            this.add(specificFilePanel);
            // this.add( exportImageInExcelFileFor2D );

            refreshEnableStatePanel();
        }

        public boolean exportImageInXLSFor2D() {
            return exportImageInExcelFileFor2D.isSelected();
        }

        public boolean isSpecificExportFileToXLSSelected() {
            return enableSpecificFileCheckBox.isSelected();
        }

        public File getSelectedSpecificFile() {
            return XLSAppendfile;
        }

        public boolean isAutomaticExportToXLSSelected() {
            return enableAutomaticCheckBox.isSelected();
        }

        public boolean isAutomaticExportToXLSAppendSelected() {
            return automaticAppendCheckBox.isSelected();
        }

        /**
         * refresh enable / not enable components
         */
        void refreshEnableStatePanel() {
            automaticLabel.setEnabled(enableAutomaticCheckBox.isSelected());
            automaticAppendCheckBox.setEnabled(enableAutomaticCheckBox.isSelected());
            appendLabel.setEnabled(enableSpecificFileCheckBox.isSelected());
            specifyFile.setEnabled(enableSpecificFileCheckBox.isSelected());
        }

        @Override
        public void actionPerformed(final @NotNull ActionEvent e) {
            refreshEnableStatePanel();

            if (e.getSource() == specifyFile) {
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                final int returnValue = fileChooser.showDialog(null, "XLS Export");
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    XLSAppendfile = fileChooser.getSelectedFile();
                    specifyFile.setText(XLSAppendfile.getAbsolutePath());
                }
            }
        }
    }

    ArrayList<String> getTagList(final @NotNull Sequence sequence, final ROI roi) {
        final ArrayList<String> tagList = new ArrayList<>();

        final Node tagNode = sequence.getNode("tags");
        if (tagNode != null) {
            final ArrayList<Element> roiElementList = XMLUtil.getElements(tagNode);
            for (final Element roiElement1 : roiElementList) {
                final int roiId = XMLUtil.getAttributeIntValue(roiElement1, "ROI_ID", -1);

                if (roiId == roi.getId()) {
                    final ArrayList<Element> roiTagList = XMLUtil.getElements(roiElement1);

                    for (final Element tag : roiTagList) {
                        final String name = XMLUtil.getAttributeValue(tag, "name", "no name");

                        tagList.add(name);
                    }
                }
            }
        }
        return tagList;
    }

    public void process(final GlobalDetectionToken gdt) throws InterruptedException {
        if (XMLPanel.isExportToXMLSelected()) {
            // Export in XML

            // String userDir = System.getProperty("user.home");
            // String XMLFileName = userDir + FileUtil.separator + "detection xml export.xml";

            final File XMLFile = XMLPanel.getSelectedFile();

            if (XMLFile == null) {
                new FailedAnnounceFrame("XML output is enabled, but no output file was specified !");
                return;
            }

            // System.out.println( XMLFileName );

            Document document = XMLUtil.loadDocument(XMLFile);

            // System.out.println( "Document null : " + (document==null) );

            if (document == null) {
                document = XMLUtil.createDocument(true);
            }

            final HashMap<ROI, ArrayList<DetectionSpot>> ROI2Detection = gdt.roi2detection;
            // ArrayList<ROI> test = gdt.inputSequence.getROIs();

            // System.out.println("roi list size: " + ROI2Detection.keySet().size() );
            // ArrayList<DetectionSpot> detectionResult = gdt.detectionResult;

            for (final ROI roi : ROI2Detection.keySet()) {
                // System.out.println("processing roi " + roi.getName() );
                final ArrayList<DetectionSpot> detection = gdt.roi2detection.get(roi);

                final Element roiElement = XMLUtil.addElement(document.getDocumentElement(), "ROI");
                XMLUtil.setAttributeValue(roiElement, "name", roi.getName());
                XMLUtil.setAttributeIntValue(roiElement, "nb", detection.size());
                XMLUtil.setAttributeIntValue(roiElement, "surface", computeROISurface(roi));
                XMLUtil.setAttributeValue(roiElement, "file_and_folder", gdt.inputSequence.getFilename());
                XMLUtil.setAttributeValue(roiElement, "folder", FileUtil.getDirectory(gdt.inputSequence.getFilename()));
                XMLUtil.setAttributeValue(roiElement, "file", FileUtil.getFileName(gdt.inputSequence.getFilename(), true));
                XMLUtil.setAttributeValue(roiElement, "process_date", new Date().toString());

                final ArrayList<String> tagList = getTagList(gdt.inputSequence, roi);
                // Element tagListElement = XMLUtil.addElement( roiElement , "tag_list" );

                for (final String tagName : tagList) {
                    XMLUtil.setAttributeValue(roiElement, "tag" + (tagList.indexOf(tagName) + 1), tagName);
                }

                // should be the good way (creating a list... but is not well displayed in Excel)
                // for ( String tagName : tagList )
                // {
                // Element tagElement = XMLUtil.addElement( tagListElement , "tag" );
                // XMLUtil.setAttributeValue( tagElement , "tag_name", tagName );
                // }

            }

            XMLUtil.saveDocument(document, XMLFile);
        }

        // createSaveLocation

        // create a save folder

        final File XLSFile;

        String fileName = gdt.inputSequence.getFilename();
        if (fileName == null) {
            fileName = System.getProperty("user.home");
            fileName += FileUtil.separator + "icy";
            FileUtil.createDir(new File(fileName));
            fileName += FileUtil.separator + "noname.tif";
            // System.out.println( "FileName: " + fileName );
        }

        final File inputSequenceFile = new File(fileName);
        String file = inputSequenceFile.getAbsolutePath();
        file = file.substring(0, file.length() - inputSequenceFile.getName().length());
        final String folder = file;
        final File dirResult = new File(folder, FileUtil.separator + "save");
        // if(!dirResult.exists()) dirResult.mkdir();

        // save Output
        // Create and save Final rendered image with ROI and detection

        {
            // save input image with ROIs and detections.

            final File withnessImageFile = new File(folder + FileUtil.separator + "save" + FileUtil.separator + inputSequenceFile.getName() + ".roi.detection.tif");

            ThreadUtil.invokeNow(() -> {
                if (exportOriginalImageWithROIAndDetection.isSelected()) {
                    final Viewer viewer = new Viewer(gdt.inputSequence, false);
                    viewer.setCanvas(Canvas2D.class.getName());
                    final Canvas2D canvas = (Canvas2D) viewer.getCanvas();

                    viewer.setSize(200, 200);
                    try {
                        final BufferedImage renderedImage = canvas.getRenderedImage(0, 0, -1, false);

                        if (!dirResult.exists())
                            dirResult.mkdir();
                        Saver.saveImage(IcyBufferedImage.createFrom(renderedImage), withnessImageFile, true);
                    }
                    catch (final InterruptedException | FormatException | IOException e) {
                        IcyLogger.error(this.getClass(), e);
                    }

                    viewer.close();
                }
            });
        }

        // save image with final detection
        if (exportBinaryImage.isSelected()) {
            final File binaryImageFile = new File(folder + FileUtil.separator + "save" + FileUtil.separator + inputSequenceFile.getName() + ".roi.binary.tif");

            // System.out.println("debug: into export binary");

            ThreadUtil.invokeNow(() -> {
                try {
                    // System.out.println("debug: into export binary run");

                    final Sequence ch = SequenceUtil.extractChannel(gdt.inputSequence, 0);
                    final Sequence sequenceBinaryDetection = SequenceUtil.convertToType(ch, DataType.UBYTE, false);

                    sequenceBinaryDetection.beginUpdate();

                    for (final IcyBufferedImage image : sequenceBinaryDetection.getAllImage()) {
                        final byte[] b = image.getDataXYAsByte(0);
                        Arrays.fill(b, (byte) 0);
                    }

                    // System.out.println("debug: into export binary run 1");

                    for (final DetectionSpot detection : gdt.detectionResult) {
                        // System.out.println( gdt.detectionResult.indexOf( detection ) + " / " + gdt.detectionResult.size() + " p#: " +
                        // detection.points.size() + " " + sequenceBinaryDetection.isUpdating() );

                        final int t = detection.t;

                        for (final Point3D p : detection.points) {
                            // sequenceBinaryDetection.getImage( t , (int)p.z ).setDataAsByte( (int)p.x , (int)p.y, 0, (byte)255 );
                            sequenceBinaryDetection.getDataXYAsByte(t, (int) p.z,
                                    0)[sequenceBinaryDetection.getWidth() * (int) p.y + (int) p.x] = (byte) 255;
                        }

                    }

                    // System.out.println("debug: into export binary run 2");
                    sequenceBinaryDetection.endUpdate();

                    // System.out.println("debug: into export binary run 3");

                    if (!dirResult.exists())
                        dirResult.mkdir();
                    Saver.save(sequenceBinaryDetection, binaryImageFile, false, true);

                }
                catch (final Exception e) {
                    IcyLogger.error(this.getClass(), e);
                    try {
                        throw e;
                    }
                    catch (final Exception e1) {
                        IcyLogger.error(this.getClass(), e1);
                    }
                }
            });

        }

        XLSFile = new File(folder + FileUtil.separator + "save" + FileUtil.separator + inputSequenceFile.getName() + ".xlsx");

        // WritableImage writableRenderedImage = new

        if (XLSPanel.isAutomaticExportToXLSSelected()) {
            if (!dirResult.exists())
                dirResult.mkdir();
            saveToXLS(XLSFile, gdt, XLSPanel.isAutomaticExportToXLSAppendSelected());
        }

        if (XLSPanel.isSpecificExportFileToXLSSelected()) {
            final File xlsAppendFile = XLSPanel.getSelectedSpecificFile();
            if (xlsAppendFile != null) {
                if (!dirResult.exists())
                    dirResult.mkdir();
                saveToXLS(xlsAppendFile, gdt, true);
            }
        }

        if (exportSwimmingPoolCheckBox.isSelected()) {
            try {
                final DetectionResult detectionResultConverted = convertDetectionToDetectionResult(gdt.detectionResult, gdt.inputSequence);
                final SwimmingObject swimmingObject = new SwimmingObject(detectionResultConverted, "detections of " + gdt.inputSequence.getName()); // inputSequenceFile.getName()
                Icy.getMainInterface().getSwimmingPool().add(swimmingObject);
            }
            catch (final Exception e) {
                IcyLogger.error(this.getClass(), e, "Error while creating detection object for swimming pool: is the plugin nchenouard.tracker installed ?");
            }
        }

        if (removePreviousSpotROIcheckBox.isSelected()) {
            final ArrayList<ROI> roiList = new ArrayList<>(gdt.inputSequence.getROIs());
            for (final ROI roi : roiList) {
                if (roi.getName().startsWith("spot #")) {
                    gdt.inputSequence.removeROI(roi);
                }
            }
        }

        if (exportToROIcheckBox.isSelected())
            gdt.inputSequence.addROIs(DetectionToROI.convertDetectionToROI(gdt.detectionResult), true);
    }

    static int computeROISurface(final ROI roi) throws InterruptedException {
        return (int) ROIInteriorDescriptor.computeInterior(roi);
    }

    // FIXME : sequence should be removed from detection results.
    private @NotNull DetectionResult convertDetectionToDetectionResult(final @NotNull ArrayList<DetectionSpot> detections, final Sequence sequence) {
        final DetectionResult detectionResult = new DetectionResult();
        for (final DetectionSpot detectionSpot : detections) {
            final Spot spot = new Spot(
                    detectionSpot.getMassCenter().x,
                    detectionSpot.getMassCenter().y,
                    detectionSpot.getMassCenter().z,
                    detectionSpot.minIntensity,
                    detectionSpot.maxIntensity,
                    detectionSpot.meanIntensity,
                    convertToChenouardPoint3D(detectionSpot.points)
            );
            detectionResult.addDetection(detectionSpot.getT(), spot);
        }

        detectionResult.setSequence(sequence);

        return detectionResult;
    }

    private @NotNull ArrayList<plugins.nchenouard.spot.Point3D> convertToChenouardPoint3D(final @NotNull ArrayList<Point3D> inputList) {
        final ArrayList<plugins.nchenouard.spot.Point3D> returnList = new ArrayList<>();
        for (final Point3D inputPoint : inputList) {
            returnList.add(new plugins.nchenouard.spot.Point3D(inputPoint.x, inputPoint.y, inputPoint.z));
        }
        return returnList;
    }

    /**
     * workaround: the loadworkbook from XLSUtil has some problem because of an extra write wish prevent me from adding data
     **/
    private Workbook loadWorkbook(final @NotNull File file) throws IOException {
        if (!file.exists())
            return XLSXUtil.loadWorkbookForWrite(file);

        return XLSXUtil.createWorkbook();
    }

    /**
     * XLS Save
     */
    void saveToXLS(final File xlsFile, final GlobalDetectionToken gdt, final boolean appendToExistingFile) throws InterruptedException {
        // XLSUtil xlsUtil = null;
        final Workbook workbook; // = null;
        // XlsManager xlsManager = null;
        try {
            if (appendToExistingFile) {
                workbook = loadWorkbook(xlsFile);

                // xlsManager = new XlsManager( xlsFile , true ); // append
            }
            else {
                xlsFile.delete();
                // xlsManager = new XlsManager( xlsFile ); // destroy previous file
                workbook = XLSXUtil.createWorkbook();
            }
        }
        catch (final IOException e) {
            IcyLogger.error(this.getClass(), e, "Cannot write the XLS file ! Is it already open in another application ?");
            return;
        }

        int row;

        final int indexPage = workbook.getNumberOfSheets(); // to avoid having the same name of page else it erase the existing one
        final Sheet page = XLSXUtil.createNewPage(workbook, indexPage + "-" + gdt.inputSequence.getName());

        // xlsManager.createNewPage( FileUtil.getFileName( gdt.inputSequence.getName() ) );

        // save settings.

        // date

        XLSXUtil.setCellString(page, 0, 0, "Date of XLS page:");
        // xlsManager.setLabel( 0 , 0 , "Date of XLS page:" );
        XLSXUtil.setCellString(page, 0, 1, new Date().toString());
        // xlsManager.setLabel( 0 , 1 , new Date().toString() );

        // input
        {
            row = page.getLastRowNum();
            // row = xlsManager.getExcelPage().getRows();
            XLSXUtil.setCellString(page, 0, row, "Input");
            // xlsManager.setLabel( 0 , row , "Input" );
            spotDetector.inputPanel.inputDetection.saveXLS(page, gdt);
        }
        addXLSSeparator(page);

        // pre processor
        {
            row = page.getLastRowNum();
            // row = xlsManager.getExcelPage().getRows();
            XLSXUtil.setCellString(page, 0, row, "Pre processor");
            // xlsManager.setLabel( 0 , row , "Pre processor" );
            spotDetector.preProcessPanel.preProcessorDetection.saveXLS(page, gdt);
        }
        addXLSSeparator(page);

        // Detector
        {
            row = page.getLastRowNum();
            // row = xlsManager.getExcelPage().getRows();
            XLSXUtil.setCellString(page, 0, row, "Detector");
            // xlsManager.setLabel( 0 , row , "Detector" );
            spotDetector.detectorPanel.detectorDetection.saveXLS(page, gdt);
        }
        addXLSSeparator(page);

        // Region of interest
        {
            row = page.getLastRowNum();
            // row = xlsManager.getExcelPage().getRows();
            XLSXUtil.setCellString(page, 0, row, "Region of interest");
            // xlsManager.setLabel( 0 , row , "Region of interest" );
            spotDetector.roiPanel.roiDetection.saveXLS(page, gdt);
        }
        addXLSSeparator(page);

        // re-set the output to save it again ( workaround de la methode load de XLSUtil qui ne marche plus correctement )

        /*
         * try {
         * workbook.setOutputFile( new File( xlsFile.getAbsoluteFile().toString() ) );
         * } catch (IOException e1) {
         * e1.printStackTrace();
         * }
         */

        // Save file

        try {
            XLSXUtil.saveAndClose(workbook, xlsFile);
        }
        catch (final IOException e) {
            IcyLogger.error(this.getClass(), e, "Cannot write the XLS file ! Is it already open in another application ?");
        }

        // xlsManager.SaveAndClose();
    }

    private void addXLSSeparator(final @NotNull Sheet page) {
        final int row = page.getLastRowNum();
        XLSXUtil.setCellString(page, 0, row, "------------");
    }
}
