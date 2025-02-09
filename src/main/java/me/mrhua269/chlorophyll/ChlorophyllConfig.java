package me.mrhua269.chlorophyll;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "chlorophyll")
public class ChlorophyllConfig implements ConfigData {
    public int worker_thread_count = Runtime.getRuntime().availableProcessors();
}
