package com.fuelnet.fuelnet.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fuelnet.fuelnet.dto.FuelRegulatedDto;

import tools.jackson.databind.ObjectMapper;

@Service
public class FuelPriceScraperService {

    @Value("${scraper.python.path}")
    private String pythonPath;

    @Value("${scraper.script.path}")
    private String scriptPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FuelRegulatedDto runScraper() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();

        if (exit != 0) {
            throw new RuntimeException("Scraper falló con código " + exit + ": " + output);
        }

        return objectMapper.readValue(output, FuelRegulatedDto.class);
    }
}
