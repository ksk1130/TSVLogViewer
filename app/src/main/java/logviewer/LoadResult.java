package logviewer;

/**
 * ログファイルの読み込み結果を表現するクラス。
 * 
 * <p>ログファイルの読み込み時に、ファイルのカラム数と、
 * ログが切り詰められたかどうかの情報を保持します。</p>
 * 
 * @since 1.0
 */
class LoadResult {
    /**
     * ログファイルのカラム数。
     */
    final int columns;
    
    /**
     * ログが切り詰められたかどうかを示すフラグ。
     * <ul>
     *   <li>{@code true} - ログが切り詰められた</li>
     *   <li>{@code false} - ログは切り詰められていない</li>
     * </ul>
     */
    final boolean truncated;
    
    /**
     * 指定されたカラム数と切り詰め状態で LoadResult を構築します。
     * 
     * @param columns カラム数
     * @param truncated ログが切り詰められたかどうか
     */
    LoadResult(int columns, boolean truncated) { 
        this.columns = columns; 
        this.truncated = truncated; 
    }
}