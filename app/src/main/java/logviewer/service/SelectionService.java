package logviewer.service;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import logviewer.LogRow;

/**
 * テーブル行選択ロジックを担当するサービスクラス。
 * Ctrl/Shift キーに応じた複数行選択を集約します。
 */
public class SelectionService {
    
    /**
     * 行番号セルのクリック処理をハンドルします。
     * 修飾キー(Ctrl/Shift)に応じて異なる選択動作を実行します。
     * 
     * @param evt         マウスイベント
     * @param cell        クリックされたセル
     * @param lineCol     行番号カラム
     * @param table       対象テーブル
     */
    public static void handleLineNumberCellPress(javafx.scene.input.MouseEvent evt, 
                                                  TableCell<LogRow, Number> cell,
                                                  TableColumn<LogRow, Number> lineCol,
                                                  TableView<LogRow> table) {
        if (cell.isEmpty() || cell.getTableRow() == null) {
            return;
        }

        int rowIndex = cell.getIndex();
        if (rowIndex < 0 || rowIndex >= table.getItems().size()) {
            return;
        }

        if (evt.isControlDown()) {
            // Ctrl: トグル選択
            handleCtrlSelection(table, rowIndex, lineCol);
        } else if (evt.isShiftDown()) {
            // Shift: 範囲選択
            handleShiftSelection(table, rowIndex, lineCol);
        } else {
            // 通常: 単一選択
            handleSimpleSelection(table, rowIndex, lineCol);
        }
    }

    /**
     * Ctrl キー押下時の行選択（トグル選択）。
     * 
     * @param table テーブル
     * @param rowIndex 行インデックス
     * @param lineCol 行番号カラム
     */
    private static void handleCtrlSelection(TableView<LogRow> table, int rowIndex, TableColumn<LogRow, Number> lineCol) {
        var selectionModel = table.getSelectionModel();
        
        if (isRowSelected(table, rowIndex, lineCol)) {
            // 既に選択されていればトグルで非選択
            deselectRow(table, rowIndex);
        } else {
            // 未選択なら選択追加
            selectRowCells(table, rowIndex);
        }
    }

    /**
     * Shift キー押下時の行選択（範囲選択）。
     * 
     * @param table テーブル
     * @param rowIndex 行インデックス
     * @param lineCol 行番号カラム
     */
    private static void handleShiftSelection(TableView<LogRow> table, int rowIndex, TableColumn<LogRow, Number> lineCol) {
        var selectionModel = table.getSelectionModel();
        var selectedCells = selectionModel.getSelectedCells();
        
        if (selectedCells.isEmpty()) {
            selectRowCells(table, rowIndex);
            return;
        }

        // 最後に選択された行インデックスを取得
        int lastSelectedRow = selectedCells.get(selectedCells.size() - 1).getRow();
        
        int start = Math.min(lastSelectedRow, rowIndex);
        int end = Math.max(lastSelectedRow, rowIndex);

        // 範囲内のセルをすべて選択
        for (int row = start; row <= end; row++) {
            for (int col = 0; col < table.getColumns().size(); col++) {
                selectionModel.select(row, table.getColumns().get(col));
            }
        }
    }

    /**
     * 修飾キーなしの行選択（単一選択）。
     * 
     * @param table テーブル
     * @param rowIndex 行インデックス
     * @param lineCol 行番号カラム
     */
    private static void handleSimpleSelection(TableView<LogRow> table, int rowIndex, TableColumn<LogRow, Number> lineCol) {
        var selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        selectRowCells(table, rowIndex);
    }

    /**
     * 指定行のすべてのセルを選択します。
     * 
     * @param table テーブル
     * @param rowIndex 行インデックス
     */
    private static void selectRowCells(TableView<LogRow> table, int rowIndex) {
        var selectionModel = table.getSelectionModel();
        for (int col = 0; col < table.getColumns().size(); col++) {
            selectionModel.select(rowIndex, table.getColumns().get(col));
        }
    }

    /**
     * 指定行のセルをすべて非選択にします。
     * 
     * @param table テーブル
     * @param rowIndex 行インデックス
     */
    private static void deselectRow(TableView<LogRow> table, int rowIndex) {
        var selectionModel = table.getSelectionModel();
        for (int col = 0; col < table.getColumns().size(); col++) {
            selectionModel.clearSelection(rowIndex, table.getColumns().get(col));
        }
    }

    /**
     * 指定行が選択されているかを判定します。
     * 
     * @param table テーブル
     * @param rowIndex 行インデックス
     * @param lineCol 行番号カラム
     * @return 選択されている場合は true
     */
    private static boolean isRowSelected(TableView<LogRow> table, int rowIndex, TableColumn<LogRow, Number> lineCol) {
        var selectionModel = table.getSelectionModel();
        // 行のいずれかのセルが選択されているか確認
        for (int col = 0; col < table.getColumns().size(); col++) {
            if (selectionModel.isSelected(rowIndex, table.getColumns().get(col))) {
                return true;
            }
        }
        return false;
    }
}
