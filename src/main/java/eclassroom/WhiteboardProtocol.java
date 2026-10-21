package eclassroom;

import java.awt.Color;

/** Decode both clients' board commands immediately on the Swing event thread. */
final class WhiteboardProtocol {
    private WhiteboardProtocol() {}

    static boolean apply(String message, WhiteboardPanel board) {
        String[] fields = message.split(" ");
        switch (fields[0]) {
            case "DRAW_BEGIN": board.beginRemoteDraw(); break;
            case "DRAW_MOVE":
                board.addRemoteLine(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]), Integer.parseInt(fields[4]),
                        new Color(Integer.parseInt(fields[5]), Integer.parseInt(fields[6]),
                                Integer.parseInt(fields[7])), Integer.parseInt(fields[8]));
                break;
            case "DRAW_END": board.endRemoteDraw(); break;
            case "ERASE_BEGIN": board.beginRemoteErase(Integer.parseInt(fields[1])); break;
            case "ERASE_MOVE": board.remoteEraseMove(Integer.parseInt(fields[1]), Integer.parseInt(fields[2])); break;
            case "ERASE_END": board.endRemoteErase(); break;
            case "UNDO": board.undoRemote(); break;
            case "REDO": board.redoRemote(); break;
            case "CLEAR": board.clearRemote(); break;
            default: return false;
        }
        return true;
    }
}
