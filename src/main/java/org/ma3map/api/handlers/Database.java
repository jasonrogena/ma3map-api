package org.ma3map.api.handlers;

import org.ma3map.api.handlers.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Jason Rogena <jasonrogena@gmail.com>
 * @since 2015-03-08
 *
 * This class handles all transations with the ma3map database
 */
public class Database {

    private static final String TAG = "ma3map.Database";
    private static final String CONNECTION_URL = "jdbc:postgresql://127.0.0.1/ma3map";
    private static final String DB_USER = "ma3map";
    private static final String DB_PASS = "kj432@cF23pl&d";

    private Connection connection;
    public Database() {
        try {
            connection = DriverManager.getConnection(CONNECTION_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
            Log.e(this.TAG, "An error occurred while trying to initialize the connection to the database");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public ResultSet execQuery(String query, boolean expectingResult) {
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(query);
            if(expectingResult) {
                ResultSet rs = ps.executeQuery();
                return rs;
            }
            else {
                ps.executeQuery();
            }
        }
        catch (SQLException e) {
            Log.e(this.TAG, "Unable to execute '"+query+"'");
            e.printStackTrace();
        }
        finally {
            if(ps != null) {
                try{
                    ps.close();
                }
                catch (SQLException e) {
                    Log.e(this.TAG, "Unable to close the prepared statement");
                }
            }
        }
        return null;
    }

    public int execInsertQuery(PreparedStatement ps) {
        try {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            int lastId = -1;
            if(rs.next()) {
                lastId = rs.getInt(1);
            }
            return lastId;
        }
        catch (SQLException e) {
            Log.e(this.TAG, "Unable to run insert prepared statement");
            e.printStackTrace();
        }
        finally {
            if(ps != null) {
                try{
                    ps.close();
                }
                catch (SQLException e) {
                    Log.e(this.TAG, "Unable to close the prepared statement");
                }
            }
        }
        return -1;
    }
    
    public boolean close() {
        if(connection != null) {
            try {
                connection.close();
                return true;
            }
            catch (SQLException e) {
                Log.e(this.TAG, "Unable to close the database connection");
                e.printStackTrace();
            }
        }
        return false;
    }
}
