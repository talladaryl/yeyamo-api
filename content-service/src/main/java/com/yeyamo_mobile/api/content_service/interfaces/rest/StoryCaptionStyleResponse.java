package com.yeyamo_mobile.api.content_service.interfaces.rest;

import com.yeyamo_mobile.api.content_service.infrastructure.persistence.StoryEntity;

/** Safe values that a mobile viewer can render without interpreting markup. */
public record StoryCaptionStyleResponse(
        String fontFamily,
        boolean bold,
        boolean italic,
        boolean underline,
        boolean strikethrough
) {
    public static StoryCaptionStyleResponse from(StoryEntity story) {
        return new StoryCaptionStyleResponse(
                story.getCaptionFontFamily(),
                story.isCaptionBold(),
                story.isCaptionItalic(),
                story.isCaptionUnderline(),
                story.isCaptionStrikethrough());
    }
}
