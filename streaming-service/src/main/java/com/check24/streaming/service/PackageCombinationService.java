package com.check24.streaming.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.check24.streaming.model.BestCombination;
import com.check24.streaming.model.Game;
import com.check24.streaming.model.StreamingOffer;
import com.check24.streaming.model.StreamingPackageDTO;
import com.check24.streaming.model.BestCombination.PackagePeriod;

/**
 * Service responsible for finding optimal streaming package combinations based on user preferences.
 * Implements multiple strategies for package selection:
 * - Greedy approach for cases with full coverage packages or evenly distributed games
 * - Sequential monthly approach for cases with high game density variation
 */

@Service
public class PackageCombinationService {
    private final DataService dataService;

    public PackageCombinationService(DataService dataService) {
        this.dataService = dataService;
    }
    
    /**
     * Determines and returns the best combination of streaming packages based on selected teams and tournaments.
     * Automatically chooses between greedy and sequential approaches based on:
     * - Existence of packages with 100% coverage - Greedy approach(returns immediately)
     * - Game density distribution across months
     *
     * @param teams List of team names to include in the analysis
     * @param tournaments List of tournament names to include in the analysis
     * @param packages Collection of available streaming packages
     * @return BestCombination containing selected packages, coverage details, and total cost
     */
    public BestCombination getBestPackageCombinations(List<String> teams, List<String> tournaments, Collection<StreamingPackageDTO> packages) {

        // First check if any package has 100% coverage
        boolean hasFullCoverage = packages.stream()
        .anyMatch(p -> p.getLiveCoveragePercentage() == 100.0 && p.getHighlightsCoveragePercentage() == 100.0);

        // If we have a package with full coverage, always use greedy approach
        if (hasFullCoverage) {
            return greedyPackageCombination(teams, tournaments, packages);
        }
        
        Map<String, Set<Game>> gamesByMonth = mapGamesByMonth(setOfAllGames(teams, tournaments));
        if(gameDensity(gamesByMonth)) 
        {
            return sequentialPackageCombination(teams, tournaments, packages);
        }
        else return greedyPackageCombination(teams, tournaments, packages);

    }

    /**
     * Implements a greedy algorithm to select packages that maximize coverage while minimizing cost.
     * Selects packages one by one based on their efficiency (coverage per cost) until maximum coverage is achieved.
     *
     * @param teams List of team names to cover
     * @param tournaments List of tournament names to cover
     * @param packages Available streaming packages to choose from
     * @return BestCombination containing selected packages and coverage details
     */
    @SuppressWarnings("unused")
    public BestCombination greedyPackageCombination(List<String> teams, List<String> tournaments, Collection<StreamingPackageDTO> packages) {
        Set<Game> games = setOfAllGames(teams, tournaments);
        Set<Game> uncoveredGames = new HashSet<>(games);
        Set<StreamingPackageDTO> selectedPackages = new HashSet<>();
        double currentCoverage = 0.0;
        double currentPrice = 0.0;
        
        while(!uncoveredGames.isEmpty()) {
            Map<StreamingPackageDTO, Double> packageEfficiencies = new HashMap<>();
            for (StreamingPackageDTO pkg : packages) {
                
                if (!selectedPackages.contains(pkg)) {
                    double additionalCoverage = calculateAdditionalCoverage(pkg.getStreamingPackageId(), uncoveredGames);
                    if(additionalCoverage > 0) {
                        double efficiency = calculateEfficiency(pkg, additionalCoverage);
                        packageEfficiencies.put(pkg, efficiency);
                    }
                }
            }

            if (packageEfficiencies.isEmpty()) {
                break;
            }

            StreamingPackageDTO bestPackage = Collections.max(packageEfficiencies.entrySet(), Map.Entry.comparingByValue()).getKey();
            selectedPackages.add(bestPackage);
            currentCoverage += packageEfficiencies.get(bestPackage);
            currentPrice += bestPackage.getMonthlyPrice();

            for(Game game : new HashSet<>(uncoveredGames)) {
                for(StreamingOffer offer : dataService.getOffersForGame(game.getId())) {
                    if(offer.getStreamingPackageId() == bestPackage.getStreamingPackageId()) {
                        uncoveredGames.remove(game);
                        break;
                    }
                }
            }
        }
        Set<Game> coveredGames = new HashSet<>(games);
        coveredGames.removeAll(uncoveredGames);
        Map<String, Set<Game>> coveredGamesOverall = new HashMap<>(); 
        Map<String, Set<Game>> uncoveredGamesOverall = new HashMap<>();

        for(String team : teams) {
            coveredGamesOverall.put(team, new HashSet<>());
            uncoveredGamesOverall.put(team, new HashSet<>());
        }

        for(String tournament : tournaments) {
            coveredGamesOverall.put(tournament, new HashSet<>());
            uncoveredGamesOverall.put(tournament, new HashSet<>());
        }

        for(Game game : coveredGames) {
            for(String team : teams) {
                if(dataService.getGamesByTeam(team).contains(game)) {
                    coveredGamesOverall.get(team).add(game);
                }
            }
            for(String tournament : tournaments)
            {
                if(dataService.getGamesByTournament(tournament).contains(game))
                {
                    coveredGamesOverall.get(tournament).add(game);
                }
            }
        }

        for(Game game : uncoveredGames) {
            for(String team : teams)
            {
                if(dataService.getGamesByTeam(team).contains(game))
                {
                    uncoveredGamesOverall.get(team).add(game);
                }
            }
            for(String tournament : tournaments) {
                if(dataService.getGamesByTournament(tournament).contains(game))
                {
                    uncoveredGamesOverall.get(tournament).add(game);
                }
            }
        }

        double coveragePercentage = (double) coveredGames.size() / games.size();

        return new BestCombination(currentPrice, selectedPackages, coveredGamesOverall, uncoveredGamesOverall, coveragePercentage);
    
        
    }


