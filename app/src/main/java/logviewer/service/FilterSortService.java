package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * フィルタとソートの連結処理を担当するサービスクラス。
 * Predicate でフィルタリングした後、Comparator でソートします。
 */
public class FilterSortService {
    /**
     * フィルタとソートを順番に実行するタスクを生成します。
     * キャンセル可能です。
     * 
     * @param data       対象データリスト
     * @param predicate  フィルタ用 Predicate (各行が条件を満たすかをテスト)
     * @param comparator ソート用 Comparator (行の並び順を指定)
     * @return フィルタ・ソート実行タスク (完了時にソート済みの結果リストを返す)
     */
    public Task<List<LogRow>> filterAndSortAsync(List<LogRow> data, Predicate<LogRow> predicate, Comparator<LogRow> comparator) {
        return new Task<>() {
            @Override
            protected List<LogRow> call() {
                List<LogRow> filtered = new java.util.ArrayList<>();
                for (LogRow row : data) {
                    if (isCancelled()) {
                        break;
                    }
                    if (predicate.test(row)) {
                        filtered.add(row);
                    }
                }

                if (!isCancelled()) {
                    filtered.sort(comparator);
                }
                return filtered;
            }
        };
    }
}
