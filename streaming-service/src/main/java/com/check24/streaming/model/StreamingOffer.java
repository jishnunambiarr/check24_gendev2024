package com.check24.streaming.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class StreamingOffer 
{
    private final int gameId;
    private final int streamingPackageId;
    private final boolean hasLive;
    private final boolean hasHighlights;

}
