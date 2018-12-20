package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import cache.ProductCache;
import model.Product;
import model.User;
import utils.Log;

public class ProductController {

  //For establishing a connection to the database
  private static DatabaseController dbCon;

  public ProductController() {
    dbCon = new DatabaseController();
  }

  /**
   *
   * @param id
   * @return
   */
  public static Product getProduct(int id) {

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the SQL query for the DB
    String sql = "SELECT * FROM product where p_id=" + id;

    // Run the query in the DB and make an empty object to return
    ResultSet rs = dbCon.query(sql);
    Product product = null;

    try {
      // Get first row and create the object and return it
      if (rs.next()) {
        product = formProduct(rs);

        // Return the product
        return product;
      } else {
        System.out.println("No product found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return empty object
    return product;
  }

  /**
   * Works in the same way as method above
   *
   * @param sku
   * @return
   */
  public static Product getProductBySku(String sku) {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    String sql = "SELECT * FROM product where sku='" + sku + "'";

    ResultSet rs = dbCon.query(sql);
    Product product = null;

    try {
      if (rs.next()) {
        product = formProduct(rs);

        return product;
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    return product;
  }

  /**
   * Get all products in database
   *
   * @return
   */
  public static ArrayList<Product> getProducts() {

    //Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // TODO: Use caching layer.: FIX (see ProductEndpoints)

      //Build SQL query
      String sql = "SELECT * FROM product";

      //Run the query
      ResultSet rs = dbCon.query(sql);

      //Instantiate an ArrayList for the products
      ArrayList<Product> products = new ArrayList<Product>();

      try {
        //Getting first row in result set, setting product with the form method and adding to ArrayList
        //before going to next row
        while (rs.next()) {
          Product product = formProduct(rs);

          products.add(product);
        }
      } catch (SQLException ex) {
        System.out.println(ex.getMessage());
      }

    return products;
  }

  /**
   *
   * @param product
   * @return
   */
  public static Product createProduct(Product product) {

    // Set creation time for product.
    product.setCreatedTime(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the product in the DB
    int productID = dbCon.insert(
        "INSERT INTO product(product_name, sku, price, description, stock, product_created_at) VALUES('"
            + product.getName()
            + "', '"
            + product.getSku()
            + "', '"
            + product.getPrice()
            + "', '"
            + product.getDescription()
            + "', '"
            + product.getStock()
            + "', '"
            + product.getCreatedTime()
            + "')");

    if (productID != 0) {
      //Update the productid of the product before returning
      product.setId(productID);
    } else{
      // Return null if product has not been inserted into database
      return null;
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
