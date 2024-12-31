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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;

import com.check24.streaming.model.Game;
import com.check24.streaming.model.StreamingOffer;
import com.check24.streaming.model.StreamingPackage;

/**
 * Service class responsible for loading, managing, and querying streaming-related data from CSV files.
 * Handles three main data types: games, streaming offers, and streaming packages.
 * Provides methods for accessing and analyzing coverage statistics for teams and tournaments.
 */

@Service
public class DataService 
{
    private List<CSVRecord> gameData;
    private List<CSVRecord> streamingOfferData;
    private List<CSVRecord> streamingPackageData;
    private Map<Integer,Game> gamesById = new HashMap<>(); /** Maps game IDs to their corresponding Game objects */  
    private Map<String, Set<Game>> gamesByTeam = new HashMap<>(); /** Maps team names to their set of associated games */
    private Map<Integer, StreamingPackage> packagesById = new HashMap<>(); /** Maps packages to their corresponding IDs */
    private Map<String, Set<Game>> gamesByTournament = new HashMap<>(); /** Maps tournament names to their set of associated games */
    private Map<Integer, List<StreamingOffer>> offersByGameId = new HashMap<>(); /** Maps game IDs to their corresponding list of StreamingOffer objects */


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
            Reader reader = new InputStreamReader(inputStream, "UTF-8"))
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

    
    /**
     * Loads game data from bc_game.csv.
     * Expected CSV format:
     * - id: unique game identifier
     * - team_home: home team name
     * - team_away: away team name
     * - starts_at: game start time
     * - tournament_name: name of the tournament
     * @return List of CSV records containing game data
     */
    
    private List<CSVRecord> loadGameData() {
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

    /**
     * Loads streaming offer data from bc_streaming_offer.csv.
     * Expected CSV format:
     * - game_id: reference to the game
     * - streaming_package_id: reference to the package
     * - live: boolean (1/0) indicating live streaming availability
     * - highlights: boolean (1/0) indicating highlights availability
     * @return List of CSV records containing streaming offer data
     */
    private List<CSVRecord> loadStreamingOfferData() {
       streamingOfferData = loadData("bc_streaming_offer.csv");
       for(CSVRecord record : streamingOfferData)
       {
        StreamingOffer offer = convertToStreamingOffer(record);
        offersByGameId.computeIfAbsent(offer.getGameId(),k -> new ArrayList<>()).add(offer);
       }
       return streamingOfferData;
    }

    /**
     * Loads streaming package data from bc_streaming_package.csv.
     * Expected CSV format:
     * - id: unique package identifier
     * - name: package name
     * - monthly_price_cents: monthly subscription price in cents
     * - monthly_price_yearly_subscription_in_cents: yearly subscription monthly price in cents
     * @return List of CSV records containing streaming package data
     */
    private List<CSVRecord> loadStreamingPackageData() {
        streamingPackageData = loadData("bc_streaming_package.csv");
        for (CSVRecord record : streamingPackageData) {
            StreamingPackage pkg = convertToStreamingPackage(record);
            packagesById.put(pkg.getStreamingPackageId(), pkg);
        }
        return streamingPackageData;
    }

    /**
     * Converts a CSV record into a Game object.
     * @param record The CSV record containing game data
     * @return A new Game object populated with the record's data
     */
    private Game convertToGame(CSVRecord record) {
        return new Game(
            Integer.parseInt(record.get("id")),
            record.get("team_home"),
            record.get("team_away"),
            record.get("starts_at"),
            record.get("tournament_name")
        );
    }

    private StreamingOffer convertToStreamingOffer(CSVRecord record) {
        return new StreamingOffer(
            Integer.parseInt(record.get("game_id")),
            Integer.parseInt(record.get("streaming_package_id")),
            record.get("live").equals("1"),
            record.get("highlights").equals("1")
            
        );
    }

    private StreamingPackage convertToStreamingPackage(CSVRecord record) {
        return new StreamingPackage(
            Integer.parseInt(record.get("id")),
            record.get("name"),
            record.get("monthly_price_cents").isEmpty() ? 0.0 : Double.parseDouble(record.get("monthly_price_cents")),
            record.get("monthly_price_yearly_subscription_in_cents").isEmpty() ? 0.0 : Double.parseDouble(record.get("monthly_price_yearly_subscription_in_cents"))
          
        );
    }


    // Getters 

    public List<String> getAllTeams() {
        Set<String> teams = gameData.stream()
            .flatMap(record -> Stream.of(
                record.get("team_home"), 
                record.get("team_away")
            ))
            .collect(Collectors.toSet());
    
        return new ArrayList<>(teams);
    }

    public List<String> getAllTournaments() {
        return gameData.stream()
            .map(record -> record.get("tournament_name"))
            .distinct()
            .collect(Collectors.toList());
    }

    public Set<Game> getGamesByTeam(String team) {
        return gamesByTeam.getOrDefault(team, new HashSet<>());
    }

    public Set<Game> getGamesByTeams(List<String> teams) {
        Set<Game> games = new HashSet<>();
        for(String team : teams) {
            games.addAll(getGamesByTeam(team));
        }

        return games;
    }

    public Set<Game> getGamesByTournament(String tournament) {
        return gamesByTournament.getOrDefault(tournament, new HashSet<>());
    }

    public List<StreamingOffer> getOffersForGame(int gameId) {
       return offersByGameId.getOrDefault(gameId, new ArrayList<>());
    }

    public StreamingPackage getPackageById(int packageId) {
        return packagesById.get(packageId);
    }

    public Collection<StreamingPackage> getAllPackages() {
        return packagesById.values();
    }

    public String getGameMonth(int gameId) {
        return gamesById.get(gameId).getStartTime().split(" ")[0].split("-")[1];
    }

    public String getGameYear(int gameId) {
        return gamesById.get(gameId).getStartTime().split(" ")[0].split("-")[0];
    }

    /**
     * Calculates the percentage of live game coverage for a specific team and streaming package.
     * @param teamName The name of the team to check coverage for
     * @param packageId The ID of the streaming package
     * @return A value between 0.0 and 1.0 representing the percentage of team's games available for live streaming
     */
    public double getTeamLiveCoverageByPackageId(String teamName, int packageId) {
        Set<Game> games = getGamesByTeam(teamName);
        if(games.isEmpty()) return 0.0;
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

    /**
     * Calculates the percentage of highlights coverage for a specific team and streaming package.
     * @param teamName The name of the team to check coverage for
     * @param packageId The ID of the streaming package
     * @return A value between 0.0 and 1.0 representing the percentage of team's games available with highlights
     */
    public double getTeamHighlightsCoverageByPackageId(String teamName, int packageId) {
        Set<Game> games = getGamesByTeam(teamName);
        if(games.isEmpty()) return 0.0;
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

    // Similar to the above methods, but for tournaments instead of teams
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
