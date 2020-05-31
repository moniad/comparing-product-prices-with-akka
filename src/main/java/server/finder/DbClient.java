package server.finder;

import lombok.Getter;

import java.sql.*;

public class DbClient {
    private static final String SQLITE_DB_FILE_PATH = "akka.db";
    private static final String SQLITE_DB_CONNECTION_STR = "jdbc:sqlite:" + SQLITE_DB_FILE_PATH;
    private static final String CREATE_TABLE = "create table if not exists request (product_name string not null unique, occurrences_count int)";
    private static final String INSERT_QUERY = "insert into request values(?,0)";
    private static final String SELECT_QUERY = "select occurrences_count from request where product_name = ?";
    private static final String UPDATE_QUERY = "update request set occurrences_count = occurrences_count + 1 where product_name = ?";
    @Getter
    private static Connection dbConnection;

    static {
        try {
            dbConnection = DriverManager.getConnection(SQLITE_DB_CONNECTION_STR);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public int updateAndGetOccurrencesCount(String productName) {
        try {
            PreparedStatement ps;
            int occurrencesCount = getOccurrencesCount(productName);
            if (occurrencesCount >= 0) {
                ps = dbConnection.prepareStatement(UPDATE_QUERY); // todo: zapis do bazy ma nie spowalniać odpowiedzi do klienta
            } else {
                ps = dbConnection.prepareStatement(INSERT_QUERY);
            }
            ps.setString(1, productName);
            ps.executeUpdate();
            return occurrencesCount + 1;
        } catch (SQLException e) {  // if the error message is "out of memory", it probably means no database file is found
            System.err.println(e.getMessage());
            try {
                if (dbConnection != null)
                    dbConnection.close();
            } catch (SQLException e2) { // connection close failed.
                System.err.println(e2.getMessage());
            }
            return -1;
        }
    }

    private int getOccurrencesCount(String productName) throws SQLException {
        PreparedStatement ps = dbConnection.prepareStatement(SELECT_QUERY);
        ps.setString(1, productName);

        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            return -1;
        }
        return Integer.parseInt(rs.getString("occurrences_count"));
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
