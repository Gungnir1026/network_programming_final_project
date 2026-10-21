package eclassroom;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Stack;
import javax.swing.*;


public class WhiteboardPanel extends JPanel {

    public interface DrawListener {
        void onDrawBegin(int x, int y, Color color, int size);
        void onDrawMove(int x1, int y1, int x2, int y2, Color color, int size);
        void onDrawEnd();

        // ===== 橡皮擦 =====
        void onEraseBegin(int size);
        void onEraseMove(int x, int y);
        void onEraseEnd();
    }

    private ArrayList<LineData> lines = new ArrayList<>();
    private DrawListener drawListener;

    private Color currentColor = Color.BLACK;
    private int brushSize = 10;
    private int eraserSize = 30;
    private int mouseX = -100, mouseY = -100;

    private ToolMode currentMode = ToolMode.PENCIL;
    private boolean isEditable = true;
    private int lastX, lastY;

    // ==== redo、undo====
    private Stack<Action> undoStack = new Stack<>();
    private Stack<Action> redoStack = new Stack<>();
    private StrokeAction currentStroke;
    private EraseStrokeAction currentEraseStroke;
    private boolean isRemoteAction = false;
    private StrokeAction remoteStroke;
    private EraseStrokeAction remoteEraseStroke;
    private int remoteEraserSize;
    private boolean localStrokeActive;



    public WhiteboardPanel() {
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (!isEditable || currentMode == ToolMode.NONE) return;
                localStrokeActive = true;

                lastX = e.getX();
                lastY = e.getY();

                if (currentMode == ToolMode.ERASER) {
                    startStroke();
                    if (drawListener != null) {
                        drawListener.onEraseBegin(eraserSize);
                    }
                    eraseAt(lastX, lastY, eraserSize);
                    if (drawListener != null) drawListener.onEraseMove(lastX, lastY);
                } else {
                    startStroke();
                    if (drawListener != null) {
                        drawListener.onDrawBegin(lastX, lastY, currentColor, brushSize);
                    }
                    addSmoothLine(lastX, lastY, lastX, lastY, currentColor, brushSize);
                    if (drawListener != null) {
                        drawListener.onDrawMove(lastX, lastY, lastX, lastY, currentColor, brushSize);
                    }
                }
            }

            public void mouseExited(MouseEvent e) {
                mouseX = -100;
                mouseY = -100;
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                if (!localStrokeActive) return;
                localStrokeActive = false;
                if (currentMode == ToolMode.ERASER) {
                    endStroke();
                    if (drawListener != null) {
                        drawListener.onEraseEnd();
                    }
                } else {
                    endStroke();
                    if (drawListener != null) {
                        drawListener.onDrawEnd();
                    }
                } 
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isEditable || !localStrokeActive) return;

                int x = e.getX();
                int y = e.getY();
                mouseX = x;
                mouseY = y;

                if (currentMode == ToolMode.ERASER) {
                    eraseAt(x, y, eraserSize);
                    if (!isRemoteAction && drawListener != null) {
                        drawListener.onEraseMove(x, y);
                    }
                } else {
                    addSmoothLine(lastX, lastY, x, y, currentColor, brushSize);
                    if (!isRemoteAction && drawListener != null) {
                        drawListener.onDrawMove(lastX, lastY, x, y, currentColor, brushSize);
                    }
                }

