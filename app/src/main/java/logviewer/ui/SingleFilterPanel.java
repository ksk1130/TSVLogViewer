package logviewer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 単一条件検索UIパネルをコンポーネント化したクラス。
 * カラム選択、フィルタ入力、クリアボタンを含みます。
 */
public class SingleFilterPanel extends HBox {
    private final ComboBox<String> columnSelector;
    private final TextField filterField;
    private final TextField searchField;
    private final Button clearBtn;
    private final Button searchBtn;
    private final Button clearSearchBtn;
    private final Button prevSearchBtn;
    private final Button nextSearchBtn;
    private final Button reloadBtn;
    private final Button toggleFilterPanelBtn;

    /**
     * SingleFilterPanel のコンストラクタ。
     * 
     * @param columnSelector カラム選択コンボボックス
     * @param filterField フィルタテキストフィールド
      * @param searchField 検索テキストフィールド
     * @param onToggleFilterPanel 複数条件検索パネル切り替え時のコールバック
     * @param onReload ファイル再読み込み時のコールバック
      * @param onSearch 検索実行時のコールバック
    * @param onClearSearch 検索クリア時のコールバック
      * @param onNextSearch 次の検索結果へ移動時のコールバック
      * @param onPrevSearch 前の検索結果へ移動時のコールバック
     */
    public SingleFilterPanel(ComboBox<String> columnSelector, TextField filterField,
                        TextField searchField, Runnable onToggleFilterPanel, Runnable onReload,
                        Runnable onSearch, Runnable onClearSearch, Runnable onNextSearch, Runnable onPrevSearch) {
        super(8);
        
        this.columnSelector = columnSelector;
        this.filterField = filterField;
                this.searchField = searchField;
        
        // 複数条件検索パネルの開閉ボタン
        toggleFilterPanelBtn = new Button("≡ 複数条件検索");
        toggleFilterPanelBtn.setStyle("-fx-font-weight: bold;");
        toggleFilterPanelBtn.setOnAction(e -> onToggleFilterPanel.run());

        // クリアボタン
        clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> filterField.clear());

        // 検索ボタン
        searchBtn = new Button("検索");
        searchBtn.setOnAction(e -> onSearch.run());

        // 検索クリアボタン
        clearSearchBtn = new Button("Clear");
        clearSearchBtn.setOnAction(e -> onClearSearch.run());

        // 前へ/次へボタン
        prevSearchBtn = new Button("前へ");
        prevSearchBtn.setOnAction(e -> onPrevSearch.run());
        nextSearchBtn = new Button("次へ");
        nextSearchBtn.setOnAction(e -> onNextSearch.run());

        // 再読み込みボタン
        reloadBtn = new Button("🔄 再読み込み");
        reloadBtn.setOnAction(e -> onReload.run());
        reloadBtn.setDisable(true);  // 初期状態では無効

        // UI設定
        setupUI();
    }

    /**
     * UI要素を設定します。
     */
    private void setupUI() {
        columnSelector.setPrefWidth(150);
        filterField.setPromptText("Filter (substring, case-insensitive). Use /regex/ for regex.");
        filterField.setPrefWidth(280);
        searchField.setPromptText("検索 (表示中のセルを部分一致)");
        searchField.setPrefWidth(240);

        Label singleConditionLabel = new Label("単一条件検索　");
        singleConditionLabel.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.setPadding(new Insets(8));
        this.setAlignment(Pos.CENTER_LEFT);
        this.getChildren().addAll(
            toggleFilterPanelBtn,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            singleConditionLabel,
            new Label("Column:"),
            columnSelector,
            new Label("Filter:"),
            filterField,
            clearBtn,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            new Label("検索:"),
            searchField,
            searchBtn,
            clearSearchBtn,
            prevSearchBtn,
            nextSearchBtn,
            reloadBtn,
            spacer
        );
    }

    /**
     * カラム選択コンボボックスを取得します。
     * 
     * @return カラム選択コンボボックス
     */
    public ComboBox<String> getColumnSelector() {
        return columnSelector;
    }

    /**
     * フィルタテキストフィールドを取得します。
     * 
     * @return フィルタテキストフィールド
     */
    public TextField getFilterField() {
        return filterField;
    }

    /**
     * 検索テキストフィールドを取得します。
     *
     * @return 検索テキストフィールド
     */
    public TextField getSearchField() {
        return searchField;
    }

    /**
     * クリアボタンを取得します。
     * 
     * @return クリアボタン
     */
    public Button getClearBtn() {
        return clearBtn;
    }

    /**
     * 検索実行ボタンを取得します。
     *
     * @return 検索実行ボタン
     */
    public Button getSearchBtn() {
        return searchBtn;
    }

    /**
     * 次の検索結果ボタンを取得します。
     *
     * @return 次へボタン
     */
    public Button getNextSearchBtn() {
        return nextSearchBtn;
    }

    /**
     * 前の検索結果ボタンを取得します。
     *
     * @return 前へボタン
     */
    public Button getPrevSearchBtn() {
        return prevSearchBtn;
    }

    /**
     * 再読み込みボタンを取得します。
     * 
     * @return 再読み込みボタン
     */
    public Button getReloadBtn() {
        return reloadBtn;
    }

    /**
     * 複数条件検索パネル切り替えボタンを取得します。
     * 
     * @return 複数条件検索パネル切り替えボタン
     */
    public Button getToggleFilterPanelBtn() {
        return toggleFilterPanelBtn;
    }
}
