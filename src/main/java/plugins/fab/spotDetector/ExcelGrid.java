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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Display a writableSheet in an Excel-like Grid.
 *
 * @author Fabrice de Chaumont
 */
public class ExcelGrid extends JScrollPane {
    private static final Logger LOGGER = Logger.getLogger(ExcelGrid.class.getName());

    private JTable table;

    public ExcelGrid(final Sheet page) {
        table = new JTable();
        updateSheet(page);

        this.setViewportView(table);
        this.setAutoscrolls(true);
    }

    public synchronized void updateSheet(final Sheet page) {
        table = new JTable();
        this.setViewportView(table);
        if (page != null) table.setModel(new SheetTableModel(page));
    }

    private static class SheetTableModel implements TableModel {
        private final Sheet sheet;

        @Contract(pure = true)
        public SheetTableModel(final Sheet sheet) {
            this.sheet = sheet;
        }

        @Override
        public int getRowCount() {
            return sheet.getLastRowNum();
        }

        @Override
        public int getColumnCount() {
            return sheet.getRow(sheet.getFirstRowNum()).getLastCellNum();
        }

        /**
         * Copied from javax.swing.table.AbstractTableModel,
         * to name columns using spreadsheet conventions:
         * A, B, C, ..., Z, AA, AB, etc.
         */
        @Override
        public @NotNull String getColumnName(int column) {
            final StringBuilder result = new StringBuilder();
            for (; column >= 0; column = column / 26 - 1) {
                result.insert(0, (char) ((char) (column % 26) + 'A'));
            }
            return result.toString();
        }

        @Contract(pure = true)
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return String.class;
        }

        @Contract(pure = true)
        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return false;
        }

        @Override
        public @Nullable Object getValueAt(final int rowIndex, final int columnIndex) {
            try {
                final Cell cell = sheet.getRow(rowIndex).getCell(columnIndex);
                return switch (cell.getCellType()) {
                    default -> cell.getStringCellValue();
                    case NUMERIC -> cell.getNumericCellValue();
                    case BOOLEAN -> cell.getBooleanCellValue();
                    case FORMULA -> cell.getCellFormula();
                    case BLANK, ERROR, _NONE -> null;
                };
            }
            catch (final Exception e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Unable to get value at position (" + rowIndex + ", " + columnIndex + ")", e);
            }
            return null;
        }

        @Contract(pure = true)
        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            //
        }

        @Contract(pure = true)
        @Override
        public void addTableModelListener(final TableModelListener l) {
            //
        }

        @Contract(pure = true)
        @Override
        public void removeTableModelListener(final TableModelListener l) {
            //
        }
    }
}

