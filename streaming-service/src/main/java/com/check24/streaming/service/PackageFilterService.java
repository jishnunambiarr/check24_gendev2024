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


/**
 * Service responsible for filtering and searching streaming packages based on teams, tournaments, and user preferences.
 * Provides functionality to calculate coverage percentages and filter packages based on various criteria.
 */

@Service
public class PackageFilterService 
{

    private final DataService dataService;

    public PackageFilterService(DataService dataService)
    {
        this.dataService = dataService;
    }

    /**
     * Searches for relevant streaming packages based on selected teams and tournaments.
     * A package is considered relevant if it has any coverage (live or highlights) for any of the selected teams or tournaments.
     *
     * @param teams List of team names to search for
     * @param tournaments List of tournament names to search for
     * @return Collection of StreamingPackageDTO with calculated coverage percentages
     */
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

    /**
     * Filters and sorts streaming packages based on specified options.
     * Filtering criteria include:
     * - Maximum price threshold
     * - Coverage preference (live/highlights)
     * Sorting options include:
     * - Price (ascending)
     * - Coverage percentage (descending)
     *
     * @param packages Collection of packages to filter
     * @param options FilterOptions containing price limits, coverage preferences, and sorting criteria
     * @return Filtered and sorted collection of StreamingPackageDTO
     */
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

    /**
     * Calculates the average coverage percentage across all selected teams and tournaments for a package.
     * Coverage can be calculated for either live streaming or highlights based on the provided options.
     *
     * @param packageId ID of the package to calculate coverage for
     * @param teams List of teams to include in calculation
     * @param tournaments List of tournaments to include in calculation
     * @param options FilterOptions containing coverage preference
     * @return Average coverage percentage between 0.0 and 1.0
     */
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
