package com.check24.streaming.model;

import java.util.List;

import io.micrometer.common.lang.Nullable;

public record FilterOptions(@Nullable SortingOptions sortingOption,@Nullable CoveragePreference preference,@Nullable Double maxPrice) {}

