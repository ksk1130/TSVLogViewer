package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * ファイルエクスポート処理を担当するサービスクラス。
 */
public class ExportService {
    
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
     * @param items           テーブルのすべてのアイテム
     * @param selectedRowIndices 選択行のインデックスリスト
     * @param visibleIndices  可視列のインデックスリスト
     * @param outputPath      出力ファイルのパス
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
