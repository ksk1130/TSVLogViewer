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
    private final Button clearBtn;
    private final Button toggleFilterPanelBtn;

    /**
     * SingleFilterPanel のコンストラクタ。
     * 
     * @param columnSelector カラム選択コンボボックス
     * @param filterField フィルタテキストフィールド
     * @param onToggleFilterPanel 複数条件検索パネル切り替え時のコールバック
     */
    public SingleFilterPanel(ComboBox<String> columnSelector, TextField filterField, Runnable onToggleFilterPanel) {
        super(8);
        
        this.columnSelector = columnSelector;
        this.filterField = filterField;
        
        // 複数条件検索パネルの開閉ボタン
        toggleFilterPanelBtn = new Button("≡ 複数条件検索");
        toggleFilterPanelBtn.setStyle("-fx-font-weight: bold;");
        toggleFilterPanelBtn.setOnAction(e -> onToggleFilterPanel.run());

        // クリアボタン
        clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> filterField.clear());

        // UI設定
        setupUI();
    }

    /**
     * UI要素を設定します。
     */
    private void setupUI() {
        columnSelector.setPrefWidth(150);
        filterField.setPromptText("Filter (substring, case-insensitive). Use /regex/ for regex.");
        filterField.setPrefWidth(400);

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
     * クリアボタンを取得します。
     * 
     * @return クリアボタン
     */
    public Button getClearBtn() {
        return clearBtn;
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
