package com.giri.ai.loanprequal.controller;

public record RiskScoreResponse(double defaultProbability, String riskBand) {}