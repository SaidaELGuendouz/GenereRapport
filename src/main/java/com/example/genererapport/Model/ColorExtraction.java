package com.example.genererapport.Model;

public class ColorExtraction {

    private final String color;

    public ColorExtraction(String color) {
        this.color = isValidColor(color) ? color : null;
    }

    public String getColor() {
        return color;
    }


    public boolean hasColor() {
        return color != null;
    }

    private boolean isValidColor(String color) {
        if (color == null) return false;

        // Regex pour RGB, RGBA, HEX et couleurs nommées
        String regex = "(rgb\\s*\\(\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\))|" +
                "(rgba\\s*\\(\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(\\.\\d+)?)\\))|" +
                "(#[0-9a-fA-F]{3,6})|" +
                "\\b(black|white|red|green|blue|yellow|cyan|magenta|silver|gray|maroon|olive|purple|teal|navy)\\b";

        return color.matches(regex);
    }
}