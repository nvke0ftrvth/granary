package com.example.granary.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.granary.business.TagService;
import com.example.granary.dto.TagDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final TagService tagService;

    // GET every available tag (defaults and user-created) with its recipe count; query filters by substring
    @GetMapping
    public ResponseEntity<List<TagDto>> list(@RequestParam(defaultValue = "") String query) {
        return ResponseEntity.ok(tagService.list(query));
    }

    // GET the most-used tags, most recipes first
    @GetMapping("/popular")
    public ResponseEntity<List<TagDto>> popular(
            @RequestParam(defaultValue = "" + TagService.DEFAULT_POPULAR_LIMIT)
            @Min(1) @Max(TagService.MAX_POPULAR_LIMIT) int limit) {
        return ResponseEntity.ok(tagService.popular(limit));
    }
}
