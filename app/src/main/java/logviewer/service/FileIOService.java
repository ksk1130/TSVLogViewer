package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ファイルの入出力処理を担当するサービスクラス。
 * ファイル読み込みとエクスポート機能を提供します。
 */
public class FileIOService {

    /**
     * 読み込み対象としてサポートされているファイルかどうか判定します。
     *
     * @param path ファイルパス
     * @return サポート対象なら true
     */
    public boolean isSupportedInputFile(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(ServiceConstants.TSV_EXTENSION) || name.endsWith(ServiceConstants.TXT_EXTENSION);
    }
    
    // ===== ファイル読み込み =====
    
    /**
     * ファイルを非同期で読み込むタスクを生成します。
     * 
     * @param path 読み込むファイルのパス
     * @return ファイル読み込みタスク
     */
    public Task<FileLoadResult> loadFileAsync(Path path) {
        return new Task<>() {
            @Override
            protected FileLoadResult call() throws Exception {
                List<LogRow> rows = new ArrayList<>();
                int columnCount = 0;
                boolean truncated = false;
                
                updateProgress(0, ServiceConstants.MAX_ROWS);
                updateMessage("0 行読み込み中...");

                try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String line;
                    int count = 0;
                    List<LogRow> buffer = new ArrayList<>(ServiceConstants.BATCH_SIZE);

                    while ((line = br.readLine()) != null) {
                        if (count >= ServiceConstants.MAX_ROWS) {
                            truncated = true;
                            break;
                        }

                        String[] parts = line.split(ServiceConstants.TAB_SEPARATOR, -1);
                        if (parts.length > columnCount) {
                            columnCount = parts.length;
                        }
                        buffer.add(new LogRow(parts, count + 1));
                        count++;

                        if (count % 1_000 == 0) {
                            updateProgress(count, ServiceConstants.MAX_ROWS);
                            updateMessage(String.format("%,d 行読み込み中...", count));
                        }

                        if (buffer.size() >= ServiceConstants.BATCH_SIZE) {
                            rows.addAll(buffer);
                            buffer.clear();
                        }
                    }

                    if (!buffer.isEmpty()) {
                        rows.addAll(buffer);
                    }
                    updateProgress(count, ServiceConstants.MAX_ROWS);
                    updateMessage(String.format("読み込み完了 処理中... (%,d 行)", count));
                }

                return new FileLoadResult(rows, columnCount, truncated);
            }
        };
    }
    
    // ===== ファイルエクスポート =====
    
    /**
     * 表示中のデータをTSVファイルにエクスポートするタスクを生成します。
     * 
     * @param data            エクスポート対象のデータ
     * @param visibleIndices  可視列のインデックスリスト
     * @param outputPath      出力ファイルのパス
     * @return エクスポート実行タスク（完了時に処理した行数を返す）
     */
    public Task<Integer> exportDisplayedDataAsync(List<LogRow> data, List<Integer> visibleIndices, Path outputPath) {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                StringBuilder content = new StringBuilder();
                int rowCount = 0;

                for (LogRow row : data) {
                    if (isCancelled()) {
                        break;
                    }

                    boolean first = true;
                    for (int colIndex : visibleIndices) {
                        if (!first) {
                            content.append(ServiceConstants.TAB_SEPARATOR);
                        }
                        first = false;
                        content.append(row.getField(colIndex));
                    }
                    content.append(ServiceConstants.LINE_SEPARATOR);
                    rowCount++;

                    // 1000行ごとに進捗更新
                    if (rowCount % 1000 == 0) {
                        updateProgress(rowCount, data.size());
                    }
                }

                if (!isCancelled()) {
                    Files.writeString(outputPath, content.toString(), StandardCharsets.UTF_8);
                }

                return rowCount;
            }
        };
    }

    /**
     * 選択されている行をTSVファイルにエクスポートするタスクを生成します。
     * 表示中の列のみを対象にします。
     * 
     * @param items              テーブルのすべてのアイテム
     * @param selectedRowIndices 選択行のインデックスリスト
     * @param visibleIndices     可視列のインデックスリスト
     * @param outputPath         出力ファイルのパス
     * @return エクスポート実行タスク（完了時に処理した行数を返す）
     */
    public Task<Integer> exportSelectedRowsAsync(
        List<LogRow> items,
        List<Integer> selectedRowIndices,
        List<Integer> visibleIndices,
        Path outputPath
    ) {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                StringBuilder content = new StringBuilder();
                int processedCount = 0;

                for (int rowIndex : selectedRowIndices) {
                    if (isCancelled()) {
                        break;
                    }

                    if (rowIndex < 0 || rowIndex >= items.size()) {
                        continue;
                    }

                    LogRow row = items.get(rowIndex);
                    boolean first = true;
                    for (int colIndex : visibleIndices) {
                        if (!first) {
                            content.append(ServiceConstants.TAB_SEPARATOR);
                        }
                        first = false;
                        content.append(row.getField(colIndex));
                    }
                    content.append(ServiceConstants.LINE_SEPARATOR);

                    processedCount++;
                    if (processedCount % 500 == 0) {
                        updateProgress(processedCount, selectedRowIndices.size());
                    }
                }

                if (!isCancelled()) {
                    Files.writeString(outputPath, content.toString(), StandardCharsets.UTF_8);
                }

                return processedCount;
            }
        };
    }
}
