package ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class PieceRenderer {

    // Keys are like "wp", "bk", "wq" etc matching your resource filenames
    private final Map<String, BufferedImage> images = new HashMap<>();

    public PieceRenderer() {
        String[] colors = {"w", "b"};
        String[] pieces = {"p", "n", "b", "r", "q", "k"};
        for (String color : colors) {
            for (String piece : pieces) {
                String key = color + piece;
                String path = "/resources/" + key + ".png";
                try {
                    InputStream is = getClass().getResourceAsStream(path);
                    if (is == null) {
                        throw new IOException("Not found: " + path);
                    }
                    images.put(key, ImageIO.read(is));
                } catch (IOException e) {
                    System.err.println("Failed to load image: " + path);
                }
            }
        }
    }

    // color: "w" or "b", pieceChar: "p","n","b","r","q","k"
    public BufferedImage getImage(String color, String pieceChar) {
        return images.get(color + pieceChar);
    }
}
