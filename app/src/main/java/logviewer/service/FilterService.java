package logviewer.service;

import javafx.concurrent.Task;
import logviewer.LogRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * フィルタリング処理を担当するサービスクラス。
 * 単一フィルタ条件、複数フィルタ条件の結合、複数の結果をタスクで非同期実行できます。
 */
public class FilterService {
    
    /**
     * フィルタ文字列とカラム指定から述語を構築します。
     * /regex/ 形式は正規表現として評価し、無効な正規表現はフィルタなしとして扱います。
     * 
     * @param filterText  フィルタ入力値
     * @param columnName  選択中のカラム名（"All" または "Column N"）
     * @param columnIndex カラムのインデックス（-1で"All"を意味する）
     * @return フィルタ用Predicate
     */
    public Predicate<LogRow> buildPredicate(String filterText, String columnName, int columnIndex) {
        if (columnName == null) {
            columnName = "All";
        }
        if (filterText == null || filterText.isBlank()) {
            return r -> true;
        }

        String trimmed = filterText.trim();
        boolean isRegex = trimmed.length() >= 2 && trimmed.startsWith("/") && trimmed.endsWith("/");

        if (isRegex) {
            String patternText = trimmed.substring(1, trimmed.length() - 1);
            Pattern pattern;
            try {
                pattern = Pattern.compile(patternText);
            } catch (PatternSyntaxException e) {
                return r -> true; // 無効な正規表現は素通り扱い
            }

            if ("All".equals(columnName)) {
                return r -> {
                    for (int i = 0; i < r.fieldCount(); i++) {
                        if (pattern.matcher(r.getField(i)).matches()) {
                            return true;
                        }
                    }
                    return false;
                };
            } else {
                return r -> {
                    if (columnIndex < 0 || columnIndex >= r.fieldCount()) {
                        return false;
                    }
                    return pattern.matcher(r.getField(columnIndex)).matches();
                };
            }
        } else {
            String q = trimmed.toLowerCase(Locale.ROOT);
            if ("All".equals(columnName)) {
                return r -> {
                    for (int i = 0; i < r.fieldCount(); i++) {
                        String v = r.getField(i);
                        if (v.toLowerCase(Locale.ROOT).contains(q)) {
                            return true;
                        }
                    }
                    return false;
                };
            } else {
                return r -> {
                    if (columnIndex < 0 || columnIndex >= r.fieldCount()) {
                        return false;
                    }
                    return r.getField(columnIndex).toLowerCase(Locale.ROOT).contains(q);
                };
            }
        }
    }
    
    /**
     * 複数のPredicateを結合します（AND条件）。
     * すべての Predicate を満たす行のみが true を返します。
     * 
     * @param predicates 複数のPredicate
     * @return 結合されたPredicate (すべての条件を満たしたら true)
     */
    public Predicate<LogRow> combinePredicates(List<Predicate<LogRow>> predicates) {
        if (predicates.isEmpty()) {
            return r -> true;
        }
        
        return r -> {
            for (Predicate<LogRow> p : predicates) {
                if (!p.test(r)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    /**
     * フィルタリングを非同期で実行するタスクを生成します。
     * キャンセル可能です。
     * 
     * @param data      対象データリスト
     * @param predicate フィルタ用Predicate
     * @return フィルタ実行タスク (完了時にフィルタ済みリストを返す)
     */
    public Task<List<LogRow>> applyFilterAsync(List<LogRow> data, Predicate<LogRow> predicate) {
        return new Task<>() {
            @Override
            protected List<LogRow> call() {
                List<LogRow> result = new ArrayList<>();
                
                for (LogRow row : data) {
                    if (isCancelled()) {
                        break;
                    }
                    if (predicate.test(row)) {
                        result.add(row);
                    }
                }
                
                return result;
            }
        };
    }
}
