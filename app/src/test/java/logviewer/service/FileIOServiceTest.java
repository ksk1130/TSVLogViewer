package logviewer.service;

import logviewer.LogRow;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * FileIOService のテストケース。
 */
public class FileIOServiceTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * isSupportedInputFile の判定を確認します。
     * 拡張子に基づく制限は撤廃されたため、すべてのファイルで true を返します。
     */
    @Test
    public void testIsSupportedInputFile() throws Exception {
        FileIOService service = new FileIOService();

        File tsv = tempFolder.newFile("sample.tsv");
        File txt = tempFolder.newFile("sample.txt");
        File csv = tempFolder.newFile("sample.csv");

        // 拡張子制限は撤廃されたため、すべてのファイルで true を返す
        assertTrue(service.isSupportedInputFile(tsv.toPath()));
        assertTrue(service.isSupportedInputFile(txt.toPath()));
        assertTrue(service.isSupportedInputFile(csv.toPath()));
        // null のみ false を返す
        assertFalse(service.isSupportedInputFile(null));
    }

    /**
     * isSupportedInputFile が大文字拡張子でも正しく判定することを確認します。
     */
    @Test
    public void testIsSupportedInputFileWithUpperCase() throws Exception {
        FileIOService service = new FileIOService();

        File tsvUpper = tempFolder.newFile("sample.TSV");
        File txtUpper = tempFolder.newFile("sample.TXT");

        // 拡張子制限は撤廃されたため、すべてのファイルで true を返す
        assertTrue(service.isSupportedInputFile(tsvUpper.toPath()));
        assertTrue(service.isSupportedInputFile(txtUpper.toPath()));
    }

    /**
     * isSupportedInputFile が混合大文字でも正しく判定することを確認します。
     */
    @Test
    public void testIsSupportedInputFileWithMixedCase() throws Exception {
        FileIOService service = new FileIOService();

        File tsvMixed = tempFolder.newFile("sample.Tsv");
        File txtMixed = tempFolder.newFile("sample.TxT");

        // 拡張子制限は撤廃されたため、すべてのファイルで true を返す
        assertTrue(service.isSupportedInputFile(tsvMixed.toPath()));
        assertTrue(service.isSupportedInputFile(txtMixed.toPath()));
    }
}
