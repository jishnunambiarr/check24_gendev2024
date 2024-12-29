package com.check24.streaming.model;

import java.util.List;


public record  SearchRequest (List<String> teams, List<String> tournaments) {}
