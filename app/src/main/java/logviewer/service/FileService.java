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
 * ファイル読み込み処理を担当するサービスクラス。
 */
public class FileService {
    private static final int MAX_ROWS = 20_000_000;
    private static final int BATCH_SIZE = 5_000;
    
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
                        if (parts.length > columnCount) {
                            columnCount = parts.length;
                        }
                        buffer.add(new LogRow(parts, count + 1));
                        count++;

                        if (count % 1_000 == 0) {
                            updateProgress(count, MAX_ROWS);
                            updateMessage(String.format("%,d 行読み込み中...", count));
                        }

                        if (buffer.size() >= BATCH_SIZE) {
                            rows.addAll(buffer);
                            buffer.clear();
                        }
                    }

                    if (!buffer.isEmpty()) {
                        rows.addAll(buffer);
                    }
                    updateProgress(count, MAX_ROWS);
                    updateMessage(String.format("読み込み完了 処理中... (%,d 行)", count));
                }

                return new FileLoadResult(rows, columnCount, truncated);
            }
        };
    }
}
