package com.example.genererapport.Service;

import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PositionCalculator {
    @Autowired
    private ConvertCssToJasperService convertCssToJasperService;
    @Autowired
    private CssService cssService;
    private Map<String, Map<String, Integer>> elementPositions = new HashMap<>();

    Map<String, Integer> calculatePositionAndSize(Element element, Map<String, String> computedStyles) {
        Map<String, Integer> result = new HashMap<>();

        int posX = 0;
        int posY = 0;
        int baseY=0;
        int baseX=0;

        Map<String, Integer> margins = calculateMargin(computedStyles);
        int marginLeft = margins.getOrDefault("marginLeft", 0);
        int marginTop = margins.getOrDefault("marginTop", 0);


        int width =computedStyles.containsKey("width")
                ?convertCssToJasperService.convertCssValueToPixels(computedStyles.get("width"))
                : 100;
        int height =computedStyles.containsKey("height")
                ?convertCssToJasperService.convertCssValueToPixels(computedStyles.get("height"))
                : 40;

        Map<String, Integer> padding = calculatePadding(computedStyles);
        int paddingLeft = padding.getOrDefault("paddingLeft", 0);
        int paddingRight = padding.getOrDefault("paddingRight", 0);
        int paddingTop = padding.getOrDefault("paddingTop", 0);
        int paddingBottom = padding.getOrDefault("paddingBottom", 0);


        int UpdateWidth = width + paddingLeft + paddingRight;
        int UpdateHeight = height + paddingTop + paddingBottom;
        if (computedStyles.containsKey("position")) {
            String position = computedStyles.get("position");
            if ("relative".equals(position)) {

                if (computedStyles.containsKey("left")) {
                    posX += convertCssToJasperService.convertCssValueToPixels(computedStyles.get("left"));
                } else if (computedStyles.containsKey("right")) {
                    posX -= convertCssToJasperService.convertCssValueToPixels(computedStyles.get("right"));
                }

                if (computedStyles.containsKey("top")) {
                    posY += convertCssToJasperService.convertCssValueToPixels(computedStyles.get("top"));
                } else if (computedStyles.containsKey("bottom")) {
                    posY -= convertCssToJasperService.convertCssValueToPixels(computedStyles.get("bottom"));
                }

            } else if ("absolute".equals(position)) {

                String AncetreId = findAncetreWithPosition(element);
                if (AncetreId != null && elementPositions.containsKey(AncetreId)) {
                    Map<String, Integer> ancestorPosition = elementPositions.get(AncetreId);
                    baseX = ancestorPosition.get("x");
                    baseY = ancestorPosition.get("y");
                    if (computedStyles.containsKey("left")) {
                        posX = baseX + convertCssToJasperService.convertCssValueToPixels(computedStyles.get("left"));
                    }

                    if (computedStyles.containsKey("top")) {
                        posY = baseY + convertCssToJasperService.convertCssValueToPixels(computedStyles.get("top"));
                    }


                    if (!computedStyles.containsKey("left") && computedStyles.containsKey("right")) {
                        int AncetreWidth = getDimension(AncetreId, "width", 500);
                        int rightOffset = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("right"));
                        posX = baseX + AncetreWidth - UpdateWidth - rightOffset;
                    }

                    if (!computedStyles.containsKey("top") && computedStyles.containsKey("bottom")) {
                        int AncetreHeight = getDimension(AncetreId, "height", 300);
                        int bottomOffset = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("bottom"));
                        posY = baseY + AncetreHeight - UpdateHeight - bottomOffset;
                    }
                }
            }
        }
        posX += marginLeft;
        posY += marginTop;
        result.put("x", posX);
        result.put("y", posY);
        result.put("width",UpdateWidth);
        result.put("height",UpdateHeight);

        return result;
        }
    private String findAncetreWithPosition(Element element) {
        Element currentElement = element.parent();
        while (currentElement != null) {
            Map<String, String> computedStyles = cssService.getComputedStyles(currentElement);

            if (computedStyles.containsKey("position")) {
                String position = computedStyles.get("position");
                if (!"static".equals(position)) {
                    Map<String, Integer> AncetrePosition = calculatePositionAndSize(currentElement, computedStyles);
                    elementPositions.put(currentElement.id(), AncetrePosition);
                    return currentElement.id();
                }
            }
            currentElement = currentElement.parent();
        }

        return null;
    }
    private int getDimension(String parentId, String dimensionType, int defaultValue) {
        if (parentId != null && elementPositions.containsKey(parentId) &&
                elementPositions.get(parentId).containsKey(dimensionType)) {
            return elementPositions.get(parentId).get(dimensionType);
        }
        return defaultValue;
    }

    private Map<String, Integer> calculateMargin(Map<String, String> computedStyles) {
        Map<String, Integer> marginMap = new HashMap<>();
        int marginLeft = 0, marginTop = 0;
        if (computedStyles.containsKey("margin-left")) {
            marginLeft = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("margin-left"));
        }

        if (computedStyles.containsKey("margin-top")) {
            marginTop = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("margin-top"));
        }

        if ((!computedStyles.containsKey("margin-left") || !computedStyles.containsKey("margin-top"))
                && computedStyles.containsKey("margin")) {
            String[] margins = computedStyles.get("margin").split("\\s+");

            switch (margins.length) {
                case 1: // margin: all (ex: "10px")
                    int marginAll = convertCssToJasperService.convertCssValueToPixels(margins[0]);
                    if (!computedStyles.containsKey("margin-top")) marginTop = marginAll;
                    if (!computedStyles.containsKey("margin-left")) marginLeft = marginAll;
                    break;

                case 2: // margin: vertical horizontal (ex: "10px 20px")
                    int marginVertical = convertCssToJasperService.convertCssValueToPixels(margins[0]);
                    int marginHorizontal = convertCssToJasperService.convertCssValueToPixels(margins[1]);
                    if (!computedStyles.containsKey("margin-top")) marginTop = marginVertical;
                    if (!computedStyles.containsKey("margin-left")) marginLeft = marginHorizontal;
                    break;

                case 3: // margin: top horizontal bottom (ex: "10px 20px 30px")
                    int marginBottom = convertCssToJasperService.convertCssValueToPixels(margins[2]);
                    if (!computedStyles.containsKey("margin-top")) marginTop = convertCssToJasperService.convertCssValueToPixels(margins[0]);
                    if (!computedStyles.containsKey("margin-left")) marginLeft = convertCssToJasperService.convertCssValueToPixels(margins[1]);
                    break;

                case 4: // margin: top right bottom left (ex: "10px 20px 30px 40px")
                    if (!computedStyles.containsKey("margin-top"))
                        marginTop = convertCssToJasperService.convertCssValueToPixels(margins[0]);
                    if (!computedStyles.containsKey("margin-left"))
                        marginLeft = convertCssToJasperService.convertCssValueToPixels(margins[3]);
                    break;
            }
        }
        marginMap.put("marginTop", marginTop);
        marginMap.put("marginLeft", marginLeft);

        return marginMap;
    }
    private Map<String, Integer> calculatePadding(Map<String, String> computedStyles) {
        Map<String, Integer> paddingMap = new HashMap<>();
        int paddingTop = 0, paddingRight = 0, paddingBottom = 0, paddingLeft = 0;

        if (computedStyles.containsKey("padding-top")) {
            paddingTop = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("padding-top"));
        }

        if (computedStyles.containsKey("padding-right")) {
            paddingRight = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("padding-right"));
        }

        if (computedStyles.containsKey("padding-bottom")) {
            paddingBottom = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("padding-bottom"));
        }

        if (computedStyles.containsKey("padding-left")) {
            paddingLeft = convertCssToJasperService.convertCssValueToPixels(computedStyles.get("padding-left"));
        }
        // Vérification du padding global si des valeurs spécifiques ne sont pas définies
        if (computedStyles.containsKey("padding")) {
            String[] paddings = computedStyles.get("padding").split("\\s+");

            switch (paddings.length) {
                case 1: // padding: all (ex: "10px")
                    int paddingAll = convertCssToJasperService.convertCssValueToPixels(paddings[0]);
                    if (!computedStyles.containsKey("padding-top")) paddingTop = paddingAll;
                    if (!computedStyles.containsKey("padding-right")) paddingRight = paddingAll;
                    if (!computedStyles.containsKey("padding-bottom")) paddingBottom = paddingAll;
                    if (!computedStyles.containsKey("padding-left")) paddingLeft = paddingAll;
                    break;

                case 2: // padding: vertical horizontal (ex: "10px 20px")
                    int paddingVertical = convertCssToJasperService.convertCssValueToPixels(paddings[0]);
                    int paddingHorizontal = convertCssToJasperService.convertCssValueToPixels(paddings[1]);
                    if (!computedStyles.containsKey("padding-top")) paddingTop = paddingVertical;
                    if (!computedStyles.containsKey("padding-right")) paddingRight = paddingHorizontal;
                    if (!computedStyles.containsKey("padding-bottom")) paddingBottom = paddingVertical;
                    if (!computedStyles.containsKey("padding-left")) paddingLeft = paddingHorizontal;
                    break;

                case 3: // padding: top horizontal bottom (ex: "10px 20px 30px")
                    int paddingTopValue = convertCssToJasperService.convertCssValueToPixels(paddings[0]);
                    int paddingHoriz = convertCssToJasperService.convertCssValueToPixels(paddings[1]);
                    int paddingBottomValue = convertCssToJasperService.convertCssValueToPixels(paddings[2]);
                    if (!computedStyles.containsKey("padding-top")) paddingTop = paddingTopValue;
                    if (!computedStyles.containsKey("padding-right")) paddingRight = paddingHoriz;
                    if (!computedStyles.containsKey("padding-bottom")) paddingBottom = paddingBottomValue;
                    if (!computedStyles.containsKey("padding-left")) paddingLeft = paddingHoriz;
                    break;

                case 4: // padding: top right bottom left (ex: "10px 20px 30px 40px")
                    if (!computedStyles.containsKey("padding-top"))
                        paddingTop = convertCssToJasperService.convertCssValueToPixels(paddings[0]);
                    if (!computedStyles.containsKey("padding-right"))
                        paddingRight = convertCssToJasperService.convertCssValueToPixels(paddings[1]);
                    if (!computedStyles.containsKey("padding-bottom"))
                        paddingBottom = convertCssToJasperService.convertCssValueToPixels(paddings[2]);
                    if (!computedStyles.containsKey("padding-left"))
                        paddingLeft = convertCssToJasperService.convertCssValueToPixels(paddings[3]);
                    break;
            }
        }

        // Stockage des résultats dans une Map
        paddingMap.put("paddingTop", paddingTop);
        paddingMap.put("paddingRight", paddingRight);
        paddingMap.put("paddingBottom", paddingBottom);
        paddingMap.put("paddingLeft", paddingLeft);

        return paddingMap;
    }















}
