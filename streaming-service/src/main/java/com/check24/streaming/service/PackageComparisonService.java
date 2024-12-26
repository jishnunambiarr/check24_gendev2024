package com.check24.streaming.service;

import java.util.ArrayList;
import java.util.List;

import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.PackageCombination;
import com.check24.streaming.model.StreamingPackage;
import com.check24.streaming.service.DataService;

import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.springframework.stereotype.Service;

@Service
public class PackageComparisonService {
    private final DataService dataService;
    private static final double MINIMUM_COVERAGE = 0.5; // 50% minimum coverage
    private static final double COVERAGE_WEIGHT = 1000.0;
    private static final double PRICE_WEIGHT = 1.0;

    public PackageComparisonService(DataService dataService) {
        this.dataService = dataService;
    }

    public PackageCombination findOptimalCombination(List<Integer> packageIds, FilterOptions selectedOptions) {
        List<StreamingPackage> packages = packageIds.stream()
            .map(dataService::getPackageById)
            .toList();

        // 1. Create constraints list
        List<LinearConstraint> constraints = new ArrayList<>();

        // 2. Binary constraints (0 ≤ x ≤ 1)
        for (int i = 0; i < packages.size(); i++) {
            double[] constraintCoefficients = new double[packages.size()];
            constraintCoefficients[i] = 1;
            constraints.add(new LinearConstraint(constraintCoefficients, Relationship.LEQ, 1));
            constraints.add(new LinearConstraint(constraintCoefficients, Relationship.GEQ, 0));
        }

        // 3. Coverage constraints for teams
        for (String team : selectedOptions.teams()) {
            double[] coefficients = new double[packages.size()];
            for (int i = 0; i < packages.size(); i++) {
                coefficients[i] = switch (selectedOptions.preference()) {
                    case LIVE -> dataService.getTeamLiveCoverageByPackageId(team, packages.get(i).getStreamingPackageId());
                    case HIGHLIGHTS -> dataService.getTeamHighlightsCoverageByPackageId(team, packages.get(i).getStreamingPackageId());
                    default -> Math.max(
                        dataService.getTeamLiveCoverageByPackageId(team, packages.get(i).getStreamingPackageId()),
                        dataService.getTeamHighlightsCoverageByPackageId(team, packages.get(i).getStreamingPackageId())
                    );
                };
            }
            constraints.add(new LinearConstraint(coefficients, Relationship.GEQ, MINIMUM_COVERAGE));
        }

        // 4. Coverage constraints for tournaments
        for (String tournament : selectedOptions.tournaments()) {
            double[] coefficients = new double[packages.size()];
            for (int i = 0; i < packages.size(); i++) {
                coefficients[i] = switch (selectedOptions.preference()) {
                    case LIVE -> dataService.getTournamentLiveCoverageByPackageId(tournament, packages.get(i).getStreamingPackageId());
                    case HIGHLIGHTS -> dataService.getTournamentHighlightsCoverageByPackageId(tournament, packages.get(i).getStreamingPackageId());
                    default -> Math.max(
                        dataService.getTournamentLiveCoverageByPackageId(tournament, packages.get(i).getStreamingPackageId()),
                        dataService.getTournamentHighlightsCoverageByPackageId(tournament, packages.get(i).getStreamingPackageId())
                    );
                };
            }
            constraints.add(new LinearConstraint(coefficients, Relationship.GEQ, MINIMUM_COVERAGE));
        }

        // 5. Create objective function coefficients
        double[] objective = createObjectiveCoefficients(packages, selectedOptions);

        // 6. Solve the linear programming problem
        LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(objective, 0);
        SimplexSolver solver = new SimplexSolver();
        
        try {
            PointValuePair solution = solver.optimize(
                new MaxIter(1000),
                objectiveFunction,
                new LinearConstraintSet(constraints),
                GoalType.MAXIMIZE,
                new NonNegativeConstraint(true)
            );

            // 7. Convert solution to package combination
            return createPackageCombination(solution.getPoint(), packages, selectedOptions);
        } catch (NoFeasibleSolutionException e) {
            throw new RuntimeException("No combination of packages satisfies the minimum coverage requirements", e);
        }
    }

