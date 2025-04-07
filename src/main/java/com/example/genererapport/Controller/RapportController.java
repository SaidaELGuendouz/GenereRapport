package com.example.genererapport.Controller;

import com.example.genererapport.Request.XhtmlRequest;
import com.example.genererapport.Service.RapportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rapport")
public class RapportController {
    @Autowired
    private RapportService rapportService;
    private static final Logger logger = LoggerFactory.getLogger(RapportController.class);

    @PostMapping("/convert")
    public ResponseEntity<String> convertXhtmlToJrxml(@RequestBody XhtmlRequest request) {
        if (request.getXhtmlContent() == null || request.getXhtmlContent().isEmpty()) {
            return ResponseEntity.badRequest().body("Le contenu XHTML est requis.");
        }

        if (request.getBasePath() == null || request.getBasePath().isEmpty()) {
            return ResponseEntity.badRequest().body("Le chemin de base pour les fichiers CSS est requis.");
        }

        try {
            String jrxml = rapportService.convertXhtmlToJrxml(request);

            return ResponseEntity.ok(jrxml);
        } catch (Exception e) {
            logger.error("Erreur lors de la conversion du XHTML en JRXML: ", e);
            return ResponseEntity.status(500).body("Erreur lors de la conversion : " + e.getMessage());
        }
    }
}