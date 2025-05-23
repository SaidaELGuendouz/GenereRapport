package com.example.genererapport.Service;

import com.example.genererapport.Request.XhtmlRequest;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.sf.jasperreports.engine.design.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class RapportService {
    private static final Logger logger = LoggerFactory.getLogger(RapportService.class);
    private JasperDesign jasperDesign;
    private String BandType;
    private Map<String, String> BandStyles;
    @Autowired
    private BorderManagementService borderService;
    @Autowired
    private JasperReportVariableExtractorService jasperReportVariableExtractorService;
    @Autowired
    private CssService cssService;
    @Autowired
    private ConvertCssToJasperService convertCssToJasperService;
    @Autowired
    private JasperReportService jasperReportService;
    @Autowired
    private PositionCalculator positionCalculator;

    public String convertXhtmlToJrxml(XhtmlRequest request) throws Exception {
        try {
            String xhtmlTemplate = request.getXhtmlContent();
            String basePath = request.getBasePath();
            Map<String, XhtmlRequest.Parameter> parameters = request.getParameters();

            Document doc = Jsoup.parse(xhtmlTemplate);
            cssService.extractAllCssFromDocument(doc, "");
            String jrxmlContent = jasperReportService.createEmptyJrxmlDocument();
            jasperDesign = JRXmlLoader.load(new ByteArrayInputStream(jrxmlContent.getBytes(StandardCharsets.UTF_8)));
            jasperReportVariableExtractorService.initialize(parameters, jasperDesign);
            logger.debug("JasperDesign initialisé avec succès");
             processBand("title", this::createTitleBand, doc);
            processBand("pageheader", this::createPageHeaderBand, doc);
            processBand("footer", this::createFooterBand, doc);
            createDetailBand(doc);
            logger.info("detailBand crée");

            logger.info("Parsing XHTML et CSS terminé avec succès");
            return JRXmlWriter.writeReport(jasperDesign, "UTF-8");
        } catch (Exception e) {
            logger.info("Erreur lors du parsing XHTML: " + e.getMessage(), e);
            throw new RuntimeException("Erreur de parsing XHTML: " + e.getMessage(), e);
        }
    }

    private void processBand(String tagName, Consumer<Element> bandCreator, Document doc) {
        Elements elements = doc.select(tagName);
        if (!elements.isEmpty()) {
            BandType = tagName;
            BandStyles = cssService.getComputedStyles(elements.first());
            bandCreator.accept(elements.first());

            BandType = null;
            BandStyles = null;
        } else {
            logger.warn("Aucun élément {} trouvé dans le document", tagName);
        }
    }
    private void createTitleBand(Element element) {
        logger.info("Création de la bande de titre à partir de l'élément: {}", element.tagName());
        JRDesignBand titleBand = new JRDesignBand();
        int bandHeight = jasperReportService.calculateBandHeight(element, "title");
        titleBand.setHeight(bandHeight);
        String innerHtml = element.html();
        logger.debug("Contenu HTML original: {}", innerHtml);

        boolean processed = false;

        // Cas 1: HTML échappé détecté
        if (innerHtml.contains("&lt;") || innerHtml.contains("&gt;")) {
            logger.debug("Contenu HTML échappé détecté dans title, tentative de correction");

            try {
                String unescaped = innerHtml
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'");

                logger.info("Contenu après remplacement manuel: {}", unescaped);

                Document tempDoc = Jsoup.parse("<wrapper>" + unescaped + "</wrapper>");
                Element wrapper = tempDoc.select("wrapper").first();

                if (wrapper != null && wrapper.children().size() > 0) {
                    Elements extractedElements = wrapper.children();
                    logger.info("Éléments extraits: {}", extractedElements.outerHtml());

                    // Continuer avec ces éléments
                    for (Element extracted : extractedElements) {
                        logger.debug("Traitement de l'élément: {}", extracted.tagName());

                        // Calculer les styles pour cet élément
                        Map<String, String> computedStyles = cssService.getComputedStyles(extracted);

                        // Traiter l'élément avec ses styles
                        processElement(extracted, titleBand, "title", computedStyles);
                    }
                    processed = true;
                }
            } catch (Exception e) {
                logger.warn("Erreur lors de la correction du contenu HTML: " + e.getMessage(), e);
            }
        }

        // Cas 2: HTML non échappé mais contenant des éléments HTML
        else if (!element.children().isEmpty()) {
            logger.debug("Éléments HTML directs trouvés dans le titre");
            processChildElements(element, titleBand, "title");
            processed = true;
        }

        // Cas 3: Texte simple sans HTML
        else if (!element.text().trim().isEmpty()) {
            logger.debug("Texte simple trouvé dans le titre: {}", element.text());

            // Créer un élément de texte statique
            JRDesignStaticText staticText = new JRDesignStaticText();
            staticText.setText(element.text());

            // Appliquer les styles
            Map<String, String> styles = cssService.getComputedStyles(element);
            applyTextStyles(staticText, styles);
            Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, styles);

            // Positionner l'élément
            staticText.setX(position.get("x"));
            staticText.setY(position.get("y"));
            staticText.setWidth(position.get("width"));
            staticText.setHeight(position.get("height"));

            titleBand.addElement(staticText);
            processed = true;
        }
        // Cas 4: Aucun contenu trouvé
        if (!processed) {
            logger.debug("Aucun contenu trouvé ou traité, utilisation du traitement standard");
            processChildElements(element, titleBand, "title");
        }
        try {
            jasperDesign.setTitle(titleBand);
            logger.debug("Bande de titre ajoutée au design Jasper");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du titre: " + e.getMessage(), e);
        }
    }

    private void applyTextStyles(JRDesignStaticText staticText, Map<String, String> styles) {
        if (staticText == null || styles == null || styles.isEmpty()) {
            logger.debug("Aucun style à appliquer");
            return;
        }

        for (Map.Entry<String, String> style : styles.entrySet()) {
            try {
                Map.Entry<String, String> jasperAttribute =
                        convertCssToJasperService.convertCssToJasperAttribute(style.getKey(), style.getValue());

                if (jasperAttribute != null) {
                    convertCssToJasperService.applyJasperAttribute(staticText, jasperAttribute.getKey(), jasperAttribute.getValue());
                } else {
                    logger.warn("Style ignoré  : {} = {}", style.getKey(), style.getValue());
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'application du style {} : {}", style.getKey(), e.getMessage());
            }
        }
    }

    private void createPageHeaderBand(Element element) {

        JRDesignBand pageHeaderBand = new JRDesignBand();
        int bandHeight = jasperReportService.calculateBandHeight(element, "pageheader");
        pageHeaderBand.setHeight(bandHeight);

        processChildElements(element, pageHeaderBand, "pageheader");
        try {
            jasperDesign.setPageHeader(pageHeaderBand);
            logger.debug("Bande d'en-tête ajoutée au design Jasper");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du pageHeader : " + e.getMessage(), e);
        }

    }

    private void createFooterBand(Element element) {
        JRDesignBand pageFooterBand = new JRDesignBand();
        int bandHeight = jasperReportService.calculateBandHeight(element, "pagefooter");
        pageFooterBand.setHeight(bandHeight);
        processChildElements(element, pageFooterBand, "pagefooter");
        try {
            jasperDesign.setPageFooter(pageFooterBand);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du pageFooter: " + e.getMessage(), e);

        }
    }

    private void createDetailBand(Document doc) {
        logger.debug("Début de la création de la bande de détails");
        JRDesignBand detailBand = new JRDesignBand();
        Document workDoc = doc.clone();

        workDoc.select("title, pageheader, footer").remove();
        Elements bodyElements = workDoc.select("body > *");

        if (bodyElements.isEmpty()) {
            logger.warn("Aucun élément trouvé pour la bande de détails après exclusion");
            return;
        }
        Elements divs = bodyElements.select("div");
        logger.info("Nombre de divs trouvés dans detail: {}", divs.size());

        BandType = "detail";
        int totalBandHeight = 0;
        Map<String, String> parentStyles = new HashMap<>();

        for (Element element : bodyElements) {
            try {

                BandStyles = cssService.getComputedStyles(element);
                Map<String, String> mergedStyles = new HashMap<>(parentStyles);
                mergedStyles.putAll(BandStyles);

                processElement(element, detailBand, "detail", mergedStyles);

                int elementHeight = jasperReportService.calculateBandHeight(element, "detail");
                totalBandHeight += Math.max(elementHeight, 50);

            } catch (Exception e) {
                logger.error("Erreur lors du traitement de l'élément {}: {}", element.tagName(), e.getMessage());
            }
        }

        detailBand.setHeight(totalBandHeight);
        BandType = null;
        BandStyles = null;

        try {
            JRDesignSection detailSection = (JRDesignSection) jasperDesign.getDetailSection();
            detailSection.addBand(detailBand);
            logger.info("Bande de détails créée avec succès. Hauteur totale: {}px", totalBandHeight);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'ajout de la bande de détails: " + e.getMessage(), e);
        }
    }

    private void processChildElements(Element parentElement, JRDesignBand band, String bandType) {
        for (Element child : parentElement.children()) {
            try {
                Map<String, String> computedStyles = cssService.getComputedStyles(child);
                processElement(child, band, bandType, computedStyles);
            } catch (Exception e) {
                logger.error("Erreur lors du traitement de l'élément enfant : " + e.getMessage(), e);
            }
        }
    }

    private void processElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        String tagName = element.tagName().toLowerCase();
        switch (tagName) {
            case "label":
                processStaticTextElement(element, band, bandType, computedStyles);
                break;
            case "span":
            case "p":
                processTextElement(element, band, bandType, computedStyles);
                break;
            case "img":
                processImageElement(element, band, bandType, computedStyles);
                break;
            case "div":
                if (isTextOnlyDiv(element)) {
                    logger.info("Traitement comme élément texte (div)");
                    processTextElement(element, band, bandType, computedStyles);
                } else {
                    logger.info("Traitement comme conteneur (div)");
                    processContainerElement(element, band, bandType, computedStyles);
                }
                break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                processHeaderElement(element, band, bandType, computedStyles);
                break;
            case "strong":
            case "b":
                processBoldElement(element, band, bandType, computedStyles);
                break;
            case "em":
            case "i":
                processItalicElement(element, band, bandType, computedStyles);
                break;
            case "br":
                break;
        }


    }

    private void processItalicElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        Map<String, String> italicStyles = new HashMap<>(computedStyles);
        italicStyles.put("font-style", "italic");
        processTextElement(element, band, bandType, italicStyles);
    }

    private void processBoldElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        Map<String, String> boldStyles = new HashMap<>(computedStyles);
        boldStyles.put("font-weight", "bold");
        processTextElement(element, band, bandType, boldStyles);
    }

    private void processContainerElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        JRDesignFrame frame = new JRDesignFrame();
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);
        frame.setX(position.get("x"));
        frame.setY(position.get("y"));
        frame.setWidth(position.get("width"));
        frame.setHeight(position.get("height"));

        applyStylesToFrame(frame, computedStyles);

        for (Element childElement : element.children()) {

            Map<String, String> childInheritedStyles = new HashMap<>(computedStyles);
            Map<String, String> childElementStyles = cssService.getComputedStyles(childElement);
            childInheritedStyles.putAll(childElementStyles);

            processElement(childElement, frame, bandType, childInheritedStyles);
        }
        band.addElement(frame);
    }
    private void processStaticTextElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        JRDesignStaticText staticText = new JRDesignStaticText();

        String text = element.text();
        staticText.setText(text);
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);

        staticText.setX(position.get("x"));
        staticText.setY(position.get("y"));
        staticText.setWidth(position.get("width"));
        staticText.setHeight(position.get("height"));

        applyTextStyles(staticText, computedStyles);

        band.addElement(staticText);

    }
    private Set<String> extractAndProcessVariables(Element element) {
        Set<String> variables = jasperReportVariableExtractorService.extractVariables(element);
        if (!variables.isEmpty()) {
            try {
                logger.info("Variables extraites : {}", variables);
                jasperReportVariableExtractorService.addParameters(variables);
                logger.info("Paramètres ajoutés");
            } catch (Exception e) {
                logger.error("Erreur lors de l'ajout des paramètres", e);
            }
        }
        return variables;
    }

    private JRDesignTextField createTextField(Element element, Map<String, String> computedStyles, boolean isHeader, Set<String> variables) {
        JRDesignTextField textField = new JRDesignTextField();

        // Fusion des styles
        Map<String, String> mergedStyles = mergeStyles(element, computedStyles, isHeader);

        // Calcul de la position et des dimensions
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, mergedStyles);

        // Configuration de l'expression
        JRDesignExpression expression = new JRDesignExpression();
        String sanitizedText = sanitizeText(element.text());
        expression.setText(JasperReportVariableExtractorService.getExpressionText(sanitizedText, variables));
        textField.setExpression(expression);

        // Configuration des propriétés du TextField
        textField.setStretchWithOverflow(true);
        textField.setX(position.get("x"));
        textField.setY(position.get("y"));
        textField.setWidth(position.get("width"));
        textField.setHeight(position.get("height"));

        // Application des styles
        applyStylesToTextField(textField, mergedStyles);

        return textField;
    }


    //// Fusionne les styles existants avec les styles spécifiques pour les headers

    private Map<String, String> mergeStyles(Element element, Map<String, String> computedStyles, boolean isHeader) {
        Map<String, String> mergedStyles = new HashMap<>();

        if (computedStyles != null && !computedStyles.isEmpty()) {
            mergedStyles.putAll(computedStyles);
        }

        if (isHeader) {
            applyHeaderStyles(element, mergedStyles);
        }

        return mergedStyles;
    }

    private void applyHeaderStyles(Element element, Map<String, String> styles) {
        int fontSize = switch (element.tagName()) {
            case "h1" -> 20;
            case "h2" -> 16;
            case "h3" -> 14;
            case "h4" -> 12;
            case "h5" -> 11;
            case "h6" -> 10;
            default -> 18;
        };

        if (!styles.containsKey("font-size")) {
            styles.put("font-size", fontSize + "px");
        }

        if (!styles.containsKey("font-weight")) {
            styles.put("font-weight", "bold");
        }
    }
    private void processElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles, boolean isHeader) {
        logger.info("Traitement de l'élément {} pour la bande {} - type: {}", element.tagName(), bandType, isHeader ? "Header" : "Texte");
        Set<String> variables = extractAndProcessVariables(element);

        JRDesignTextField textField = createTextField(element, computedStyles, isHeader, variables);

        band.addElement(textField);
    }

    private String sanitizeText(String text) {
        return text.replace("\"", "\\\"")
                .replace("\n", " ")
                .trim();
    }

    private void processHeaderElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        processElement(element, band, bandType, computedStyles, true);
    }

    private void processTextElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        processElement(element, band, bandType, computedStyles, false);
    }

    private void processImageElement(Element element, JRDesignBand band, String bandType, Map<String, String> computedStyles) {
        JRDesignImage image = new JRDesignImage(null);

        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);
        image.setX(position.get("x"));
        image.setY(position.get("y"));
        image.setWidth(position.get("width"));
        image.setHeight(position.get("height"));


        String src = element.attr("src");
        if (!src.isEmpty()) {

            JRDesignExpression expression = new JRDesignExpression();
            expression.setText("\"" + src + "\"");
            image.setExpression(expression);
        }

        band.addElement(image);
    }

    private void applyStylesToTextField(JRDesignTextField textField, Map<String, String> styles) {
        // Validation des entrées
        if (textField == null || styles == null || styles.isEmpty()) {
            logger.debug("Aucun style à appliquer");
            return;
        }

        for (Map.Entry<String, String> style : styles.entrySet()) {
            try {
                Map.Entry<String, String> jasperAttribute =
                        convertCssToJasperService.convertCssToJasperAttribute(style.getKey(), style.getValue());

                if (jasperAttribute != null) {
                    convertCssToJasperService.applyJasperAttribute(textField, jasperAttribute.getKey(), jasperAttribute.getValue());
                } else {
                    logger.warn("Style ignoré : {} = {}", style.getKey(), style.getValue());
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'application du style {} : {}", style.getKey(), e.getMessage());
            }
        }
    }

    private boolean isTextOnlyDiv(Element element) {
        // Vérifier si c'est un élément vide avec du texte directement dedans
        if (element.children().isEmpty() && !element.text().trim().isEmpty()) {
            logger.debug("L'élément n'a pas d'enfants mais contient du texte: '{}', c'est un div de texte simple", element.text());
            return true;
        }
        // Vérifier si tous les enfants sont des éléments de formatage de texte
        boolean allChildrenAreTextElements = !element.children().isEmpty() &&
                element.children().stream().allMatch(this::isTextElement);
        if (allChildrenAreTextElements) {
            logger.debug("Tous les enfants sont des éléments de formatage de texte, c'est un div de texte simple");
            return true;
        }
        logger.debug("L'élément n'est pas un div contenant uniquement du texte");
        return false;
    }

    private boolean isTextElement(Element element) {
        String tagName = element.tagName().toLowerCase();
        if (tagName.equals("div") && element.children().isEmpty() && !element.text().isEmpty()) {
            return true;
        }
        return tagName.equals("strong") ||
                tagName.equals("em") ||
                tagName.equals("b") ||
                tagName.equals("i");
    }

    private void applyStylesToFrame(JRDesignFrame frame, Map<String, String> styles) {

        for (Map.Entry<String, String> style : styles.entrySet()) {
            try {
                Map.Entry<String, String> jasperAttribute =
                        convertCssToJasperService.convertCssToJasperAttribute(style.getKey(), style.getValue());

                if (jasperAttribute != null) {
                    // Appliquer l'attribut au frame
                    convertCssToJasperService.applyJasperAttribute(frame, jasperAttribute.getKey(), jasperAttribute.getValue());
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'application du style au frame : {}", e.getMessage());
            }
        }
    }

    private void processElement(Element childElement, JRDesignFrame frame, String bandType, Map<String, String> computedStyles) {
        String tagName = childElement.tagName().toLowerCase();
       
        switch (tagName) {
            case "span":
            case "p":
                processTextElementInFrame(childElement, frame, bandType, computedStyles
);
                break;

            case "img":
                processImageElementInFrame(childElement, frame, bandType, computedStyles
);
                break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                processHeaderElementInFrame(childElement, frame, bandType, computedStyles);
                break;

            case "div":
                if (isSimpleTextDiv(childElement)) {
                    processTextElementInFrame(childElement, frame, bandType, computedStyles
);
                } else if (isTextOnlyDiv(childElement)) {
                    processTextElementInFrame(childElement, frame, bandType, computedStyles);
                } else {
                    processNestedContainerElement(childElement, frame, bandType, computedStyles);
                }
                break;
            default:
                logger.warn("Élément non supporté dans un frame : {}", tagName);
                break;
        }
    }

    private boolean isSimpleTextDiv(Element element) {
        return element.tagName().equalsIgnoreCase("div") &&
                element.children().isEmpty() &&
                !element.text().isEmpty();
    }


    private void processNestedContainerElement(Element element, JRDesignFrame parentFrame, String bandType, Map<String, String> computedStyles) {
        JRDesignFrame nestedFrame = new JRDesignFrame();
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);

        nestedFrame.setX(position.get("x"));
        nestedFrame.setY(position.get("y"));
        nestedFrame.setWidth(position.get("width"));
        nestedFrame.setHeight(position.get("height"));

        applyStylesToFrame(nestedFrame, computedStyles);

        for (Element childElement : element.children()) {

            Map<String, String> childInheritedStyles = new HashMap<>(computedStyles);
            Map<String, String> childElementStyles = cssService.getComputedStyles(childElement);
            childInheritedStyles.putAll(childElementStyles);


            if (childElement.tagName().equalsIgnoreCase("div") &&
                    childElement.children().isEmpty() &&
                    !childElement.text().isEmpty()) {

                processTextElementInFrame(childElement, nestedFrame, bandType, childInheritedStyles);
            } else {

                processElement(childElement, nestedFrame, bandType, childInheritedStyles);
            }
        }

        parentFrame.addElement(nestedFrame);
    }


   private void processTextElementInFrame(Element element, JRDesignFrame frame, String bandType, Map<String, String> computedStyles) {
        JRDesignTextField textField = new JRDesignTextField();
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);
       JRDesignExpression expression = new JRDesignExpression();
        expression.setText("\"" + element.text().replace("\"", "\\\"") + "\"");

        textField.setExpression(expression);
        textField.setStretchWithOverflow(true);
        textField.setX(position.get("x"));
        textField.setY(position.get("y"));
        textField.setWidth(position.get("width"));
        textField.setHeight(position.get("height"));

        applyStylesToTextField(textField, computedStyles);
        frame.addElement(textField);
    }

    private void processHeaderElementInFrame(Element element, JRDesignFrame frame, String bandType, Map<String, String> computedStyles) {
       applyHeaderStyles(element, computedStyles);
        JRDesignTextField textField = new JRDesignTextField();
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("\"" + element.text().replace("\"", "\\\"") + "\"");

        textField.setExpression(expression);
        textField.setStretchWithOverflow(true);
        textField.setX(position.get("x"));
        textField.setY(position.get("y"));
        textField.setWidth(position.get("width"));
        textField.setHeight(position.get("height"));

        applyStylesToTextField(textField, computedStyles);
        frame.addElement(textField);
    }

    private void processImageElementInFrame(Element element, JRDesignFrame frame, String bandType, Map<String, String> computedStyles) {
        JRDesignImage image = new JRDesignImage(null);
        Map<String, Integer> position = positionCalculator.calculatePositionAndSize(element, computedStyles);

        image.setX(position.get("x"));
        image.setY(position.get("y"));
        image.setWidth(position.get("width"));
        image.setHeight(position.get("height"));

        String src = element.attr("src");
        if (!src.isEmpty()) {
            JRDesignExpression expression = new JRDesignExpression();
            expression.setText("\"" + src + "\"");
            image.setExpression(expression);
        }
        frame.addElement(image);
    }

}

