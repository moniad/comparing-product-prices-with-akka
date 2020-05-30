import akka.actor.AbstractActor;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;

import java.sql.*;
import java.time.Duration;
import java.util.Random;

public class Server extends AbstractActor {
    private static final String SQLITE_DB_FILE_PATH = "akka.db";
    private static final String SQLITE_DB_CONNECTION_STR = "jdbc:sqlite:" + SQLITE_DB_FILE_PATH;
    private static final String CREATE_TABLE = "create table if not exists request (product_name string not null unique, occurrences_count int)";
    private static final String INSERT_QUERY = "insert into request values(?,0)";
    private static final String SELECT_QUERY = "select occurrences_count from request where product_name = ?";
    private static final String UPDATE_QUERY = "update request set occurrences_count = occurrences_count + 1 where product_name = ?";
    private static Connection dbConnection;
    private final String SERVER_LOG_STRING = "[SERVER] ";

    private static final SupervisorStrategy strategy //todo: modify
            = new AllForOneStrategy( // OneForOneStrategy
            10,
            Duration.ofMinutes(1),
            DeciderBuilder
                    .match(ArithmeticException.class, e -> SupervisorStrategy.resume())
                    .matchAny(o -> SupervisorStrategy.restart())
                    .build());
    private final Random random = new Random();
    // for logging
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    // must be implemented -> creates initial behaviour
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, productName -> {
                    System.out.println(SERVER_LOG_STRING + "RECEIVED msg: " + productName);

                    int price1 = getPrice();
                    int price2 = getPrice(); // TODO: needs to be paralleled
                    // todo: handle timeouts

                    int occurrenceCount = handleClientRequest(productName);
                    System.out.println(SERVER_LOG_STRING + productName.toUpperCase() + " handled " + occurrenceCount + " times");

                    int smallerPrice = Math.min(price1, price2);

                    ComparisonResponse comparisonResponse = ComparisonResponse.builder()
                            .smallerPrice(smallerPrice)
                            .occurrenceCount(occurrenceCount)
                            .build();

                    getSender().tell(comparisonResponse, getSelf());

                    System.out.println(SERVER_LOG_STRING + "SENT MSG: " + comparisonResponse.toString());

//                    if (s.startsWith("m")) {
//                        context().child("multiplyWorker").get().tell(s, getSelf()); // send task to child
//                    } else if (s.startsWith("d")) {
//                        context().child("divideWorker").get().tell(s, getSelf()); // send task to child
//                    } else if (s.startsWith("result")) {
//                        System.out.println(s);              // result from child
//                    }

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private int handleClientRequest(String productName) {
        try {
            return updateAndGetOccurrencesCount(productName);
        } catch (SQLException e) {
            // if the error message is "out of memory", it probably means no database file is found
            System.err.println(e.getMessage());
            try {
                if (dbConnection != null)
                    dbConnection.close();
            } catch (SQLException e2) {
//                 connection close failed.
                System.err.println(e2.getMessage());
            }
            return -1;
        }
    }

    private int updateAndGetOccurrencesCount(String productName) throws SQLException {
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


    // optional
    @Override
    public void preStart() {
        context().actorOf(Props.create(Z1_MultiplyWorker.class), "multiplyWorker"); // todo: remove
        context().actorOf(Props.create(Z1_DivideWorker.class), "divideWorker");

        try {
            // create a database connection
            dbConnection = DriverManager.getConnection(SQLITE_DB_CONNECTION_STR);
            Statement statement = dbConnection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public int getPrice() {
        try {
            Thread.sleep(random.nextInt(401) + 100); // 100-500 ms
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        return random.nextInt(10) + 1; // 1-10
    }
}
