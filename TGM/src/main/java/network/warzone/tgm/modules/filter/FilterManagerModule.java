package network.warzone.tgm.modules.filter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import network.warzone.tgm.TGM;
import network.warzone.tgm.match.Match;
import network.warzone.tgm.match.MatchModule;
import network.warzone.tgm.modules.filter.evaluate.AllowFilterEvaluator;
import network.warzone.tgm.modules.filter.evaluate.DenyFilterEvaluator;
import network.warzone.tgm.modules.filter.evaluate.FilterEvaluator;
import network.warzone.tgm.modules.filter.type.BlockExplodeFilterType;
import network.warzone.tgm.modules.filter.type.BuildFilterType;
import network.warzone.tgm.modules.filter.type.EnterFilterType;
import network.warzone.tgm.modules.filter.type.FilterType;
import network.warzone.tgm.modules.region.Region;
import network.warzone.tgm.modules.region.RegionManagerModule;
import network.warzone.tgm.modules.team.MatchTeam;
import network.warzone.tgm.modules.team.TeamManagerModule;
import network.warzone.tgm.util.Parser;
import com.sk89q.minecraft.util.commands.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class FilterManagerModule extends MatchModule {
    private List<FilterType> filterTypes = new ArrayList<>();

    @Override
    public void load(Match match) {
        if (match.getMapContainer().getMapInfo().getJsonObject().has("filters")) {
            for (JsonElement filterElement : match.getMapContainer().getMapInfo().getJsonObject().getAsJsonArray("filters")) {
                JsonObject filterJson = filterElement.getAsJsonObject();
                for (FilterType filterType : initFilter(match, filterJson)) {
                    filterTypes.add(filterType);
                    if (filterType instanceof Listener) {
                        TGM.get().registerEvents((Listener) filterType);
                    }
                }
            }
        }
    }

    @Override
    public void unload() {
        for (FilterType filterType : filterTypes) {
            if (filterType instanceof Listener) {
                HandlerList.unregisterAll((Listener) filterType);
            }
        }
    }

    private List<FilterType> initFilter(Match match, JsonObject jsonObject) {
        List<FilterType> filterTypes = new ArrayList<>();

        String type = jsonObject.get("type").getAsString().toLowerCase();

        if (type.equals("build")) {
            List<MatchTeam> matchTeams = Parser.getTeamsFromElement(match.getModule(TeamManagerModule.class), jsonObject.get("teams"));
            List<Region> regions = new ArrayList<>();

            for (JsonElement regionElement : jsonObject.getAsJsonArray("regions")) {
                Region region = match.getModule(RegionManagerModule.class).getRegion(match, regionElement);
                if (region != null) {
                    regions.add(region);
                }
            }

            FilterEvaluator filterEvaluator = initEvaluator(match, jsonObject);
            String message = ChatColor.translateAlternateColorCodes('&', jsonObject.get("message").getAsString());

            filterTypes.add(new BuildFilterType(matchTeams, regions, filterEvaluator, message));
        } else if (type.equals("enter")) {
            List<MatchTeam> matchTeams = Parser.getTeamsFromElement(match.getModule(TeamManagerModule.class), jsonObject.get("teams"));
            List<Region> regions = new ArrayList<>();

            for (JsonElement regionElement : jsonObject.getAsJsonArray("regions")) {
                Region region = match.getModule(RegionManagerModule.class).getRegion(match, regionElement);
                if (region != null) {
                    regions.add(region);
                }
            }

            FilterEvaluator filterEvaluator = initEvaluator(match, jsonObject);
            String message = ChatColor.translateAlternateColorCodes('&', jsonObject.get("message").getAsString());

            filterTypes.add(new EnterFilterType(matchTeams, regions, filterEvaluator, message));
        } else if (type.equals("block-explode")) {
            List<Region> regions = new ArrayList<>();
            for (JsonElement regionElement : jsonObject.getAsJsonArray("regions")) {
                Region region = match.getModule(RegionManagerModule.class).getRegion(match, regionElement);
                if (region != null) {
                    regions.add(region);
                }
            }

            FilterEvaluator filterEvaluator = initEvaluator(match, jsonObject);
            filterTypes.add(new BlockExplodeFilterType(regions, filterEvaluator));
        }

        return filterTypes;
    }

    private FilterEvaluator initEvaluator(Match match, JsonObject parent) {
        switch (parent.get("evaluate").getAsString()) {
            case "allow":
                return new AllowFilterEvaluator();
            case "deny":
                return new DenyFilterEvaluator();
            default:
                return null;
        }
    }
}
