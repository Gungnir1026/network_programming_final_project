package eclassroom;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/** Classpath resources also work when the application runs from a JAR. */
public final class AppResources {
    private AppResources() {}

    public static JButton iconButton(String filename, String label) {
        JButton button = new JButton(label);
        URL resource = AppResources.class.getResource("/icons/" + filename);
        if (resource != null) {
            try {
                Image image = ImageIO.read(resource);
                if (image != null) {
                    button.setIcon(new ImageIcon(image.getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
                    button.setText(null);
                }
            } catch (IOException ignored) {
                // The text label remains usable if an icon cannot be decoded.
            }
        }
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        button.setFocusable(false);
        return button;
    }
}
