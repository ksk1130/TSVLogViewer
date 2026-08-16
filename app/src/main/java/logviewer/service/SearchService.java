package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * テーブル検索処理を担当するサービスクラス。
 * 表示中データから一致セルの座標を列挙し、前後移動のための補助を提供します。
 */
public class SearchService {

    /**
     * 一致したセルの座標を表します。
     *
     * @param rowIndex    行インデックス
     * @param columnIndex データカラムのインデックス（Line列は含まない）
     */
    public record CellMatch(int rowIndex, int columnIndex) {
    }

    /**
     * 表示データから検索文字列に一致するセル座標を列挙します。
     * 大文字小文字は区別せず、部分一致で判定します。
     *
     * @param data                対象データ
     * @param searchText          検索文字列
     * @param targetColumnIndices 検索対象カラム（データカラム基準）
     * @return 一致セルの座標リスト
     */
    public List<CellMatch> findMatches(List<LogRow> data, String searchText, List<Integer> targetColumnIndices) {
        if (data == null || data.isEmpty() || searchText == null || searchText.isBlank()) {
            return List.of();
        }

        String query = searchText.toLowerCase(Locale.ROOT);
        List<CellMatch> matches = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            LogRow row = data.get(rowIndex);
            for (int columnIndex : normalizeTargetColumns(row, targetColumnIndices)) {
                String value = row.getField(columnIndex);
                if (value.toLowerCase(Locale.ROOT).contains(query)) {
                    matches.add(new CellMatch(rowIndex, columnIndex));
                }
            }
        }

        return matches;
    }

    /**
     * 一致セルを非同期で検索するタスクを生成します。
     *
     * @param data                対象データ
     * @param searchText          検索文字列
     * @param targetColumnIndices 検索対象カラム（データカラム基準）
     * @return 検索タスク
     */
    public Task<List<CellMatch>> findMatchesAsync(List<LogRow> data, String searchText, List<Integer> targetColumnIndices) {
        return new Task<>() {
            @Override
            protected List<CellMatch> call() {
                if (data == null || data.isEmpty() || searchText == null || searchText.isBlank()) {
                    updateProgress(1, 1);
                    updateMessage("検索準備完了");
                    return List.of();
                }

                String query = searchText.toLowerCase(Locale.ROOT);
                List<CellMatch> matches = new ArrayList<>();
                int totalRows = data.size();

                updateMessage("検索中...");
                for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
                    if (isCancelled()) {
                        break;
                    }

                    LogRow row = data.get(rowIndex);
                    for (int columnIndex : normalizeTargetColumns(row, targetColumnIndices)) {
                        String value = row.getField(columnIndex);
                        if (value.toLowerCase(Locale.ROOT).contains(query)) {
                            matches.add(new CellMatch(rowIndex, columnIndex));
                        }
                    }

                    if (rowIndex % 200 == 0 || rowIndex == totalRows - 1) {
                        updateProgress(rowIndex + 1L, totalRows);
                        updateMessage(String.format("検索中... %,d / %,d 行", rowIndex + 1L, totalRows));
                    }
                }

                updateProgress(1, 1);
                updateMessage(String.format("検索完了: %,d 件ヒット", matches.size()));
                return matches;
            }
        };
    }

    /**
     * 次の一致インデックスを返します。末尾では先頭へ循環します。
     *
     * @param currentIndex 現在の一致インデックス
     * @param matchSize    一致件数
     * @return 次の一致インデックス（一致なしは -1）
     */
    public int nextIndex(int currentIndex, int matchSize) {
        if (matchSize <= 0) {
            return -1;
        }
        if (currentIndex < 0) {
            return 0;
        }
        return (currentIndex + 1) % matchSize;
    }

    /**
     * 前の一致インデックスを返します。先頭では末尾へ循環します。
     *
     * @param currentIndex 現在の一致インデックス
     * @param matchSize    一致件数
     * @return 前の一致インデックス（一致なしは -1）
     */
    public int previousIndex(int currentIndex, int matchSize) {
        if (matchSize <= 0) {
            return -1;
        }
        if (currentIndex < 0) {
            return matchSize - 1;
        }
        return (currentIndex - 1 + matchSize) % matchSize;
    }

    private List<Integer> normalizeTargetColumns(LogRow row, List<Integer> targetColumnIndices) {
        if (targetColumnIndices == null || targetColumnIndices.isEmpty()) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < row.fieldCount(); i++) {
                all.add(i);
            }
            return all;
        }

        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer index : targetColumnIndices) {
            if (index == null) {
                continue;
            }
            if (index >= 0 && index < row.fieldCount()) {
                unique.add(index);
            }
        }
        return new ArrayList<>(unique);
    }
}