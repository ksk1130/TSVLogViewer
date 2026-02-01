package logviewer.ui;

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

/**
 * 進捗ダイアログを表示するユーティリティ。
 */
public class ProgressDialogService {
    /**
     * タスクの進捗を表示するダイアログを表示します。
     * タスク完了時に自動的にダイアログを閉じます。
     * ユーザーがダイアログを閉じるとタスクはキャンセルされます。
     * 
     * @param task  進捗を表示するタスク
     * @param title ダイアログのタイトル
     * @param owner 親ステージ (null 可)
     */
    public void show(Task<?> task, String title, Stage owner) {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.progressProperty().bind(task.progressProperty());

        Label messageLabel = new Label("処理中...");
        messageLabel.textProperty().bind(task.messageProperty());

        VBox content = new VBox(10, messageLabel, progressBar);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Stage progressStage = new Stage();
        progressStage.setTitle(title);
        if (owner != null) {
            progressStage.initOwner(owner);
        }
        progressStage.setScene(new Scene(content));
        progressStage.setResizable(false);
        progressStage.setOnCloseRequest(evt -> {
            if (task.isRunning()) {
                task.cancel();
            }
        });

        task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_SUCCEEDED, evt -> progressStage.close());
        task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_FAILED, evt -> progressStage.close());
        task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_CANCELLED, evt -> progressStage.close());

        progressStage.show();
    }
}
