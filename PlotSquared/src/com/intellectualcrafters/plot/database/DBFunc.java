/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = DBFunc.java
 * >> Generated by: Citymonstret at 2014-08-09 01:43
 */

package com.intellectualcrafters.plot.database;

import static com.intellectualcrafters.plot.PlotMain.connection;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Biome;

import com.intellectualcrafters.plot.Flag;
import com.intellectualcrafters.plot.FlagManager;
import com.intellectualcrafters.plot.Logger;
import com.intellectualcrafters.plot.Logger.LogLevel;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.PlotHomePosition;
import com.intellectualcrafters.plot.PlotId;
import com.intellectualcrafters.plot.PlotMain;

/**
 * @author Citymonstret
 */
public class DBFunc {
    
    // TODO MongoDB @Brandon

    /**
     * Set Plot owner
     * 
     * @param plot
     * @param uuid
     */
    public static void setOwner(final Plot plot, final UUID uuid) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE `plot` SET `owner` = ? WHERE `plot_id_x` = ? AND `plot_id_z` = ? ");
                    statement.setString(1, uuid.toString());
                    statement.setInt(2, plot.id.x);
                    statement.setInt(3, plot.id.y);
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.DANGER, "Could not set owner for plot " + plot.id);
                }
            }
        });
    }
    
    public static void createAllSettingsAndHelpers(ArrayList<Plot> plots) {
        HashMap<String, HashMap<PlotId, Integer>> stored = new HashMap< String, HashMap<PlotId, Integer>>();
        HashMap<Integer, ArrayList<UUID>> helpers = new HashMap<Integer, ArrayList<UUID>>();
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT `id`, `plot_id_x`, `plot_id_z`, `world` FROM `plot`");
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                int id = result.getInt("id");
                int idx = result.getInt("plot_id_x");
                int idz = result.getInt("plot_id_z");
                String world = result.getString("world");
                
                if (!stored.containsKey(world)) {
                    stored.put(world,new HashMap<PlotId, Integer>());
                }
                stored.get(world).put(new PlotId(idx,idz), id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        for (Plot plot:plots) {
            String world = Bukkit.getWorld(plot.world).getName();
            if (stored.containsKey(world)) {
                Integer id = stored.get(world).get(plot.id);
                if (id!=null) {
                    helpers.put(id,plot.helpers);
                }
            }
        }
        
        if (helpers.size()==0) {
            return;
        }
        
        // add plot settings
        Integer[] ids = helpers.keySet().toArray(new Integer[0]);
        StringBuilder statement = new StringBuilder("INSERT INTO `plot_settings` (`plot_plot_id`) values ");
        for (int i = 0; i<ids.length-1; i++) {
            statement.append("(?),");
        }
        statement.append("(?)");
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(statement.toString());
            for (int i = 0; i<ids.length; i++) {
                stmt.setInt(i+1, ids[i]);
            }
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // add plot helpers
        String prefix = "";
        statement = new StringBuilder("INSERT INTO `plot_helpers` (`plot_plot_id`, `user_uuid`) values ");
        for (Integer id:helpers.keySet()) {
            for (UUID helper:helpers.get(id)) {
                statement.append(prefix+"(?, ?)");
                prefix = ",";
            }
        }
        if (prefix.equals("")) {
            return;
        }
        try {
            stmt = connection.prepareStatement(statement.toString());
            int counter = 0;
            for (Integer id:helpers.keySet()) {
                for (UUID helper:helpers.get(id)) {
                    
                    stmt.setInt(counter*2+1, id);
                    stmt.setString(counter*2+2, helper.toString());
                    
                    counter++;
                }
            }
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            Logger.add(LogLevel.WARNING, "Failed to set helper for plots");
            e.printStackTrace();
        }
    }
    
    /**
     * Create a plot
     * 
     * @param plot
     */
    public static void createPlots(ArrayList<Plot> plots) {
        if (plots.size()==0) {
            return;
        }
        StringBuilder statement = new StringBuilder("INSERT INTO `plot`(`plot_id_x`, `plot_id_z`, `owner`, `world`) values ");
        
        for (int i = 0; i<plots.size()-1; i++) {
            statement.append("(?, ?, ?, ?),");
        }
        statement.append("(?, ?, ?, ?)");

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(statement.toString());
            for (int i = 0; i<plots.size(); i++) {
                Plot plot = plots.get(i);
                stmt.setInt(i*4+1, plot.id.x);
                stmt.setInt(i*4+2, plot.id.y);
                stmt.setString(i*4+3, plot.owner.toString());
                stmt.setString(i*4+4, plot.world);
            }
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.add(LogLevel.DANGER, "Failed to save plots!");
        }
    }
    
    /**
     * Create a plot
     * 
     * @param plot
     */
    public static void createPlot(Plot plot) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("INSERT INTO `plot`(`plot_id_x`, `plot_id_z`, `owner`, `world`) VALUES(?, ?, ?, ?)");
            stmt.setInt(1, plot.id.x);
            stmt.setInt(2, plot.id.y);
            stmt.setString(3, plot.owner.toString());
            stmt.setString(4, plot.world);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.add(LogLevel.DANGER, "Failed to save plot " + plot.id);
        }
    }

    /**
     * Create tables
     * 
     * @throws SQLException
     */
    public static void createTables(String database, boolean add_constraint) throws SQLException {
        boolean mysql = database.equals("mysql");

        Statement stmt = connection.createStatement();

        if (mysql) {
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot` (" + "`id` INT(11) NOT NULL AUTO_INCREMENT," + "`plot_id_x` INT(11) NOT NULL," + "`plot_id_z` INT(11) NOT NULL," + "`owner` VARCHAR(45) NOT NULL," + "`world` VARCHAR(45) NOT NULL," + "`timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," + "PRIMARY KEY (`id`)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_denied` (" + "`plot_plot_id` INT(11) NOT NULL," + "`user_uuid` VARCHAR(40) NOT NULL" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_helpers` (" + "`plot_plot_id` INT(11) NOT NULL," + "`user_uuid` VARCHAR(40) NOT NULL" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_trusted` (" + "`plot_plot_id` INT(11) NOT NULL," + "`user_uuid` VARCHAR(40) NOT NULL" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_settings` (" + "  `plot_plot_id` INT(11) NOT NULL," + "  `biome` VARCHAR(45) DEFAULT 'FOREST'," + "  `rain` INT(1) DEFAULT 0," + "  `custom_time` TINYINT(1) DEFAULT '0'," + "  `time` INT(11) DEFAULT '8000'," + "  `deny_entry` TINYINT(1) DEFAULT '0'," + "  `alias` VARCHAR(50) DEFAULT NULL," + "  `flags` VARCHAR(512) DEFAULT NULL," + "  `merged` INT(11) DEFAULT NULL," + "  `position` VARCHAR(50) NOT NULL DEFAULT 'DEFAULT'," + "  PRIMARY KEY (`plot_plot_id`)," + "  UNIQUE KEY `unique_alias` (`alias`)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            if (add_constraint) {
                stmt.addBatch("ALTER TABLE `plot_settings` ADD CONSTRAINT `plot_settings_ibfk_1` FOREIGN KEY (`plot_plot_id`) REFERENCES `plot` (`id`) ON DELETE CASCADE");
            }

        } else {
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot` (" + "`id` INTEGER(11) PRIMARY KEY," + "`plot_id_x` INT(11) NOT NULL," + "`plot_id_z` INT(11) NOT NULL," + "`owner` VARCHAR(45) NOT NULL," + "`world` VARCHAR(45) NOT NULL," + "`timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_denied` (" + "`plot_plot_id` INT(11) NOT NULL," + "`user_uuid` VARCHAR(40) NOT NULL" + ")");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_helpers` (" + "`plot_plot_id` INT(11) NOT NULL," + "`user_uuid` VARCHAR(40) NOT NULL" + ")");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_trusted` (" + "`plot_plot_id` INT(11) NOT NULL," + "`user_uuid` VARCHAR(40) NOT NULL" + ")");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS `plot_settings` (" + "  `plot_plot_id` INT(11) NOT NULL," + "  `biome` VARCHAR(45) DEFAULT 'FOREST'," + "  `rain` INT(1) DEFAULT 0," + "  `custom_time` TINYINT(1) DEFAULT '0'," + "  `time` INT(11) DEFAULT '8000'," + "  `deny_entry` TINYINT(1) DEFAULT '0'," + "  `alias` VARCHAR(50) DEFAULT NULL," + "  `flags` VARCHAR(512) DEFAULT NULL," + "  `merged` INT(11) DEFAULT NULL," + "  `position` VARCHAR(50) NOT NULL DEFAULT 'DEFAULT'," + "  PRIMARY KEY (`plot_plot_id`)" + ")");
        }

        stmt.executeBatch();
        stmt.clearBatch();
        stmt.close();

    }

    /**
     * Delete a plot
     * 
     * @param plot
     */
    public static void delete(final String world, final Plot plot) {
        PlotMain.removePlot(world, plot.id, false);
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                int id = getId(world, plot.id);
                try {
                    stmt = connection.prepareStatement("DELETE FROM `plot_settings` WHERE `plot_plot_id` = ?");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = connection.prepareStatement("DELETE FROM `plot_helpers` WHERE `plot_plot_id` = ?");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = connection.prepareStatement("DELETE FROM `plot_trusted` WHERE `plot_plot_id` = ?");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = connection.prepareStatement("DELETE FROM `plot` WHERE `id` = ?");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.DANGER, "Failed to delete plot " + plot.id);
                }
            }
        });
    }

    /**
     * Create plot settings
     * 
     * @param id
     * @param plot
     */
    public static void createPlotSettings(final int id, final Plot plot) {
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("INSERT INTO `plot_settings`(`plot_plot_id`) VALUES(" + "?)");
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public static int getId(String world, PlotId id2) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT `id` FROM `plot` WHERE `plot_id_x` = ? AND `plot_id_z` = ? AND world = ? ORDER BY `timestamp` ASC");
            stmt.setInt(1, id2.x);
            stmt.setInt(2, id2.y);
            stmt.setString(3, world);
            ResultSet r = stmt.executeQuery();
            int id = Integer.MAX_VALUE;
            while (r.next()) {
                id = r.getInt("id");
            }
            stmt.close();
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Get a plot id
     * 
     * @param plot_id
     * @return
     */
    /*
     * public static int getId(String world, PlotId id2) { Statement stmt =
     * null; try { stmt = connection.createStatement(); ResultSet r =
     * stmt.executeQuery("SELECT `id` FROM `plot` WHERE `plot_id_x` = '" + id2.x
     * + "' AND `plot_id_z` = '" + id2.y + "' AND `world` = '" + world +
     * "' ORDER BY `timestamp` ASC"); int id = Integer.MAX_VALUE;
     * while(r.next()) { id = r.getInt("id"); } stmt.close(); return id; }
     * catch(SQLException e) { e.printStackTrace(); } return Integer.MAX_VALUE;
     * }
     */

    /**
     * @return
     */
    public static HashMap<String, HashMap<PlotId, Plot>> getPlots() {
        try {
            DatabaseMetaData data = connection.getMetaData();
            ResultSet rs = data.getColumns(null, null, "plot", "plot_id");
            boolean execute = rs.next();
            if (execute) {
                Statement statement = connection.createStatement();
                statement.addBatch("ALTER IGNORE TABLE `plot` ADD `plot_id_x` int(11) DEFAULT 0");
                statement.addBatch("ALTER IGNORE TABLE `plot` ADD `plot_id_z` int(11) DEFAULT 0");
                statement.addBatch("UPDATE `plot` SET\n" + "    `plot_id_x` = IF(" + "        LOCATE(';', `plot_id`) > 0," + "        SUBSTRING(`plot_id`, 1, LOCATE(';', `plot_id`) - 1)," + "        `plot_id`" + "    )," + "    `plot_id_z` = IF(" + "        LOCATE(';', `plot_id`) > 0," + "        SUBSTRING(`plot_id`, LOCATE(';', `plot_id`) + 1)," + "        NULL" + "    )");
                statement.addBatch("ALTER TABLE `plot` DROP `plot_id`");
                statement.addBatch("ALTER IGNORE TABLE `plot_settings` ADD `flags` VARCHAR(512) DEFAULT NULL");
                statement.executeBatch();
                statement.close();
            }
            rs = data.getColumns(null, null, "plot_settings", "merged");
            if (!rs.next()) {
                Statement statement = connection.createStatement();
                statement.addBatch("ALTER TABLE `plot_settings` ADD `merged` int(11) DEFAULT NULL");
                statement.executeBatch();
                statement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap<String, HashMap<PlotId, Plot>> plots = new HashMap<String, HashMap<PlotId, Plot>>();
        new HashMap<String, World>();
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet r = stmt.executeQuery("SELECT `id`, `plot_id_x`, `plot_id_z`, `owner`, `world` FROM `plot`");
            PlotId plot_id;
            int id;
            Plot p;
            while (r.next()) {
                plot_id = new PlotId(r.getInt("plot_id_x"), r.getInt("plot_id_z"));
                id = r.getInt("id");
                String worldname = r.getString("world");
                HashMap<String, Object> settings = getSettings(id);
                UUID owner = UUID.fromString(r.getString("owner"));
                Biome plotBiome = Biome.FOREST;
                String[] flags_string;
                if (settings.get("flags") == null) {
                    flags_string = new String[] {};
                } else {
                    flags_string = ((String) settings.get("flags")).split(",");
                }
                Flag[] flags = new Flag[flags_string.length];
                for (int i = 0; i < flags.length; i++) {
                    if (flags_string[i].contains(":")) {
                        String[] split = flags_string[i].split(":");
                        flags[i] = new Flag(FlagManager.getFlag(split[0], true), split[1]);
                    } else {
                        flags[i] = new Flag(FlagManager.getFlag(flags_string[i], true), "");
                    }
                }
                ArrayList<UUID> helpers = plotHelpers(id);
                ArrayList<UUID> trusted = plotTrusted(id);
                ArrayList<UUID> denied = plotDenied(id);
                // boolean changeTime = ((Short) settings.get("custom_time") ==
                // 0) ? false : true;
                long time = 8000l;
                // if(changeTime) {
                // time = Long.parseLong(settings.get("time").toString());
                // }
                // boolean rain =
                // Integer.parseInt(settings.get("rain").toString()) == 1 ? true
                // : false;
                boolean rain;
                try {
                    rain = (int) settings.get("rain") == 1 ? true : false;
                } catch(Exception e) {
                    rain = false;
                }
                String alias = (String) settings.get("alias");
                if ((alias == null) || alias.equalsIgnoreCase("NEW")) {
                    alias = "";
                }
                PlotHomePosition position = null;
                for (PlotHomePosition plotHomePosition : PlotHomePosition.values()) {
                    if (settings.get("position") == null) {
                        position = PlotHomePosition.DEFAULT;
                        break;
                    }
                    if (plotHomePosition.isMatching((String) settings.get("position"))) {
                        position = plotHomePosition;
                    }
                }
                if (position == null) {
                    position = PlotHomePosition.DEFAULT;
                }
                int merged_int = settings.get("merged") == null ? 0 : (int) settings.get("merged");
                
                boolean[] merged = new boolean[4];
                for (int i = 0; i < 4; i++) {
                    merged[3-i] = (merged_int & (1 << i)) != 0;
                }
                p = new Plot(plot_id, owner, plotBiome, helpers, trusted, denied, /* changeTime */false, time, rain, alias, position, flags, worldname, merged);
                if (plots.containsKey(worldname)) {
                    plots.get(worldname).put((plot_id), p);
                } else {
                    HashMap<PlotId, Plot> map = new HashMap<PlotId, Plot>();
                    map.put((plot_id), p);
                    plots.put(worldname, map);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            Logger.add(LogLevel.WARNING, "Failed to load plots.");
            e.printStackTrace();
        }
        return plots;
    }

    /**
     * @param plot
     * @param rain
     */
    public static void setWeather(final String world, final Plot plot, final boolean rain) {
        plot.settings.setRain(rain);
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    int weather = rain ? 1 : 0;
                    PreparedStatement stmt = connection.prepareStatement("UPDATE `plot_settings` SET `rain` = ? WHERE `plot_plot_id` = ?");
                    stmt.setInt(1, weather);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.execute();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Could not set weather for plot " + plot.id);
                }
            }
        });
    }
    
    public static void setMerged(final String world, final Plot plot, final boolean[] merged) {
        plot.settings.setMerged(merged);
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    int n = 0;
                    for (int i = 0; i < 4; ++i) {
                        n = (n << 1) + (merged[i] ? 1 : 0);
                    }
                    PreparedStatement stmt = connection.prepareStatement("UPDATE `plot_settings` SET `merged` = ? WHERE `plot_plot_id` = ?");
                    stmt.setInt(1, n);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.execute();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Could not set merged for plot " + plot.id);
                }
            }
        });
    }

    public static void setFlags(final String world, final Plot plot, final Flag[] flags) {
        plot.settings.setFlags(flags);
        final StringBuilder flag_string = new StringBuilder();
        int i = 0;
        for (Flag flag : flags) {
            if (i != 0) {
                flag_string.append(",");
            }
            flag_string.append(flag.getKey() + ":" + flag.getValue());
            i++;
        }
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement stmt = connection.prepareStatement("UPDATE `plot_settings` SET `flags` = ? WHERE `plot_plot_id` = ?");
                    stmt.setString(1, flag_string.toString());
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.execute();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Could not set flag for plot " + plot.id);
                }
            }
        });
    }

    /**
     * @param plot
     * @param alias
     */
    public static void setAlias(final String world, final Plot plot, final String alias) {
        plot.settings.setAlias(alias);
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("UPDATE `plot_settings` SET `alias` = ?  WHERE `plot_plot_id` = ?");
                    stmt.setString(1, alias);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set alias for plot " + plot.id);
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * @param r
     */
    private static void runTask(Runnable r) {
        PlotMain.getMain().getServer().getScheduler().runTaskAsynchronously(PlotMain.getMain(), r);
    }

    /**
     * @param plot
     * @param position
     */
    public static void setPosition(final String world, final Plot plot, final String position) {
        plot.settings.setPosition(PlotHomePosition.valueOf(position));
        runTask(new Runnable() {
            @Override
            public void run() {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("UPDATE `plot_settings` SET `position` = ?  WHERE `plot_plot_id` = ?");
                    stmt.setString(1, position);
                    stmt.setInt(2, getId(world, plot.id));
                    stmt.executeUpdate();
                    stmt.close();
                } catch (SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set position for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * @param id
     * @return
     */
    public static HashMap<String, Object> getSettings(int id) {
        HashMap<String, Object> h = new HashMap<String, Object>();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT * FROM `plot_settings` WHERE `plot_plot_id` = ?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            String var;
            Object val;
            while (r.next()) {
                var = "biome";
                val = r.getObject(var);
                h.put(var, val);
                var = "rain";
                val = r.getObject(var);
                h.put(var, val);
                var = "custom_time";
                val = r.getObject(var);
                h.put(var, val);
                var = "time";
                val = r.getObject(var);
                h.put(var, val);
                var = "deny_entry";
                val = r.getObject(var);
                h.put(var, (short) 0);
                var = "alias";
                val = r.getObject(var);
                h.put(var, val);
                var = "position";
                val = r.getObject(var);
                h.put(var, val);
                var = "flags";
                val = r.getObject(var);
                h.put(var, val);
                var = "merged";
                val = r.getObject(var);
                h.put(var, val);
            }
            stmt.close();
            ;
        } catch (SQLException e) {
            Logger.add(LogLevel.WARNING, "Failed to load settings for plot: " + id);
            e.printStackTrace();
        }
        return h;
    }

    /**
     *
     */
    public static UUID everyone = UUID.fromString("1-1-3-3-7");

    /**
     * @param id
     * @return
     */
    private static ArrayList<UUID> plotDenied(int id) {
        ArrayList<UUID> l = new ArrayList<UUID>();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT `user_uuid` FROM `plot_denied` WHERE `plot_plot_id` = ?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            UUID u;
            while (r.next()) {
                u = UUID.fromString(r.getString("user_uuid"));
                l.add(u);
            }
            stmt.close();
        } catch (Exception e) {
            Logger.add(LogLevel.DANGER, "Failed to load denied for plot: " + id);
            e.printStackTrace();
        }
        return l;
    }

    /**
     * @param id
     * @return
     */
    private static ArrayList<UUID> plotHelpers(int id) {
        ArrayList<UUID> l = new ArrayList<UUID>();
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet r = stmt.executeQuery("SELECT `user_uuid` FROM `plot_helpers` WHERE `plot_plot_id` = " + id);
            UUID u;
            while (r.next()) {
                u = UUID.fromString(r.getString("user_uuid"));
                l.add(u);
            }
            stmt.close();
        } catch (SQLException e) {
            Logger.add(LogLevel.WARNING, "Failed to load helpers for plot: " + id);
            e.printStackTrace();
        }
        return l;
    }
    
    /**
     * @param id
     * @return
     */
    private static ArrayList<UUID> plotTrusted(int id) {
        ArrayList<UUID> l = new ArrayList<UUID>();
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet r = stmt.executeQuery("SELECT `user_uuid` FROM `plot_trusted` WHERE `plot_plot_id` = " + id);
            UUID u;
            while (r.next()) {
                u = UUID.fromString(r.getString("user_uuid"));
                l.add(u);
            }
            stmt.close();
        } catch (SQLException e) {
            Logger.add(LogLevel.WARNING, "Failed to load trusted users for plot: " + id);
            e.printStackTrace();
        }
        return l;
    }

    /**
     * @param plot
     * @param player
     */
    public static void removeHelper(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM `plot_helpers` WHERE `plot_plot_id` = ? AND `user_uuid` = ?");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Failed to remove helper for plot " + plot.id);
                }
            }
        });
    }
    
    /**
     * @param plot
     * @param player
     */
    public static void removeTrusted(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM `plot_trusted` WHERE `plot_plot_id` = ? AND `user_uuid` = ?");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Failed to remove trusted user for plot " + plot.id);
                }
            }
        });
    }

    /**
     * @param plot
     * @param player
     */
    public static void setHelper(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO `plot_helpers` (`plot_plot_id`, `user_uuid`) VALUES(?,?)");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set helper for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * @param plot
     * @param player
     */
    public static void setTrusted(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO `plot_trusted` (`plot_plot_id`, `user_uuid`) VALUES(?,?)");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set plot trusted for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * @param plot
     * @param player
     */
    public static void removeDenied(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM `plot_denied` WHERE `plot_plot_id` = ? AND `user_uuid` = ?");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Logger.add(LogLevel.WARNING, "Failed to remove denied for plot " + plot.id);
                }
            }
        });
    }

    /**
     * @param plot
     * @param player
     */
    public static void setDenied(final String world, final Plot plot, final OfflinePlayer player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO `plot_denied` (`plot_plot_id`, `user_uuid`) VALUES(?,?)");
                    statement.setInt(1, getId(world, plot.id));
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    Logger.add(LogLevel.WARNING, "Failed to set denied for plot " + plot.id);
                    e.printStackTrace();
                }
            }
        });
    }
}
