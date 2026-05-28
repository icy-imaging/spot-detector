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

package plugins.fab.spotDetector.preProcessing;

import org.apache.poi.ss.usermodel.Sheet;
import fr.icy.common.collection.array.Array1DUtil;
import fr.icy.common.type.DataType;
import fr.icy.gui.GuiUtil;
import fr.icy.io.xls.XLSXUtil;
import fr.icy.model.image.IcyBufferedImage;
import fr.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;
import plugins.fab.spotDetector.GlobalDetectionToken;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class MeanOfAllChannelsPreProcessor extends PreProcessingAbstract {
    public MeanOfAllChannelsPreProcessor() {
        getPanel().setLayout(new BoxLayout(getPanel(), BoxLayout.PAGE_AXIS));
        getPanel().setBorder(new TitledBorder("Mean (Average) of all channels"));

        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("The input creates the average of all channels")));
        getPanel().add(GuiUtil.createLineBoxPanel(new JLabel("and keep z and t image location")));
        getPanel().add(GuiUtil.createLineBoxPanel(Box.createVerticalGlue()));
    }

    @Override
    public void process(final @NotNull GlobalDetectionToken gdt) {
        gdt.inputComputationSequence = new Sequence();

        for (int t = 0; t < gdt.inputSequence.getSizeT(); t++)
            for (int z = 0; z < gdt.inputSequence.getSizeZ(t); z++) {
                final IcyBufferedImage image = gdt.inputSequence.getImage(t, z);
                final IcyBufferedImage averageImage = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1, DataType.DOUBLE);
                final double[] result = new double[image.getSizeX() * image.getSizeY()];

                final double[][] imageArray = new double[image.getSizeC()][];

                for (int c = 0; c < image.getSizeC(); c++) {
                    imageArray[c] = Array1DUtil.arrayToDoubleArray(image.getDataCopyXY(0), image.isSignedDataType());
                }

                for (int i = 0; i < image.getSizeX() * image.getSizeY(); i++) {
                    result[i] = 0;
                    for (int c = 0; c < image.getSizeC(); c++) {
                        result[i] += imageArray[c][i];
                    }
                    result[i] /= image.getSizeC();
                }

                averageImage.setDataXYAsDouble(0, result);

                gdt.inputComputationSequence.setImage(t, z, averageImage);

                //Icy.addSequence( gdt.inputComputationSequence );
            }
    }

    @Override
    public void saveXLS(final @NotNull Sheet page, final GlobalDetectionToken gdt) {
        final int row = page.getLastRowNum();
        XLSXUtil.setCellString(page, 0, row, "Pre processor :");
        XLSXUtil.setCellString(page, 1, row, "Average of all channels :");
//		xlsManager.setLabel( 0 , row , "Pre processor :" );
//		xlsManager.setLabel( 1 , row , "Average of all channels" );
    }
}
