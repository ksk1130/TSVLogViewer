package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * ソート処理を担当するサービスクラス。
 * Comparator を構築してデータをソートします。
 */
public class SortService {
    
    /**
     * ソート用のComparatorを構築します。
     * 行番号ソート時は整数比較、データ列ソート時は小文字化した文字列比較を行います。
     * 
     * @param columnIndex ソート対象カラムインデックス（-1で行番号）
     * @param ascending   昇順ならtrue、降順ならfalse
     * @return ロウ比較用Comparator
     */
    public Comparator<LogRow> buildComparator(int columnIndex, boolean ascending) {
        Comparator<LogRow> comparator;
        if (columnIndex < 0) {
            comparator = Comparator.comparingInt(LogRow::getLineNumber);
        } else {
            comparator = Comparator.comparing(r -> r.getField(columnIndex).toLowerCase(Locale.ROOT));
        }
        return ascending ? comparator : comparator.reversed();
    }
    
    /**
     * データをソートするタスクを生成します。
     * キャンセル可能です。
     * 
     * @param data       対象データリスト
     * @param comparator ソート用Comparator
     * @return ソート実行タスク (完了時にソート済みリストを返す)
     */
    public Task<List<LogRow>> sortAsync(List<LogRow> data, Comparator<LogRow> comparator) {
        return new Task<>() {
            @Override
            protected List<LogRow> call() {
                List<LogRow> result = new ArrayList<>(data);
                
                if (!isCancelled()) {
                    result.sort(comparator);
                }
                
                return result;
            }
        };
    }
}