    /**
     * Implements a sequential algorithm to select packages that maximize coverage while minimizing cost.
     * Selects packages for each month based on their efficiency (coverage per cost) until maximum coverage is achieved.
     *
     * @param teams List of team names to cover
     * @param tournaments List of tournament names to cover
     * @param packages Available streaming packages to choose from
     * @return BestCombination containing selected packages and coverage details
     */
    public BestCombination sequentialPackageCombination(List<String> teams, List<String> tournaments, Collection<StreamingPackageDTO> packages) {
        Set<Game> games = setOfAllGames(teams, tournaments);
        Map<String, Set<Game>> gamesByMonth = mapGamesByMonth(games);
        List<PackagePeriod> packagePeriods = new ArrayList<>();
        double totalCost = 0.0;

        Set<Game> allCoveredGames = new HashSet<>();
        Set<Game> allUncoveredGames = new HashSet<>(games);

        List<String> months = new ArrayList<>(gamesByMonth.keySet());
        Collections.sort(months, (a, b) -> {
            // Split "MM-YYYY" format
            String[] partsA = a.split("-");
            String[] partsB = b.split("-");
            
            // Compare years first
            int yearCompare = partsA[1].compareTo(partsB[1]);
            if (yearCompare != 0) {
                return yearCompare;
            }
            
            // If years are equal, compare months
            return partsA[0].compareTo(partsB[0]);
        });
        
        for(String monthYear : months) {
            Set<Game> gamesInMonth = gamesByMonth.get(monthYear);
            Set<StreamingPackageDTO> bestPackages = findBestPackagesForMonth(gamesInMonth, packages);

            double monthCost = bestPackages.stream()
            .mapToDouble(StreamingPackageDTO::getMonthlyPrice)
            .sum();
            
            totalCost += monthCost;
    
            packagePeriods.add(new PackagePeriod(monthYear, monthYear, bestPackages, monthCost));

            for(Game game : gamesInMonth)
            {
                for(StreamingPackageDTO pkg : bestPackages)
                {
                    for(StreamingOffer offer : dataService.getOffersForGame(game.getId()))
                    {
                        if(offer.getStreamingPackageId() == pkg.getStreamingPackageId())
                        {
                            allCoveredGames.add(game);
                            allUncoveredGames.remove(game);
                            break;
                        }
                    }
                }
            }
        }

        Map<String, Set<Game>> coveredGamesOverall = new HashMap<>();
        Map<String, Set<Game>> uncoveredGamesOverall = new HashMap<>();

        for(String team : teams)
        {
            coveredGamesOverall.put(team, new HashSet<>());
            uncoveredGamesOverall.put(team, new HashSet<>());
        }

        for(String tournament : tournaments)
        {
            coveredGamesOverall.put(tournament, new HashSet<>());
            uncoveredGamesOverall.put(tournament, new HashSet<>());
        }

        for(Game game : allCoveredGames)
        {
            for(String team : teams)
            {
                if(dataService.getGamesByTeam(team).contains(game))
                {
                    coveredGamesOverall.get(team).add(game);
                }
            }
            for(String tournament : tournaments)
            {
                if(dataService.getGamesByTournament(tournament).contains(game))
                {
                    coveredGamesOverall.get(tournament).add(game);
                }
            }
        }

        for(Game game : allUncoveredGames)
        {
            for(String team : teams)
            {
                if(dataService.getGamesByTeam(team).contains(game))
                {
                    uncoveredGamesOverall.get(team).add(game);
                }
            }
            for(String tournament : tournaments)
            {
                if(dataService.getGamesByTournament(tournament).contains(game))
                {
                    uncoveredGamesOverall.get(tournament).add(game);
                }
            }
        }

        double coveragePercentage = (double) allCoveredGames.size() / games.size();
        return new BestCombination(totalCost, packagePeriods, coveredGamesOverall, uncoveredGamesOverall, coveragePercentage);
        
    }

    // Helper method to find best packages for a given month, based on the same greedy algorithm as above.

