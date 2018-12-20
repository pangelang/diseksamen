package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import model.User;
import utils.Config;

public class DatabaseConnection {

  private static Connection connection;

  public DatabaseConnection() {
    connection = getConnection();
  }

  /**
   * Get database connection
   *
   * @return a Connection object
   */
  public static Connection getConnection() {
    try {

      //Method won't create new connection everytime.
      //Change was made to enable transactions in get order methods to work.
      if (connection == null || connection.isClosed()) {
        // Set the dataabase connect with the data from the config
        String url =
                "jdbc:mysql://"
                        + Config.getDatabaseHost()
                        + ":"
                        + Config.getDatabasePort()
                        + "/"
                        + Config.getDatabaseName()
                        + "?serverTimezone=CET";

        String user = Config.getDatabaseUsername();
        String password = Config.getDatabasePassword();

        // Register the driver in order to use it
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());

        // create a connection to the database
        connection = DriverManager.getConnection(url, user, password);
      }

    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    return connection;
  }
}