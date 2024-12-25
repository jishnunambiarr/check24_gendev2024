package com.check24.streaming.model;

import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PackageCombination 
{
    private final List<StreamingPackage> packages;
    private final double totalLiveCoverage;
    private final double totalHighlightCoverage;
    private final double totalPrice;

}
