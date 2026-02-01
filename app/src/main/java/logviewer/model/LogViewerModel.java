package logviewer.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import logviewer.LogRow;

import java.util.ArrayList;
import java.util.List;

/**
 * ログビューアのデータモデルクラス。
 * ログデータ、フィルタ・ソート状態、UI状態を管理します。
 */
public class LogViewerModel {
    // ===== データ管理 =====
    private final List<LogRow> baseData = new ArrayList<>();
    private final ObservableList<LogRow> tableData = FXCollections.observableArrayList();
    
    // ===== フィルタ・ソート状態 =====
    private int sortColumnIndex = -1; // -1 は行番号を意味する
    private boolean sortAscending = true;
    private List<Integer> visibleColumnIndices = new ArrayList<>();
    private String singleFilterText = "";
    private String singleFilterColumn = "All";
    
    // ===== UI状態 =====
    private final StringProperty statusMessage = new SimpleStringProperty("準備完了");
    private String currentFileName = "";  // 現在読み込んでいるファイル名（パス除外）
    private int columnCount = 0;
    private long operationStartTime = 0;
    private boolean skipFilterStatusUpdate = false;
    
    // ===== Getters =====
    
    /**
     * ベースデータ（フィルタ前の全データ）を取得します。
     */
    public List<LogRow> getBaseData() {
        return baseData;
    }
    
    /**
     * テーブルに表示されるデータ（フィルタ後）を取得します。
     */
    public ObservableList<LogRow> getTableData() {
        return tableData;
    }
    
    /**
     * ステータスメッセージプロパティを取得します。
     * リアルタイムでメッセージを監視するため、このプロパティにリスナーを追加できます。
     * 
     * @return ステータスメッセージプロパティ
     */
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    /**
     * 現在のステータスメッセージを取得します。
     * 
     * @return ステータスメッセージ
     */
    public String getStatusMessage() {
        return statusMessage.get();
    }
    
    /**
     * ソート列インデックスを取得します。
     * -1は行番号でソートすることを意味します。
     * 
     * @return ソート対象カラムのインデックス（-1で行番号）
     */
    public int getSortColumnIndex() {
        return sortColumnIndex;
    }
    
    /**
     * ソート順序が昇順かどうかを取得します。
     * 
     * @return 昇順なら true、降順なら false
     */
    public boolean isSortAscending() {
        return sortAscending;
    }
    
    /**
     * 表示中のカラムインデックスリストを取得します。
     * 非表示カラムは含まれません。
     * 
     * @return 表示中のカラムインデックスリスト
     */
    public List<Integer> getVisibleColumnIndices() {
        return visibleColumnIndices;
    }
    
    /**
     * 単一フィルタのテキストを取得します。
     * 
     * @return フィルタテキスト
     */
    public String getSingleFilterText() {
        return singleFilterText;
    }
    
    /**
     * 単一フィルタのカラム選択を取得します。
     * 
     * @return カラム名（"All" または "Column N"）
     */
    public String getSingleFilterColumn() {
        return singleFilterColumn;
    }
    
    /**
     * 列数を取得します。
     * 
     * @return カラムの総数
     */
    public int getColumnCount() {
        return columnCount;
    }
    
    /**
     * 操作開始時刻（ナノ秒）を取得します。
     * 処理時間計測に使用されます。
     * 
     * @return 操作開始時刻
     */
    public long getOperationStartTime() {
        return operationStartTime;
    }
    
    /**
     * フィルタステータス更新をスキップするかどうかを取得します。
     * 
     * @return スキップ対象なら true
     */
    public boolean isSkipFilterStatusUpdate() {
        return skipFilterStatusUpdate;
    }

    /**
     * 現在読み込んでいるファイル名を取得します。
     * 
     * @return ファイル名（パスを除いた部分のみ）
     */
    public String getCurrentFileName() {
        return currentFileName;
    }
    
    // ===== Setters =====
    
    /**
     * ソート設定を更新します。
     * 
     * @param columnIndex ソート対象カラムのインデックス（-1で行番号）
     * @param ascending   昇順ならtrue、降順ならfalse
     */
    public void setSortConfig(int columnIndex, boolean ascending) {
        this.sortColumnIndex = columnIndex;
        this.sortAscending = ascending;
    }
    
    /**
     * 単一フィルタの設定を更新します。
     * 
     * @param text   フィルタテキスト
     * @param column フィルタ対象カラム
     */
    public void setSingleFilter(String text, String column) {
        this.singleFilterText = text != null ? text : "";
        this.singleFilterColumn = column != null ? column : "All";
    }
    
    /**
     * 列数を設定します。
     * 
     * @param count カラムの総数
     */
    public void setColumnCount(int count) {
        this.columnCount = count;
    }
    
    /**
     * 操作開始時刻を設定します。
     * 処理時間の計測に使用します。
     * 
     * @param nanoTime System.nanoTime() の値
     */
    public void setOperationStartTime(long nanoTime) {
        this.operationStartTime = nanoTime;
    }
    
    /**
     * フィルタステータス更新スキップフラグを設定します。
     * 
     * @param skip スキップするなら true
     */
    public void setSkipFilterStatusUpdate(boolean skip) {
        this.skipFilterStatusUpdate = skip;
    }

    /**
     * 現在読み込んでいるファイル名を設定します。
     * 
     * @param fileName ファイル名（パスを除いた部分のみ）
     */
    public void setCurrentFileName(String fileName) {
        this.currentFileName = fileName != null ? fileName : "";
    }
    
    /**
     * ステータスメッセージを更新します。
     * 
     * @param message 新しいステータスメッセージ
     */
    public void setStatusMessage(String message) {
        this.statusMessage.set(message);
    }
    
    /**
     * 表示カラムインデックスリストを更新します。
     * 
     * @param indices 表示するカラムのインデックスリスト
     */
    public void setVisibleColumnIndices(List<Integer> indices) {
        this.visibleColumnIndices.clear();
        this.visibleColumnIndices.addAll(indices);
    }
    
    // ===== ビジネスロジック =====
    
    /**
     * すべてのデータと設定をクリアします。
     * ファイルを新たに読み込む際に呼び出されます。
     */
    public void clearAllData() {
        baseData.clear();
        tableData.clear();
        columnCount = 0;
        sortColumnIndex = -1;
        sortAscending = true;
        visibleColumnIndices.clear();
        singleFilterText = "";
        singleFilterColumn = "All";
    }
    
    /**
     * ベースデータに1行を追加します。
     * 
     * @param row 追加するログロー
     */
    public void addBaseDataRow(LogRow row) {
        baseData.add(row);
    }
    
    /**
     * 複数行をベースデータに追加します。
     * 一括追加時に使用すると効率的です。
     * 
     * @param rows 追加するログロー一覧
     */
    public void addBaseDataRows(List<LogRow> rows) {
        baseData.addAll(rows);
    }
    
    /**
     * テーブルに表示するデータを更新します。
     * フィルタ・ソート後のデータをここに設定します。
     * 
     * @param rows テーブルに表示するログロー一覧
     */
    public void setTableData(List<LogRow> rows) {
        tableData.setAll(rows);
    }
}
