package logviewer.controller;

import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import logviewer.LogRow;
import logviewer.service.FileIOService;
import logviewer.ui.ProgressDialogService;
import logviewer.model.LogViewerModel;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * エクスポート処理を担当するコントローラー。
 * 表示データエクスポート、選択行エクスポートのロジックを集約します。
 */
public class ExportController {
    private final FileIOService fileIOService;
    private final ProgressDialogService progressDialogService;
    private final LogViewerModel model;
    private final TableView<LogRow> table;

    /**
     * ExportController のコンストラクタ。
     * 
     * @param fileIOService ファイルI/Oサービス
     * @param progressDialogService プログレスダイアログサービス
     * @param model ログビューアモデル
     * @param table ログテーブル
     */
    public ExportController(FileIOService fileIOService, ProgressDialogService progressDialogService,
                           LogViewerModel model, TableView<LogRow> table) {
        this.fileIOService = fileIOService;
        this.progressDialogService = progressDialogService;
        this.model = model;
        this.table = table;
    }

    /**
     * 表示中のデータをエクスポートします。
     * ファイル保存ダイアログを表示して、選択されたパスに TSV 形式で保存します。
     * 
     * @param stage ダイアログの親ステージ
     */
    public void exportDisplayedData(Stage stage) {
        if (model.getTableData().isEmpty()) {
            showAlert("情報", "エクスポートするデータがありません。");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("表示データをエクスポート");
        chooser.setInitialFileName("exported_data.tsv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV files", "*.tsv"));
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            // データのスナップショットを取得（UIスレッドで）
            List<LogRow> dataSnapshot = new ArrayList<>(model.getTableData());
            List<Integer> visibleIndicesSnapshot = new ArrayList<>(model.getVisibleColumnIndices());
            Path outputPath = file.toPath();

            // FileIOService でバックグラウンドタスクを生成
            Task<Integer> task = fileIOService.exportDisplayedDataAsync(dataSnapshot, visibleIndicesSnapshot, outputPath);

            task.setOnSucceeded(evt -> {
                int rowCount = task.getValue();
                showAlert("成功", String.format("%d 行をエクスポートしました。", rowCount));
            });

            task.setOnFailed(evt -> {
                Throwable ex = task.getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("エラー");
                alert.setHeaderText("エクスポートに失敗しました");
                alert.setContentText(ex == null ? "不明なエラー" : ex.getMessage());
                alert.showAndWait();
            });

            // プログレスダイアログを表示
            progressDialogService.show(task, "エクスポート中...", stage);

            Thread t = new Thread(task, "export-data-thread");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 選択されている行をエクスポートします。
     * ファイル保存ダイアログを表示して、選択されたパスに TSV 形式で保存します。
     * 
     * @param stage ダイアログの親ステージ
     */
    public void exportSelectedRows(Stage stage) {
        @SuppressWarnings("unchecked")
        ObservableList<TablePosition<LogRow, ?>> selectedCells = (ObservableList<TablePosition<LogRow, ?>>) (ObservableList<?>) table
                .getSelectionModel().getSelectedCells();

        if (selectedCells.isEmpty()) {
            showAlert("情報", "エクスポートする行が選択されていません。");
            return;
        }

        if (model.getVisibleColumnIndices().isEmpty()) {
            showAlert("情報", "表示されている列がありません。");
            return;
        }

        // 選択行インデックスをユニークかつ昇順で取得
        List<Integer> selectedRowIndices = new ArrayList<>();
        for (TablePosition<LogRow, ?> pos : selectedCells) {
            int row = pos.getRow();
            if (!selectedRowIndices.contains(row)) {
                selectedRowIndices.add(row);
            }
        }
        selectedRowIndices.sort(Integer::compareTo);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("選択行をエクスポート");
        chooser.setInitialFileName("selected_rows.tsv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV files", "*.tsv"));
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            // スナップショットを取得（UIスレッドで）
            List<LogRow> itemsSnapshot = new ArrayList<>(table.getItems());
            List<Integer> visibleIndicesSnapshot = new ArrayList<>(model.getVisibleColumnIndices());
            Path outputPath = file.toPath();

            // FileIOService でバックグラウンドタスクを生成
            Task<Integer> task = fileIOService.exportSelectedRowsAsync(
                itemsSnapshot, selectedRowIndices, visibleIndicesSnapshot, outputPath);

            task.setOnSucceeded(evt -> {
                int rowCount = task.getValue();
                showAlert("成功", String.format("%d 行をエクスポートしました。", rowCount));
            });

            task.setOnFailed(evt -> {
                Throwable ex = task.getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("エラー");
                alert.setHeaderText("エクスポートに失敗しました");
                alert.setContentText(ex == null ? "不明なエラー" : ex.getMessage());
                alert.showAndWait();
            });

            // プログレスダイアログを表示
            progressDialogService.show(task, "エクスポート中...", stage);

            Thread t = new Thread(task, "export-selected-rows-thread");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * アラートを表示します。
     * 
     * @param title ダイアログタイトル
     * @param message メッセージ
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
