package com.check24.streaming.model;
import lombok.Data;

@Data
public class StreamingPackageDTO {

    private int streamingPackageId;
    private String name;
    private double monthlyPriceCents;
    private double yearlyPriceCents;
    private double liveCoveragePercentage;
    private double highlightsCoveragePercentage;

    public StreamingPackageDTO(int streamingPackageId, String name, double monthlyPriceCents,
    double yearlyPriceCents, double liveCoveragePercentage,
    double highlightsCoveragePercentage) {
    this.streamingPackageId = streamingPackageId;
    this.name = name;
    this.monthlyPriceCents = monthlyPriceCents;
    this.yearlyPriceCents = yearlyPriceCents;
    this.liveCoveragePercentage = liveCoveragePercentage;
    this.highlightsCoveragePercentage = highlightsCoveragePercentage;
    }
    
    public StreamingPackageDTO() {}
    
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
