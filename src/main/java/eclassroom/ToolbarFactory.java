package eclassroom;

import java.awt.*;
import javax.swing.*;

public class ToolbarFactory {

    public interface ToolAction {
        void onToolSelected(ToolMode mode);

        void onPenSettings();

        void onEraserSettings();

        void onUndo();
        
        void onRedo();

        void onClear();
    }

    public static JToolBar createHorizontalToolbar(ToolAction action, boolean isStudent) {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        //toolbar.setBackground(new Color(200, 200, 200));

        // 1. 畫筆
        JButton penBtn = createButtonWithIcon("pencil.png", "畫筆");
        penBtn.setPreferredSize(new Dimension(64, 64));
        penBtn.setName("BTN_PENCIL");

        penBtn.addActionListener(e -> {
            action.onToolSelected(ToolMode.PENCIL);
            action.onPenSettings();
        });
        toolbar.add(penBtn);


        // 2. 橡皮擦
        JButton eraserBtn = createButtonWithIcon("eraser.png", "橡皮擦");
        eraserBtn.setPreferredSize(new Dimension(64, 64));
        eraserBtn.setName("BTN_ERASER");

        eraserBtn.addActionListener(e -> {
            action.onToolSelected(ToolMode.ERASER);
            action.onEraserSettings();
        });
        toolbar.add(eraserBtn);

        // --- undo按鈕 ---
        JButton undoBtn = createButtonWithIcon("undo.png", "上一步");
        undoBtn.setPreferredSize(new Dimension(64, 64));
        undoBtn.setName("BTN_UNDO");

        undoBtn.addActionListener(e -> {
            action.onUndo();
        });
        toolbar.add(undoBtn);

        //====ondo按鈕====
        JButton redoBtn = createButtonWithIcon("redo.png", "下一步");
        redoBtn.setPreferredSize(new Dimension(64, 64));
        redoBtn.setName("BTN_REDO");
        redoBtn.addActionListener(e -> {
            action.onRedo();
        });
        toolbar.add(redoBtn);

        //====clear按鈕=====
        JButton clearBtn = createButtonWithIcon("trash.png", "一鍵清除");
        clearBtn.setPreferredSize(new Dimension(64, 64));
        clearBtn.setName("BTN_CLEAR");

        clearBtn.addActionListener(e -> {
            action.onClear();
        });
        toolbar.add(clearBtn);

        penBtn.setEnabled(!isStudent);
        eraserBtn.setEnabled(!isStudent);
        undoBtn.setEnabled(!isStudent);
        redoBtn.setEnabled(!isStudent);
        clearBtn.setEnabled(!isStudent);


        
        return toolbar;
    }

    // ===== 嘗試載入圖片否則改成文字圖示 =====
    private static JButton createButtonWithIcon(String path, String fallbackText) {
        return AppResources.iconButton(path, fallbackText);
    }
}
