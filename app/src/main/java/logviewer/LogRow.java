package logviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * TSVログの1行分を表すクラス。
 */
public class LogRow {
    private final List<String> fields;
    private final int lineNumber;
    /**
     * 1行分のフィールド配列と行番号を保持するLogRowを生成します。
     * @param parts TSVを分割したフィールド配列
     * @param lineNumber 元ファイルでの行番号（1始まり）
     */
    public LogRow(String[] parts, int lineNumber) {
        this.lineNumber = lineNumber;
        fields = new ArrayList<>(parts.length);
        for (String p : parts) fields.add(p);
    }
    /**
     * 指定インデックスのフィールド値を返します。範囲外の場合は空文字を返します。
     * @param index 取得するフィールドのインデックス
     * @return フィールド文字列または空文字
     */
    public String getField(int index) {
        if (index < 0) return "";
        if (index >= fields.size()) return "";
        return fields.get(index);
    }
    /**
     * 行番号を返します。
     * @return 元ファイルの行番号
     */
    public int getLineNumber() {
        return lineNumber;
    }
    /**
     * 保持しているフィールド数を返します。
     * @return フィールド数
     */
    public int fieldCount() {
        return fields.size();
    }
}