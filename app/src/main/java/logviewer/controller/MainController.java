package logviewer.controller;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import logviewer.model.LogViewerModel;
import logviewer.service.FileLoadResult;
import logviewer.service.FileIOService;
import logviewer.service.FilterService;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Main画面のハンドラを集約するコントローラ。
 */
public class MainController {
    private final LogViewerModel model;
    private final FileIOService fileIOService;
    private final FilterService filterService;
    private final AtomicReference<Task<?>> currentLoadTask = new AtomicReference<>();
    private Path currentLoadPath = null;  // 現在読み込んでいるファイルのパス

    /**
     * コンストラクタ。
     * 
     * @param model          LogViewerModel インスタンス
     * @param fileIOService  FileIOService インスタンス
     * @param filterService  FilterService インスタンス
     */
    public MainController(LogViewerModel model, FileIOService fileIOService, FilterService filterService) {
        this.model = model;
        this.fileIOService = fileIOService;
        this.filterService = filterService;
    }

    /**
     * モデルを取得します。
     * 
     * @return LogViewerModel インスタンス
     */
    public LogViewerModel getModel() {
        return model;
    }

    /**
     * ファイルI/Oサービスを取得します。
     * 
     * @return FileIOService インスタンス
     */
    public FileIOService getFileIOService() {
        return fileIOService;
    }

    /**
     * フィルタサービスを取得します。
     * 
     * @return FilterService インスタンス
     */
    public FilterService getFilterService() {
        return filterService;
    }

    /**
     * ファイル選択ダイアログを開いて読み込み処理を開始します。
     * 
     * @param stage     ダイアログを表示するステージ
     * @param onStart   読み込み開始時の処理
     * @param onSuccess 読み込み成功時の処理
     * @param onFailed  読み込み失敗時の処理
     * @return 実行中のタスク (未選択なら null)
     */
    public Task<FileLoadResult> handleOpenFile(
        Stage stage,
        Runnable onStart,
        Consumer<FileLoadResult> onSuccess,
        Consumer<Throwable> onFailed
    ) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("ログファイルを開く");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f == null) {
            return null;
        }
        return handleLoadFile(f.toPath(), onStart, onSuccess, onFailed);
    }

    /**
     * 指定パスのファイル読み込み処理を開始します。
     * 
     * @param path      読み込むファイルのパス
     * @param onStart   読み込み開始時の処理
     * @param onSuccess 読み込み成功時の処理
     * @param onFailed  読み込み失敗時の処理
     * @return 実行中のタスク
     */
    public Task<FileLoadResult> handleLoadFile(
        Path path,
        Runnable onStart,
        Consumer<FileLoadResult> onSuccess,
        Consumer<Throwable> onFailed
    ) {
        Task<?> previous = currentLoadTask.getAndSet(null);
        if (previous != null) {
            previous.cancel();
        }

        this.currentLoadPath = path;  // ファイルパスを記録

        if (onStart != null) {
            onStart.run();
        }

        Task<FileLoadResult> task = fileIOService.loadFileAsync(path);
        task.setOnSucceeded(ev -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(ev -> {
            if (onFailed != null) {
                onFailed.accept(task.getException());
            }
        });

        currentLoadTask.set(task);
        Thread t = new Thread(task, "log-load-thread");
        t.setDaemon(true);
        t.start();
        return task;
    }

    /**
     * 現在読み込んでいるファイルのパスを取得します。
     *
     * @return ファイルパス
     */
    public Path getCurrentLoadPath() {
        return currentLoadPath;
    }
}
