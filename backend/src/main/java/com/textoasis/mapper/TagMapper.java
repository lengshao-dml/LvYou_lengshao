package com.textoasis.mapper;

import com.textoasis.dto.TagDto;
import com.textoasis.model.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagDto toDto(Tag tag) {
        if (tag == null) {
            return null;
        }

        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setCategory(tag.getCategory());

        return dto;
    }
}
