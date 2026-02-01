package logviewer.service;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * クリップボード操作を担当するサービスクラス。
 */
public class ClipboardService {
    /**
     * テキストをシステムクリップボードにコピーします。
     * 
     * @param text コピーするテキスト (null の場合は何もしない)
     */
    public void copyText(String text) {
        if (text == null) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
