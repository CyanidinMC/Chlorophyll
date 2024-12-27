package me.mrhua269.chlorophyll.utils.bridges;

import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;

public interface ITaskSchedulingLevel {
    ChlorophyllLevelTickLoop chlorophyll$getTickLoop();

    void chlorophyll$setupTickLoop();
}
