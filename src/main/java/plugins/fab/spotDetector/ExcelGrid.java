/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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

import jxl.Cell;
import jxl.Sheet;
import jxl.write.WritableSheet;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Display a writableSheet in an Excel-like Grid.
 *
 * @author Fabrice de Chaumont
 */
public class ExcelGrid extends JScrollPane {
    private JTable table;

    public ExcelGrid(final WritableSheet page) {
        table = new JTable();
        updateSheet(page);

        this.setViewportView(table);
        this.setAutoscrolls(true);
    }

    public synchronized void updateSheet(final WritableSheet page) {
        table = new JTable();
        this.setViewportView(table);
        if (page != null) table.setModel(new SheetTableModel(page));
    }

    private static class SheetTableModel implements TableModel {

        private final Sheet sheet;

        public SheetTableModel(final Sheet sheet) {
            this.sheet = sheet;
        }

        @Override
        public int getRowCount() {
            return sheet.getRows();
        }

        @Override
        public int getColumnCount() {
            return sheet.getColumns();
        }

        /**
         * Copied from javax.swing.table.AbstractTableModel,
         * to name columns using spreadsheet conventions:
         * A, B, C, ..., Z, AA, AB, etc.
         */
        @Override
        public String getColumnName(int column) {
            final StringBuilder result = new StringBuilder();
            for (; column >= 0; column = column / 26 - 1) {
                result.insert(0, (char) ((char) (column % 26) + 'A'));
            }
            return result.toString();
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            try {
                final Cell cell = sheet.getCell(columnIndex, rowIndex);
                return cell.getContents();
            }
            catch (final Exception e) {
                //
            }
            return null;
        }

        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        }

        @Override
        public void addTableModelListener(final TableModelListener l) {
        }

        @Override
        public void removeTableModelListener(final TableModelListener l) {
        }
    }
}

