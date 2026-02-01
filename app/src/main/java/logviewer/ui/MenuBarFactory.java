package logviewer.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import java.util.function.Consumer;

import logviewer.controller.MainController;
import logviewer.model.LogViewerModel;
import logviewer.service.FileLoadResult;

/**
 * メニューバーの構築を担当するファクトリクラス。
 * ファイル、編集、カラム、移動メニューの構築ロジックをカプセル化します。
 */
public class MenuBarFactory {
    private final MainController controller;
    private final LogViewerModel model;
    private final javafx.scene.control.TableView<logviewer.LogRow> table;
    private final ProgressDialogService progressDialogService;
    private final Runnable onExportDisplayedData;
    private final Runnable onExportSelectedRows;
    private final Runnable onShowColumnVisibilityDialog;
    private final Runnable onShowGoToLineDialog;
    private final Runnable onCopySelection;

    /**
     * MenuBarFactory のコンストラクタ。
     * 
     * @param controller メインコントローラー
     * @param model ログビューアモデル
     * @param table ログテーブル
     * @param progressDialogService プログレスダイアログサービス
     * @param onExportDisplayedData 表示データエクスポート実行時のコールバック
     * @param onExportSelectedRows 選択行エクスポート実行時のコールバック
     * @param onShowColumnVisibilityDialog カラム表示/非表示ダイアログ表示時のコールバック
     * @param onShowGoToLineDialog 行移動ダイアログ表示時のコールバック
     * @param onCopySelection コピー実行時のコールバック
     */
    public MenuBarFactory(MainController controller, LogViewerModel model,
                         javafx.scene.control.TableView<logviewer.LogRow> table,
                         ProgressDialogService progressDialogService,
                         Runnable onExportDisplayedData, Runnable onExportSelectedRows,
                         Runnable onShowColumnVisibilityDialog, Runnable onShowGoToLineDialog,
                         Runnable onCopySelection) {
        this.controller = controller;
        this.model = model;
        this.table = table;
        this.progressDialogService = progressDialogService;
        this.onExportDisplayedData = onExportDisplayedData;
        this.onExportSelectedRows = onExportSelectedRows;
        this.onShowColumnVisibilityDialog = onShowColumnVisibilityDialog;
        this.onShowGoToLineDialog = onShowGoToLineDialog;
        this.onCopySelection = onCopySelection;
    }

    /**
     * メニューバーを構築します。
     * 
     * @param primaryStage メインステージ
     * @param onPrepareForFileLoad ファイル読み込み準備時のコールバック
     * @param onFileLoaded ファイル読み込み成功時のコールバック
     * @param onFileLoadFailed ファイル読み込み失敗時のコールバック
     * @return 構築されたメニューバー
     */
    public MenuBar build(Stage primaryStage, 
                        Runnable onPrepareForFileLoad,
                        Consumer<FileLoadResult> onFileLoaded,
                        Consumer<Throwable> onFileLoadFailed) {
        MenuBar menuBar = new MenuBar();
        
        Menu fileMenu = buildFileMenu(primaryStage, onPrepareForFileLoad, onFileLoaded, onFileLoadFailed);
        Menu editMenu = buildEditMenu();
        Menu columnMenu = buildColumnMenu();
        Menu goMenu = buildGoMenu();
        
        menuBar.getMenus().addAll(fileMenu, editMenu, columnMenu, goMenu);
        return menuBar;
    }

    /**
     * ファイルメニューを構築します。
     */
    private Menu buildFileMenu(Stage primaryStage,
                              Runnable onPrepareForFileLoad,
                              Consumer<FileLoadResult> onFileLoaded,
                              Consumer<Throwable> onFileLoadFailed) {
        Menu fileMenu = new Menu("ファイル(_F)");
        
        MenuItem openItem = new MenuItem("開く...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> {
            Task<FileLoadResult> task = controller.handleOpenFile(
                primaryStage,
                onPrepareForFileLoad,
                onFileLoaded,
                onFileLoadFailed
            );
            if (task != null) {
                progressDialogService.show(task, "ファイル読み込み中...", primaryStage);
            }
        });

        MenuItem exportDisplayedItem = new MenuItem("表示中のデータをエクスポート...");
        exportDisplayedItem.setOnAction(e -> onExportDisplayedData.run());
        exportDisplayedItem.disableProperty().bind(Bindings.isEmpty(model.getTableData()));

        MenuItem exportSelectedRowsItem = new MenuItem("選択行をエクスポート...");
        exportSelectedRowsItem.setAccelerator(
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        exportSelectedRowsItem.setOnAction(e -> onExportSelectedRows.run());
        exportSelectedRowsItem.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedCells()));

        MenuItem exitItem = new MenuItem("終了");
        exitItem.setOnAction(e -> Platform.exit());
        
        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), exportDisplayedItem, exportSelectedRowsItem,
            new SeparatorMenuItem(), exitItem);
        
        return fileMenu;
    }

    /**
     * 編集メニューを構築します。
     */
    private Menu buildEditMenu() {
        Menu editMenu = new Menu("編集(_E)");
        
        MenuItem copyItem = new MenuItem("コピー");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> onCopySelection.run());
        copyItem.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedCells()));
        
        editMenu.getItems().addAll(copyItem);
        return editMenu;
    }

    /**
     * カラムメニューを構築します。
     */
    private Menu buildColumnMenu() {
        Menu columnMenu = new Menu("カラム(_C)");
        
        MenuItem columnVisibilityItem = new MenuItem("カラムの表示/非表示...");
        columnVisibilityItem.setOnAction(e -> onShowColumnVisibilityDialog.run());
        
        columnMenu.getItems().add(columnVisibilityItem);
        return columnMenu;
    }

    /**
     * 移動メニューを構築します。
     */
    private Menu buildGoMenu() {
        Menu goMenu = new Menu("移動(_G)");
        
        MenuItem goToLineItem = new MenuItem("指定行へ移動...");
        goToLineItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));
        goToLineItem.setOnAction(e -> onShowGoToLineDialog.run());
        
        goMenu.getItems().add(goToLineItem);
        return goMenu;
    }
}
