package main.java.com.djrapitops.plan.database.tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import main.java.com.djrapitops.plan.Log;
import main.java.com.djrapitops.plan.database.databases.SQLDB;
import org.bukkit.Location;
import org.bukkit.World;

/**
 *
 * @author Rsl1122
 */
public class LocationsTable extends Table {

    private final String columnUserID;
    private final String columnID;
    private final String columnCoordinatesX;
    private final String columnCoordinatesZ;
    private final String columnWorld;

    public LocationsTable(SQLDB db, boolean usingMySQL) {
        super("plan_locations", db, usingMySQL);
        columnID = "id";
        columnUserID = "user_id";
        columnCoordinatesX = "x";
        columnCoordinatesZ = "z";
        columnWorld = "world_name";
    }

    @Override
    public boolean createTable() {
        UsersTable usersTable = db.getUsersTable();
        try {
            execute("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + columnID + " integer " + ((usingMySQL) ? "NOT NULL AUTO_INCREMENT" : "PRIMARY KEY") + ", "
                    + columnUserID + " integer NOT NULL, "
                    + columnCoordinatesX + " integer NOT NULL, "
                    + columnCoordinatesZ + " integer NOT NULL, "
                    + columnWorld + " varchar(64) NOT NULL, "
                    + (usingMySQL ? "PRIMARY KEY (" + usersTable.getColumnID() + "), " : "")
                    + "FOREIGN KEY(" + columnUserID + ") REFERENCES " + usersTable.getTableName() + "(" + usersTable.getColumnID() + ")"
                    + ")"
            );
            return true;
        } catch (SQLException ex) {
            Log.toLog(this.getClass().getName(), ex);
            return false;
        }
    }

    public boolean removeUserLocations(int userId) {
        PreparedStatement statement = null;
        try {
            statement = prepareStatement("DELETE FROM " + tableName + " WHERE (" + columnUserID + "=?)");
            statement.setInt(1, userId);
            statement.execute();
            return true;
        } catch (SQLException ex) {
            Log.toLog(this.getClass().getName(), ex);
            return false;
        } finally {
            close(statement);
        }
    }

    public List<Location> getLocations(int userId, HashMap<String, World> worlds) throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try {
            statement = prepareStatement("SELECT * FROM " + tableName + " WHERE (" + columnUserID + "=?)");
            statement.setInt(1, userId);
            set = statement.executeQuery();
            List<Location> locations = new ArrayList<>();
            while (set.next()) {
                locations.add(new Location(worlds.get(set.getString(columnWorld)), set.getInt(columnCoordinatesX), 0, set.getInt(columnCoordinatesZ)));
            }
            return locations;
        } finally {
            close(set);
            close(statement);
        }
    }

    public void saveAdditionalLocationsList(int userId, List<Location> locations) throws SQLException {
        if (locations == null || locations.isEmpty()) {
            return;
        }
        List<Location> newLocations = new ArrayList<>();
        newLocations.addAll(locations);
        PreparedStatement statement = null;
        try {
            statement = prepareStatement("INSERT INTO " + tableName + " ("
                    + columnUserID + ", "
                    + columnCoordinatesX + ", "
                    + columnCoordinatesZ + ", "
                    + columnWorld
                    + ") VALUES (?, ?, ?, ?)");
            boolean commitRequired = false;
            for (Location location : newLocations) {
                if (location == null) {
                    continue;
                }
                statement.setInt(1, userId);
                statement.setInt(2, (int) location.getBlockX());
                statement.setInt(3, (int) location.getBlockZ());
                World world = location.getWorld();
                if (world == null) {
                    continue;
                }
                statement.setString(4, world.getName());
                statement.addBatch();
                commitRequired = true;
            }
            if (commitRequired) {
                statement.executeBatch();
            }
        } finally {
            close(statement);
        }
    }
}