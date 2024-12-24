package com.check24.streaming.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.check24.streaming.model.Game;
import com.check24.streaming.service.DataService;

import java.util.List;
import java.util.Set;

public class WebController 
{
    private final DataService dataService;

    public WebController(DataService dataService)
    {
        this.dataService = dataService;
    }

    @GetMapping("/")
    public String index(Model model)
    {
        return "index";
    }

    @PostMapping("/compare")
    public String compare(@RequestParam List<String> teams, Model model) 
    {
        Set<Game> games = dataService.getGamesByTeams(teams);
        model.addAttribute("games", games);
        return "comparison";
    }

}
