package com.example.genererapport.Service;
import com.example.genererapport.Enum.LineDirection;
import com.example.genererapport.Model.ColorExtraction;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.JRPen;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConvertCssToJasperService {
    private static final Logger logger = LoggerFactory.getLogger(ConvertCssToJasperService.class);

    public Map.Entry<String, String> convertCssToJasperAttribute(String property, String value) {
        switch (property) {
            case "font-family":
                return Map.entry("fontName", value.replaceAll("'|\"", ""));
            case "font-size":
                int fontSizeInPixels = convertCssValueToPixels(value);
                return Map.entry("fontSize", String.valueOf(fontSizeInPixels));
            case "font-weight":
                if (value.equals("bold") || value.equals("700") || value.equals("800") || value.equals("900")) {
                    return Map.entry("isBold", "true");

                } else {
                    return Map.entry("isBold", "false");
                }
            case "font-style":
                if (value.equals("italic")) {
                    return Map.entry("isItalic", "true");
                }
                break;
            case "text-decoration":
                if (value.contains("underline")) {
                    return Map.entry("isUnderline", "true");
                } else if (value.contains("line-through")) {
                    return Map.entry("isStrikeThrough", "true");
                }
                break;
            case "color":
                return Map.entry("forecolor", convertColorToHex(value));

            case "background-color":
                return Map.entry("backcolor", convertColorToHex(value));

            case "border":
                return handleBorderShorthand(value);
            case "border-top":
                return handleBorderSide(value, "Top");
            case "border-right":
                return handleBorderSide(value, "Right");
            case "border-bottom":
                return handleBorderSide(value, "Bottom");
            case "border-left":
                return handleBorderSide(value, "Left");

            case "border-style":
                return handleBorderStyleShorthand(value);
            case "border-top-style":
                return Map.entry("borderTopStyle", convertBorderStyle(value));
            case "border-right-style":
                return Map.entry("borderRightStyle", convertBorderStyle(value));
            case "border-bottom-style":
                return Map.entry("borderBottomStyle", convertBorderStyle(value));
            case "border-left-style":
                return Map.entry("borderLeftStyle", convertBorderStyle(value));

            case "border-width":
                return handleBorderWidthShorthand(value);
            case "border-top-width":
                return Map.entry("borderTopWidth", convertBorderWidth(value));
            case "border-right-width":
                return Map.entry("borderRightWidth", convertBorderWidth(value));
            case "border-bottom-width":
                return Map.entry("borderBottomWidth", convertBorderWidth(value));
            case "border-left-width":
                return Map.entry("borderLeftWidth", convertBorderWidth(value));

            case "border-color":
                return handleBorderColorShorthand(value);
            case "border-top-color":
                return Map.entry("borderTopColor", convertColorToHex(value));
            case "border-right-color":
                return Map.entry("borderRightColor", convertColorToHex(value));
            case "border-bottom-color":
                return Map.entry("borderBottomColor", convertColorToHex(value));
            case "border-left-color":
                return Map.entry("borderLeftColor", convertColorToHex(value));

            case "padding-left":
                int leftPadding = convertCssValueToPixels(value);
                return Map.entry("leftPadding", String.valueOf(leftPadding));
            case "padding-right":
                int rightPadding = convertCssValueToPixels(value);
                return Map.entry("rightPadding", String.valueOf(rightPadding));
            case "padding-top":
                int topPadding = convertCssValueToPixels(value);
                return Map.entry("topPadding", String.valueOf(topPadding));
            case "padding-bottom":
                int bottomPadding = convertCssValueToPixels(value);
                return Map.entry("bottomPadding", String.valueOf(bottomPadding));
            case "margin-left":
                int leftMargin = convertCssValueToPixels(value);
                return Map.entry("leftMargin", String.valueOf(leftMargin));
            case "margin-right":
                int rightMargin = convertCssValueToPixels(value);
                return Map.entry("rightMargin", String.valueOf(rightMargin));

            case "margin-top":
                int topMargin = convertCssValueToPixels(value);
                return Map.entry("topMargin", String.valueOf(topMargin));

            case "margin-bottom":
                int bottomMargin = convertCssValueToPixels(value);
                return Map.entry("bottomMargin", String.valueOf(bottomMargin));

            case "visibility":
                if (value.equals("hidden")) {
                    return Map.entry("isVisible", "false");
                } else {
                    return Map.entry("isVisible", "true");
                }

            case "text-align":
                return Map.entry("horizontalAlignment", convertHorizontalAlignment(value));

            case "vertical-align":
                return Map.entry("verticalAlignment", convertVerticalAlignment(value));
            default:
                throw new IllegalArgumentException("Propriété CSS non supportée : " + property);

        }
        return null;
    }

    private Map.Entry<String, String> handleBorderShorthand(String value) {
        // Structure standard: border: width style color;
        String originalValue = value.trim();
        logger.info("Valeur originale: '{}'", originalValue);

        String width = "1px";
        String style = "solid";
        String color = "#000000";

        Matcher rgbMatcher = Pattern.compile("rgb\\s*\\([^)]+\\)").matcher(originalValue);
        Matcher rgbaMatcher = Pattern.compile("rgba\\s*\\([^)]+\\)").matcher(originalValue);
        Matcher hexMatcher = Pattern.compile("#[0-9a-fA-F]{3,6}").matcher(originalValue);

        String colorValue = null;


        if (rgbMatcher.find()) {
            colorValue = rgbMatcher.group();
        } else if (rgbaMatcher.find()) {
            colorValue = rgbaMatcher.group();
        } else if (hexMatcher.find()) {
            colorValue = hexMatcher.group();
        }
        String remainingValue = originalValue;
        if (colorValue != null) {
            color = colorValue;
            remainingValue = originalValue.replace(colorValue, "").trim();
            logger.info("Valeur restante: '{}'", remainingValue);
        }
        String[] parts = remainingValue.split("\\s+");

        if (parts.length >= 1 && !parts[0].isEmpty()) {
            width = parts[0];
        }

        if (parts.length >= 2 && !parts[1].isEmpty()) {
            style = parts[1];
        }
        if (colorValue == null && parts.length >= 3) {
            color = parts[2];
        }
        StringBuilder jasperBorder = new StringBuilder();
        jasperBorder.append(convertBorderWidth(width)).append(",");
        jasperBorder.append(convertBorderStyle(style)).append(",");

        logger.info("width: '{}', style: '{}', color: '{}'", width, style, color);
        jasperBorder.append(convertColorToHex(color));

        logger.info("Border: {}", jasperBorder.toString());
        return Map.entry("border", jasperBorder.toString());
    }

    private String convertBorderWidth(String width) {
        if (width.equalsIgnoreCase("thin")) {
            return "1";
        } else if (width.equalsIgnoreCase("medium")) {
            return "3";
        } else if (width.equalsIgnoreCase("thick")) {
            return "5";
        } else if (width.endsWith("px")) {
            return width.replace("px", "");
        } else if (width.endsWith("pt")) {
            return width.replace("pt", "");
        } else {
            return width;
        }
    }

    private String convertBorderStyle(String style) {
        return switch (style.toLowerCase()) {
            case "dotted" -> "Dotted";
            case "dashed" -> "Dashed";
            case "double" -> "Double";
            default -> "Solid";
        };
    }

    private Map.Entry<String, String> handleBorderSide(String value, String side) {
        String originalValue = value.trim();
        String width = "1px";
        String style = "solid";
        String color = "#000000";

        Matcher rgbMatcher = Pattern.compile("rgb\\s*\\([^)]+\\)").matcher(originalValue);
        Matcher rgbaMatcher = Pattern.compile("rgba\\s*\\([^)]+\\)").matcher(originalValue);
        Matcher hexMatcher = Pattern.compile("#[0-9a-fA-F]{3,6}").matcher(originalValue);

        String colorValue = null;
        if (rgbMatcher.find()) {
            colorValue = rgbMatcher.group();
        } else if (rgbaMatcher.find()) {
            colorValue = rgbaMatcher.group();
        } else if (hexMatcher.find()) {
            colorValue = hexMatcher.group();
        }
        String remainingValue = originalValue;
        if (colorValue != null) {
            color = colorValue;
            remainingValue = originalValue.replace(colorValue, "").trim();
        }
        String[] parts = remainingValue.split("\\s+");

        if (parts.length >= 1 && !parts[0].isEmpty()) {
            width = parts[0];
        }

        if (parts.length >= 2 && !parts[1].isEmpty()) {
            style = parts[1];
        }
        if (colorValue == null && parts.length >= 3) {
            color = parts[2];
        }
        StringBuilder jasperBorder = new StringBuilder();
        jasperBorder.append(convertBorderWidth(width)).append(",");
        jasperBorder.append(convertBorderStyle(style)).append(",");

        logger.info("width: '{}', style: '{}', color: '{}'", width, style, color);
        jasperBorder.append(convertColorToHex(color));
        return Map.entry("border" + side, jasperBorder.toString());

    }

    private Map.Entry<String, String> handleBorderWidthShorthand(String value) {

        String[] values = value.trim().split("\\s+");

        if (values.length == 1) {
            String width = convertBorderWidth(values[0]);
            return Map.entry("borderWidth", width + "," + width + "," + width + "," + width);
        } else if (values.length == 2) {
            String topBottom = convertBorderWidth(values[0]);
            String rightLeft = convertBorderWidth(values[1]);
            return Map.entry("borderWidth", topBottom + "," + rightLeft + "," + topBottom + "," + rightLeft);
        } else if (values.length == 3) {
            String top = convertBorderWidth(values[0]);
            String rightLeft = convertBorderWidth(values[1]);
            String bottom = convertBorderWidth(values[2]);
            return Map.entry("borderWidth", top + "," + rightLeft + "," + bottom + "," + rightLeft);
        } else if (values.length == 4) {
            return Map.entry("borderWidth",
                    convertBorderWidth(values[0]) + "," +
                            convertBorderWidth(values[1]) + "," +
                            convertBorderWidth(values[2]) + "," +
                            convertBorderWidth(values[3]));
        }

        return Map.entry("borderWidth", "1,1,1,1");
    }

    private Map.Entry<String, String> handleBorderColorShorthand(String value) {
        ArrayList<String> colorValues = new ArrayList<>();
        String workingValue = value;

        Map<String, String> placeholders = new HashMap<>();
        int placeholderCount = 0;

        Pattern colorPattern = Pattern.compile("(rgb\\s*\\([^)]+\\)|rgba\\s*\\([^)]+\\)|#[0-9a-fA-F]{3,6})");
        Matcher matcher = colorPattern.matcher(workingValue);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String colorMatch = matcher.group();
            String placeholder = "COLOR_PLACEHOLDER_" + placeholderCount++;
            placeholders.put(placeholder, colorMatch);
            matcher.appendReplacement(sb, placeholder);
        }
        matcher.appendTail(sb);
        workingValue = sb.toString();
        String[] parts = workingValue.trim().split("\\s+");

        for (String part : parts) {
            if (placeholders.containsKey(part)) {
                colorValues.add(placeholders.get(part));
            } else {
                colorValues.add(part);
            }
        }
         if (colorValues.size() == 1) {
            String color = convertColorToHex(colorValues.get(0));
            return Map.entry("borderColor", color + "," + color + "," + color + "," + color);
        } else if (colorValues.size() == 2) {
            String topBottom = convertColorToHex(colorValues.get(0));
            String rightLeft = convertColorToHex(colorValues.get(1));
            return Map.entry("borderColor", topBottom + "," + rightLeft + "," + topBottom + "," + rightLeft);
        } else if (colorValues.size() == 3) {
            String top = convertColorToHex(colorValues.get(0));
            String rightLeft = convertColorToHex(colorValues.get(1));
            String bottom = convertColorToHex(colorValues.get(2));
            return Map.entry("borderColor", top + "," + rightLeft + "," + bottom + "," + rightLeft);
        } else if (colorValues.size() == 4) {
            return Map.entry("borderColor",
                    convertColorToHex(colorValues.get(0)) + "," +
                            convertColorToHex(colorValues.get(1)) + "," +
                            convertColorToHex(colorValues.get(2)) + "," +
                            convertColorToHex(colorValues.get(3)));
        }

        return Map.entry("borderColor", "#000000,#000000,#000000,#000000");
    }
    private String convertHorizontalAlignment(String cssAlignment) {
        switch (cssAlignment.toLowerCase().trim()) {
            case "left":
                return "Left";
            case "center":
                return "Center";
            case "right":
                return "Right";
            case "justify":
                return "Justified";
            default:
                return "Left";
        }
    }

    private String convertVerticalAlignment(String align) {
        switch (align.toLowerCase()) {
            case "top":
                return "Top";
            case "middle":
                return "Middle";
            case "bottom":
                return "Bottom";
            default:
                return "Top";
        }
    }
    private Map.Entry<String, String> handleBorderStyleShorthand(String value) {
        String[] values = value.trim().split("\\s+");

        if (values.length == 1) {
            // Même style pour tous les côtés
            String style = convertBorderStyle(values[0]);
            return Map.entry("borderStyle", style + "," + style + "," + style + "," + style);
        } else if (values.length == 2) {
            // top/bottom, right/left
            String topBottom = convertBorderStyle(values[0]);
            String rightLeft = convertBorderStyle(values[1]);
            return Map.entry("borderStyle", topBottom + "," + rightLeft + "," + topBottom + "," + rightLeft);
        } else if (values.length == 3) {

            String top = convertBorderStyle(values[0]);
            String rightLeft = convertBorderStyle(values[1]);
            String bottom = convertBorderStyle(values[2]);
            return Map.entry("borderStyle", top + "," + rightLeft + "," + bottom + "," + rightLeft);
        } else if (values.length == 4) {
            return Map.entry("borderStyle",
                    convertBorderStyle(values[0]) + "," +
                            convertBorderStyle(values[1]) + "," +
                            convertBorderStyle(values[2]) + "," +
                            convertBorderStyle(values[3]));
        }

        return Map.entry("borderStyle", "Solid,Solid,Solid,Solid");
    }
    public String extractColor(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String originalvalue = value.trim();

        Matcher rgbMatcher = Pattern.compile("rgb\\s*\\([^)]+\\)").matcher(originalvalue);
        Matcher rgbaMatcher = Pattern.compile("rgba\\s*\\([^)]+\\)").matcher(originalvalue);
        Matcher hexMatcher = Pattern.compile("#[0-9a-fA-F]{3,6}").matcher(originalvalue);
        Matcher namedColorMatcher = Pattern.compile("\\b(black|white|red|green|blue|yellow|cyan|magenta|silver|gray|maroon|olive|purple|teal|navy)\\b", Pattern.CASE_INSENSITIVE).matcher(originalvalue);

        if (rgbMatcher.find()) {
            return rgbMatcher.group();
        } else if (rgbaMatcher.find()) {
            return rgbaMatcher.group();
        } else if (hexMatcher.find()) {
            return hexMatcher.group();
        } else if (namedColorMatcher.find()) {
            return namedColorMatcher.group();
        }

        return null;
    }


    private String convertColorToHex(String color) {
        if (color == null || color.trim().isEmpty()) {
            return "#000000";
        }

        color = color.trim().toLowerCase();

        // Gestion des couleurs nommées
        Map<String, String> namedColors = new HashMap<>();
        namedColors.put("black", "#000000");
        namedColors.put("white", "#FFFFFF");
        namedColors.put("red", "#FF0000");
        namedColors.put("green", "#008000");
        namedColors.put("blue", "#0000FF");
        namedColors.put("yellow", "#FFFF00");
        namedColors.put("cyan", "#00FFFF");
        namedColors.put("magenta", "#FF00FF");
        namedColors.put("silver", "#C0C0C0");
        namedColors.put("gray", "#808080");
        namedColors.put("maroon", "#800000");
        namedColors.put("olive", "#808000");
        namedColors.put("purple", "#800080");
        namedColors.put("teal", "#008080");
        namedColors.put("navy", "#000080");

        if (namedColors.containsKey(color)) {
            return namedColors.get(color);
        }

        // Format #RGB ou #RRGGBB
        if (color.startsWith("#")) {
            if (color.length() == 4) {
                // Format #RGB -> #RRGGBB
                char r = color.charAt(1);
                char g = color.charAt(2);
                char b = color.charAt(3);
                return "#" + r + r + g + g + b + b;
            }
            return color;
        }

        // Format rgb(r,g,b) - version robuste
        if (color.startsWith("rgb(") && color.endsWith(")")) {
            try {
                // Extraire le contenu entre parenthèses et nettoyer
                String rgbContent = color.substring(4, color.length() - 1).replaceAll("\\s+", "");
                String[] rgbParts = rgbContent.split(",");

                if (rgbParts.length == 3) {
                    int r = Integer.parseInt(rgbParts[0]);
                    int g = Integer.parseInt(rgbParts[1]);
                    int b = Integer.parseInt(rgbParts[2]);

                    // Vérifier que les valeurs sont dans la plage 0-255
                    r = Math.min(255, Math.max(0, r));
                    g = Math.min(255, Math.max(0, g));
                    b = Math.min(255, Math.max(0, b));

                    return String.format("#%02X%02X%02X", r, g, b);
                }
            } catch (NumberFormatException e) {
                logger.warn("Impossible de parser la couleur RGB: {}", color);
            }
        }

        // Format rgba(r,g,b,a)
        if (color.startsWith("rgba(") && color.endsWith(")")) {
            try {

                String rgbaContent = color.substring(5, color.length() - 1).replaceAll("\\s+", "");
                String[] rgbaParts = rgbaContent.split(",");

                if (rgbaParts.length == 4) {
                    int r = Integer.parseInt(rgbaParts[0]);
                    int g = Integer.parseInt(rgbaParts[1]);
                    int b = Integer.parseInt(rgbaParts[2]);
                    // float alpha = Float.parseFloat(rgbaParts[3]); // Si besoin de l'alpha plus tard

                    r = Math.min(255, Math.max(0, r));
                    g = Math.min(255, Math.max(0, g));
                    b = Math.min(255, Math.max(0, b));

                    return String.format("#%02X%02X%02X", r, g, b);
                }
            } catch (NumberFormatException e) {
                logger.warn("Impossible de parser la couleur RGBA: {}", color);
            }
        }

        logger.warn("Format de couleur non reconnu: {}", color);
        return "#000000";
    }
    int convertCssValueToPixels(String cssValue) {
        if (cssValue == null || cssValue.trim().isEmpty()) {
            return 0;
        }
        if (cssValue.endsWith("px")) {
            try {
                return Integer.parseInt(cssValue.substring(0, cssValue.length() - 2));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        if (cssValue.endsWith("pt")) {
            try {
                // Conversion approximative de points en pixels (1pt ≈ 1.33px)
                return (int) (Float.parseFloat(cssValue.substring(0, cssValue.length() - 2)) * 1.33);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        if (cssValue.endsWith("em") || cssValue.endsWith("rem")) {
            try {
                return (int) (Float.parseFloat(cssValue.substring(0, cssValue.length() - 2)) * 16);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        try {
            return Integer.parseInt(cssValue);
        } catch (NumberFormatException e) {

            switch (cssValue.toLowerCase()) {
                case "xx-small":
                    return 8;
                case "x-small":
                    return 10;
                case "small":
                    return 12;
                case "medium":
                    return 16;
                case "large":
                    return 18;
                case "x-large":
                    return 24;
                case "xx-large":
                    return 32;
                default:
                    return 0;
            }
        }
    }

    public void applyJasperAttribute(JRDesignTextField textField, String attribute, String value) {
        try {
            switch (attribute) {
                case "fontName":
                    textField.setFontName(value);
                    break;
                case "fontSize":
                    textField.setFontSize(Float.parseFloat(value));
                    break;
                case "isBold":
                    textField.setBold(Boolean.parseBoolean(value));
                    break;
                case "isItalic":
                    textField.setItalic(Boolean.parseBoolean(value));
                    break;
                case "isUnderline":
                    textField.setUnderline(Boolean.parseBoolean(value));
                    break;
                case "isStrikeThrough":
                    textField.setStrikeThrough(Boolean.parseBoolean(value));
                    break;
                case "forecolor":
                    textField.setForecolor(Color.decode(value));
                    break;
                case "backcolor":
                    textField.setBackcolor(Color.decode(value));
                    textField.setMode(ModeEnum.OPAQUE);
                    break;
                case "border":
                    applyBorderToAll(textField, value);
                    break;
                case "borderTop":
                    applyBorderToSide(textField, value, LineDirection.TOP);
                    break;
                case "borderRight":
                    applyBorderToSide(textField, value, LineDirection.RIGHT);
                    break;
                case "borderBottom":
                    applyBorderToSide(textField, value, LineDirection.BOTTOM);
                    break;
                case "borderLeft":
                    applyBorderToSide(textField, value, LineDirection.LEFT);
                    break;
                case "borderStyle":
                    applyBorderStyleToAll(textField, value);
                    break;
                case "borderTopStyle":
                    textField.getLineBox().getTopPen().setLineStyle(getLineStyleEnum(value));
                    break;
                case "borderRightStyle":
                    textField.getLineBox().getRightPen().setLineStyle(getLineStyleEnum(value));
                    break;
                case "borderBottomStyle":
                    textField.getLineBox().getBottomPen().setLineStyle(getLineStyleEnum(value));
                    break;
                case "borderLeftStyle":
                    textField.getLineBox().getLeftPen().setLineStyle(getLineStyleEnum(value));
                    break;
                case "borderWidth":
                    applyBorderWidthToAll(textField, value);
                    break;
                case "borderTopWidth":
                    textField.getLineBox().getTopPen().setLineWidth(Float.parseFloat(value));
                    break;
                case "borderRightWidth":
                    textField.getLineBox().getRightPen().setLineWidth(Float.parseFloat(value));
                    break;
                case "borderBottomWidth":
                    textField.getLineBox().getBottomPen().setLineWidth(Float.parseFloat(value));
                    break;
                case "borderLeftWidth":
                    textField.getLineBox().getLeftPen().setLineWidth(Float.parseFloat(value));
                    break;

                case "borderColor":
                    logger.info("borderColor: " + value);
                    applyBorderColorToAll(textField, value);
                    break;
                case "borderTopColor":
                    logger.info("borderTopColor: " + value);
                    textField.getLineBox().getTopPen().setLineColor(Color.decode(value));
                    break;
                case "borderRightColor":
                    logger.info("borderRightColor: " + value);
                    textField.getLineBox().getRightPen().setLineColor(Color.decode(value));
                    break;
                case "borderBottomColor":
                    logger.info("borderBottomColor: " + value);
                    textField.getLineBox().getBottomPen().setLineColor(Color.decode(value));
                    break;
                case "borderLeftColor":
                    logger.info("borderLeftColor: " + value);
                    textField.getLineBox().getLeftPen().setLineColor(Color.decode(value));
                    break;

                case "horizontalAlignment":
                    switch (value) {
                        case "Left":
                            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
                            break;
                        case "Center":
                            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
                            break;
                        case "Right":
                            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
                            break;
                        case "Justified":
                            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.JUSTIFIED);
                            break;
                    }
                    break;
                case "verticalAlignment":
                    switch (value) {
                        case "Top":
                            textField.setVerticalTextAlign(VerticalTextAlignEnum.TOP);
                            break;
                        case "Middle":
                            textField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
                            break;
                        case "Bottom":
                            textField.setVerticalTextAlign(VerticalTextAlignEnum.BOTTOM);
                            break;
                    }
                    break;

                case "width":
                    textField.setWidth(Integer.parseInt(value));
                    break;
                case "height":
                    textField.setHeight(Integer.parseInt(value));
                    break;
                default:
            }


        } catch (Exception e) {
            logger.warn("Error applying Jasper attribute {}: {}", attribute, e.getMessage());
        }
    }

    private void applyBorderToAll(JRDesignTextField textField, String value) {
        if (value.equals("None")) {
            JRLineBox lineBox = textField.getLineBox();
            lineBox.getTopPen().setLineWidth(0f);
            lineBox.getRightPen().setLineWidth(0f);
            lineBox.getBottomPen().setLineWidth(0f);
            lineBox.getLeftPen().setLineWidth(0f);
            return;
        }

        String[] parts = value.split(",");
        if (parts.length != 3) {
            System.err.println("Erreur : format de bordure invalide.");
            return;
        }

        try {
            float width = Float.parseFloat(parts[0]);
            LineStyleEnum style = getLineStyleEnum(parts[1]);
            Color color = Color.decode(parts[2]);

            JRLineBox lineBox = textField.getLineBox();

            lineBox.getTopPen().setLineWidth(width);
            lineBox.getTopPen().setLineStyle(style);
            lineBox.getTopPen().setLineColor(color);

            lineBox.getRightPen().setLineWidth(width);
            lineBox.getRightPen().setLineStyle(style);
            lineBox.getRightPen().setLineColor(color);

            lineBox.getBottomPen().setLineWidth(width);
            lineBox.getBottomPen().setLineStyle(style);
            lineBox.getBottomPen().setLineColor(color);

            lineBox.getLeftPen().setLineWidth(width);
            lineBox.getLeftPen().setLineStyle(style);
            lineBox.getLeftPen().setLineColor(color);

        } catch (IllegalArgumentException e) {
            System.err.println("Erreur de conversion : " + e.getMessage());
        }
    }
    private void applyBorderStyleToAll(JRDesignTextField textField, String value) {
        String[] styles = value.split(",");
        if (styles.length == 4) {
            JRLineBox lineBox = textField.getLineBox();

            lineBox.getTopPen().setLineStyle(getLineStyleEnum(styles[0]));
            lineBox.getRightPen().setLineStyle(getLineStyleEnum(styles[1]));
            lineBox.getBottomPen().setLineStyle(getLineStyleEnum(styles[2]));
            lineBox.getLeftPen().setLineStyle(getLineStyleEnum(styles[3]));
        }
    }
    private void applyBorderWidthToAll(JRDesignTextField textField, String value) {
        String[] widths = value.split(",");
        if (widths.length == 4) {
            JRLineBox lineBox = textField.getLineBox();

            lineBox.getTopPen().setLineWidth(Float.parseFloat(widths[0]));
            lineBox.getRightPen().setLineWidth(Float.parseFloat(widths[1]));
            lineBox.getBottomPen().setLineWidth(Float.parseFloat(widths[2]));
            lineBox.getLeftPen().setLineWidth(Float.parseFloat(widths[3]));
        }
    }

    private void applyBorderColorToAll(JRDesignTextField textField, String value) {
        String[] colors = value.split(",");
        if (colors.length == 4) {
            JRLineBox lineBox = textField.getLineBox();

            lineBox.getTopPen().setLineColor(Color.decode(colors[0]));
            lineBox.getRightPen().setLineColor(Color.decode(colors[1]));
            lineBox.getBottomPen().setLineColor(Color.decode(colors[2]));
            lineBox.getLeftPen().setLineColor(Color.decode(colors[3]));
        }
    }



    public void applyJasperAttribute(JRDesignFrame frame, String attributeName, String attributeValue) {
        switch (attributeName.toLowerCase()) {
            case "border":
                applyBorderToFrame(frame, attributeValue);
                break;

            case "backcolor":
                try {
                    Color bgColor = Color.decode((attributeValue));
                    frame.setMode(ModeEnum.OPAQUE);
                    frame.setBackcolor(bgColor);
                } catch (Exception e) {
                    logger.warn("Couleur de fond invalide : {}", attributeValue);
                }
                break;
            case "forecolor":
                try {
                    Color fgColor = Color.decode(attributeValue);
                    frame.setForecolor(fgColor);
                } catch (Exception e) {
                    logger.warn("Couleur de texte invalide : {}", attributeValue);
                }
                break;

            default:
                break;

        }
    }
    private void applyBorderToFrame(JRDesignFrame frame, String borderValue) {
        if (borderValue == null || borderValue.trim().isEmpty()) {
            logger.warn("Valeur de bordure vide ou null");
            return;
        }
        try {
            String[] parts = borderValue.split(",");
            if (parts.length != 3) {
                logger.warn("Format de bordure invalide : {}", borderValue);
                return;
            }
            float borderWidth = Float.parseFloat(parts[0]);
            LineStyleEnum borderStyle = getLineStyleEnum(parts[1]);

            Color borderColor;
            try {
                borderColor = Color.decode(parts[2]);
            } catch (NumberFormatException e) {
                logger.warn("Couleur de bordure invalide : {}", parts[2]);
                borderColor = Color.BLACK;
            }
            applyBorderToBox(frame.getLineBox(), borderWidth, borderStyle, borderColor);

            logger.info("Bordure appliquée au frame : largeur={}, style={}, couleur={}",
                    borderWidth, borderStyle, borderColor);

            } catch (Exception e) {
            logger.error("Erreur lors de l'application de la bordure : {}", e.getMessage(), e);
        }
    }

    private void applyBorderToBox(JRLineBox lineBox, float width, LineStyleEnum style, Color color) {

        lineBox.getPen().setLineWidth(width);
        lineBox.getPen().setLineStyle(style);
        lineBox.getPen().setLineColor(color);

        lineBox.getTopPen().setLineWidth(width);
        lineBox.getTopPen().setLineStyle(style);
        lineBox.getTopPen().setLineColor(color);

        lineBox.getLeftPen().setLineWidth(width);
        lineBox.getLeftPen().setLineStyle(style);
        lineBox.getLeftPen().setLineColor(color);

        lineBox.getBottomPen().setLineWidth(width);
        lineBox.getBottomPen().setLineStyle(style);
        lineBox.getBottomPen().setLineColor(color);

        lineBox.getRightPen().setLineWidth(width);
        lineBox.getRightPen().setLineStyle(style);
        lineBox.getRightPen().setLineColor(color);
    }

    private LineStyleEnum getLineStyleEnum(String style) {
        return switch (style.toLowerCase()) {
            case "dotted" -> LineStyleEnum.DOTTED;
            case "dashed" -> LineStyleEnum.DASHED;
            case "double" -> LineStyleEnum.DOUBLE;
            default -> LineStyleEnum.SOLID;
        };
    }

    private void applyBorderToSide(JRDesignTextField textField, String value, LineDirection direction) {
        JRPen pen;
        switch (direction) {
            case TOP:
                pen = textField.getLineBox().getTopPen();
                break;
            case RIGHT:
                pen = textField.getLineBox().getRightPen();
                break;
            case BOTTOM:
                pen = textField.getLineBox().getBottomPen();
                break;
            case LEFT:
                pen = textField.getLineBox().getLeftPen();
                break;
            default:
                return;
        }

        if (value.equals("None")) {
            pen.setLineWidth(0f);
            return;
        }

        String[] parts = value.split(",");
        if (parts.length >= 3) {
            float width = Float.parseFloat(parts[0]);
            LineStyleEnum style = getLineStyleEnum(parts[1]);
            Color color = Color.decode(parts[2]);

            pen.setLineWidth(width);
            pen.setLineStyle(style);
            pen.setLineColor(color);
        }
    }
}