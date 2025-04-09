package com.example.genererapport.Service;

import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
@Service
public class BorderManagementService {
    private static final Logger logger = LoggerFactory.getLogger(BorderManagementService.class);

    /**
     * Vérifie si un élément a des styles de bordure définis
     */
    public boolean hasBorderStyles(Map<String, String> styles) {
        return styles.containsKey("border") ||
                styles.containsKey("border-top") ||
                styles.containsKey("border-right") ||
                styles.containsKey("border-bottom") ||
                styles.containsKey("border-left");
    }

    /**
     * Vérifie si un JRDesignFrame a des bordures définies
     */
    public boolean hasBorder(JRDesignFrame frame) {
        if (frame == null) return false;

        JRLineBox lineBox = frame.getLineBox();
        return lineBox.getTopPen().getLineWidth() > 0 ||
                lineBox.getRightPen().getLineWidth() > 0 ||
                lineBox.getBottomPen().getLineWidth() > 0 ||
                lineBox.getLeftPen().getLineWidth() > 0;
    }

    /**
     * Supprime les styles de bordure qui seraient dupliqués
     */
    public Map<String, String> removeDuplicateBorders(Map<String, String> parentStyles, Map<String, String> childStyles) {
        // Si aucun des deux n'a de bordures, rien à faire
        if (!hasBorderStyles(parentStyles) || !hasBorderStyles(childStyles)) {
            return childStyles;
        }

        Map<String, String> result = new HashMap<>(childStyles);

        // Vérifier la bordure complète
        if (parentStyles.containsKey("border")) {
            result.remove("border");
            // Si le parent a une bordure globale, supprimer aussi les bordures par côté
            result.remove("border-top");
            result.remove("border-right");
            result.remove("border-bottom");
            result.remove("border-left");
            return result;
        }

        // Vérifier chaque côté individuellement
        String[] sides = {"top", "right", "bottom", "left"};
        for (String side : sides) {
            if (parentStyles.containsKey("border-" + side)) {
                result.remove("border-" + side);
            }
        }

        return result;
    }

    /**
     * Supprime complètement toutes les bordures
     */
    public Map<String, String> removeAllBorders(Map<String, String> styles) {
        Map<String, String> result = new HashMap<>(styles);
        result.remove("border");
        result.remove("border-top");
        result.remove("border-right");
        result.remove("border-bottom");
        result.remove("border-left");

        // Également supprimer les propriétés spécifiques des bordures
        result.remove("border-style");
        result.remove("border-color");
        result.remove("border-width");

        for (String side : new String[]{"top", "right", "bottom", "left"}) {
            result.remove("border-" + side + "-style");
            result.remove("border-" + side + "-color");
            result.remove("border-" + side + "-width");
        }

        return result;
    }

    /**
     * Détermine si les bordures de l'enfant doivent être conservées ou supprimées
     * en fonction des bordures du parent et de la stratégie choisie
     */
    public Map<String, String> manageBorderInheritance(JRDesignFrame parentFrame, Map<String, String> parentStyles, Map<String, String> childStyles) {
        // Si le parent n'a pas de bordure, conserver les bordures de l'enfant
        if (!hasBorder(parentFrame) && !hasBorderStyles(parentStyles)) {
            return childStyles;
        }

        // Sinon, supprimer les bordures de l'enfant
        return removeAllBorders(childStyles);
    }
}
