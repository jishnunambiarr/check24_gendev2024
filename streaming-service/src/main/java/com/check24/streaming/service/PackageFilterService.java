package com.check24.streaming.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.check24.streaming.model.CoveragePreference;
import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.StreamingPackage;
import com.check24.streaming.model.StreamingPackageDTO;

@Service
public class PackageFilterService 
{

    private final DataService dataService;

    public PackageFilterService(DataService dataService)
    {
        this.dataService = dataService;
    }


    public Collection<StreamingPackageDTO> searchByTeamsAndTournaments(List<String> teams, List<String> tournaments) {
        Collection<StreamingPackage> allPackages = dataService.getAllPackages();
        System.out.println("Total packages before filtering: " + allPackages.size());
        
        Collection<StreamingPackage> relevantPackages = allPackages.stream()
            .filter(pkg -> {
                if (teams.isEmpty() && tournaments.isEmpty()) {
                    return true;
                }
                
                // Check teams
                for (String team : teams) {
                    double liveCoverage = dataService.getTeamLiveCoverageByPackageId(team, pkg.getStreamingPackageId());
                    double highlightsCoverage = dataService.getTeamHighlightsCoverageByPackageId(team, pkg.getStreamingPackageId());
                    
                    if (liveCoverage > 0 || highlightsCoverage > 0) {
                        return true;
                    }
                }
                
                // Check tournaments
                for (String tournament : tournaments) {
                    double liveCoverage = dataService.getTournamentLiveCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                    double highlightsCoverage = dataService.getTournamentHighlightsCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                    
                    if (liveCoverage > 0 || highlightsCoverage > 0) {
                        return true;
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());
        
        System.out.println("Packages after filtering: " + relevantPackages.size());
        Collection<StreamingPackageDTO> result = new ArrayList<>();
        for(StreamingPackage pkg : relevantPackages) {
           double liveCoverage = calculateTotalCoverage(pkg.getStreamingPackageId(),teams, tournaments, new FilterOptions(null, CoveragePreference.LIVE, null));
           double highlightsCoverage = calculateTotalCoverage(pkg.getStreamingPackageId(), teams, tournaments, new FilterOptions(null, CoveragePreference.HIGHLIGHTS, null));
              result.add(StreamingPackageDTO.fromStreamingPackage(pkg, liveCoverage, highlightsCoverage));
        }
        return result;
    }
    
    public Collection<StreamingPackageDTO> filter(Collection<StreamingPackageDTO> packages, FilterOptions options) {
        return packages.stream()
            .filter(pkg -> {
            
                if(options.maxPrice() != null && pkg.getMonthlyPrice() > options.maxPrice()) {
                    return false;
                }

                if(options.preference() != null)
                {
                    double relevantCoverage = switch(options.preference()) {
                        case LIVE -> pkg.getLiveCoveragePercentage();
                        case HIGHLIGHTS -> pkg.getHighlightsCoveragePercentage();
                    };

                    if(relevantCoverage <= 0) {
                        return false;
                    }
                }
                return true;
            })
            .sorted((pkg1, pkg2) -> {
                if(options.sortingOption() == null) {
                    return 0;
                }

                return switch(options.sortingOption()) {
                    case PRICE -> Double.compare(pkg1.getMonthlyPrice(), pkg2.getMonthlyPrice());
                    case COVERAGE -> {
                        if(options.preference() == null) {
                            yield Double.compare(
                                Math.max(pkg1.getLiveCoveragePercentage(), pkg1.getHighlightsCoveragePercentage()),
                                Math.max(pkg2.getLiveCoveragePercentage(), pkg2.getHighlightsCoveragePercentage())
                            );
                        } else {
                            double coverage1 = switch(options.preference()) {
                                case LIVE -> pkg1.getLiveCoveragePercentage();
                                case HIGHLIGHTS -> pkg1.getHighlightsCoveragePercentage();
                            };
                            double coverage2 = switch(options.preference()) {
                                case LIVE -> pkg2.getLiveCoveragePercentage();
                                case HIGHLIGHTS -> pkg2.getHighlightsCoveragePercentage();
                            };
                            yield Double.compare(coverage1, coverage2);
                        }
                    }
                };
            })
            .collect(Collectors.toList());

    }
    
    private double calculateTotalCoverage(int packageId, List<String> teams, List<String> tournaments, FilterOptions options) {
        double totalCoverage = 0.0;

        int numSources = teams.size() + tournaments.size();
        
        for (String team : teams) {
            if (options.preference() == null) {
                totalCoverage += Math.max(
                    dataService.getTeamLiveCoverageByPackageId(team, packageId),
                    dataService.getTeamHighlightsCoverageByPackageId(team, packageId)
                );
            } else {
                totalCoverage += switch (options.preference()) {
                    case LIVE -> dataService.getTeamLiveCoverageByPackageId(team, packageId);
                    case HIGHLIGHTS -> dataService.getTeamHighlightsCoverageByPackageId(team, packageId);
                };
            }
        }
    
        for (String tournament : tournaments) {
            if (options.preference() == null) {
                totalCoverage += Math.max(
                    dataService.getTournamentLiveCoverageByPackageId(tournament, packageId),
                    dataService.getTournamentHighlightsCoverageByPackageId(tournament, packageId)
                );
            } else {
                totalCoverage += switch (options.preference()) {
                    case LIVE -> dataService.getTournamentLiveCoverageByPackageId(tournament, packageId);
                    case HIGHLIGHTS -> dataService.getTournamentHighlightsCoverageByPackageId(tournament, packageId);
                };
            }
        }
        
        return numSources > 0 ? totalCoverage / numSources : 0.0;
    }

    


}
