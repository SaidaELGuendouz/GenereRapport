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

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/css")
public class CssController {
/*
    @Autowired
    private CssService cssService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyCssToElement(@RequestBody XhtmlRequest request) {
        try {
            // Validation des paramètres d'entrée
            if (request == null ||
                    request.getHtmlContent() == null ||
                    request.getHtmlContent().isEmpty() ||
                    request.getSelector() == null ||
                    request.getSelector().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid request parameters"));
            }

            // Charger le document HTML
            Document document = Jsoup.parse(request.getHtmlContent());

            // Extraire les règles CSS
            Map<String, Map<String, String>> cssRules = cssService.extractAllCssFromDocument(
                    document,
                    request.getBasePath() != null ? request.getBasePath() : ""
            );

            // Trouver l'élément par son sélecteur
            Element element = document.select(request.getSelector()).first();

            if (element != null) {
                // Récupérer les styles calculés pour l'élément
                Map<String, String> computedStyles = cssService.getComputedStyles(element);

                // Ajouter les informations supplémentaires si nécessaire
                Map<String, Object> response = new HashMap<>();
                response.put("computedStyles", computedStyles);
                response.put("cssRules", cssRules);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Element not found for selector: " + request.getSelector()));
            }
        } catch (Exception e) {
            // Log de l'erreur (à ajouter avec un logger)
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal server error",
                            "details", e.getMessage()
                    ));
        }
    }*/
}
