package uml.model;

import java.util.Objects;

public record Style(StyleColor color, int fontSize) {

    public static final Style DEFAULT = new Style(StyleColor.BLACK, 16);

    public Style {
        Objects.requireNonNull(color, "color");
        if (fontSize <= 0) {
            throw new IllegalArgumentException("fontSize must be positive: " + fontSize);
        }
    }
}
