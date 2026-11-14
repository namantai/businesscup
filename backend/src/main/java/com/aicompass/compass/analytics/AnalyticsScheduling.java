package com.aicompass.compass.analytics;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@Configuration
@EnableScheduling
@ConditionalOnWebApplication
public class AnalyticsScheduling {}