    public Set<StreamingPackageDTO> findBestPackagesForMonth(Set<Game> gamesInMonth, Collection<StreamingPackageDTO> packages) {
        Set<Game> uncoveredGames = new HashSet<>(gamesInMonth);
        Set<StreamingPackageDTO> selectedPackages = new HashSet<>();
        
        while(!uncoveredGames.isEmpty()) {
            Map<StreamingPackageDTO, Double> packageEfficiencies = new HashMap<>();
            for (StreamingPackageDTO pkg : packages) {
                
                if (!selectedPackages.contains(pkg)) {
                    double additionalCoverage = calculateAdditionalCoverage(pkg.getStreamingPackageId(), uncoveredGames);
                    if(additionalCoverage > 0) 
                    {
                        double efficiency = calculateEfficiency(pkg, additionalCoverage);
                        packageEfficiencies.put(pkg, efficiency);
                    }
                }
            }

            if (packageEfficiencies.isEmpty()) {
                break;
            }

            StreamingPackageDTO bestPackage = Collections.max(packageEfficiencies.entrySet(), Map.Entry.comparingByValue()).getKey();
            selectedPackages.add(bestPackage);

            for(Game game : new HashSet<>(uncoveredGames))
            {
                for(StreamingOffer offer : dataService.getOffersForGame(game.getId()))
                {
                    if(offer.getStreamingPackageId() == bestPackage.getStreamingPackageId())
                    {
                        uncoveredGames.remove(game);
                        break;
                    }
                }
            }
        }
        return selectedPackages;
   
    }

    // Helper Methods

    private Set<Game> setOfAllGames(List<String> teams, List<String> tournaments) {
        Set<Game> games = new HashSet<>();
        for (String team : teams) {
            games.addAll(dataService.getGamesByTeam(team));
        }
        for (String tournament : tournaments) {
            games.addAll(dataService.getGamesByTournament(tournament));
        }
        return games;
    }

    private Map<String, Set<Game>> mapGamesByMonth(Set<Game> games) {
        Map<String, Set<Game>> gamesByMonth = new HashMap<>();
        for (Game game : games) {
            String monthKey = dataService.getGameMonth(game.getId()) + "-" + dataService.getGameYear(game.getId());
            gamesByMonth.computeIfAbsent(monthKey, k -> new HashSet<>()).add(game);
        }
        return gamesByMonth;
    }

    /**
     * Calculates how many additional games would be covered by adding a specific package.
     *
     * @param selectedPackageId ID of the package to evaluate
     * @param uncoveredGames Set of currently uncovered games
     * @return Percentage of uncovered games that would be covered by this package
     */

    private double calculateAdditionalCoverage(int selectedPackageId, Set<Game> uncoveredGames)
    {
        if (uncoveredGames.isEmpty()) {
            return 0.0;
        }

        int additionalGames = 0;
        for(Game game : uncoveredGames)
        {
            for(StreamingOffer offer : dataService.getOffersForGame(game.getId()))
            {
                if(offer.getStreamingPackageId() == selectedPackageId)
                {
                    additionalGames++;
                    break;
                }
            }
        }
        return (double) additionalGames / uncoveredGames.size();
    }

    /**
     * Calculates the game density characteristics to determine the appropriate package selection strategy.
     * Uses coefficient of variation and average games per month to assess distribution.
     *
     * @param gamesByMonth Map of games grouped by month
     * @return true if games are unevenly distributed (high variance), false otherwise
     */
    private boolean gameDensity(Map<String, Set<Game>> gamesByMonth) {
        int noOfMonths = gamesByMonth.size();
        int noOfGames = gamesByMonth.values().stream()
        .mapToInt(Set::size)
        .sum();

        double averageGamesPerMonth = (double) noOfGames / noOfMonths;

        double variance = gamesByMonth.values().stream()
            .mapToDouble(games -> Math.pow(games.size() - averageGamesPerMonth, 2))
            .sum() / noOfMonths;
        
        // Calculate coefficient of variation 
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / averageGamesPerMonth;
    
        // Thresholds based on given data
        return coefficientOfVariation > 0.4 && averageGamesPerMonth < 200;
    }


    /**
     * Calculates the efficiency score of a package based on its coverage and price.
     * Includes a boost factor for packages offering live coverage.
     *
     * @param pkg The package to evaluate
     * @param additionalCoverage The additional coverage this package would provide
     * @return Efficiency score (higher is better)
     */

    private double calculateEfficiency(StreamingPackageDTO pkg, double additionalCoverage) {
        double coverageBoost = pkg.getLiveCoveragePercentage() > 0 ? 0.5 : 0.0; // Boost for packages with live coverage
        if (pkg.getMonthlyPrice() == 0) {
         return additionalCoverage * 100 * coverageBoost; // Multiply by 100 to give free packages with good coverage priority and avoid division by zero
        }
        return additionalCoverage / pkg.getMonthlyPrice() * coverageBoost;
    }

}
