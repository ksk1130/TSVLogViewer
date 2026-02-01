package logviewer.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;

import logviewer.LogRow;

/**
 * テーブルの初期化を担当するクラス。
 * テーブル設定、行ダブルクリック処理、ソート設定、キー操作を集約します。
 */
public class TableInitializer {
    private static final String LINE_SEPARATOR = "\r\n";
    
    private final TableView<LogRow> table;

    /**
     * TableInitializer のコンストラクタ。
     * 
     * @param table 初期化対象のテーブル
     */
    public TableInitializer(TableView<LogRow> table) {
        this.table = table;
    }

    /**
     * テーブルを初期化します。
     * プレースホルダー、選択モード、行ファクトリを設定します。
     */
    public void initialize(Callback<TableView<LogRow>, Boolean> sortPolicy,
                           EventHandler<KeyEvent> keyHandler) {
        setupPlaceholder();
        setupSelectionMode();
        setupRowFactory();
        setupSortPolicy(sortPolicy);
        setupKeyHandler(keyHandler);
    }

    /**
     * プレースホルダーテキストを設定します。
     */
    private void setupPlaceholder() {
        table.setPlaceholder(new Label("ログファイルを開いてください (ファイル -> Openまたは、ログファイルをドラッグ＆ドロップ)"));
    }

    /**
     * テーブルの選択モードを設定します。
     */
    private void setupSelectionMode() {
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * 行ファクトリ（ダブルクリック処理）を設定します。
     */
    private void setupRowFactory() {
        table.setRowFactory(tv -> {
            TableRow<LogRow> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    // ダブルクリック -> 詳細を表示
                    LogRow r = row.getItem();
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Log Detail");
                    a.setHeaderText(null);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < r.fieldCount(); i++) {
                        sb.append("[").append(i).append("]\t").append(r.getField(i)).append(LINE_SEPARATOR);
                    }
                    a.setContentText(sb.toString());
                    a.showAndWait();
                }
            });
            return row;
        });
    }

    /**
     * テーブルのソートポリシーを設定します。
     * ここで必要な場合はカスタムソート処理を設定できます。
     */
    private void setupSortPolicy(Callback<TableView<LogRow>, Boolean> sortPolicy) {
        if (sortPolicy != null) {
            table.setSortPolicy(sortPolicy);
        }
    }

    /**
     * テーブルのキー操作ハンドラを設定します。
     *
     * @param keyHandler キーイベントハンドラ
     */
    private void setupKeyHandler(EventHandler<KeyEvent> keyHandler) {
        if (keyHandler != null) {
            table.setOnKeyPressed(keyHandler);
        }
    }
}
