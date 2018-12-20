package controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import model.Product;
import utils.DatabaseConnection;
import utils.Log;

public class ProductController {

  //For establishing a connection to the database
  private static DatabaseConnection dbCon;

  public ProductController() {
    dbCon = new DatabaseConnection();
  }

  /**
   *
   * @param id
   * @return
   */
  public static Product getProduct(int id) {

    //Initializing result set
    ResultSet rs = null;

    try {
      // check for connection
      if (dbCon == null) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "SELECT * FROM product where p_id = ?";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setInt(1, id);

      //Executing query
      rs = preparedStatement.executeQuery();

      //Declaring object
      Product product;

      // Get first row and create the object and return it
      if (rs.next()) {
        product = formProduct(rs);

        // Return the product
        return product;

      } else {
        System.out.println("No product found");
      }
    }catch (SQLException e){
      e.printStackTrace();
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
    // Return empty object
    return null;
  }

  /**
   * Works in the same way as method above
   *
   * @param sku
   * @return
   */
  public static Product getProductBySku(String sku) {

    ResultSet rs = null;

    try {
      // check for connection
      if (dbCon == null) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement and executing query
      String sql = "SELECT * FROM product where sku = ?";

      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setString(1, sku);

      rs = preparedStatement.executeQuery();

      Product product;

      if (rs.next()) {
        product = formProduct(rs);
        return product;
      } else {
        System.out.println("No product found");
      }
    }catch (SQLException e){
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Get all products in database
   *
   * @return
   */
  public static ArrayList<Product> getProducts() {

    //Initializing result set
    ResultSet rs = null;

    try {
      //Check for connection
      if (dbCon == null) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "SELECT * FROM product";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);

      //Executing query
      rs = preparedStatement.executeQuery();

      // TODO: Use caching layer.: FIX
      ArrayList<Product> products = new ArrayList<>();

      //Getting first row in result set, setting product with the form method and adding to ArrayList
      //before going to next row
      while (rs.next()) {
        Product product = formProduct(rs);
        products.add(product);
      }

      //Returning products
      return products;

    }catch(SQLException ex){
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
    return null;
  }

  /**
   *
   * @param product
   * @return
   */
  public static Product createProduct(Product product) {

    try {
      // Set creation time for product.
      product.setCreatedTime(System.currentTimeMillis());


      // Check for DB Connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "INSERT INTO product(product_name, sku, price, description, stock, product_created_at) VALUES(?,?,?,?,?,?)";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setString(1, product.getName());
      preparedStatement.setString(2, product.getSku());
      preparedStatement.setFloat(3, product.getPrice());
      preparedStatement.setString(4, product.getDescription());
      preparedStatement.setLong(5, product.getStock());
      preparedStatement.setLong(6, product.getCreatedTime());

      //Executing update
      int rowsAffected = preparedStatement.executeUpdate();

      // Get our key back in order to apply it to an object as ID
      ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

      if (generatedKeys.next()&&rowsAffected==1) {

        product.setId(generatedKeys.getInt(1));

        //Returning product
        return product;

      } else {
        // Return null if product has not been inserted into database
        return null;
      }
    }catch (SQLException e){
      e.printStackTrace();
    } finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    // Return product
    return product;
  }

  //Method for forming products
  public static Product formProduct(ResultSet rs) {
    try {
        Product product = new Product(rs.getInt("p_id"),
                rs.getString("product_name"),
                rs.getString("sku"),
                rs.getFloat("price"),
                rs.getString("description"),
                rs.getInt("stock"));

      return product;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
}
