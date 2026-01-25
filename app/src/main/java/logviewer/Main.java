package logviewer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * TSV形式のログファイルを効率的に閲覧するためのJavaFXアプリケーション。
 * 大容量ファイルにも対応し、フィルタリングやソート、セル選択・コピー機能を備えています。
 */
public class Main extends Application {
    // 定数: 最大行数
    private static final int MAX_ROWS = 20_000_000;
    // 定数: バッチサイズ
    private static final int BATCH_SIZE = 5_000;

    private static final String LINE_SEPARATOR = "\r\n";

    private final List<LogRow> baseData = new ArrayList<>();
    private final ObservableList<LogRow> tableData = FXCollections.observableArrayList();
    private final TableView<LogRow> table = new TableView<>();
    private final AtomicReference<Task<?>> currentTask = new AtomicReference<>();
    private ComboBox<String> columnSelector = new ComboBox<>();
    private TextField filterField = new TextField();
    private TableColumn<LogRow, ?> lineNumberColumn;
    private Label statusLabel;
    private int sortColumnIndex = -1; // -1 は行番号を意味する
    private boolean sortAscending = true;
    private long operationStartTime;
    private boolean skipFilterStatusUpdate = false;

    /**
     * アプリケーションのエントリポイント。メニュー、フィルタ入力、テーブルを構築し、
     * ソートポリシーやコピー動作などのハンドラを束ねて表示します。
     * @param primaryStage メインステージ
     */
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // ファイルを開くメニュー
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("ファイル(_F)");
        MenuItem openItem = new MenuItem("開く...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> openFile(primaryStage));
        
        MenuItem exportDisplayedItem = new MenuItem("表示中のデータをエクスポート...");
        exportDisplayedItem.setOnAction(e -> exportDisplayedData(primaryStage));
        exportDisplayedItem.disableProperty().bind(Bindings.isEmpty(tableData));
        
        MenuItem exportSelectionItem = new MenuItem("選択セルをエクスポート...");
        exportSelectionItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        exportSelectionItem.setOnAction(e -> exportSelectedCells(primaryStage));
        exportSelectionItem.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedCells()));
        
        MenuItem exitItem = new MenuItem("終了");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), exportDisplayedItem, exportSelectionItem, new SeparatorMenuItem(), exitItem);
        
        Menu columnMenu = new Menu("カラム(_C)");
        MenuItem columnVisibilityItem = new MenuItem("カラムの表示/非表示...");
        columnVisibilityItem.setOnAction(e -> showColumnVisibilityDialog());
        columnMenu.getItems().add(columnVisibilityItem);
        
        Menu goMenu = new Menu("移動(_G)");
        MenuItem goToLineItem = new MenuItem("指定行へ移動...");
        goToLineItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));
        goToLineItem.setOnAction(e -> showGoToLineDialog());
        goMenu.getItems().add(goToLineItem);
        
        // 編集メニュー: コピー(Ctrl-C)
        Menu editMenu = new Menu("編集(_E)");
        MenuItem copyItem = new MenuItem("コピー");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> copySelection());
        // 選択がないときは無効化
        copyItem.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedCells()));
        editMenu.getItems().addAll(copyItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, columnMenu, goMenu);

        // 上部コントロール: カラム選択 + フィルタ
        columnSelector.getItems().add("All");
        columnSelector.getSelectionModel().selectFirst();
        columnSelector.setPrefWidth(150);

        filterField.setPromptText("Filter (substring, case-insensitive). Use /regex/ for regex.");
        filterField.setPrefWidth(400);

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> filterField.clear());

        HBox topBox = new HBox(8, new Label("Column:"), columnSelector, new Label("Filter:"), filterField, clearBtn);
        topBox.setPadding(new Insets(8));

        // テーブル設定
        table.setPlaceholder(new Label("ログファイルを開いてください (ファイル -> Openまたは、ログファイルをドラッグ＆ドロップ)"));
        table.getSelectionModel().setCellSelectionEnabled(true); // セル単位の選択を有効化
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // コンテキストメニュー: セルのコピー
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

        // データバインディング
        table.setItems(tableData);
        table.setSortPolicy(tv -> {
            if (tv.getSortOrder().isEmpty()) {
                sortColumnIndex = -1;
                sortAscending = true;
            } else {
                TableColumn<LogRow, ?> sortCol = tv.getSortOrder().get(0);
                sortAscending = sortCol.getSortType() != TableColumn.SortType.DESCENDING;
                if (sortCol == lineNumberColumn) {
                    sortColumnIndex = -1;
                } else {
                    Object ud = sortCol.getUserData();
                    sortColumnIndex = (ud instanceof Integer) ? (Integer) ud : -1;
                }
            }
            refreshAsync();
            return true;
        });

        // フィルタ/ソートの変更時に更新
        filterField.textProperty().addListener((obs, oldVal, newVal) -> refreshAsync());
        columnSelector.valueProperty().addListener((obs, oldVal, newVal) -> refreshAsync());

        // Ctrl+C コピー処理を設定
        table.setOnKeyPressed(event -> {
            KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
            if (ctrlC.match(event)) {
                copySelection();
                event.consume();
            }
        });

        // ドラッグ＆ドロップの設定
        setupDragAndDrop(table, primaryStage);

        root.setTop(new VBox(menuBar, topBox));
        root.setCenter(table);

        // ステータスバー
        statusLabel = new Label("準備完了");
        HBox statusBar = new HBox(10, statusLabel);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("TSV Log Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * テーブルにドラッグ＆ドロップ機能を設定します。
     * TSV/TXTファイルがドロップされた際に自動的に開きます。
     * @param table ドラッグ＆ドロップを有効にするテーブル
     * @param stage 親ステージ
     */
    private void setupDragAndDrop(TableView<LogRow> table, Stage stage) {
        // ドラッグオーバー時の処理
        table.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                // ファイルがドラッグされている場合、コピーモードを受け入れる
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // ドラッグエンター時の処理（視覚的フィードバック）
        table.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                table.setOpacity(0.7);
            }
            event.consume();
        });

        // ドラッグ退出時の処理
        table.setOnDragExited(event -> {
            table.setOpacity(1.0);
            event.consume();
        });

        // ドロップ時の処理
        table.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (!files.isEmpty()) {
                    File file = files.get(0); // 最初のファイルを開く
                    String fileName = file.getName().toLowerCase(Locale.ROOT);
                    // TSV/TXTファイルのみを受け入れる
                    if (fileName.endsWith(".tsv") || fileName.endsWith(".txt")) {
                        streamLoadFile(file.toPath(), stage);
                        success = true;
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("サポートされていないファイル形式");
                        alert.setHeaderText(null);
                        alert.setContentText("TSV または TXT ファイルのみをドロップできます。");
                        alert.showAndWait();
                    }
                }
            }
            table.setOpacity(1.0);
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * ファイル選択ダイアログを開き、選択されたTSV/TXTファイルの読み込みを開始します。
     * @param stage ダイアログを表示するステージ
     */
    private void openFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("ログファイルを開く");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV / Text files", "*.tsv", "*.txt", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f != null) {
            streamLoadFile(f.toPath(), stage);
        }
    }

    /**
     * 選択されたファイルをバックグラウンドでストリーミング読み込みし、ベースデータに蓄積します。
     * 既存のフィルタ/ソート計算タスクはキャンセルし、UIを初期化してから読み込みを開始します。
     * 行数がMAX_ROWSに達したら読み込みを打ち切り、完了後にカラムを再構築します。
     * @param path 読み込むファイルのパス
     */
    private void streamLoadFile(Path path, Stage stage) {
        Task<?> previous = currentTask.getAndSet(null);
        // 以前のタスクがあればキャンセル
        if (previous != null) {
            previous.cancel();
        }

        // FXスレッド上でUI状態を初期化
        baseData.clear();
        tableData.clear();
        table.getColumns().clear();
        columnSelector.getItems().setAll("All");
        columnSelector.getSelectionModel().selectFirst();
        table.setPlaceholder(new Label("Loading..."));
        updateStatus("ファイルを読み込み中...");
        operationStartTime = System.nanoTime();

        // 非同期でファイル読み込み
        Task<LoadResult> task = new Task<>() {
            @Override
            protected LoadResult call() throws Exception {
                int localColumnCount = 0;
                boolean truncated = false;
                updateProgress(0, MAX_ROWS);
                updateMessage("0 行読み込み中...");

                try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String line;
                    int count = 0;
                    List<LogRow> buffer = new ArrayList<>(BATCH_SIZE);

                    while ((line = br.readLine()) != null) {
                        if (count >= MAX_ROWS) {
                            truncated = true;
                            break;
                        }

                        String[] parts = line.split("\t", -1);
                        if (parts.length > localColumnCount) localColumnCount = parts.length;
                        buffer.add(new LogRow(parts, count + 1));
                        count++;

                        if (count % 1_000 == 0) {
                            updateProgress(count, MAX_ROWS);
                            updateMessage(String.format("%,d 行読み込み中...", count));
                        }

                        if (buffer.size() >= BATCH_SIZE) {
                            baseData.addAll(buffer);
                            buffer.clear();
                        }
                    }

                    if (!buffer.isEmpty()) {
                        baseData.addAll(buffer);
                    }
                    updateProgress(count, MAX_ROWS);
                    updateMessage(String.format("読み込み完了 処理中... (%,d 行)", count));
                }

                return new LoadResult(localColumnCount, truncated);
            }
        };

        // 読み込み成功時はTaskのハンドラでUI更新（JavaFXスレッド上で実行される）
        task.setOnSucceeded(ev -> {
            LoadResult res = task.getValue();
            finalizeLoad(res.columns, res.truncated);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Alert a = new Alert(Alert.AlertType.ERROR, "ファイルの読み込みに失敗しました: " + (ex == null ? "不明なエラー" : ex.getMessage()), ButtonType.OK);
            a.showAndWait();
            table.setPlaceholder(new Label("TSVログファイルを開いてください (ファイル -> 開く...)"));
            updateStatus("ファイル読み込みに失敗しました");
        });

        // 進捗ダイアログを表示
        showProgressDialog(task, "ファイル読み込み中...", stage);

        Thread t = new Thread(task, "log-load-thread");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 読み込み完了後にカラムを再構築し、列選択リストを更新して初回のフィルタ/ソートを実行します。
     * 行数が上限で打ち切られた場合は情報ダイアログで通知します。
     * @param columns 検出された列数
     * @param truncated 行数上限で打ち切られた場合は true
     */
    private void finalizeLoad(int columns, boolean truncated) {
        // columnSelectorの設定でリスナーが発火してrefreshAsyncが呼ばれる可能性があるため、
        // 最初にフラグを設定してoperationStartTimeが上書きされないようにする
        skipFilterStatusUpdate = true;
        
        if (columns == 0) {
            table.setPlaceholder(new Label("No data"));
            return;
        }

        rebuildColumns(columns);

        List<String> cols = new ArrayList<>();
        cols.add("All");
        for (int i = 0; i < columns; i++) {
            cols.add("Column " + i);
        }
        columnSelector.getItems().setAll(cols);
        columnSelector.getSelectionModel().selectFirst();

        table.setPlaceholder(new Label("No rows"));

        long elapsedMillis = (System.nanoTime() - operationStartTime) / 1_000_000;
        double elapsedSeconds = elapsedMillis / 1000.0;
        updateStatus(String.format("ファイル読み込みが完了しました。読み込み行数 %,d 行、処理時間 %.2f 秒", baseData.size(), elapsedSeconds));

        // 読み込んだデータをテーブルに直接設定（初期状態は行番号順なのでソート不要）
        tableData.setAll(baseData);

        if (truncated) {
            String message = String.format("メモリ使用量を抑えるため、ファイルは %,d 行で打ち切られました。", MAX_ROWS);
            Alert info = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            info.setHeaderText(null);
            info.showAndWait();
        }
    }

    /**
     * 行番号列とデータ列を動的に生成し、列ごとにソート用インデックスやセルコピー動作を付与します。
     * @param columns 生成するデータ列数
     */
    private void rebuildColumns(int columns) {
        table.getColumns().clear();

        // 行番号カラム（データモデルには含まれない）
        TableColumn<LogRow, Number> lineCol = new TableColumn<>("Line");
        lineCol.setReorderable(false);
        lineCol.setPrefWidth(81);
        lineCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().getLineNumber()));
        lineCol.setSortable(false); // ボタン操作でのみソート

        Label lineHeaderLabel = new Label(lineCol.getText());
        Button lineSortAscButton = new Button("▲");
        Button lineSortDescButton = new Button("▼");
        lineSortAscButton.setFocusTraversable(false);
        lineSortDescButton.setFocusTraversable(false);
        lineSortAscButton.setPadding(new Insets(0, 4, 0, 4));
        lineSortDescButton.setPadding(new Insets(0, 4, 0, 4));
        lineSortAscButton.setOnAction(e -> {
            table.getSortOrder().setAll(lineCol);
            lineCol.setSortType(TableColumn.SortType.ASCENDING);
            table.sort();
        });
        lineSortDescButton.setOnAction(e -> {
            table.getSortOrder().setAll(lineCol);
            lineCol.setSortType(TableColumn.SortType.DESCENDING);
            table.sort();
        });
        Region lineSpacer = new Region();
        HBox.setHgrow(lineSpacer, Priority.ALWAYS);
        HBox lineHeaderBox = new HBox(4, lineHeaderLabel, lineSpacer, lineSortAscButton, lineSortDescButton);
        lineHeaderBox.setAlignment(Pos.CENTER_RIGHT);
        lineCol.setText(null);
        lineCol.setGraphic(lineHeaderBox);

        lineCol.setCellFactory(col -> new TableCell<>() {
            {
                setStyle("-fx-alignment: CENTER-RIGHT;");
                addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, evt -> {
                    if (!isEmpty() && getTableRow() != null) {
                        int rowIndex = getIndex();
                        if (rowIndex >= 0 && rowIndex < table.getItems().size()) {
                            // イベントの状態を事前にキャプチャ
                            boolean ctrlDown = evt.isControlDown();
                            boolean shiftDown = evt.isShiftDown();
                            int currentFocusedRow = table.getFocusModel().getFocusedIndex();
                            
                            TableView.TableViewSelectionModel<LogRow> sm = table.getSelectionModel();
                            List<TableColumn<LogRow, ?>> columns = table.getColumns();
                            
                            if (columns.isEmpty()) return;
                            
                            // Ctrlキー押下時: トグル選択
                            if (ctrlDown) {
                                // 現在の選択を保存
                                @SuppressWarnings("unchecked")
                                ObservableList<TablePosition<LogRow, ?>> selectedCells = 
                                    (ObservableList<TablePosition<LogRow, ?>>) (ObservableList<?>) sm.getSelectedCells();
                                List<TablePosition<LogRow, ?>> savedSelection = new ArrayList<>(selectedCells);
                                
                                // この行が選択されているかチェック
                                boolean alreadySelected = savedSelection.stream()
                                    .anyMatch(pos -> pos.getRow() == rowIndex);
                                
                                sm.clearSelection();
                                
                                if (alreadySelected) {
                                    // 選択解除: この行以外を再選択
                                    for (TablePosition<LogRow, ?> pos : savedSelection) {
                                        if (pos.getRow() != rowIndex) {
                                            sm.select(pos.getRow(), pos.getTableColumn());
                                        }
                                    }
                                } else {
                                    // 既存の選択を再適用
                                    for (TablePosition<LogRow, ?> pos : savedSelection) {
                                        sm.select(pos.getRow(), pos.getTableColumn());
                                    }
                                    // この行の全セルを追加
                                    for (TableColumn<LogRow, ?> column : columns) {
                                        sm.select(rowIndex, column);
                                    }
                                }
                                table.getFocusModel().focus(rowIndex, lineCol);
                                evt.consume();
                            }
                            // Shiftキー押下時: 範囲選択
                            else if (shiftDown) {
                                if (currentFocusedRow >= 0 && currentFocusedRow < table.getItems().size()) {
                                    int startRow = Math.min(currentFocusedRow, rowIndex);
                                    int endRow = Math.max(currentFocusedRow, rowIndex);
                                    
                                    sm.clearSelection();
                                    for (int r = startRow; r <= endRow; r++) {
                                        for (TableColumn<LogRow, ?> column : columns) {
                                            sm.select(r, column);
                                        }
                                    }
                                } else {
                                    sm.clearSelection();
                                    for (TableColumn<LogRow, ?> column : columns) {
                                        sm.select(rowIndex, column);
                                    }
                                }
                                table.getFocusModel().focus(rowIndex, lineCol);
                                evt.consume();
                            }
                            // 修飾キーなし: 通常の単一行選択
                            else {
                                sm.clearSelection();
                                for (TableColumn<LogRow, ?> column : columns) {
                                    sm.select(rowIndex, column);
                                }
                                table.getFocusModel().focus(rowIndex, lineCol);
                                evt.consume();
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.format(Locale.US, "%,d", item.intValue()));
            }
        });
        table.getColumns().add(lineCol);
        lineNumberColumn = lineCol;

        for (int i = 0; i < columns; i++) {
            final int colIndex = i;
            TableColumn<LogRow, String> col = new TableColumn<>("Col " + i);
            col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getField(colIndex)));
            col.setPrefWidth(150);
            col.setComparator((a, b) -> a.compareToIgnoreCase(b));
            col.setUserData(colIndex); // カスタムソート用にインデックスを保持
            col.setSortable(false); // ヘッダ全体のクリックでソートさせず、ボタン操作に限定

            Label headerLabel = new Label(col.getText());
            Button sortAscButton = new Button("▲");
            Button sortDescButton = new Button("▼");
            sortAscButton.setFocusTraversable(false);
            sortDescButton.setFocusTraversable(false);
            sortAscButton.setPadding(new Insets(0, 4, 0, 4));
            sortDescButton.setPadding(new Insets(0, 4, 0, 4));
            sortAscButton.setOnAction(e -> {
                table.getSortOrder().setAll(col);
                col.setSortType(TableColumn.SortType.ASCENDING);
                table.sort();
            });
            sortDescButton.setOnAction(e -> {
                table.getSortOrder().setAll(col);
                col.setSortType(TableColumn.SortType.DESCENDING);
                table.sort();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox headerBox = new HBox(4, headerLabel, spacer, sortAscButton, sortDescButton);
            headerBox.setAlignment(Pos.CENTER_RIGHT);
            col.setText(null);
            col.setGraphic(headerBox);
            
            // カラムヘッダ用のコンテキストメニュー
            ContextMenu headerMenu = new ContextMenu();
            MenuItem hideItem = new MenuItem("このカラムを非表示");
            hideItem.setOnAction(e -> col.setVisible(false));
            headerMenu.getItems().add(hideItem);
            col.setContextMenu(headerMenu);
            
            // カラム値コピー用のコンテキストメニュー
            col.setCellFactory(tc -> {
                TableCell<LogRow, String> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item);
                    }
                };
                cell.setOnMouseClicked(ev -> {
                    if (!cell.isEmpty() && ev.isControlDown()) {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(cell.getItem());
                        Clipboard.getSystemClipboard().setContent(content);
                    }
                });
                return cell;
            });
            table.getColumns().add(col);
        }
    }

    /**
     * フィルタとソートをバックグラウンドで計算し、完了後にテーブルへ反映します。
     * 直前の計算タスクが残っていればキャンセルし、最新の入力・ソート条件で再計算します。
     */
    private void refreshAsync() {
        List<LogRow> snapshot = new ArrayList<>(baseData);
        String filterText = filterField.getText();
        String selectedColumn = columnSelector.getValue();
        int targetSortIndex = sortColumnIndex;
        boolean ascending = sortAscending;
        // ファイル読み込み直後の初回フィルタでは時刻を上書きしない
        if (!skipFilterStatusUpdate) {
            operationStartTime = System.nanoTime();
        }

        Task<List<LogRow>> task = new Task<>() {
            @Override
            protected List<LogRow> call() {
                Predicate<LogRow> predicate = buildPredicate(filterText, selectedColumn);
                Comparator<LogRow> comparator = buildComparator(targetSortIndex, ascending);

                List<LogRow> result = new ArrayList<>();
                for (LogRow row : snapshot) {
                    if (isCancelled()) break;
                    if (predicate.test(row)) {
                        result.add(row);
                    }
                }
                if (!isCancelled()) {
                    result.sort(comparator);
                }
                return result;
            }
        };

        task.setOnSucceeded(evt -> {
            List<LogRow> result = task.getValue();
            long elapsedMillis = (System.nanoTime() - operationStartTime) / 1_000_000;
            double elapsedSeconds = elapsedMillis / 1000.0;
            // ファイル読み込み直後のリスナー発火ではステータス更新をスキップ
            if (!skipFilterStatusUpdate && !baseData.isEmpty()) {
                updateStatus(String.format("フィルタ/ソートが完了しました。結果 %,d 行、処理時間 %.2f 秒", result.size(), elapsedSeconds));
            }
            skipFilterStatusUpdate = false;
            tableData.setAll(result);
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
     * ステータスバーのメッセージを更新します。
     * @param message 表示するメッセージ
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * カラムの表示/非表示を切り替えるダイアログを表示します。
     */
    private void showColumnVisibilityDialog() {
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

        for (TableColumn<LogRow, ?> column : table.getColumns()) {
            // 行番号カラムは非表示にできない
            if (column == lineNumberColumn) {
                continue;
            }
            
            CheckBox checkBox = new CheckBox(column.getText());
            checkBox.setSelected(column.isVisible());
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                column.setVisible(newVal);
            });
            content.getChildren().add(checkBox);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * 現在のフィルタ文字列と列指定から述語を構築します。/regex/ 形式は正規表現として評価し、
     * 無効な正規表現はフィルタなしとして扱います。"All" 指定時は全カラムを走査します。
     * @param text フィルタ入力値
     * @param selected 選択中のカラム名（"All" または "Column N"）
     * @return フィルタ用Predicate
     */
    private Predicate<LogRow> buildPredicate(String text, String selected) {
        if (selected == null) {
            selected = "All";
        }
        if (text == null || text.isBlank()) {
            return r -> true;
        }

        String trimmed = text.trim();
        boolean isRegex = trimmed.length() >= 2 && trimmed.startsWith("/") && trimmed.endsWith("/");

        if (isRegex) {
            String patternText = trimmed.substring(1, trimmed.length() - 1);
            Pattern pattern;
            try {
                pattern = Pattern.compile(patternText);
            } catch (PatternSyntaxException e) {
                return r -> true; // 無効な正規表現は素通り扱い
            }

            if ("All".equals(selected)) {
                return r -> {
                    for (int i = 0; i < r.fieldCount(); i++) {
                        if (pattern.matcher(r.getField(i)).matches()) return true;
                    }
                    return false;
                };
            } else {
                int idx = columnSelector.getSelectionModel().getSelectedIndex() - 1; // "All" が0のためインデックスを1つ引く
                return r -> {
                    if (idx < 0 || idx >= r.fieldCount()) return false;
                    return pattern.matcher(r.getField(idx)).matches();
                };
            }
        } else {
            String q = trimmed.toLowerCase(Locale.ROOT);
            if ("All".equals(selected)) {
                return r -> {
                    for (int i = 0; i < r.fieldCount(); i++) {
                        String v = r.getField(i);
                        if (v.toLowerCase(Locale.ROOT).contains(q)) return true;
                    }
                    return false;
                };
            } else {
                int idx = columnSelector.getSelectionModel().getSelectedIndex() - 1;
                return r -> {
                    if (idx < 0 || idx >= r.fieldCount()) return false;
                    return r.getField(idx).toLowerCase(Locale.ROOT).contains(q);
                };
            }
        }
    }

    /**
     * ソート対象カラムと昇順/降順指定からComparatorを組み立てます。
     * 行番号ソート時は整数比較、データ列ソート時は小文字化した文字列比較を行います。
     * @param columnIndex ソート対象カラムインデックス（-1で行番号）
     * @param ascending 昇順ならtrue、降順ならfalse
     * @return ロウ比較用Comparator
     */
    private Comparator<LogRow> buildComparator(int columnIndex, boolean ascending) {
        Comparator<LogRow> comparator;
        if (columnIndex < 0) {
            comparator = Comparator.comparingInt(LogRow::getLineNumber);
        } else {
            comparator = Comparator.comparing(r -> r.getField(columnIndex).toLowerCase(Locale.ROOT));
        }
        return ascending ? comparator : comparator.reversed();
    }

    /**
     * 選択中のセルまたは行をクリップボードへコピーします。
     * 1行の全カラムが選ばれている場合はタブ区切りで行全体をコピーし、
     * 部分選択の場合は選択セルを行順に改行区切りでコピーします。
     */
    private void copySelection() {
        TableView.TableViewSelectionModel<LogRow> sm = table.getSelectionModel();
        @SuppressWarnings("unchecked")
        ObservableList<TablePosition<LogRow, ?>> selectedCells = (ObservableList<TablePosition<LogRow, ?>>) (ObservableList<?>) sm.getSelectedCells();
        
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
                        if (i > 0) clipboardString.append("\t");
                        clipboardString.append(logRow.getField(i));
                    }
                    clipboardString.append(LINE_SEPARATOR);
                }
            }
        } else {
            // 単一セルまたは部分選択の場合 - 選択セルをコピー
            for (TablePosition<LogRow, ?> pos : selectedCells) {
                if (currentRow != -1 && currentRow != pos.getRow()) {
                    clipboardString.append(LINE_SEPARATOR);
                }
                currentRow = pos.getRow();
                
                TableColumn<LogRow, ?> column = pos.getTableColumn();
                Object cellValue = column.getCellData(pos.getRow());
                
                if (cellValue != null) {
                    clipboardString.append(cellValue.toString());
                }
            }
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(clipboardString.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * 指定行へ移動するダイアログを表示します。
     * ユーザーが入力した行番号が表示中のデータ（フィルタ適用後）に存在する場合、
     * その行を選択してスクロールします。
     */
    private void showGoToLineDialog() {
        if (tableData.isEmpty()) {
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
                int foundIndex = -1;
                for (int i = 0; i < tableData.size(); i++) {
                    if (tableData.get(i).getLineNumber() == lineNumber) {
                        foundIndex = i;
                        break;
                    }
                }

                if (foundIndex >= 0) {
                    // 該当行を選択してスクロール
                    table.getSelectionModel().clearSelection();
                    table.getSelectionModel().select(foundIndex);
                    table.scrollTo(foundIndex);
                    table.requestFocus();
                } else {
                    // 該当行が見つからない場合、フィルタで除外されているか存在しない
                    if (lineNumber <= baseData.size()) {
                        showAlert("情報", String.format("行番号 %d は現在のフィルタ条件で非表示になっています。", lineNumber));
                    } else {
                        showAlert("情報", String.format("行番号 %d は存在しません。%s（最大行番号: %d）", lineNumber, LINE_SEPARATOR, baseData.size()));
                    }
                }
            } catch (NumberFormatException ex) {
                showAlert("エラー", "有効な整数を入力してください。");
            }
        });
    }

    /**
     * 現在表示されているデータをTSVファイルにエクスポートします。
     * フィルタ/ソート適用後のデータが対象です。
     * @param stage ファイル保存ダイアログを表示するステージ
     */
    private void exportDisplayedData(Stage stage) {
        if (tableData.isEmpty()) {
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
            List<LogRow> dataSnapshot = new ArrayList<>(tableData);
            int columnCount = table.getColumns().size() - 1; // Line列を除く
            Path outputPath = file.toPath();

            // バックグラウンドタスクで処理
            Task<Integer> task = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    StringBuilder content = new StringBuilder();
                    int rowCount = 0;

                    for (LogRow row : dataSnapshot) {
                        if (isCancelled()) break;
                        
                        for (int i = 0; i < row.fieldCount() && i < columnCount; i++) {
                            if (i > 0) content.append("\t");
                            content.append(row.getField(i));
                        }
                        content.append(LINE_SEPARATOR);
                        rowCount++;

                        // 1000行ごとに進捗更新
                        if (rowCount % 1000 == 0) {
                            updateProgress(rowCount, dataSnapshot.size());
                        }
                    }

                    if (!isCancelled()) {
                        Files.writeString(outputPath, content.toString(), StandardCharsets.UTF_8);
                    }
                    
                    return rowCount;
                }
            };

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
            showProgressDialog(task, "エクスポート中...", stage);

            Thread t = new Thread(task, "export-data-thread");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 選択されているセルをTSVファイルにエクスポートします。
     * 行全体が選択されている場合はタブ区切り、部分選択の場合は選択セルのみをエクスポートします。
     * @param stage ファイル保存ダイアログを表示するステージ
     */
    private void exportSelectedCells(Stage stage) {
        TableView.TableViewSelectionModel<LogRow> sm = table.getSelectionModel();
        @SuppressWarnings("unchecked")
        ObservableList<TablePosition<LogRow, ?>> selectedCells = 
            (ObservableList<TablePosition<LogRow, ?>>) (ObservableList<?>) sm.getSelectedCells();

        if (selectedCells.isEmpty()) {
            showAlert("情報", "エクスポートするセルが選択されていません。");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("選択セルをエクスポート");
        chooser.setInitialFileName("selected_cells.tsv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV files", "*.tsv"));
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            // 選択セルのスナップショットを取得（UIスレッドで）
            List<TablePosition<LogRow, ?>> cellsSnapshot = new ArrayList<>(selectedCells);
            List<TableColumn<LogRow, ?>> columnsSnapshot = new ArrayList<>(table.getColumns());
            List<LogRow> itemsSnapshot = new ArrayList<>(table.getItems());
            Path outputPath = file.toPath();

            // バックグラウンドタスクで処理
            Task<Integer> task = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    StringBuilder content = new StringBuilder();
                    int currentRow = -1;
                    boolean isFullRowSelection = false;
                    
                    // 行全体が選択されているか確認
                    if (cellsSnapshot.size() >= columnsSnapshot.size()) {
                        int firstRow = cellsSnapshot.get(0).getRow();
                        boolean allColumnsSelected = true;
                        for (TableColumn<LogRow, ?> col : columnsSnapshot) {
                            boolean found = false;
                            for (TablePosition<LogRow, ?> pos : cellsSnapshot) {
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
                        // 行全体が選択されている場合 - タブ区切りで行全体をエクスポート
                        List<Integer> processedRows = new ArrayList<>();
                        int processedCount = 0;
                        
                        for (TablePosition<LogRow, ?> pos : cellsSnapshot) {
                            if (isCancelled()) break;
                            
                            int row = pos.getRow();
                            if (!processedRows.contains(row)) {
                                processedRows.add(row);
                                LogRow logRow = itemsSnapshot.get(row);
                                for (int i = 0; i < logRow.fieldCount(); i++) {
                                    if (i > 0) content.append("\t");
                                    content.append(logRow.getField(i));
                                }
                                content.append(LINE_SEPARATOR);
                                
                                processedCount++;
                                if (processedCount % 100 == 0) {
                                    updateProgress(processedCount, cellsSnapshot.size() / columnsSnapshot.size());
                                }
                            }
                        }
                    } else {
                        // 単一セルまたは部分選択の場合 - 選択セルをエクスポート
                        int processedCount = 0;
                        for (TablePosition<LogRow, ?> pos : cellsSnapshot) {
                            if (isCancelled()) break;
                            
                            if (currentRow != -1 && currentRow != pos.getRow()) {
                                content.append(LINE_SEPARATOR);
                            }
                            currentRow = pos.getRow();
                            
                            TableColumn<LogRow, ?> column = pos.getTableColumn();
                            Object cellValue = column.getCellData(pos.getRow());
                            
                            if (cellValue != null) {
                                content.append(cellValue.toString());
                            }
                            
                            processedCount++;
                            if (processedCount % 1000 == 0) {
                                updateProgress(processedCount, cellsSnapshot.size());
                            }
                        }
                        content.append(LINE_SEPARATOR);
                    }

                    if (!isCancelled()) {
                        Files.writeString(outputPath, content.toString(), StandardCharsets.UTF_8);
                    }
                    
                    return cellsSnapshot.size();
                }
            };

            task.setOnSucceeded(evt -> {
                int cellCount = task.getValue();
                showAlert("成功", String.format("選択セル (%d セル) をエクスポートしました。", cellCount));
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
            showProgressDialog(task, "選択セルをエクスポート中...", stage);

            Thread t = new Thread(task, "export-selection-thread");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * タスクの進捗を表示するダイアログを表示します。
     * @param task 実行中のタスク
     * @param title ダイアログのタイトル
     * @param owner 親ウィンドウ
     */
    private void showProgressDialog(Task<?> task, String title, Stage owner) {
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
        progressStage.initOwner(owner);
        progressStage.setScene(new Scene(content));
        progressStage.setResizable(false);
        progressStage.setOnCloseRequest(evt -> {
            if (task.isRunning()) {
                task.cancel();
            }
        });

        // タスク完了時にダイアログを閉じる（既存ハンドラを保持するためaddEventHandlerを使用）
        task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_SUCCEEDED, evt -> progressStage.close());
        task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_FAILED, evt -> progressStage.close());
        task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_CANCELLED, evt -> progressStage.close());

        progressStage.show();
    }

    /**
     * アラートダイアログを表示します。
     * @param title ダイアログのタイトル
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
     * @param args 起動引数
     */
    public static void main(String[] args) {
        launch(args);
    }
}