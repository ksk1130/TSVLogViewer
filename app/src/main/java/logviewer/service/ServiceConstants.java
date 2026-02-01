package logviewer.service;

/**
 * サービス層で使用する共通定数を集約したクラス。
 */
public final class ServiceConstants {

    // ===== ファイル処理関連 =====
    
    /** ファイル読み込み時の最大行数 */
    public static final int MAX_ROWS = 20_000_000;
    
    /** バッファサイズ（一度に処理する行数） */
    public static final int BATCH_SIZE = 5_000;
    
    /** 改行コード（CRLF） */
    public static final String LINE_SEPARATOR = "\r\n";
    
    /** タブ区切り文字 */
    public static final String TAB_SEPARATOR = "\t";
    
    // ===== サポートされるファイル形式 =====
    
    /** TSVファイル拡張子 */
    public static final String TSV_EXTENSION = ".tsv";
    
    /** テキストファイル拡張子 */
    public static final String TXT_EXTENSION = ".txt";

    // プライベートコンストラクタ - インスタンス化を防ぐ
    private ServiceConstants() {
        throw new AssertionError("ServiceConstants はインスタンス化できません");
    }
}
