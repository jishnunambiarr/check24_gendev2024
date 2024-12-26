package com.check24.streaming.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.PackageCombination;
import com.check24.streaming.model.StreamingPackage;
import com.check24.streaming.service.DataService;
import com.check24.streaming.service.PackageComparisonService;
import com.check24.streaming.service.PackageFilterService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For development. Adjust for production
public class WebController {
    private final DataService dataService;
    private final PackageFilterService packageFilterService;
    private final PackageComparisonService packageComparisonService;

    public WebController(DataService dataService, 
                        PackageFilterService packageFilterService,
                        PackageComparisonService packageComparisonService) {
        this.dataService = dataService;
        this.packageFilterService = packageFilterService;
        this.packageComparisonService = packageComparisonService;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<String>> getAllTeams() {
        return ResponseEntity.ok(dataService.getAllTeams());
    }

    @GetMapping("/tournaments")
    public ResponseEntity<List<String>> getAllTournaments() {
        return ResponseEntity.ok(dataService.getAllTournaments());
    }

    @PostMapping("/filter")
    public ResponseEntity<Collection<StreamingPackage>> filterPackages(
            @RequestBody FilterOptions options) {
        return ResponseEntity.ok(packageFilterService.filter(options));
    }

    // Create a request record for the compare endpoint
    public record ComparisonRequest(List<Integer> packageIds, FilterOptions options) {}

    @PostMapping("/compare")
    public ResponseEntity<PackageCombination> comparePackages(
            @RequestBody ComparisonRequest request) {
        return ResponseEntity.ok(
            packageComparisonService.findOptimalCombination(
                request.packageIds(), 
                request.options()
            )
        );
    }

    // Add error handling
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity
            .badRequest()
            .body("Error processing request: " + e.getMessage());
    }
}