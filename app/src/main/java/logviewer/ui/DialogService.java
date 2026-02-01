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

/**
 * ダイアログ処理を担当するサービスクラス。
 * 行移動ダイアログ、カラム表示/非表示ダイアログを集約します。
 */
public class DialogService {
    private static final String LINE_SEPARATOR = "\r\n";

    private final LogViewerModel model;
    private final NavigationService navigationService;
    private final TableView<LogRow> table;

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
     * ユーザーがカラムの表示/非表示を設定できます。
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
            String colName = col.getText();
            
            CheckBox checkbox = new CheckBox(colName);
            checkbox.setSelected(col.isVisible());
            checkbox.setOnAction(e -> col.setVisible(checkbox.isSelected()));
            
            content.getChildren().add(checkbox);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
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
