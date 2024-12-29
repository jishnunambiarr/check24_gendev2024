package com.check24.streaming.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Lombok;

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

    public BestCombination(double totalCost, Set<StreamingPackageDTO> packages, Map<String, Set<Game>> coveredGames, Map<String, Set<Game>> uncoveredGames, double coveragePercentage, String startMonthYear, String endMonthYear) {
        PackagePeriod period = new PackagePeriod(startMonthYear, endMonthYear, packages, totalCost);
        this.type = CombinationType.STATIC;
        this.totalCost = totalCost;
        this.packages.add(period);
        this.coveredGames = coveredGames;
        this.uncoveredGames = uncoveredGames;
        this.coveragePercentage = coveragePercentage;
    }

    /* What needs to be displayed?
     * The number of packages in the combination, The detailed coverage of games across the packages, The total cost of the combination, The coverage percentage of the combination Map<Covered Games, Teams/Tournaments> Map<Uncovered Games, Teams/Tournaments>
     */

}
