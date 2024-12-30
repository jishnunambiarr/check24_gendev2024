package com.check24.streaming.model;

import lombok.Data;

import java.util.Set;

import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class Game {
    private final int id;
    private final String homeTeam;
    private final String awayTeam;
    private final String startTime;
    private final String tournament;


    public Set<String> getTeams()
    {
        return Set.of(homeTeam, awayTeam);

    }

    public String formatGameInfo() {
        return String.format("%s vs %s - %s", 
            this.getHomeTeam(), 
            this.getAwayTeam(),
            this.getStartTime());
    }
}

