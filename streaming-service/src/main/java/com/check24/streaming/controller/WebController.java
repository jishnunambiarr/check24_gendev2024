package com.check24.streaming.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.SearchRequest;
import com.check24.streaming.service.DataService;
import com.check24.streaming.service.PackageCombinationService;
import com.check24.streaming.service.PackageFilterService;

import java.util.Collection;
import java.util.List;

import com.check24.streaming.model.BestCombination;
import com.check24.streaming.model.StreamingPackageDTO;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WebController {
    private final DataService dataService;
    private final PackageFilterService packageFilterService;
    private final PackageCombinationService packageCombinationService;

    public WebController(DataService dataService, 
                        PackageFilterService packageFilterService, PackageCombinationService packageCombinationService
                       ) {
        this.dataService = dataService;
        this.packageFilterService = packageFilterService;
        this.packageCombinationService = packageCombinationService;
    }
    
    // Get all teams
    @GetMapping("/teams")
    public ResponseEntity<List<String>> getAllTeams() {
        return ResponseEntity.ok(dataService.getAllTeams());
    }

    // Get all tournaments
    @GetMapping("/tournaments")
    public ResponseEntity<List<String>> getAllTournaments() {
        return ResponseEntity.ok(dataService.getAllTournaments());
    }

    // Filter packages
    public record FilterRequest(
    Collection<StreamingPackageDTO> packages,
    FilterOptions filterOptions
    ) {}

    @PostMapping("/filter")
    public Collection<StreamingPackageDTO> filter(@RequestBody FilterRequest request) {
        return packageFilterService.filter(request.packages(), request.filterOptions());
    }


    // Search packages
    @PostMapping("/search")
    public ResponseEntity<Collection<StreamingPackageDTO>> searchPackages(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(
            packageFilterService.searchByTeamsAndTournaments(
                request.teams(), 
                request.tournaments()
            )
        );
    }


    // Compare packages
    public record CompareRequest(
        List<String> teams,
        List<String> tournaments,
        Collection<StreamingPackageDTO> packages
    ) {}

    @PostMapping("/best-combination")
    public @ResponseBody ResponseEntity<BestCombination> comparePackages(
        @RequestBody CompareRequest request) {
        return ResponseEntity.ok(
            packageCombinationService.getBestPackageCombinations(
                request.teams, 
                request.tournaments, 
                request.packages
            )
        );
    }


    //Error handling
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity
            .badRequest()
            .body("Error processing request: " + e.getMessage());
    }
}
