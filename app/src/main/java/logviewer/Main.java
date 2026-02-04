package logviewer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ScrollPane;

import logviewer.controller.MainController;
import logviewer.controller.ExportController;
import logviewer.model.LogViewerModel;
import logviewer.service.ClipboardService;
import logviewer.service.FileIOService;
import logviewer.service.FilterService;
import logviewer.service.FilterSortService;
import logviewer.service.NavigationService;
import logviewer.service.SortService;
import logviewer.service.SelectionService;
import logviewer.service.FileLoadResult;
import logviewer.service.ServiceConstants;
import logviewer.ui.DragAndDropHandler;
import logviewer.ui.FilterConditionPanel;
import logviewer.ui.MenuBarFactory;
import logviewer.ui.TableInitializer;
import logviewer.ui.SingleFilterPanel;
import logviewer.ui.DialogService;
import logviewer.ui.ProgressDialogService;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * TSV形式のログファイルを効率的に閲覧するためのJavaFXアプリケーション。
 * 大容量ファイルにも対応し、フィルタリングやソート、セル選択・コピー機能を備えています。
 */
public class Main extends Application {
    // ===== モデル =====
    private final LogViewerModel model = new LogViewerModel();
    
    // ===== サービス =====
    private final FileIOService fileIOService = new FileIOService();
    private final FilterService filterService = new FilterService();
    private final SortService sortService = new SortService();
    private final FilterSortService filterSortService = new FilterSortService();
    private final ClipboardService clipboardService = new ClipboardService();
    private final NavigationService navigationService = new NavigationService();
    private final DragAndDropHandler dragAndDropHandler = new DragAndDropHandler(fileIOService);
    private final ProgressDialogService progressDialogService = new ProgressDialogService();
    private final MainController controller = new MainController(model, fileIOService, filterService);

    // ===== UI コンポーネント =====
    private final TableView<LogRow> table = new TableView<>();
    private final AtomicReference<Task<?>> currentTask = new AtomicReference<>();
    private ComboBox<String> columnSelector = new ComboBox<>();
    private TextField filterField = new TextField();
    private TableColumn<LogRow, ?> lineNumberColumn;
    private Label statusLabel;

    // ===== コントローラー・ファクトリ =====
    private ExportController exportController;
    private MenuBarFactory menuBarFactory;
    private TableInitializer tableInitializer;
    private SingleFilterPanel singleFilterPanel;
    private DialogService dialogService;
    
    // ===== 複数検索条件用 =====
    private FilterConditionPanel filterConditionPanel;
    private SplitPane centerPane;

    /**
     * アプリケーションのエントリポイント。メニュー、フィルタ入力、テーブルを構築し、
     * ソートポリシーやコピー動作などのハンドラを束ねて表示します。
     * 
     * @param primaryStage メインステージ
     */
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // コントローラー/サービスの初期化
        exportController = new ExportController(fileIOService, progressDialogService, model, table);
        dialogService = new DialogService(model, navigationService, table);
        menuBarFactory = new MenuBarFactory(
            controller, model, table, progressDialogService,
            () -> exportController.exportDisplayedData(primaryStage),
            () -> exportController.exportSelectedRows(primaryStage),
            () -> dialogService.showColumnVisibilityDialog(),
            () -> dialogService.showGoToLineDialog(),
            this::copySelection
        );

        MenuBar menuBar = menuBarFactory.build(primaryStage,
            this::prepareForFileLoad,
            this::onFileLoaded,
            this::onFileLoadFailed,
            this::cleanupAfterCloseFile
        );

        // 左側パネル: 複数検索条件
        filterConditionPanel = createFilterConditionPanel();
        
        // 上部コントロール: 単一検索UI
        columnSelector.getItems().add("All");
        columnSelector.getSelectionModel().selectFirst();
        singleFilterPanel = new SingleFilterPanel(columnSelector, filterField, this::toggleFilterPanel);
        HBox topBox = singleFilterPanel;

        // テーブル初期化
        tableInitializer = new TableInitializer(table);
        tableInitializer.initialize(tv -> {
            if (tv.getSortOrder().isEmpty()) {
                model.setSortConfig(-1, true);
            } else {
                TableColumn<LogRow, ?> sortCol = tv.getSortOrder().get(0);
                boolean ascending = sortCol.getSortType() != TableColumn.SortType.DESCENDING;
                if (sortCol == lineNumberColumn) {
                    model.setSortConfig(-1, ascending);
                } else {
                    Object ud = sortCol.getUserData();
                    int columnIndex = (ud instanceof Integer) ? (Integer) ud : -1;
                    model.setSortConfig(columnIndex, ascending);
                }
            }
            refreshAsync();
            return true;
        }, event -> {
            KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
            if (ctrlC.match(event)) {
                copySelection();
                event.consume();
            }
        });

