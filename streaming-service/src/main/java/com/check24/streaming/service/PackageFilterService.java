package com.check24.streaming.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.StreamingPackage;

@Service
public class PackageFilterService 
{

    private final DataService dataService;

    public PackageFilterService(DataService dataService)
    {
        this.dataService = dataService;
    }

    public Collection<StreamingPackage> filter(FilterOptions options)
    {
        Collection<StreamingPackage> allPackages = dataService.getAllPackages();
        return allPackages.stream()
        .filter( pkg -> {
            if(options.teams().isEmpty() && options.tournaments().isEmpty()) return true;

            for(String team : options.teams())
            {
               double coverage = switch(options.preference())
               {
                case LIVE -> dataService.getTeamLiveCoverageByPackageId(team, pkg.getStreamingPackageId());
                case HIGHLIGHTS -> dataService.getTeamHighlightsCoverageByPackageId(team, pkg.getStreamingPackageId());
                default -> Math.max(dataService.getTeamLiveCoverageByPackageId(team, pkg.getStreamingPackageId()),
                dataService.getTeamHighlightsCoverageByPackageId(team, pkg.getStreamingPackageId()));
               };

               if(coverage <= 0) return false;
            }

            for (String tournament : options.tournaments()) 
            {
                double coverage = switch(options.preference())
               {
                case LIVE -> dataService.getTournamentLiveCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                case HIGHLIGHTS -> dataService.getTournamentHighlightsCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                default -> Math.max(dataService.getTournamentLiveCoverageByPackageId(tournament, pkg.getStreamingPackageId()),
                dataService.getTournamentHighlightsCoverageByPackageId(tournament, pkg.getStreamingPackageId()));
               };
                
               if(coverage <= 0) return false;
            }
            return true;

        })
        .filter(pkg -> {
            if (options.maxPrice() == null) return true;
            return pkg.getMonthlyPrice() <= options.maxPrice();     
        })
        .sorted((pkg1, pkg2) -> {
            switch(options.sortingOption())
            {
                case PRICE ->{return Double.compare(pkg1.getMonthlyPrice(), pkg2.getMonthlyPrice());}
                
                case COVERAGE ->
                {
                    double coverage1 = calculateTotalCoverage(pkg1.getStreamingPackageId(), options);
                    double coverage2 = calculateTotalCoverage(pkg2.getStreamingPackageId(), options);
                    return Double.compare(coverage1, coverage2);
                }   
                default ->{return 0;}
            }

        })
        .collect(Collectors.toList());
        
    }

    private double calculateTotalCoverage(int packageId, FilterOptions options) {
        double totalCoverage = 0.0;
        int numSources = options.teams().size() + options.tournaments().size();
        for(String team : options.teams())
        {
            totalCoverage += switch(options.preference()) {
                case LIVE -> dataService.getTeamLiveCoverageByPackageId(team, packageId);
                case HIGHLIGHTS -> dataService.getTeamHighlightsCoverageByPackageId(team, packageId);
                default -> Math.max(dataService.getTeamLiveCoverageByPackageId(team, packageId), dataService.getTeamHighlightsCoverageByPackageId(team, packageId));
            };
        }

        for(String tournament : options.tournaments())
        {
            totalCoverage += switch(options.preference()) {
                case LIVE -> dataService.getTournamentLiveCoverageByPackageId(tournament, packageId);
                case HIGHLIGHTS -> dataService.getTournamentHighlightsCoverageByPackageId(tournament, packageId);
                default -> Math.max(dataService.getTournamentLiveCoverageByPackageId(tournament, packageId), dataService.getTournamentHighlightsCoverageByPackageId(tournament, packageId));
            };
        }
        
        return (double) totalCoverage / numSources;

    }

    

}
