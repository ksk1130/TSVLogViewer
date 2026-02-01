package logviewer.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import logviewer.LogRow;
import logviewer.service.FileIOService;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * ドラッグ＆ドロップ操作をハンドリングするクラス。
 */
public class DragAndDropHandler {
    private final FileIOService fileIOService;

    /**
     * コンストラクタ。
     * 
     * @param fileIOService ファイルI/Oサービス (ファイル形式の検証に使用)
     */
    public DragAndDropHandler(FileIOService fileIOService) {
        this.fileIOService = fileIOService;
    }

    /**
     * テーブルへドラッグ＆ドロップ処理を設定します。
     * 
     * @param table           対象テーブル
     * @param stage           親ステージ
     * @param onFileAccepted  有効ファイルがドロップされた時の処理
     */
    public void attach(TableView<LogRow> table, Stage stage, Consumer<Path> onFileAccepted) {
        table.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        table.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                table.setOpacity(0.7);
            }
            event.consume();
        });

        table.setOnDragExited(event -> {
            table.setOpacity(1.0);
            event.consume();
        });

        table.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (!files.isEmpty()) {
                    File file = files.get(0);
                    Path path = file.toPath();
                    if (isSupported(path)) {
                        onFileAccepted.accept(path);
                        success = true;
                    } else {
                        showUnsupportedFileAlert(stage);
                    }
                }
            }
            table.setOpacity(1.0);
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * ファイルがサポート対象かどうか判定します。
     * 
     * @param path ファイルパス
     * @return サポート対象なら true
     */
    private boolean isSupported(Path path) {
        if (fileIOService.isSupportedInputFile(path)) {
            return true;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".tsv") || name.endsWith(".txt");
    }

    /**
     * サポートされていないファイル形式のアラートを表示します。
     * 
     * @param owner 親ステージ
     */
    private void showUnsupportedFileAlert(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("サポートされていないファイル形式");
        alert.setHeaderText(null);
        alert.setContentText("TSV または TXT ファイルのみをドロップできます。");
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
}
