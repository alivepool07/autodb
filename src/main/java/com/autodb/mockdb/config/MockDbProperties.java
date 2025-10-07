package com.autodb.mockdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mockdb")
public class MockDbProperties {

    private boolean enabled = true;
    private SeedLevel level = SeedLevel.LOW;
    private boolean useFaker = false;

    public enum SeedLevel { LOW, MID, HIGH }

    public int resolveCount() {
        return switch (level) {
            case LOW -> 100;
            case MID -> 500;
            case HIGH -> 1000;
        };
    }


    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public SeedLevel getLevel() { return level; }
    public void setLevel(SeedLevel level) { this.level = level; }

    public boolean isUseFaker() { return useFaker; }
    public void setUseFaker(boolean useFaker) { this.useFaker = useFaker; }
}
