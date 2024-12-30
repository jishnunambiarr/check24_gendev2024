package com.check24.streaming;


import com.check24.streaming.service.DataService;
import com.check24.streaming.service.PackageFilterService;

@SuppressWarnings("unused")
public class testFilterService 
{ 
    PackageFilterService packageFilterService = new PackageFilterService(new DataService());
}
