package com.example.genererapport.Controller;

import com.example.genererapport.Request.XhtmlRequest;
import com.example.genererapport.Service.CssService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/css")
public class CssController {

    @Autowired
    private CssService cssService;

    @PostMapping("/extract")
    public ResponseEntity<?> extractCss(@RequestBody XhtmlRequest request) {
        try {
            // Analyse du contenu XHTML
            Document doc = Jsoup.parse(request.getXhtmlContent());

            // Extraction de tous les styles CSS
            Map<String, Map<String, String>> allCssRules =
                    cssService.extractAllCssFromDocument(doc, request.getBasePath());

            // Retourne la map des styles CSS extraites
            return ResponseEntity.ok(allCssRules);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/explicit-styles")
    public ResponseEntity<?> testExplicitStyles(@RequestBody XhtmlRequest request) {
        try {
            // Analyse du contenu XHTML
            Document doc = Jsoup.parse(request.getXhtmlContent());

            // Extraction de tous les styles CSS
            cssService.extractAllCssFromDocument(doc, request.getBasePath());

            // Récupérer l'élément spécifié par le sélecteur
            Element element = doc.selectFirst(request.getSelector());
            if (element == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sélecteur non trouvé: " + request.getSelector()));
            }

            // Obtenir les styles explicites pour cet élément
            Map<String, String> explicitStyles = cssService.getExplicitStyles(element);

            // Retourne la map des styles explicites
            return ResponseEntity.ok(Map.of(
                    "selector", request.getSelector(),
                    "styles", explicitStyles
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/computed-styles")
    public ResponseEntity<?> testComputedStyles(@RequestBody XhtmlRequest request) {
        try {
            // Analyse du contenu XHTML
            Document doc = Jsoup.parse(request.getXhtmlContent());

            // Extraction de tous les styles CSS
            cssService.extractAllCssFromDocument(doc, request.getBasePath());

            // Récupérer l'élément spécifié par le sélecteur
            Element element = doc.selectFirst(request.getSelector());
            if (element == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sélecteur non trouvé: " + request.getSelector()));
            }

            // Obtenir les styles calculés pour cet élément
            Map<String, String> computedStyles = cssService.getComputedStyles(element);

            // Retourne la map des styles calculés
            return ResponseEntity.ok(Map.of(
                    "selector", request.getSelector(),
                    "computedStyles", computedStyles
            ));

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
    