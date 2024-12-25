package com.check24.streaming.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;

import com.check24.streaming.model.Game;
import com.check24.streaming.model.StreamingOffer;
import com.check24.streaming.model.StreamingPackage;

@Service
public class DataService 
{
    private List<CSVRecord> gameData;
    private List<CSVRecord> streamingOfferData;
    private List<CSVRecord> streamingPackageData;
    private Map<Integer,Game> gamesById = new HashMap<>();
    private Map<String, Set<Game>> gamesByTeam = new HashMap<>();
    private Map<Integer, StreamingPackage> packagesById = new HashMap<>();
    private Map<String, Set<Game>> gamesByTournament = new HashMap<>();
    private final Map<Integer, List<StreamingOffer>> offersByGameId = new HashMap<>();


    public DataService()
    {
        initializeData();
    }

    private void initializeData()
    {
        loadGameData();
        loadStreamingOfferData();
        loadStreamingPackageData();
    }

    private List<CSVRecord> loadData(String fileName)
    {
        try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
            Reader reader = new InputStreamReader(inputStream))
            {
                CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setHeader()
                    .setTrim(true)
                    .setIgnoreHeaderCase(true)
                    .build();

                    CSVParser csvParser = csvFormat.parse(reader);
                    return csvParser.getRecords();
            } catch(IOException e) {
                throw new RuntimeException("Error loading CSV File: " + fileName, e);

            }
            
    }

    private List<CSVRecord> loadGameData() 
    {
        gameData = loadData("bc_game.csv");

        for(CSVRecord record : gameData)
        {
            Game game = convertToGame(record);
            gamesById.putIfAbsent(game.getId(), game);

            gamesByTeam.computeIfAbsent(game.getHomeTeam(), k -> new HashSet<>()).add(game);
            gamesByTeam.computeIfAbsent(game.getAwayTeam(), k -> new HashSet<>()).add(game);
            
            gamesByTournament.computeIfAbsent(game.getTournament(), k -> new HashSet<>()).add(game);
        }
        return gameData;
    }

    private List<CSVRecord> loadStreamingOfferData() 
    {
       streamingOfferData = loadData("bc_streaming_offer.csv");
       for(CSVRecord record : streamingOfferData)
       {
        StreamingOffer offer = convertToStreamingOffer(record);
        offersByGameId.computeIfAbsent(offer.getGameId(),k -> new ArrayList<>()).add(offer);
       }
       return streamingOfferData;
    }

    private List<CSVRecord> loadStreamingPackageData() 
    {
        streamingPackageData = loadData("bc_streaming_package.csv");
        for (CSVRecord record : streamingPackageData) {
            StreamingPackage pkg = convertToStreamingPackage(record);
            packagesById.put(pkg.getStreamingPackageId(), pkg);
        }
        return streamingPackageData;
    }

    private Game convertToGame(CSVRecord record)
    {
        return new Game(
            Integer.parseInt(record.get("id")),
            record.get("team_home"),
            record.get("team_away"),
            record.get("starts_at"),
            record.get("tournament_name")
        );
    }

    private StreamingOffer convertToStreamingOffer(CSVRecord record)
    {
        return new StreamingOffer(
            Integer.parseInt(record.get("game_id")),
            Integer.parseInt(record.get("streaming_package_id")),
            Boolean.parseBoolean(record.get("live")),
            Boolean.parseBoolean(record.get("highlights"))
            
        );
    }

    private StreamingPackage convertToStreamingPackage(CSVRecord record)
    {
        return new StreamingPackage(
            Integer.parseInt(record.get("id")),
            record.get("name"),
            record.get("monthly_price_cents").isEmpty() ? 0.0 : Double.parseDouble(record.get("monthly_price_cents")),
            record.get("monthly_price_yearly_subscription_in_cents").isEmpty() ? 0.0 : Double.parseDouble(record.get("monthly_price_yearly_subscription_in_cents"))
          
        );
    }


    // Getters 

    public Set<Game> getGamesByTeam(String team)
    {
        return gamesByTeam.getOrDefault(team, new HashSet<>());
    }

    public Set<Game> getGamesByTeams(List<String> teams)
    {
        Set<Game> games = new HashSet<>();
        for(String team : teams)
        {
            games.addAll(getGamesByTeam(team));
        }

        return games;
    }

    public Set<Game> getGamesByTournament(String tournament)
    {
        return gamesByTournament.getOrDefault(tournament, new HashSet<>());
    }

    public List<StreamingOffer> getOffersForGame(int gameId)
    {
       return offersByGameId.getOrDefault(gameId, new ArrayList<>());
    }

    public StreamingPackage getPackageById(int packageId)
    {
        return packagesById.get(packageId);
    }

    public Collection<StreamingPackage> getAllPackages()
    {
        return packagesById.values();
    }


    public double getTeamLiveCoverageByPackageId(String teamName, int packageId)
    {
        Set<Game> games = getGamesByTeam(teamName);
        if(games.isEmpty()) return 0.0;
        int liveGamesCovered = 0;
        for(Game game : games)
        {
            for(StreamingOffer offer : getOffersForGame(game.getId()))
            {
                if(offer.getStreamingPackageId() == packageId && offer.isHasLive())
                {
                    liveGamesCovered++;
                    break;
                } 
            }
        }
        return (double) liveGamesCovered / games.size();
    }

    public double getTeamHighlightsCoverageByPackageId(String teamName, int packageId)
    {
        Set<Game> games = getGamesByTeam(teamName);
        if(games.isEmpty()) return 0.0;
        int highlightGamesCovered = 0;
        for(Game game : games)
        {
            for(StreamingOffer offer : getOffersForGame(game.getId()))
            {
                if(offer.getStreamingPackageId() == packageId && offer.isHasHighlights())
                {
                    highlightGamesCovered++;
                    break;
                } 
            }
        }
        return (double) highlightGamesCovered / games.size();

    }

    public double getTournamentLiveCoverageByPackageId(String tournamentName, int packageId) {
        Set<Game> games = getGamesByTournament(tournamentName);
        if (games.isEmpty()) return 0.0;
    
        int liveGamesCovered = 0;
        for(Game game : games) {
            for(StreamingOffer offer : getOffersForGame(game.getId())) {
                if(offer.getStreamingPackageId() == packageId && offer.isHasLive()) {
                    liveGamesCovered++;
                    break;
                }
            }
        }
        return (double) liveGamesCovered / games.size();
    }
    
    public double getTournamentHighlightsCoverageByPackageId(String tournamentName, int packageId) {
        Set<Game> games = getGamesByTournament(tournamentName);
        if (games.isEmpty()) return 0.0;
    
        int highlightGamesCovered = 0;
        for(Game game : games) {
            for(StreamingOffer offer : getOffersForGame(game.getId())) {
                if(offer.getStreamingPackageId() == packageId && offer.isHasHighlights()) {
                    highlightGamesCovered++;
                    break;
                }
            }
        }
        return (double) highlightGamesCovered / games.size();
    }


}
