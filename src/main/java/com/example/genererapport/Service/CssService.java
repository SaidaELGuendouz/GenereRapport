package com.example.genererapport.Service;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.parser.CSSOMParser;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
@Service
public class CssService {
    private static final Logger logger = LoggerFactory.getLogger(CssService.class);
    private static int idCounter = 1;
    private Map<String, Map<String, String>> cssRules;
    private String basePath;

    // Extrait les styles CSS internes d'un document HTML.
    //Parcourt les balises <style> du document, récupère leur contenu CSS et le transmet pour analyse.
    void extractInternalCss(Document doc) {
        logger.info("Extraction des styles CSS internes...");
        Elements styleElements = doc.select("style");
        for (Element styleElement : styleElements) {
            String cssContent = styleElement.html();
            if (!cssContent.isEmpty()) {
                logger.info("Styles CSS internes trouvées : " + cssContent);

                parseCssContent(cssContent);
            }else {
                logger.info("Styles CSS internes non trouvées");
            }

        }

    }
   // Parse le contenu CSS, identifie les sélecteurs et leurs propriétés et les stocke dans une map (cssRules)
    private void parseCssContent(String cssContent) {
        try {
            CSSOMParser parser = new CSSOMParser();
            InputSource source = new InputSource(new StringReader(cssContent));
            CSSStyleSheetImpl stylesheet = (CSSStyleSheetImpl) parser.parseStyleSheet(source, null, null);

            CSSRuleList ruleList = stylesheet.getCssRules();
            for (int i = 0; i < ruleList.getLength(); i++) {
                CSSRule rule = ruleList.item(i);
                if (rule instanceof CSSStyleRule) {
                    CSSStyleRule styleRule = (CSSStyleRule) rule;
                    String selectorText = styleRule.getSelectorText();
                    logger.info("CSSRule: " + rule.getCssText());


                    for (String selector : selectorText.split(",")) {
                        selector = selector.trim();
                        if (!selector.isEmpty()) {
                            CSSStyleDeclaration styleDeclaration = styleRule.getStyle();
                            Map<String, String> properties = new HashMap<>();
                            for (int j = 0; j < styleDeclaration.getLength(); j++) {
                                String propertyName = styleDeclaration.item(j);
                                String propertyValue = styleDeclaration.getPropertyValue(propertyName);
                                properties.put(propertyName, propertyValue);
                            }
                            cssRules.put(selector, properties);
                            logger.info("Règle CSS trouvée pour le sélecteur: " + selector);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Erreur lors du parsing du CSS : " + e.getMessage(), e, Level.WARNING);
        }
    }
        // * Extrait les styles CSS externes d'un document HTML.
        // * Recherche les balises <link> ayant l'attribut rel="stylesheet",
        // * charge le contenu des fichiers CSS associés, et les transmet pour analyse.
    void extractExternalCss(Document doc) {
        logger.info("Extraction des styles CSS externes...");
        Elements linkElements = doc.select("link[rel=stylesheet]");
        for (Element linkElement : linkElements) {
            String href = linkElement.attr("href");
            if (!href.isEmpty()) {
                try {
                    String cssContent = loadExternalCss(href);
                    if (cssContent != null && !cssContent.isEmpty()) {
                        parseCssContent(cssContent);
                        logger.info("CSS externe chargé depuis: " + href);
                    }
                }catch (Exception e) {
                    logger.info("Impossible de charger le CSS externe: " + href, e, Level.WARNING);
                }
            }
        }
    }
    //Charge le contenu d'un fichier CSS externe à partir de son chemin.
    private String loadExternalCss(String href) throws IOException {
        if (basePath == null || basePath.isEmpty()) {
            throw new IOException("Le chemin de base (basePath) est nul ou vide.");
        }

        Path path = href.startsWith("/") ? Paths.get(basePath, href.substring(1)) : Paths.get(basePath, href);

        if (Files.exists(path)) {
            try {
                return new String(Files.readAllBytes(path));
            } catch (IOException e) {
                logger.info("Fichier CSS non trouvé: " + path,e,Level.WARNING);
                throw e;
            }
        } else {
            logger.info("Fichier CSS inexistant: " + path,Level.WARNING);
            throw new IOException("Le fichier CSS n'existe pas: " + path);
        }

    }
    //Extrait les styles CSS en ligne des éléments HTML.
    // * Parcourt les éléments avec l'attribut "style" pour extraire les propriétés CSS
    // * et les stocke dans une structure de données sous forme de règles associées à leurs ID.

    void extractInlineCss(Document doc) {

        logger.info("Extraction des styles CSS en ligne...");
        Elements elementsWithStyle = doc.select("[style]");
        for (Element element : elementsWithStyle) {
            String style = element.attr("style");
            if ( !style.isEmpty()) {
                logger.info("Style en ligne trouvé : " + style);

                String elementId = element.id();
                if (elementId.isEmpty()) {
                    elementId = generateElementId(element);
                    element.attr("id", elementId);
                }
                Map<String, String> properties = parseInlineStyle(style);

                logger.info(" Propriétés extraites:");
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    logger.info("  - " + entry.getKey() + ": " + entry.getValue());
                }
                cssRules.put("#" + elementId, properties);

                logger.info("Style en ligne trouvé pour #" + elementId + ": " + style);
            }
        }
    }
   //Parse les propriétés CSS en ligne et les stocke dans une map
    private Map<String, String> parseInlineStyle(String styleContent) {
        Map<String, String> properties = new HashMap<>();

        String[] declarations = styleContent.split(";");
        for (String declaration : declarations) {
            declaration = declaration.trim();
            if ( declaration.contains(":")) {
                String[] parts = declaration.split(":", 2);
                String propertyName = parts[0].trim();
                String propertyValue = parts[1].trim();
                properties.put(propertyName, propertyValue);
            }
        }

        return properties;
    }

    private String generateElementId(Element element) {
        String tagName = element.tagName();
        String className = element.className();

        String baseId = tagName;

        if (!className.isEmpty()) {
            baseId += "-" + className.replaceAll("\\s+", "-");
        }
        baseId += "-" + idCounter++;
        return baseId;
    }

    /**
     * Extrait tous les styles CSS (externes, internes et en ligne) d'un document HTML.
     * Les styles sont extraits dans l'ordre de priorité croissant : externes, internes, puis en ligne,
     */
    public Map<String, Map<String, String>> extractAllCssFromDocument(Document doc, String basePath) {
        // Initialiser la map des règles CSS et le chemin de base pour les fichiers externes
        this.cssRules = new HashMap<>();
        this.basePath = basePath;

        // Extraction des styles dans l'ordre de priorité (faible à forte)
        extractExternalCss(doc);
        extractInternalCss(doc);
        extractInlineCss(doc);

        // Retourner toutes les règles CSS extraites
        return this.cssRules;
    }
    /**
     * Récupère les styles CSS explicitement définis sur un élément HTML.
     * Cela inclut uniquement les styles définis directement via l'attribut 'style'
     * ou par des règles CSS ciblant spécifiquement cet élément.
     *
     * @param element L'élément HTML pour lequel récupérer les styles explicites
     * @return Une Map contenant les paires propriété/valeur des styles explicitement définis
     */
    public Map<String, String> getExplicitStyles(Element element) {
        validateCssRules();
        Map<String, String> explicitStyles = new HashMap<>();

        String id = element.id();
        if (!id.isEmpty()) {
            applyRuleIfExists(explicitStyles, "#" + id);
        }

        String classNames = element.className();
        if (!classNames.isEmpty()) {
            for (String className : classNames.split("\\s+")) {
                applyRuleIfExists(explicitStyles, "." + className);
            }
        }
        String inlineStyle = element.attr("style");
        if (!inlineStyle.isEmpty()) {
            Map<String, String> inlineProperties = parseInlineStyle(inlineStyle);
            explicitStyles.putAll(inlineProperties);
        }
        return explicitStyles;
    }
    public Map<String, String> getComputedStyles(Element element) {
        validateCssRules();

        Map<String, String> computedStyles = new HashMap<>();

        applyMatchingRules(computedStyles, element);

        computedStyles = applyInheritedStyles(computedStyles, element);

        addInlineStyles(computedStyles, element);

        return computedStyles;
    }
    private void validateCssRules() {
        if (cssRules == null || cssRules.isEmpty()) {
            throw new IllegalStateException("Aucune règle CSS extraite. Appelez extractAllCssFromDocument() d'abord.");
        }
    }
    /**
     * Applique les règles CSS correspondantes à un élément HTML en fonction de son tag,
     * de ses classes, de son ID et des sélecteurs complexes dans le document.
     * L'ordre d'application est le suivant :
     * 1. On applique d'abord les règles liées au tag de l'élément.
     * 2. Ensuite, on applique les règles liées aux classes de l'élément.
     * 3. Puis, on applique les règles liées à l'ID de l'élément.
     * 4. Enfin, on applique les règles CSS pour les sélecteurs complexes, qui peuvent être plus spécifiques et dépendent des relations contextuelles entre les éléments.
     */
    private void applyMatchingRules(Map<String, String> styles, Element element) {
        List<String> complexSelectors = cssRules.keySet().stream()
                .filter(selector ->
                        selector.contains(" ") ||
                                selector.contains(">") ||
                                selector.contains("+") ||
                                selector.contains("~")
                )
                .collect(Collectors.toList());

        // 1. Appliquer les règles par tag
        String tagName = element.tagName().toLowerCase();
        applyRuleIfExists(styles, tagName);

        // 2. Appliquer les règles par classe
        String classNames = element.className();
        if (!classNames.isEmpty()) {
            for (String className : classNames.split("\\s+")) {
                applyRuleIfExists(styles, "." + className);
            }
        }

        // 3. Appliquer les règles par ID
        String id = element.id();
        if (!id.isEmpty()) {
            applyRuleIfExists(styles, "#" + id);
        }

        // 4. Appliquer les règles contextuelles complexes
        for (String selector : complexSelectors) {
            try {
                Elements matchingElements = element.ownerDocument().select(selector);
                if (matchingElements.contains(element)) {
                    applyRuleIfExists(styles, selector);
                }
            } catch (Exception e) {
                logger.warn("Erreur lors de l'évaluation du sélecteur complexe '{}': {}", selector, e.getMessage());
            }
        }
    }
    private void applyRuleIfExists(Map<String, String> styles, String selector) {
        Map<String, String> ruleProperties = cssRules.get(selector);
        if (ruleProperties != null) {
            styles.putAll(ruleProperties);
        }
    }
    private Map<String, String> applyInheritedStyles(Map<String, String> originalStyles, Element element) {
        Map<String, String> styles = new HashMap<>(originalStyles);
        Element parent = element.parent();
        while (parent != null) {
            Map<String, String> parentStyles = getComputedStyles(parent);
            styles.putAll(parentStyles);
            parent = parent.parent();
        }
        return styles;
    }
    private void addInlineStyles(Map<String, String> styles, Element element) {
        String inlineStyle = element.attr("style");
        if (!inlineStyle.isEmpty()) {
            Map<String, String> inlineProperties = parseInlineStyle(inlineStyle);
            styles.putAll(inlineProperties);
        }
    }
}