        // データバインディング
        table.setItems(model.getTableData());

        // フィルタ/ソートの変更時に更新
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            model.setSingleFilter(newVal, columnSelector.getValue());
            refreshAsync();
        });
        columnSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setSingleFilter(filterField.getText(), newVal);
            refreshAsync();
        });

        // ドラッグ＆ドロップの設定
        dragAndDropHandler.attach(table, primaryStage, path -> {
            Task<FileLoadResult> task = controller.handleLoadFile(
                path,
                this::prepareForFileLoad,
                this::onFileLoaded,
                this::onFileLoadFailed
            );
            if (task != null) {
                progressDialogService.show(task, "ファイル読み込み中...", primaryStage);
            }
        });

        // 中央部分: 左パネル + テーブルをSplitPaneで配置
        centerPane = new SplitPane();
        centerPane.getItems().add(table); // 初期状態はテーブルのみ
        
        root.setTop(new VBox(menuBar, topBox));
        root.setCenter(centerPane);

        // ステータスバー
        statusLabel = new Label(model.getStatusMessage());
        statusLabel.textProperty().bind(model.statusMessageProperty());
        HBox statusBar = new HBox(10, statusLabel);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1500, 800);
        primaryStage.setTitle("TSV Log Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * メニューバーを構築します。
     * ファイル開く、エクスポート、カラム表示/非表示、行移動、編集メニューを含みます。
     * 
     * @param primaryStage メインステージ
     * @return メニューバー
     */
    private MenuBar getMenuBar(Stage primaryStage) {
        return menuBarFactory.build(primaryStage,
            this::prepareForFileLoad,
            this::onFileLoaded,
            this::onFileLoadFailed,
            this::cleanupAfterCloseFile
        );
    }

    /**
     * ファイル読み込み開始時のUI初期化を行います。
     */
    private void prepareForFileLoad() {
        model.clearAllData();
        table.getColumns().clear();
        columnSelector.getItems().setAll("All");
        columnSelector.getSelectionModel().selectFirst();
        table.setPlaceholder(new Label("Loading..."));
        model.setStatusMessage("ファイルを読み込み中...");
        model.setOperationStartTime(System.nanoTime());
    }

    /**
     * ファイルクローズ時のUI初期化を行います。
     */
    private void cleanupAfterCloseFile() {
        table.getColumns().clear();
        columnSelector.getItems().setAll("All");
        columnSelector.getSelectionModel().selectFirst();
        filterField.clear();
        table.setPlaceholder(new Label("ファイルを開いてください"));
    }

    /**
     * ファイル読み込み完了時の処理を実行します。
     * テーブルにデータを反映します。
     * 
     * @param result ファイル読み込み結果
     */
    private void onFileLoaded(FileLoadResult result) {
        if (result == null) {
            return;
        }
        
        // ファイル名をモデルに設定
        Path currentPath = controller.getCurrentLoadPath();
        if (currentPath != null) {
            String fileName = currentPath.getFileName().toString();
            model.setCurrentFileName(fileName);
        }
        
        model.addBaseDataRows(result.rows);
        finalizeLoad(result.columns, result.truncated);
    }

    /**
     * ファイル読み込み失敗時の処理を行います。
     * 
     * @param ex 例外
     */
    private void onFileLoadFailed(Throwable ex) {
        Alert a = new Alert(Alert.AlertType.ERROR,
                "ファイルの読み込みに失敗しました: " + (ex == null ? "不明なエラー" : ex.getMessage()),
                ButtonType.OK);
        a.showAndWait();
        table.setPlaceholder(new Label("TSVログファイルを開いてください (ファイル -> 開く...)"));
        model.setStatusMessage("ファイル読み込みに失敗しました");
    }

    /**
     * 読み込み完了後にカラムを再構築し、列選択リストを更新して初回のフィルタ/ソートを実行します。
     * 行数が上限で打ち切られた場合は情報ダイアログで通知します。
     * 
     * @param columns   検出された列数
     * @param truncated 行数上限で打ち切られた場合は true
     */
    private void finalizeLoad(int columns, boolean truncated) {
        // columnSelectorの設定でリスナーが発火してrefreshAsyncが呼ばれる可能性があるため、
        // 最初にフラグを設定してoperationStartTimeが上書きされないようにする
        model.setSkipFilterStatusUpdate(true);

        if (columns == 0) {
            table.setPlaceholder(new Label("No data"));
            return;
        }

        model.setColumnCount(columns);
        rebuildColumns(columns);
        
        // カラムの表示/非表示設定を復元
        String fileName = model.getCurrentFileName();
        if (fileName != null && !fileName.isEmpty()) {
            dialogService.restoreColumnVisibility(fileName);
        }

        List<String> cols = new ArrayList<>();
        cols.add("All");
        for (int i = 0; i < columns; i++) {
            cols.add("Column " + i);
        }
        columnSelector.getItems().setAll(cols);
        columnSelector.getSelectionModel().selectFirst();
        
        // 既存の検索条件のカラムセレクタも更新
        filterConditionPanel.updateColumns(cols);

        table.setPlaceholder(new Label("No rows"));

        long elapsedMillis = (System.nanoTime() - model.getOperationStartTime()) / 1_000_000;
        double elapsedSeconds = elapsedMillis / 1000.0;
        model.setStatusMessage(String.format("ファイル読み込みが完了しました。読み込み行数 %,d 行、処理時間 %.2f 秒", model.getBaseData().size(), elapsedSeconds));

        // 読み込んだデータをテーブルに直接設定（初期状態は行番号順なのでソート不要）
        model.setTableData(new ArrayList<>(model.getBaseData()));

        if (truncated) {
            String message = String.format("メモリ使用量を抑えるため、ファイルは %,d 行で打ち切られました。", ServiceConstants.MAX_ROWS);
            Alert info = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            info.setHeaderText(null);
            info.showAndWait();
        }
    }

    /**
     * テーブルのカラムを動的に再構築します。
     * 指定されたカラム数に応じてカラムを追加します。
     * 
     * @param columns カラム数
     */
    private void rebuildColumns(int columns) {
        table.getColumns().clear();
        
        // 行番号カラムを生成・追加
        lineNumberColumn = createLineNumberColumn();
        table.getColumns().add(lineNumberColumn);

        // データカラムを生成・追加
        for (int i = 0; i < columns; i++) {
            table.getColumns().add(createDataColumn(i));
        }

        // 初期状態のvisibleColumnIndicesを構築
        updateVisibleColumnIndices();
    }

    /**
     * 行番号カラムを生成します。
     * 
     * @return 行番号カラム
     */
    private TableColumn<LogRow, Number> createLineNumberColumn() {
        TableColumn<LogRow, Number> lineCol = new TableColumn<>("Line");
        lineCol.setReorderable(false);
        lineCol.setPrefWidth(81);
        lineCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().getLineNumber()));
        lineCol.setSortable(false); // ボタン操作でのみソート

        // ヘッダUI設定
        HBox lineHeaderBox = createColumnHeader("Line", lineCol, 
            () -> sortColumn(lineCol, TableColumn.SortType.ASCENDING),
            () -> sortColumn(lineCol, TableColumn.SortType.DESCENDING));
        lineCol.setText(null);
        lineCol.setGraphic(lineHeaderBox);

        // セルファクトリ設定（選択処理を含む）
        lineCol.setCellFactory(col -> createLineNumberCell(lineCol));
        
        return lineCol;
    }

    /**
     * データカラムを生成します。
     * 
     * @param colIndex カラムのインデックス
     * @return データカラム
     */
    private TableColumn<LogRow, String> createDataColumn(int colIndex) {
        TableColumn<LogRow, String> col = new TableColumn<>("Col " + colIndex);
        col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getField(colIndex)));
        col.setPrefWidth(150);
        col.setComparator((a, b) -> a.compareToIgnoreCase(b));
        col.setUserData(colIndex); // カスタムソート用にインデックスを保持
        col.setSortable(false); // ボタン操作に限定

        // ヘッダUI設定
        HBox headerBox = createColumnHeader("Col " + colIndex, col,
            () -> sortColumn(col, TableColumn.SortType.ASCENDING),
            () -> sortColumn(col, TableColumn.SortType.DESCENDING));
        col.setText(null);
        col.setGraphic(headerBox);

        // ヘッダコンテキストメニュー
        ContextMenu headerMenu = new ContextMenu();
        MenuItem hideItem = new MenuItem("このカラムを非表示");
        hideItem.setOnAction(e -> col.setVisible(false));
        headerMenu.getItems().add(hideItem);
        col.setContextMenu(headerMenu);

        // 表示状態変更リスナー
        col.visibleProperty().addListener((obs, oldVal, newVal) -> updateVisibleColumnIndices());

        // セルファクトリ設定（コピー機能）
        col.setCellFactory(tc -> createDataCell());
        
        return col;
    }

    /**
     * カラムヘッダを作成します。
     * 昇順・降順ボタンを含みます。
     * 
     * @param columnName カラム名
     * @param column     TableColumn
     * @param onAsc      昇順ボタン押下時の処理
     * @param onDesc     降順ボタン押下時の処理
     * @return ヘッダパネル
     */
    private HBox createColumnHeader(String columnName, TableColumn<?, ?> column, Runnable onAsc, Runnable onDesc) {
        Label headerLabel = new Label(columnName);
        Button sortAscButton = new Button("▲");
        Button sortDescButton = new Button("▼");
        
        sortAscButton.setFocusTraversable(false);
        sortDescButton.setFocusTraversable(false);
        sortAscButton.setPadding(new Insets(0, 4, 0, 4));
        sortDescButton.setPadding(new Insets(0, 4, 0, 4));
        
        sortAscButton.setOnAction(e -> onAsc.run());
        sortDescButton.setOnAction(e -> onDesc.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox headerBox = new HBox(4, headerLabel, spacer, sortAscButton, sortDescButton);
        headerBox.setAlignment(Pos.CENTER_RIGHT);
        column.getProperties().put("columnName", columnName);
        
        return headerBox;
    }

    /**
     * カラムをソートします。
     * 
     * @param column カラム
     * @param sortType ソート方向
     */
    private void sortColumn(TableColumn<LogRow, ?> column, TableColumn.SortType sortType) {
        table.getSortOrder().setAll(java.util.List.of(column));
        column.setSortType(sortType);
        table.sort();
    }

    /**
     * 行番号カラムのセルを生成します。
     * 複数行選択機能（Ctrl/Shift キー対応）を含みます。
     * 
     * @param lineCol 行番号カラム
     * @return 行番号セル
     */
    private TableCell<LogRow, Number> createLineNumberCell(TableColumn<LogRow, Number> lineCol) {
        return new TableCell<>() {
            {
                setStyle("-fx-alignment: CENTER-RIGHT;");
                addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, evt -> 
                    handleLineNumberCellPress(evt, this, lineCol));
            }

            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.format(Locale.US, "%,d", item.intValue()));
            }
        };
    }

    /**
     * 行番号セルのクリック処理をハンドルします。
     * 修飾キー(Ctrl/Shift)に応じて異なる選択動作を実行します。
     * 
     * @param evt         マウスイベント
     * @param cell        クリックされたセル
     * @param lineCol     行番号カラム
     */
    private void handleLineNumberCellPress(javafx.scene.input.MouseEvent evt, TableCell<LogRow, Number> cell,
                                           TableColumn<LogRow, Number> lineCol) {
        SelectionService.handleLineNumberCellPress(evt, cell, lineCol, table);
        evt.consume();
    }

    /**
     * データカラムのセルを生成します。
     * Ctrl+クリックでセル値をコピー機能を含みます。
     * 
     * @return データセル
     */
    private TableCell<LogRow, String> createDataCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }

            {
                setOnMouseClicked(ev -> {
                    if (!isEmpty() && ev.isControlDown()) {
                        clipboardService.copyText(getItem());
                    }
                });
            }
        };
    }

    /**
     * テーブルの表示・非表示カラムのインデックスリストを更新します。
     * このリストはエクスポート時に使用されます。
     */
    private void updateVisibleColumnIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 1; i < table.getColumns().size(); i++) { // Line列(0)をスキップ
            TableColumn<LogRow, ?> col = table.getColumns().get(i);
            if (col.isVisible()) {
                indices.add(i - 1); // LogRowのフィールドインデックス
            }
        }
        model.setVisibleColumnIndices(indices);
    }

    /**
     * フィルタとソートをバックグラウンドで計算し、完了後にテーブルへ反映します。
     * 直前の計算タスクが残っていればキャンセルし、最新の入力・ソート条件で再計算します。
     */
    private void refreshAsync() {
        List<LogRow> snapshot = new ArrayList<>(model.getBaseData());
        String filterText = model.getSingleFilterText();
        String selectedColumn = model.getSingleFilterColumn();
        int targetSortIndex = model.getSortColumnIndex();
        boolean ascending = model.isSortAscending();
        // ファイル読み込み直後の初回フィルタでは時刻を上書きしない
        if (!model.isSkipFilterStatusUpdate()) {
            model.setOperationStartTime(System.nanoTime());
        }

        // 複数条件が存在する場合は複数条件を優先
        Predicate<LogRow> predicate;
        if (!filterConditionPanel.getConditions().isEmpty()) {
            predicate = buildMultiplePredicate();
        } else {
            // FilterServiceを使用してPredicateを構築
            int columnIndex = "All".equals(selectedColumn) ? -1 : columnSelector.getSelectionModel().getSelectedIndex() - 1;
            predicate = filterService.buildPredicate(filterText, selectedColumn, columnIndex);
        }
        
        // SortServiceを使用してComparatorを構築
        Comparator<LogRow> comparator = sortService.buildComparator(targetSortIndex, ascending);

        // FilterSortService でフィルタとソートを実行
        Task<List<LogRow>> task = filterSortService.filterAndSortAsync(snapshot, predicate, comparator);

        task.setOnSucceeded(evt -> {
            List<LogRow> result = task.getValue();
            long elapsedMillis = (System.nanoTime() - model.getOperationStartTime()) / 1_000_000;
            double elapsedSeconds = elapsedMillis / 1000.0;
            // ファイル読み込み直後のリスナー発火ではステータス更新をスキップ
            if (!model.isSkipFilterStatusUpdate() && !model.getBaseData().isEmpty()) {
                model.setStatusMessage(String.format("フィルタ/ソートが完了しました。結果 %,d 行、処理時間 %.2f 秒", result.size(), elapsedSeconds));
            }
            model.setSkipFilterStatusUpdate(false);
            model.setTableData(result);
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            String msg = ex == null ? "不明なエラー" : ex.getMessage();
            Alert a = new Alert(Alert.AlertType.ERROR, "フィルタ/ソートに失敗しました: " + msg, ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        });

        Task<?> previous = currentTask.getAndSet(task);
        if (previous != null) {
            previous.cancel();
        }

        Thread t = new Thread(task, "refresh-task");
        t.setDaemon(true);
        t.start();
    }

    /**
     * カラムの表示/非表示を切り替えるダイアログを表示します。
     */
    /**
     * カラムの表示/非表示を切り替えるダイアログを表示します。
     * ユーザーがカラムの表示/非表示を設定できます。
     */
    private void showColumnVisibilityDialog() {
        dialogService.showColumnVisibilityDialog();
        updateVisibleColumnIndices();
    }

    /**
     * 複数の検索条件を組み合わせたPredicateを構築します。
     * すべての条件にマッチする行のみを通過させます（AND条件）。
     * 
     * @return 複数条件を組み合わせたPredicate
     */
    private Predicate<LogRow> buildMultiplePredicate() {
        List<Predicate<LogRow>> predicates = new ArrayList<>();

        for (FilterConditionPanel.FilterCondition condition : filterConditionPanel.getConditions()) {
            String text = condition.filterField.getText();
            String selected = condition.columnSelector.getValue();
            
            if (text == null || text.isBlank()) {
                continue; // 空の条件はスキップ
            }
            
            // FilterServiceを使用してPredicateを作成
            int columnIndex = "All".equals(selected) ? -1 : condition.columnSelector.getSelectionModel().getSelectedIndex() - 1;
            Predicate<LogRow> p = filterService.buildPredicate(text, selected, columnIndex);
            predicates.add(p);
        }
        
        // FilterServiceを使用して複数条件を結合
        return filterService.combinePredicates(predicates);
    }

    /**
     * テーブルの選択セルをテキストにしてクリップボードにコピーします。
     * 複数セルが選択されている場合はタブ区切りで結合します。
     */
    private void copySelection() {
        TableView.TableViewSelectionModel<LogRow> sm = table.getSelectionModel();
        @SuppressWarnings("unchecked")
        ObservableList<TablePosition<LogRow, ?>> selectedCells = (ObservableList<TablePosition<LogRow, ?>>) (ObservableList<?>) sm
                .getSelectedCells();

        if (selectedCells.isEmpty()) {
            return;
        }

        StringBuilder clipboardString = new StringBuilder();

        // 選択セルを行ごとにグループ化
        int currentRow = -1;
        boolean isFullRowSelection = false;

        // 行全体が選択されているか確認（行内の全カラムが選択されているか）
        if (selectedCells.size() >= table.getColumns().size()) {
            int firstRow = selectedCells.get(0).getRow();
            boolean allColumnsSelected = true;
            for (TableColumn<LogRow, ?> col : table.getColumns()) {
                boolean found = false;
                for (TablePosition<LogRow, ?> pos : selectedCells) {
                    if (pos.getRow() == firstRow && pos.getTableColumn() == col) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    allColumnsSelected = false;
                    break;
                }
            }
            isFullRowSelection = allColumnsSelected;
        }

        if (isFullRowSelection) {
            // 行全体が選択されている場合 - タブ区切りで行全体をコピー
            List<Integer> processedRows = new ArrayList<>();
            for (TablePosition<LogRow, ?> pos : selectedCells) {
                int row = pos.getRow();
                if (!processedRows.contains(row)) {
                    processedRows.add(row);
                    LogRow logRow = table.getItems().get(row);
                    for (int i = 0; i < logRow.fieldCount(); i++) {
                        if (i > 0)
                            clipboardString.append("\t");
                        clipboardString.append(logRow.getField(i));
                    }
                    clipboardString.append(ServiceConstants.LINE_SEPARATOR);
                }
            }
        } else {
            // 単一セルまたは部分選択の場合 - 選択セルをコピー
            for (TablePosition<LogRow, ?> pos : selectedCells) {
                if (currentRow != -1 && currentRow != pos.getRow()) {
                    clipboardString.append(ServiceConstants.LINE_SEPARATOR);
                }
                currentRow = pos.getRow();

                TableColumn<LogRow, ?> column = pos.getTableColumn();
                Object cellValue = column.getCellData(pos.getRow());

                if (cellValue != null) {
                    clipboardString.append(cellValue.toString());
                }
            }
        }

        clipboardService.copyText(clipboardString.toString());
    }

    /**
     * 指定行へ移動するダイアログを表示します。
     * ユーザーが入力した行番号が表示中のデータ（フィルタ適用後）に存在する場合、
     * その行を選択してスクロールします。
     */
    private void showGoToLineDialog() {
        dialogService.showGoToLineDialog();
    }

    /**
     * 表示中のデータをTSVファイルにエクスポートします。
     * フィルタ/ソート適用後のデータが対象です。
     * 
     * @param stage ファイル保存ダイアログを表示するステージ
     */
    private void exportDisplayedData(Stage stage) {
        exportController.exportDisplayedData(stage);
    }

    /**
     * 選択されている行をTSVファイルにエクスポートします。
     * 表示中の列のみを対象にします。
     * 
     * @param stage ファイル保存ダイアログを表示するステージ
     */
    private void exportSelectedRows(Stage stage) {
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
                showAlert("成功", String.format("選択した %,d 行をエクスポートしました。", rowCount));
            });

            task.setOnFailed(evt -> {
                Throwable ex = task.getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("エラー");
                alert.setHeaderText("エクスポートに失敗しました");
                alert.setContentText(ex == null ? "不明なエラー" : ex.getMessage());
                alert.showAndWait();
            });

            progressDialogService.show(task, "選択行をエクスポート中...", stage);

            Thread t = new Thread(task, "export-selected-rows-thread");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 検索条件パネルの表示/非表示を切り替えます。
     */
    private void toggleFilterPanel() {
        if (centerPane.getItems().contains(filterConditionPanel)) {
            // パネルを閉じる
            centerPane.getItems().remove(filterConditionPanel);
        } else {
            // パネルを開く
            centerPane.getItems().add(0, filterConditionPanel);
            Platform.runLater(() -> centerPane.setDividerPositions(0.20));
        }
    }
    
    /**
     * 複数検索条件パネルを作成します。
     * 
     * @return 検索条件パネル
     */
    private FilterConditionPanel createFilterConditionPanel() {
        List<String> columns = new ArrayList<>();
        columns.add("All");
        return new FilterConditionPanel(columns, this::refreshAsync, this::refreshAsync);
    }
    
    /**
     * アラートダイアログを表示します。
     * 
     * @param title   ダイアログのタイトル
     * @param message 表示するメッセージ
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * JavaFXアプリケーションのエントリポイント。
     * 
     * @param args 起動引数
     */
    public static void main(String[] args) {
        launch(args);
    }
}