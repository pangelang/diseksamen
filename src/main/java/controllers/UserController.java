package controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import model.User;
import utils.DatabaseConnection;
import utils.Hashing;
import utils.Log;
import utils.Token;

public class UserController {

  private static DatabaseConnection dbCon;

  public UserController() {
    dbCon = new DatabaseConnection();
  }

  /**
   *
   * @param id
   * @return user
   */
  public static User getUser(int id) {

    //Initializing result set
    ResultSet rs = null;

    try{
      // Check for connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "SELECT * FROM user where u_id = ?";

      // Build the query for DB
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setInt(1, id);

      //Executing query
      rs = preparedStatement.executeQuery();

      //Declaring object
      User user;

      // Get first object since we only have one, form the user and return it.
      if (rs.next()) {
        user = formUser(rs);

        //Return user
        return user;

      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    } finally {
      try {
        rs.close();
      } catch (SQLException h) {
        h.printStackTrace();
        try {
          dbCon.getConnection().close();

        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
    // Return null
    return null;
  }

  /**
   * Get all users in database
   *
   * @return users
   */
  public static ArrayList<User> getUsers() {

    //Initializing result set
    ResultSet rs = null;

    try{
      // Check for DB connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "SELECT * FROM user";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);

      //Executing query
      rs = preparedStatement.executeQuery();

      //Instantiating users ArrayList
      ArrayList<User> users = new ArrayList<>();

      // Loop through DB Data
      while (rs.next()) {
        User user = formUser(rs);
        // Add element to list
        users.add(user);
      }

      //Return users ArrayList
      return users;

    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    } finally {
      try {
        rs.close();
      } catch (SQLException h) {
        h.printStackTrace();
        try {
          dbCon.getConnection().close();

        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
    // Return the list of users
    return null;
  }

  /**
   *
   * @param user
   * @return user
   */
  public static User createUser(User user) {

    try{
      // Set creation time for user.
      user.setCreatedTime(System.currentTimeMillis() / 1000L);

      // Check for DB Connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }


      // TODO: Hash the user password before saving it. FIX
      //Hashing pw
      user.setPassword(Hashing.sha(user.getPassword()));

      //Building SQL statement
      String sql = "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES(?,?,?,?,?)";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setString(1, user.getFirstname());
      preparedStatement.setString(2, user.getLastname());
      preparedStatement.setString(3, user.getPassword()  );
      preparedStatement.setString(4, user.getEmail());
      preparedStatement.setLong(5, user.getCreatedTime());

      //Executing update
      int affectedRows = preparedStatement.executeUpdate();

      // Get our key back in order to apply it to an object as ID
      ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
      if (generatedKeys.next()&&affectedRows==1) {
        user.setId(generatedKeys.getInt(1));

        //Return user
        return user;

      } else {
        // Return null if user has not been inserted into database
        return null;
      }
    }catch (SQLException e){
      e.printStackTrace();
    }finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    // Return user
    return user;
  }

  /**
   *
   * @param user
   * @return affected
   */
  public static boolean updateUser(User user) {

    try {

      //Checking for connection
      if (dbCon == null || dbCon.getConnection().isValid(1)) {
        dbCon = new DatabaseConnection();
      }

      //Hashing pw
      user.setPassword(Hashing.sha(user.getPassword()));

      //Building SQL statement
      String sql = "UPDATE user SET first_name = ?, last_name = ?, password = ?, email = ? WHERE u_id = ?";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setString(1, user.getFirstname());
      preparedStatement.setString(2, user.getLastname());
      preparedStatement.setString(3, user.getPassword());
      preparedStatement.setString(4, user.getEmail());
      preparedStatement.setLong(5, user.getId());

      //Executing update
      int rowsAffected = preparedStatement.executeUpdate();

      if (rowsAffected==1){

        //Returning true
        return true;

      } else {
        return false;
      }

    }catch (SQLException e){
      e.printStackTrace();
    }
    finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   *
   * @param user
   * @return user
   */
  public static User login(User user) {

    ResultSet rs = null;

    //Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseConnection();
    }

    //Hashing pw
    user.setPassword(Hashing.sha(user.getPassword()));

    try {
      //Building SQL query
      String sql = "SELECT * FROM user WHERE email = ? AND password = ?";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setString(1, user.getEmail());
      preparedStatement.setString(2, user.getPassword());

      //Executing the query
      rs = preparedStatement.executeQuery();

      //Looping through result set once, forming user and creating a token
      if (rs.next()) {
        user = formUser(rs);

        user.setToken(Token.createToken(user));

        System.out.println("Logged in");

        //Return user
        return user;

      } else {
        System.out.println("No user found");
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    } finally {
      try {
        rs.close();

      } catch (SQLException h) {
        h.printStackTrace();
        try {
          dbCon.getConnection().close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }


  /**
   *
   * @param idUser
   * @return deleted
   */
  public static boolean deleteUser(int idUser) {

    try {
      // Check for DB Connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "Delete FROM user where u_id = ?";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setInt(1, idUser);

      //Executing update
      int affectedRows = preparedStatement.executeUpdate();

      if (affectedRows == 1) {

        //Return true
        return true;

      }else{
        return false;
      }
    }catch (SQLException e){
      e.printStackTrace();
    }finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  //Method forming user
  public static User formUser(ResultSet rs){
    try{
      User user = new User(rs.getInt("u_id"),
              rs.getString("first_name"),
              rs.getString("last_name"),
              rs.getString("password"),
              rs.getString("email"));

      return user;
    }catch(SQLException e){
      e.printStackTrace();
    }
    return null;
  }
}