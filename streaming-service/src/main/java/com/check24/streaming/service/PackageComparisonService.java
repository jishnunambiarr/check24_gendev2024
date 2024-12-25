package com.check24.streaming.service;

import org.springframework.stereotype.Service;

import com.check24.streaming.model.FilterOptions;
import com.check24.streaming.model.Game;
import com.check24.streaming.model.StreamingOffer;
import com.check24.streaming.model.StreamingPackage;

import java.util.*;

import com.check24.streaming.model.PackageCombination;


public class PackageComparisonService 
{
    private final DataService dataService;

    public PackageComparisonService(DataService dataService)
    {
        this.dataService = dataService;
    }

    // public List<PackageCombination> findBestCombinations(List<Integer> packageIds, FilterOptions selectedOptions)
    // {
    //     if(packageIds.size() > 3) throw new IllegalArgumentException("Cannot compare more than 3 Packages!");
    //     if(selectedOptions.teams().isEmpty() && selectedOptions.tournaments().isEmpty()) throw new IllegalArgumentException("Select at least one team or tournament!");

    //     List<StreamingPackage> selectedPackages = packageIds.stream()
    //                                             .map(dataService::getPackageById)
    //                                             .toList();
        
    //     List<PackageCombination> bestCombination = new ArrayList<>();

            

    // }



}
