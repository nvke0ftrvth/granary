package com.example.granary.web;

import java.net.URI;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import com.example.granary.business.RecipeService;
import com.example.granary.dto.RecipeImageDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ShareController {

    private final RecipeService recipeService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @GetMapping(value = "/recipes/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareRecipe(@PathVariable Long id) {
        String pageUrl = frontendBaseUrl + "/recipes/" + id;

        RecipeResponseDto recipe;
        try {
            recipe = recipeService.getById(id);
        } catch (RecipeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendBaseUrl)
                    .build();
        }

        String title = HtmlUtils.htmlEscape(recipe.getTitle());
        String description = HtmlUtils.htmlEscape(
                recipe.getDescription() != null ? recipe.getDescription() : "A recipe shared from Granary");
        String escapedPageUrl = HtmlUtils.htmlEscape(pageUrl);
        String imageUrl = firstImageUrl(recipe);

        String html = buildShareHtml(title, description, escapedPageUrl, imageUrl);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private String firstImageUrl(RecipeResponseDto recipe) {
        if (recipe.getImages() == null || recipe.getImages().isEmpty()) {
            return null;
        }
        RecipeImageDto first = recipe.getImages().stream()
                .min(Comparator.comparing(
                        RecipeImageDto::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(recipe.getImages().get(0));

        URI backendOrigin = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        String absolute = backendOrigin.toString() + first.getImageUrl();
        return HtmlUtils.htmlEscape(absolute);
    }

    private String buildShareHtml(String title, String description, String pageUrl, String imageUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(title).append(" · Granary</title>\n");
        html.append("<meta property=\"og:type\" content=\"article\">\n");
        html.append("<meta property=\"og:site_name\" content=\"Granary\">\n");
        html.append("<meta property=\"og:title\" content=\"").append(title).append("\">\n");
        html.append("<meta property=\"og:description\" content=\"").append(description).append("\">\n");
        html.append("<meta property=\"og:url\" content=\"").append(pageUrl).append("\">\n");
        if (imageUrl != null) {
            html.append("<meta property=\"og:image\" content=\"").append(imageUrl).append("\">\n");
        }
        html.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        html.append("<meta http-equiv=\"refresh\" content=\"0; url=").append(pageUrl).append("\">\n");
        html.append("</head>\n<body>\n");
        html.append("<p>Redirecting to <a href=\"").append(pageUrl).append("\">").append(title).append("</a>…</p>\n");
        html.append("</body>\n</html>\n");
        return html.toString();
    }
}
