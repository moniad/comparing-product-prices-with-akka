package server.finder;

import lombok.Getter;

import java.sql.*;

public class DbClient {
    private static final String SQLITE_DB_FILE_PATH = "akka.db";
    private static final String SQLITE_DB_CONNECTION_STR = "jdbc:sqlite:" + SQLITE_DB_FILE_PATH;
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS request (product_name string NOT NULL UNIQUE, occurrences_count int)";
    private static final String UPSERT_QUERY = "INSERT INTO request values(?,1) ON CONFLICT(product_name) DO UPDATE SET occurrences_count=occurrences_count+1;";
    private static final String SELECT_QUERY = "SELECT occurrences_count FROM request WHERE product_name = ?";
    @Getter
    private static Connection dbConnection;

    static {
        try {
            dbConnection = DriverManager.getConnection(SQLITE_DB_CONNECTION_STR);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void updateOccurrencesCount(String productName) {
        try {
            PreparedStatement ps = dbConnection.prepareStatement(UPSERT_QUERY);
            ps.setString(1, productName);
            ps.executeUpdate();
        } catch (SQLException e) {  // if the error message is "out of memory", it probably means no database file is found
            System.err.println(e.getMessage());
            tryToCloseConnection();
        }
    }

    private static void tryToCloseConnection() {
        try {
            if (dbConnection != null)
                dbConnection.close();
        } catch (SQLException e2) { // connection close failed.
            System.err.println(e2.getMessage());
        }
    }

    public Integer getOccurrencesCount(String productName) {
        try {
            PreparedStatement ps = dbConnection.prepareStatement(SELECT_QUERY);
            ps.setString(1, productName);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return -1;
            }
            return Integer.parseInt(rs.getString("occurrences_count"));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public void createRequestTable() {
        try {
            // create a database connection
            Statement statement = dbConnection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