                lastX = x;
                lastY = y;
            }
        });
    }

    public void addLine(int x1, int y1, int x2, int y2, Color c, int size) {
        LineData line = new LineData(x1, y1, x2, y2, c, size);
        lines.add(line);

        StrokeAction stroke = isRemoteAction ? remoteStroke : currentStroke;
        if (stroke != null) stroke.strokeLines.add(line);

        repaint();
    }

    public void addSmoothLine(int x1, int y1, int x2, int y2, Color c, int size) {

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = Math.hypot(dx, dy);

        int step = 3; // 每一小段線最長 3px（2~4 都可以）

        int segments = Math.max(1, (int)(dist / step));

        for (int i = 0; i < segments; i++) {
            int sx1 = (int)(x1 + dx * i / segments);
            int sy1 = (int)(y1 + dy * i / segments);
            int sx2 = (int)(x1 + dx * (i + 1) / segments);
            int sy2 = (int)(y1 + dy * (i + 1) / segments);

            addLine(sx1, sy1, sx2, sy2, c, size);
        }
    }
    public void beginRemoteDraw() {
        remoteStroke = new StrokeAction();
    }

    public void endRemoteDraw() {
        if (remoteStroke != null && !remoteStroke.strokeLines.isEmpty()) {
            undoStack.push(remoteStroke);
            redoStack.clear();
        }
        remoteStroke = null;
        repaint();
    }

    public void addRemoteLine(int x1, int y1, int x2, int y2, Color color, int size) {
        isRemoteAction = true;
        try {
            addSmoothLine(x1, y1, x2, y2, color, size);
        } finally {
            isRemoteAction = false;
        }
    }

    public void beginRemoteErase(int size) {
        remoteEraserSize = size;
        remoteEraseStroke = new EraseStrokeAction();
    }

    public void remoteEraseMove(int x, int y) {
        isRemoteAction = true;
        try {
            eraseAt(x, y, remoteEraserSize);
        } finally {
            isRemoteAction = false;
        }
    }

    public void endRemoteErase() {
        if (remoteEraseStroke != null && !remoteEraseStroke.erasedLines.isEmpty()) {
            remoteEraseStroke.after = new ArrayList<>(lines);
            undoStack.push(remoteEraseStroke);
            redoStack.clear();
        }
        remoteEraseStroke = null;
        repaint();
    }

    public void undoRemote(){
        isRemoteAction = true;
        undo();
        isRemoteAction = false;
    }

    public void redoRemote(){
        isRemoteAction = true;
        redo();
        isRemoteAction = false;
    }

    public void clearRemote(){
        isRemoteAction = true;
        clearAll();
        isRemoteAction = false;
    }


    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(LineData l:lines){
            g2.setColor(l.color);
            g2.setStroke(new BasicStroke(
                l.size,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            ));
            g2.drawLine(l.x1,l.y1,l.x2,l.y2);
        }

        if (isEditable && mouseX >= 0 && mouseY >= 0) {

            int r = currentMode == ToolMode.ERASER ? eraserSize : brushSize;

            if (currentMode == ToolMode.ERASER) {
                // ===== 橡皮擦游標（虛線）=====
                float dash[] = {6f, 6f};
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(2f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND,
                        1f, dash, 0f));

                g2.drawOval(mouseX - r/2, mouseY - r/2, r, r);

            } else {
                // ===== 畫筆游標（實心圓）=====
                g2.setColor(new Color(
                        currentColor.getRed(),
                        currentColor.getGreen(),
                        currentColor.getBlue(),
                        240)); // 半透明

                g2.fillOval(mouseX - r/2, mouseY - r/2, r, r);

                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(mouseX - r/2, mouseY - r/2, r, r);
            }
        }
    }

    abstract class Action {
        abstract void undo();
        abstract void redo();
    }

    class StrokeAction extends Action {
        ArrayList<LineData> strokeLines = new ArrayList<>();

        @Override
        void undo() {
            lines.removeAll(strokeLines);
        }

        @Override
        void redo() {
            lines.addAll(strokeLines);
        }
    }

    class EraseStrokeAction extends Action {
        ArrayList<LineData> erasedLines = new ArrayList<>();
        ArrayList<LineData> before = new ArrayList<>(lines);
        ArrayList<LineData> after;

        @Override
        void undo() {
            lines.clear();
            lines.addAll(before);
        }

        @Override
        void redo() {
            lines.clear();
            lines.addAll(after);
        }
    }

    public void startStroke() {
        if (currentMode == ToolMode.ERASER) {
            currentEraseStroke = new EraseStrokeAction();
        } else {
            currentStroke = new StrokeAction();
        }
    }

    public void endStroke() {
        if (currentMode == ToolMode.ERASER) {
            if (currentEraseStroke != null && !currentEraseStroke.erasedLines.isEmpty()) {
                currentEraseStroke.after = new ArrayList<>(lines);
                undoStack.push(currentEraseStroke);
                redoStack.clear();
            }
            currentEraseStroke = null;
        } else {
            if (currentStroke != null && !currentStroke.strokeLines.isEmpty()) {
                undoStack.push(currentStroke);
                redoStack.clear();
            }
            currentStroke = null;
        }
    }

    class ClearAction extends Action {
        ArrayList<LineData> backup;
        ClearAction(ArrayList<LineData> src){
            backup = new ArrayList<>(src);
        }
        void undo(){
            lines.clear();
            lines.addAll(backup);
        }
        void redo(){
            lines.clear();
        }
    }

    private void eraseAt(int x, int y, int size) {

        for (int i = lines.size() - 1; i >= 0; i--) {
            LineData l = lines.get(i);
            double d = pointDist(x, y, l);

            if (d <= size / 2.0 + l.size / 2.0) {
                lines.remove(i);

                EraseStrokeAction stroke = isRemoteAction ? remoteEraseStroke : currentEraseStroke;
                if (stroke != null) stroke.erasedLines.add(l);
            }
        }
        repaint();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Action action = undoStack.pop();
            action.undo();
            redoStack.push(action);
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Action action = redoStack.pop();
            action.redo();
            undoStack.push(action);
            repaint();
        }
    }

    public void clearAll(){
        if(lines.isEmpty()) return;
        undoStack.push(new ClearAction(lines));
        redoStack.clear();
        lines.clear();
        repaint();
    }

    private double pointDist(int px, int py, LineData l) {
        // ===== 如果是「點」（零長度線段）=====
        if (l.x1 == l.x2 && l.y1 == l.y2) {
            return Math.hypot(px - l.x1, py - l.y1);
        }

        // ===== 一般線段 =====
        double dx = l.x2 - l.x1;
        double dy = l.y2 - l.y1;

        double t = ((px - l.x1) * dx + (py - l.y1) * dy)
                / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double cx = l.x1 + t * dx;
        double cy = l.y1 + t * dy;

        return Math.hypot(px - cx, py - cy);
    }

    // Setters & Getters
    public void setDrawListener(DrawListener listener) {
        this.drawListener = listener;
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public void setToolMode(ToolMode mode) {
        this.currentMode = mode;
    }

    public void setEditable(boolean editable) {
        this.isEditable = editable;
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setEraserSize(int size) {
        this.eraserSize = size;
    }

    public int getEraserSize() {
        return eraserSize;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public ToolMode getToolMode() {
        return currentMode;
    }
}
