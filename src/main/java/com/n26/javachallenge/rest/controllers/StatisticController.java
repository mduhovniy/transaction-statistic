package com.n26.javachallenge.rest.controllers;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.services.StatisticService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/statistics")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping
    public Statistic getStatisticForLastMinute() {
        return statisticService.getStatisticForLastMinute();
    }
}
