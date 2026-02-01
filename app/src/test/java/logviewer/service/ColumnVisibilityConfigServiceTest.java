package logviewer.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ColumnVisibilityConfigService クラスのテストケース。
 */
public class ColumnVisibilityConfigServiceTest {
    private ColumnVisibilityConfigService service;
    private Path configFilePath;

    @Before
    public void setUp() {
        service = new ColumnVisibilityConfigService();
        configFilePath = service.getConfigFilePath();
    }

    @After
    public void tearDown() throws IOException {
        // テスト後に設定ファイルをクリーンアップ
        if (Files.exists(configFilePath)) {
            Files.delete(configFilePath);
        }
        Path configDir = configFilePath.getParent();
        if (configDir != null && Files.exists(configDir)) {
            try {
                if (Files.list(configDir).count() == 0) {
                    Files.delete(configDir);
                }
            } catch (Exception e) {
                // ディレクトリが空でない場合は削除しない
            }
        }
    }

    /**
     * 通常系：設定ファイルが存在しない場合は空のリストを返す。
     */
    @Test
    public void testLoadHiddenColumnsNoFile() {
        List<Integer> hiddenColumns = service.loadHiddenColumns("sample.tsv");
        
        assertNotNull(hiddenColumns);
        assertEquals(0, hiddenColumns.size());
    }

    /**
     * 通常系：カラム非表示設定を保存して読み込む。
     */
    @Test
    public void testSaveAndLoadHiddenColumns() {
        String fileName = "sample_log.tsv";
        List<Integer> hiddenColumns = Arrays.asList(0, 2, 5);
        
        service.saveHiddenColumns(fileName, hiddenColumns);
        List<Integer> loaded = service.loadHiddenColumns(fileName);
        
        assertEquals(3, loaded.size());
        assertEquals(Integer.valueOf(0), loaded.get(0));
        assertEquals(Integer.valueOf(2), loaded.get(1));
        assertEquals(Integer.valueOf(5), loaded.get(2));
    }

    /**
     * 通常系：複数のファイルの設定を保存して読み込む。
     */
    @Test
    public void testSaveAndLoadMultipleFiles() {
        String file1 = "sample1.tsv";
        String file2 = "sample2.tsv";
        List<Integer> hidden1 = Arrays.asList(0, 1);
        List<Integer> hidden2 = Arrays.asList(2, 3, 4);
        
        service.saveHiddenColumns(file1, hidden1);
        service.saveHiddenColumns(file2, hidden2);
        
        List<Integer> loaded1 = service.loadHiddenColumns(file1);
        List<Integer> loaded2 = service.loadHiddenColumns(file2);
        
        assertEquals(2, loaded1.size());
        assertEquals(3, loaded2.size());
        assertEquals(Integer.valueOf(0), loaded1.get(0));
        assertEquals(Integer.valueOf(2), loaded2.get(0));
    }

    /**
     * 通常系：同じファイル名の設定を上書き。
     */
    @Test
    public void testOverwriteHiddenColumns() {
        String fileName = "sample.tsv";
        List<Integer> hidden1 = Arrays.asList(0, 1);
        List<Integer> hidden2 = Arrays.asList(3, 4, 5);
        
        service.saveHiddenColumns(fileName, hidden1);
        service.saveHiddenColumns(fileName, hidden2);
        
        List<Integer> loaded = service.loadHiddenColumns(fileName);
        
        assertEquals(3, loaded.size());
        assertEquals(Integer.valueOf(3), loaded.get(0));
        assertEquals(Integer.valueOf(4), loaded.get(1));
        assertEquals(Integer.valueOf(5), loaded.get(2));
    }

    /**
     * 通常系：空のリストを保存。
     */
    @Test
    public void testSaveEmptyList() {
        String fileName = "sample.tsv";
        List<Integer> emptyList = Arrays.asList();
        
        service.saveHiddenColumns(fileName, emptyList);
        List<Integer> loaded = service.loadHiddenColumns(fileName);
        
        assertNotNull(loaded);
        assertEquals(0, loaded.size());
    }

    /**
     * 通常系：特定ファイルの設定を削除。
     */
    @Test
    public void testDeleteHiddenColumnsConfig() {
        String file1 = "sample1.tsv";
        String file2 = "sample2.tsv";
        List<Integer> hidden = Arrays.asList(0, 1);
        
        service.saveHiddenColumns(file1, hidden);
        service.saveHiddenColumns(file2, hidden);
        
        service.deleteHiddenColumnsConfig(file1);
        
        List<Integer> loaded1 = service.loadHiddenColumns(file1);
        List<Integer> loaded2 = service.loadHiddenColumns(file2);
        
        assertEquals(0, loaded1.size());
        assertEquals(2, loaded2.size());
    }

    /**
     * 通常系：全設定をクリア。
     */
    @Test
    public void testClearAllConfig() {
        String file1 = "sample1.tsv";
        String file2 = "sample2.tsv";
        List<Integer> hidden = Arrays.asList(0, 1);
        
        service.saveHiddenColumns(file1, hidden);
        service.saveHiddenColumns(file2, hidden);
        
        service.clearAllConfig();
        
        assertFalse(Files.exists(configFilePath));
        
        List<Integer> loaded1 = service.loadHiddenColumns(file1);
        List<Integer> loaded2 = service.loadHiddenColumns(file2);
        
        assertEquals(0, loaded1.size());
        assertEquals(0, loaded2.size());
    }

    /**
     * 通常系：設定ファイルパスが取得できること。
     */
    @Test
    public void testGetConfigFilePath() {
        Path path = service.getConfigFilePath();
        
        assertNotNull(path);
        assertTrue(path.toString().contains(".logviewer"));
        assertTrue(path.toString().contains("column_config.json"));
    }

    /**
     * 通常系：存在しないファイルを削除してもエラーにならない。
     */
    @Test
    public void testDeleteNonExistentFile() {
        service.deleteHiddenColumnsConfig("nonexistent.tsv");
        // エラーが発生しないことを確認（例外がスローされない）
    }

    /**
     * 通常系：設定ファイルが存在しない状態でクリアしてもエラーにならない。
     */
    @Test
    public void testClearAllConfigNoFile() {
        service.clearAllConfig();
        // エラーが発生しないことを確認（例外がスローされない）
    }

    /**
     * 通常系：特殊文字を含むファイル名。
     */
    @Test
    public void testSaveAndLoadSpecialCharacters() {
        String fileName = "sample-log_2024.01.01.tsv";
        List<Integer> hidden = Arrays.asList(1, 3);
        
        service.saveHiddenColumns(fileName, hidden);
        List<Integer> loaded = service.loadHiddenColumns(fileName);
        
        assertEquals(2, loaded.size());
        assertEquals(Integer.valueOf(1), loaded.get(0));
        assertEquals(Integer.valueOf(3), loaded.get(1));
    }

    /**
     * 通常系：大きな数値のインデックスを保存。
     */
    @Test
    public void testSaveLargeIndexNumbers() {
        String fileName = "large.tsv";
        List<Integer> hidden = Arrays.asList(100, 200, 300);
        
        service.saveHiddenColumns(fileName, hidden);
        List<Integer> loaded = service.loadHiddenColumns(fileName);
        
        assertEquals(3, loaded.size());
        assertEquals(Integer.valueOf(100), loaded.get(0));
        assertEquals(Integer.valueOf(200), loaded.get(1));
        assertEquals(Integer.valueOf(300), loaded.get(2));
    }
}