    private double[] createObjectiveCoefficients(List<StreamingPackage> packages, FilterOptions options) {
        double[] coefficients = new double[packages.size()];
        
        for (int i = 0; i < packages.size(); i++) {
            StreamingPackage pkg = packages.get(i);
            double averageCoverage = calculateAverageCoverage(pkg, options);
            double normalizedPrice = pkg.getMonthlyPrice() / 100.0; // Convert cents to currency
            
            coefficients[i] = (averageCoverage * COVERAGE_WEIGHT) - (normalizedPrice * PRICE_WEIGHT);
        }
        
        return coefficients;
    }

    private double calculateAverageCoverage(StreamingPackage pkg, FilterOptions options) {
        double totalCoverage = 0.0;
        int count = 0;

        // Calculate team coverage
        for (String team : options.teams()) {
            totalCoverage += switch (options.preference()) {
                case LIVE -> dataService.getTeamLiveCoverageByPackageId(team, pkg.getStreamingPackageId());
                case HIGHLIGHTS -> dataService.getTeamHighlightsCoverageByPackageId(team, pkg.getStreamingPackageId());
                default -> Math.max(
                    dataService.getTeamLiveCoverageByPackageId(team, pkg.getStreamingPackageId()),
                    dataService.getTeamHighlightsCoverageByPackageId(team, pkg.getStreamingPackageId())
                );
            };
            count++;
        }

        // Calculate tournament coverage
        for (String tournament : options.tournaments()) {
            totalCoverage += switch (options.preference()) {
                case LIVE -> dataService.getTournamentLiveCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                case HIGHLIGHTS -> dataService.getTournamentHighlightsCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                default -> Math.max(
                    dataService.getTournamentLiveCoverageByPackageId(tournament, pkg.getStreamingPackageId()),
                    dataService.getTournamentHighlightsCoverageByPackageId(tournament, pkg.getStreamingPackageId())
                );
            };
            count++;
        }

        return count > 0 ? totalCoverage / count : 0.0;
    }

    private PackageCombination createPackageCombination(double[] solution, List<StreamingPackage> packages, FilterOptions options) {
        List<StreamingPackage> selectedPackages = new ArrayList<>();
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] > 0.5) { // Threshold for binary decision
                selectedPackages.add(packages.get(i));
            }
        }

        // Calculate final coverage and price
        double totalLiveCoverage = calculateFinalCoverage(selectedPackages, options, true);
        double totalHighlightCoverage = calculateFinalCoverage(selectedPackages, options, false);
        double totalPrice = selectedPackages.stream()
            .mapToDouble(StreamingPackage::getMonthlyPrice)
            .sum();

        return new PackageCombination(selectedPackages, totalLiveCoverage, totalHighlightCoverage, totalPrice);
    }

    private double calculateFinalCoverage(List<StreamingPackage> packages, FilterOptions options, boolean isLive) {
        double totalCoverage = 0.0;
        int count = 0;

        // Calculate team coverage
        for (String team : options.teams()) {
            double maxCoverage = 0.0;
            for (StreamingPackage pkg : packages) {
                double coverage = isLive ? 
                    dataService.getTeamLiveCoverageByPackageId(team, pkg.getStreamingPackageId()) :
                    dataService.getTeamHighlightsCoverageByPackageId(team, pkg.getStreamingPackageId());
                maxCoverage = Math.max(maxCoverage, coverage);
            }
            totalCoverage += maxCoverage;
            count++;
        }

        // Calculate tournament coverage
        for (String tournament : options.tournaments()) {
            double maxCoverage = 0.0;
            for (StreamingPackage pkg : packages) {
                double coverage = isLive ?
                    dataService.getTournamentLiveCoverageByPackageId(tournament, pkg.getStreamingPackageId()) :
                    dataService.getTournamentHighlightsCoverageByPackageId(tournament, pkg.getStreamingPackageId());
                maxCoverage = Math.max(maxCoverage, coverage);
            }
            totalCoverage += maxCoverage;
            count++;
        }

        return count > 0 ? totalCoverage / count : 0.0;
    }
}