package com.example.granary.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.granary.business.BookmarkService;
import com.example.granary.dto.RecipeResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public ResponseEntity<List<RecipeResponseDto>> getMyBookmarks() {
        return ResponseEntity.ok(bookmarkService.getMyBookmarks());
    }
}