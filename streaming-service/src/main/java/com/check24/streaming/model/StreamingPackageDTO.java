package com.check24.streaming.model;
import lombok.Data;

@Data
public class StreamingPackageDTO {

    private final int streamingPackageId;
    private final String name;
    private final double monthlyPriceCents;
    private final double yearlyPriceCents;
    private final double liveCoveragePercentage;
    private final double highlightsCoveragePercentage;

    public static StreamingPackageDTO fromStreamingPackage(StreamingPackage pkg, 
            double liveCoveragePercentage, 
            double highlightsCoveragePercentage) {
                System.out.println("Creating DTO for package: " + pkg.getName());
                System.out.println("Live Coverage: " + liveCoveragePercentage);
                System.out.println("Highlights Coverage: " + highlightsCoveragePercentage);
                System.out.println("Monthly Price: " + pkg.getMonthlyPriceCents());        
        return new StreamingPackageDTO(
            pkg.getStreamingPackageId(),
            pkg.getName(),
            pkg.getMonthlyPriceCents(),
            pkg.getYearlyPriceCents(),
            liveCoveragePercentage,
            highlightsCoveragePercentage
        );
    }

    public double getMonthlyPrice() {
        return (double) monthlyPriceCents / 100;
    }

    public double getYearlyPrice() {
        return (double) yearlyPriceCents / 100;
    }
}
