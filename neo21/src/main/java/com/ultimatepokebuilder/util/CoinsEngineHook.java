package com.ultimatepokebuilder.util;

import com.ultimatepokebuilder.UltimatePokeBuilder;
import com.ultimatepokebuilder.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class CoinsEngineHook {

    private static Connection getConnection() throws Exception {
        if (Config.SERVER.useMySQL.get()) {
            String url = "jdbc:mysql://" + Config.SERVER.dbHost.get() + ":" + Config.SERVER.dbPort.get() + "/" + Config.SERVER.dbName.get() + "?useSSL=false&allowPublicKeyRetrieval=true";
            return DriverManager.getConnection(url, Config.SERVER.dbUser.get(), Config.SERVER.dbPass.get());
        } else {
            // Directly targets the SQLite file from your CoinsEngine config
            return DriverManager.getConnection("jdbc:sqlite:plugins/CoinsEngine/data.db");
        }
    }

    public static int getBalance(UUID uuid, String currency) {
        // FIX: We now treat the currency name as the COLUMN we are selecting
        String query = "SELECT " + currency + " FROM " + Config.SERVER.dbTable.get() + " WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return (int) rs.getDouble(currency);
            }
        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("CoinsEngine Database Error (getBalance): Could not find column '" + currency + "' or table '" + Config.SERVER.dbTable.get() + "'", e);
        }
        return 0;
    }

    public static boolean takeBalance(UUID uuid, String currency, int amount) {
        if (getBalance(uuid, currency) < amount) return false;

        // FIX: We subtract directly from the specific currency column
        String query = "UPDATE " + Config.SERVER.dbTable.get() + " SET " + currency + " = " + currency + " - ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, amount);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("CoinsEngine Deduct Error", e);
            return false;
        }
    }
}