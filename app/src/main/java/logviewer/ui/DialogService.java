package logviewer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import logviewer.LogRow;
import logviewer.model.LogViewerModel;
import logviewer.service.NavigationService;
import logviewer.service.ColumnVisibilityConfigService;

import java.util.ArrayList;
import java.util.List;

/**
 * ダイアログ処理を担当するサービスクラス。
 * 行移動ダイアログ、カラム表示/非表示ダイアログを集約します。
 */
public class DialogService {
    private static final String LINE_SEPARATOR = "\r\n";

    private final LogViewerModel model;
    private final NavigationService navigationService;
    private final TableView<LogRow> table;
    private final ColumnVisibilityConfigService configService;

    /**
     * DialogService のコンストラクタ。
     * 
     * @param model ログビューアモデル
     * @param navigationService ナビゲーションサービス
     * @param table ログテーブル
     */
    public DialogService(LogViewerModel model, NavigationService navigationService,
                        TableView<LogRow> table) {
        this.model = model;
        this.navigationService = navigationService;
        this.table = table;
        this.configService = new ColumnVisibilityConfigService();
    }

    /**
     * 指定行番号へジャンプするダイアログを表示します。
     * ユーザーが行番号を入力するとその行にスクロール移動します。
     */
    public void showGoToLineDialog() {
        if (model.getTableData().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("移動");
            alert.setHeaderText(null);
            alert.setContentText("データが読み込まれていません。");
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("指定行へ移動");
        dialog.setHeaderText(null);
        dialog.setContentText("移動先の行番号を入力してください:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                int lineNumber = Integer.parseInt(input.trim());

                if (lineNumber < 1) {
                    showAlert("エラー", "行番号は1以上の整数を入力してください。");
                    return;
                }

                // 表示中のデータから該当する行番号を持つ行を検索
                int foundIndex = navigationService.findRowIndexByLineNumber(model.getTableData(), lineNumber);

                if (foundIndex >= 0) {
                    // 該当行を選択してスクロール
                    table.getSelectionModel().clearSelection();
                    table.getSelectionModel().select(foundIndex);
                    table.scrollTo(foundIndex);
                    table.requestFocus();
                } else {
                    // 該当行が見つからない場合、フィルタで除外されているか存在しない
                    if (lineNumber <= model.getBaseData().size()) {
                        showAlert("情報", String.format("行番号 %d は現在のフィルタ条件で非表示になっています。", lineNumber));
                    } else {
                        showAlert("情報", String.format("行番号 %d は存在しません。%s（最大行番号: %d）", lineNumber, LINE_SEPARATOR,
                                model.getBaseData().size()));
                    }
                }
            } catch (NumberFormatException ex) {
                showAlert("エラー", "有効な整数を入力してください。");
            }
        });
    }

    /**
     * カラムの表示/非表示を切り替えるダイアログを表示します。
     * ユーザーがカラムの表示/非表示を設定でき、OKボタンでの確定時に設定が自動保存されます。
     */
    public void showColumnVisibilityDialog() {
        if (table.getColumns().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "カラムがありません。先にファイルを開いてください。", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("カラムの表示/非表示");
        dialog.setHeaderText("表示するカラムを選択してください");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // 行番号カラムはスキップ（常に表示）
        for (int i = 1; i < table.getColumns().size(); i++) {
            var col = table.getColumns().get(i);
            
            // カラム名を取得（UserDataからインデックスを取得して名前を生成）
            String colName;
            Object userData = col.getUserData();
            if (userData instanceof Integer) {
                int colIndex = (Integer) userData;
                colName = "Col " + colIndex;
            } else {
                // フォールバック：インデックスから直接生成
                colName = "Column " + (i - 1);
            }
            
            CheckBox checkbox = new CheckBox(colName);
            checkbox.setSelected(col.isVisible());
            checkbox.setOnAction(e -> col.setVisible(checkbox.isSelected()));
            
            content.getChildren().add(checkbox);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        dialog.setOnCloseRequest(e -> {
            // OKボタンまたはXボタンで閉じた時に設定を保存
            saveColumnVisibility();
        });
        
        dialog.showAndWait();
    }

    /**
     * 現在のカラム表示/非表示設定を設定ファイルに保存します。
     */
    public void saveColumnVisibility() {
        String fileName = model.getCurrentFileName();
        if (fileName == null || fileName.isEmpty()) {
            return;  // ファイルが読み込まれていない場合はスキップ
        }

        List<Integer> hiddenColumns = new ArrayList<>();
        // 行番号カラムはスキップ（インデックス0）
        for (int i = 1; i < table.getColumns().size(); i++) {
            if (!table.getColumns().get(i).isVisible()) {
                // インデックス1以上のカラムが非表示の場合、i-1をカラムインデックスとして記録
                hiddenColumns.add(i - 1);
            }
        }

        configService.saveHiddenColumns(fileName, hiddenColumns);
    }

    /**
     * 指定されたファイル名のカラム表示/非表示設定を復元して、テーブルに反映します。
     *
     * @param fileName ファイル名（パスを除いたファイル名のみ）
     */
    public void restoreColumnVisibility(String fileName) {
        if (table.getColumns().isEmpty()) {
            return;  // テーブルが初期化されていない場合はスキップ
        }

        List<Integer> hiddenColumns = configService.loadHiddenColumns(fileName);

        // 最初はすべてのカラムを表示
        for (int i = 1; i < table.getColumns().size(); i++) {
            table.getColumns().get(i).setVisible(true);
        }

        // 非表示設定されていたカラムを非表示にする
        for (Integer hiddenIndex : hiddenColumns) {
            if (hiddenIndex >= 0 && hiddenIndex < table.getColumns().size() - 1) {
                table.getColumns().get(hiddenIndex + 1).setVisible(false);
            }
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
