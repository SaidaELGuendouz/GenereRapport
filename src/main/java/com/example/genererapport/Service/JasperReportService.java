package com.example.genererapport.Service;

import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class
JasperReportService {
    @Autowired
    private CssService cssService;
    @Autowired
    private ConvertCssToJasperService convertCssToJasperService;

    private static final Logger logger = LoggerFactory.getLogger(JasperReportService.class);

    public String createEmptyJrxmlDocument() {
        try {

            JasperDesign jasperDesign = new JasperDesign();

            jasperDesign.setName("XhtmlTojasperReport");
            jasperDesign.setPageWidth(595);
            jasperDesign.setPageHeight(842);
            jasperDesign.setColumnWidth(555);
            jasperDesign.setColumnSpacing(0);
            jasperDesign.setLeftMargin(20);
            jasperDesign.setRightMargin(20);
            jasperDesign.setTopMargin(20);
            jasperDesign.setBottomMargin(20);
            return JRXmlWriter.writeReport(jasperDesign, "UTF-8");


        } catch (Exception e) {
            logger.error("Erreur lors de la création du document JRXML vide", e);
            throw new RuntimeException("Erreur lors de la création du document JRXML: " + e.getMessage(), e);
        }
    }

    int calculateBandHeight(Element element, String bandType) {

        if (element.hasAttr("height")) {
            try {
                return Integer.parseInt(element.attr("height"));
            } catch (NumberFormatException e) {
                logger.warn("La valeur de hauteur n'est pas un nombre valide pour " + bandType);
            }
        }else {

            Map<String, String> computedStyles = cssService.getComputedStyles(element);

            if (computedStyles.containsKey("height")) {
                String heightValue = computedStyles.get("height");
                return convertCssToJasperService.convertCssValueToPixels(heightValue);
            }
        }

        logger.warn("Aucune hauteur définie pour {}. Valeur par défaut appliquée.", bandType);
        return 50;
    }

}
