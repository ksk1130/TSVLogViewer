package logviewer.service;

import logviewer.LogRow;

import java.util.List;

/**
 * ファイル読み込み結果を保持するクラス。
 */
public class FileLoadResult {
    public final List<LogRow> rows;
    public final int columns;
    public final boolean truncated;
    
    public FileLoadResult(List<LogRow> rows, int columns, boolean truncated) {
        this.rows = rows;
        this.columns = columns;
        this.truncated = truncated;
    }
}
