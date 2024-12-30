package com.check24.streaming.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BestCombination {
    private CombinationType type;
    private double totalCost;
    private List<PackagePeriod> packages;
    private Map<String, Set<Game>> coveredGames;
    private Map<String, Set<Game>> uncoveredGames;
    private double coveragePercentage;


    @Data
    @AllArgsConstructor
    public static class PackagePeriod {
        private String startMonthYear;
        private String endMonthYear;
        private Set<StreamingPackageDTO> packages;
        private double periodCost;
    }

    public enum CombinationType {
        STATIC, SEQUENTIAL;
    }

    public BestCombination(double totalCost, List<PackagePeriod> packages, Map<String, Set<Game>> coveredGames, Map<String, Set<Game>> uncoveredGames, double coveragePercentage) {
        this.type = CombinationType.SEQUENTIAL;
        this.totalCost = totalCost;
        this.packages = packages;
        this.coveredGames = coveredGames;
        this.uncoveredGames = uncoveredGames;
        this.coveragePercentage = coveragePercentage;
    }

    public BestCombination(double totalCost, Set<StreamingPackageDTO> packages, Map<String, Set<Game>> coveredGames, Map<String, Set<Game>> uncoveredGames, double coveragePercentage) {
        this.type = CombinationType.STATIC;
        this.totalCost = totalCost;
        this.packages = new ArrayList<>();
        PackagePeriod period = new PackagePeriod("", "", packages, totalCost);
        this.packages.add(period);
        this.coveredGames = coveredGames;
        this.uncoveredGames = uncoveredGames;
        this.coveragePercentage = coveragePercentage;
    }

}
