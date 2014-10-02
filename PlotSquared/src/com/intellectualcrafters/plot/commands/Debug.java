/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = Debug.java
 * >> Generated by: Citymonstret at 2014-08-09 01:41
 */

package com.intellectualcrafters.plot.commands;

import com.intellectualcrafters.plot.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author Citymonstret \\SuperCharged Compiler made by Citymonstret\\
 *         ||#Compiler:ALPHA-1.0#########################
 *         ||#ST:Java(1.7.*)\impl(bukkit)->api(s[])######
 *         ||#Section:\Debug\############################
 *         ||##Debug->Debug.properties|Debug.txt#########
 *         ||############################################ ||#Signed
 *         By:Citymonstret@IC##################
 *         \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
 */
public class Debug extends SubCommand {

    // private extends SubCommand^Implements {Command, Information} from
    // >>\\S.txt6\\
    public Debug() {
        super(Command.DEBUG, "Show debug information", "debug [msg]", CommandCategory.INFO);
        {
            /**
             * This.
             */
        }
    }

    @Override
    public boolean execute(Player plr, String... args) {
        PlotMain.getWorldSettings(plr.getWorld());
        if ((args.length > 0) && args[0].equalsIgnoreCase("msg")) {
            StringBuilder msg = new StringBuilder();
            for (C c : C.values()) {
                msg.append(c.s() + "\n");
            }
            PlayerFunctions.sendMessage(plr, msg.toString());
            return true;
        }
        StringBuilder information;
        String header, line, section;
        /**
         * {$notnull || compile:: \\Debug:Captions\\}
         */
        {
            information = new StringBuilder();
            header = C.DEUBG_HEADER.s();
            line = C.DEBUG_LINE.s();
            section = C.DEBUG_SECTION.s();
        }
        /**
         * {||direct:: load: debug::I>Captions::trsl} \\ if(missing)
         * set(default) -> this->(){} \\ echo line->line(Compiler.cpp ->
         * lineCompiler); when finished: now = this();
         * now(getter)->setter(this())->{ "string" = getter(this);
         * setter(string) = getter(this->setter); } when ^ finished compile;
         * if(^compile failed -> |this->failed.|tests->failed.| ||run test
         * {this->test}|on fail(action(){return FAILED})|
         */
        {
            StringBuilder worlds = new StringBuilder("");
            for (String world : PlotMain.getPlotWorlds()) {
                worlds.append(world + " ");
            }
            information.append(header);
            information.append(getSection(section, "Lag / TPS"));
            information.append(getLine(line, "Ticks Per Second", Lag.getTPS()));
            information.append(getLine(line, "Lag Percentage", (int) Lag.getPercentage() + "%"));
            information.append(getLine(line, "TPS Percentage", (int) Lag.getFullPercentage() + "%"));
            information.append(getSection(section, "PlotWorld"));
            information.append(getLine(line, "Plot Worlds", worlds));
            information.append(getLine(line, "Owned Plots", PlotMain.getPlots().size()));
            //information.append(getLine(line, "PlotWorld Size", PlotHelper.getWorldFolderSize() + "MB"));
            for(String world : PlotMain.getPlotWorlds()) {
                information.append(getLine(line, "World: " + world + " size", PlotHelper.getWorldFolderSize(Bukkit.getWorld(world))));
            }
            information.append(getLine(line, "Entities", PlotHelper.getEntities(plr.getWorld())));
            information.append(getLine(line, "Loaded Tile Entities", PlotHelper.getTileEntities(plr.getWorld())));
            information.append(getLine(line, "Loaded Chunks", PlotHelper.getLoadedChunks(plr.getWorld())));
            information.append(getSection(section, "RAM"));
            information.append(getLine(line, "Free Ram", RUtils.getFreeRam() + "MB"));
            information.append(getLine(line, "Total Ram", RUtils.getTotalRam() + "MB"));
            information.append(getSection(section, "Messages"));
            information.append(getLine(line, "Total Messages", C.values().length));
            information.append(getLine(line, "View all captions", "/plot debug msg"));
        }
        /**
         * {function:: SEND_MESSAGE |local player -> plr|local string ->
         * information.toString())}
         */
        {
            PlayerFunctions.sendMessage(plr, information.toString());
        }
        return true;
    }

    private String getSection(String line, String val) {
        return line.replaceAll("%val%", val) + "\n";
    }

    private String getLine(String line, String var, Object val) {
        return line.replaceAll("%var%", var).replaceAll("%val%", "" + val) + "\n";
    }

}
