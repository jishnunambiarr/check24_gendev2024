package com.check24.streaming.service;

import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Service;

import com.check24.streaming.model.BestCombination;
import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.Game;
import com.check24.streaming.model.PackageCombination;
import com.check24.streaming.model.StreamingOffer;
import com.check24.streaming.model.StreamingPackageDTO;
import com.check24.streaming.model.BestCombination.PackagePeriod;
import com.check24.streaming.model.StreamingPackage;

@Service
public class PackageCombinationService {
    private final PackageFilterService packageFilterService;
    private final DataService dataService;

    public PackageCombinationService(PackageFilterService packageFilterService, DataService dataService) {
        this.packageFilterService = packageFilterService;
        this.dataService = dataService;
    }

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
    
    private double caclulateAdditionalCoverage(int selectedPackageId, Set<Game> uncoveredGames)
    {
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

    private boolean gameDensity(Map<String, Set<Game>> gamesByMonth) {
        int noOfMonths = gamesByMonth.size();
        int noOfGames = gamesByMonth.values().size();
        double averageGamesPerMonth = (double) noOfGames / noOfMonths;

        //variance of games per month
        double variance = gamesByMonth.values().stream()
                .map(game -> Math.pow(gamesByMonth.values().size() - averageGamesPerMonth, 2))
                .reduce(0.0, Double::sum) / noOfMonths;

        return variance > 5.0 && averageGamesPerMonth < 3.0; //if variance is high and average games per month is low
    }
    
    
    public BestCombination getBestPackageCombinations(List<String> teams, List<String> tournaments, Collection<StreamingPackageDTO> packages) {

        Map<String, Set<Game>> gamesByMonth = mapGamesByMonth(setOfAllGames(teams, tournaments));
        if(gameDensity(gamesByMonth)) 
        {
            return sequentialPackageCombination(teams, tournaments, packages);
        }
        else return greedyPackageCombination(teams, tournaments, packages);

    }

    public BestCombination greedyPackageCombination(List<String> teams, List<String> tournaments, Collection<StreamingPackageDTO> packages)
    {
        Set<Game> games = setOfAllGames(teams, tournaments);
        Set<Game> uncoveredGames = new HashSet<>(games);
        Set<StreamingPackageDTO> selectedPackages = new HashSet<>();
        double currentCoverage = 0.0;
        double currentPrice = 0.0;
        
        while(!uncoveredGames.isEmpty()) {
            Map<StreamingPackageDTO, Double> packageEfficiencies = new HashMap<>();
            for (StreamingPackageDTO pkg : packages) {
                
                if (!selectedPackages.contains(pkg)) {
                    double additionalCoverage = caclulateAdditionalCoverage(pkg.getStreamingPackageId(), uncoveredGames);
                    double efficiency = additionalCoverage / pkg.getMonthlyPrice();
                    if(efficiency > 5.0)
                    packageEfficiencies.put(pkg, efficiency);
                }
            }

            if (packageEfficiencies.isEmpty()) {
                break;
            }

            StreamingPackageDTO bestPackage = Collections.max(packageEfficiencies.entrySet(), Map.Entry.comparingByValue()).getKey();
            selectedPackages.add(bestPackage);
            currentCoverage += packageEfficiencies.get(bestPackage);
            currentPrice += bestPackage.getMonthlyPrice();

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
        Set<Game> coveredGames = new HashSet<>(games);
        coveredGames.removeAll(uncoveredGames);
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

        for(Game game : coveredGames)
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

        for(Game game : uncoveredGames)
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

        return new BestCombination(currentPrice, selectedPackages, coveredGamesOverall, uncoveredGamesOverall, currentCoverage, null, null);
    
        
    }



    public BestCombination sequentialPackageCombination(List<String> teams, List<String> tournaments, Collection<StreamingPackageDTO> packages) {
        Set<Game> games = setOfAllGames(teams, tournaments);
        Map<String, Set<Game>> gamesByMonth = mapGamesByMonth(games);
        List<PackagePeriod> packagePeriods = new ArrayList<>();
        double totalCost = 0.0;

        Set<Game> allCoveredGames = new HashSet<>();
        Set<Game> allUncoveredGames = new HashSet<>(games);

        List<String> months = new ArrayList<>(gamesByMonth.keySet());
        Collections.sort(months);
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

    public Set<StreamingPackageDTO> findBestPackagesForMonth(Set<Game> gamesInMonth, Collection<StreamingPackageDTO> packages) {
        Set<Game> uncoveredGames = new HashSet<>(gamesInMonth);
        Set<StreamingPackageDTO> selectedPackages = new HashSet<>();
        
        while(!uncoveredGames.isEmpty()) {
            Map<StreamingPackageDTO, Double> packageEfficiencies = new HashMap<>();
            for (StreamingPackageDTO pkg : packages) {
                
                if (!selectedPackages.contains(pkg)) {
                    double additionalCoverage = caclulateAdditionalCoverage(pkg.getStreamingPackageId(), uncoveredGames);
                    double efficiency = additionalCoverage / pkg.getMonthlyPrice();
                    if(efficiency > 5.0)
                    packageEfficiencies.put(pkg, efficiency);
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
}