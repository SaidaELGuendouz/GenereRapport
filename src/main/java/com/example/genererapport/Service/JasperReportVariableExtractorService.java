package com.example.genererapport.Service;

import com.example.genererapport.Request.XhtmlRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Setter
@Service
@AllArgsConstructor
@NoArgsConstructor
public class JasperReportVariableExtractorService {
    private static final Logger logger = LoggerFactory.getLogger(JasperReportVariableExtractorService.class);
   private  JasperDesign jasperDesign;
    private Map<String, XhtmlRequest.Parameter> parameters;
    public void initialize(Map<String, XhtmlRequest.Parameter> parameters, JasperDesign jasperDesign) {
        this.parameters = parameters;
        this.jasperDesign = jasperDesign;
        logger.info("Service initialisé avec {} paramètres", parameters.size());
    }

    Set<String> extractVariables(Element element) {
        Set<String> variables = new HashSet<>();
        String text = element.text();

        Pattern variablePattern = Pattern.compile("\\$\\{([^}]+)\\}|\\$([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = variablePattern.matcher(text);

        while (matcher.find()) {
            String var1 = matcher.group(1); // groupe ${var}
            String var2 = matcher.group(2); // groupe $var
            if (var1 != null) {
                variables.add(var1);
            } else if (var2 != null) {
                variables.add(var2);
            }
        }
        return variables;
    }
    public String determineParameterType(String variableName) {
        if (parameters == null) {
            logger.warn("Paramètres non initialisés");
            return "";
        }

        if (parameters.containsKey(variableName)) {
            String type = parameters.get(variableName).getType();
            logger.debug("Type trouvé pour {} : {}", variableName, type);
            return type;
        }

        logger.warn("Variable '{}' non trouvée dans les paramètres", variableName);
        return "";
    }


    public void addParameters(Set<String> variables) throws Exception {

        if (jasperDesign == null) {
            throw new IllegalStateException("JasperDesign doit être initialisé avant d'ajouter des paramètres");
        }

        for (String variableName : variables) {
            // Vérifier si le paramètre existe déjà pour éviter les doublons
            if (jasperDesign.getParametersMap().containsKey(variableName)) {
                logger.debug("Paramètre {} déjà ajouté", variableName);
                continue;
            }

            String parameterType = determineParameterType(variableName);
            if (parameterType.isEmpty()) {
                logger.warn("Type non trouvé pour {}, paramètre ignoré", variableName);
                continue;
            }

            JRDesignParameter parameter = new JRDesignParameter();
            parameter.setName(variableName);

            Class<?> typeClass;
            switch (parameterType.toLowerCase()) {
                case "string":
                    typeClass = java.lang.String.class;
                    break;
                case "integer":
                case "int":
                    typeClass = java.lang.Integer.class;
                    break;
                case "double":
                case "float":
                    typeClass = java.lang.Double.class;
                    break;
                case "date":
                    typeClass = java.util.Date.class;
                    break;
                case "boolean":
                    typeClass = java.lang.Boolean.class;
                    break;
                case "decimal":
                    typeClass = java.math.BigDecimal.class;
                    break;
                default:
                    logger.warn("Type inconnu: {}, utilisation de String par défaut", parameterType);
                    typeClass = java.lang.String.class;
            }
            parameter.setValueClass(typeClass);
            logger.info("Paramètre créé : {}", parameter);

            jasperDesign.addParameter(parameter);
        }
    }

  public static String getExpressionText(String text, Set<String> variables) {
      if (variables == null || variables.isEmpty()) {
          return "\"" + text.replace("\"", "\\\"") + "\"";
      }
      return transformToJasperExpression(text);
  }
    public static String transformToJasperExpression(String htmlContent) {
        // Diviser le contenu en segments: texte et variables
        List<String> segments = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\$(\\{[^}]+\\}|[a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(htmlContent);

        int lastEnd = 0;
        while (matcher.find()) {
            // Ajouter le texte avant la variable si non vide
            if (matcher.start() > lastEnd) {
                segments.add("TEXT:" + htmlContent.substring(lastEnd, matcher.start()));
            }
            // Ajouter la variable
            String var = matcher.group(1);
            String cleanVar = var.startsWith("{") && var.endsWith("}")
                    ? var.substring(1, var.length() - 1)
                    : var;
            segments.add("VAR:" + cleanVar);
            lastEnd = matcher.end();
        }

        // Ajouter le texte restant
        if (lastEnd < htmlContent.length()) {
            segments.add("TEXT:" + htmlContent.substring(lastEnd));

        }

        // Construire l'expression CDATA
        StringBuilder expression = new StringBuilder();

        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);

            if (segment.startsWith("TEXT:")) {
                String text = segment.substring(5);
                if (!text.isEmpty()) {
                    if (expression.length() > 0) {
                        expression.append(" + ");
                    }
                    expression.append("\"").append(text.replace("\"", "\\\"")).append("\"");
                }
            } else if (segment.startsWith("VAR:")) {
                String varName = segment.substring(4);
                if (expression.length() > 0) {
                    expression.append(" + ");
                }
                expression.append("$P{").append(varName).append("}");
            }
        }

        String result = expression.toString();
        logger.debug("Expression Jasper finale: {}", result);
        return result;
    }

}