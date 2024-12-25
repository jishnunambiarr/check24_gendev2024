package com.check24.streaming.model;

import java.util.List;

public record FilterOptions(List<String> teams, List<String> tournaments, SortingOptions sortingOption, CoveragePreference preference, Double maxPrice) {}

