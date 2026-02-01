package logviewer.service;

import logviewer.LogRow;

import java.util.List;

/**
 * 行移動などのナビゲーション処理を担当するサービスクラス。
 */
public class NavigationService {
    /**
     * 指定行番号に一致する行のインデックスを返します。
     * 線形探索で最初にマッチした行を返します。
     * 
     * @param data       表示中のデータリスト
     * @param lineNumber 検索する行番号
     * @return 見つかった場合はインデックス、見つからない場合は -1
     */
    public int findRowIndexByLineNumber(List<LogRow> data, int lineNumber) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getLineNumber() == lineNumber) {
                return i;
            }
        }
        return -1;
    }
}
