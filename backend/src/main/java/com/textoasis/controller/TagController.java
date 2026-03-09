package com.textoasis.controller;

import com.textoasis.dto.TagDto;
import com.textoasis.mapper.TagMapper;
import com.textoasis.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @GetMapping("/tags")
    public ResponseEntity<List<TagDto>> getAllTags() {
        List<TagDto> tags = tagRepository.findAll().stream()
                .map(tagMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tags);
    }
}
