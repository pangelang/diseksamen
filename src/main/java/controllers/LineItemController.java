package controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import model.LineItem;
import model.Product;
import utils.DatabaseConnection;

public class LineItemController {

  private static DatabaseConnection dbCon;

  public LineItemController() {
    dbCon = new DatabaseConnection();
  }

  public static LineItem createLineItem(LineItem lineItem, int orderID) {

    try {
      // Check for DB Connection
      if (dbCon == null) {
        dbCon = new DatabaseConnection();
      }

      //Building SQL statement
      String sql = "INSERT INTO line_item (product_id, order_id, l_price, quantity) VALUES(?,?,?,?)";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setInt(1, lineItem.getProduct().getId());
      preparedStatement.setInt(2, orderID);
      preparedStatement.setFloat(3, lineItem.getPrice());
      preparedStatement.setInt(4, lineItem.getQuantity());

      //Executing update
      int rowsAffected = preparedStatement.executeUpdate();

      // Get our key back in order to apply it to an object as ID
      ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
      if (generatedKeys.next()&&rowsAffected==1) {

        lineItem.setId(generatedKeys.getInt(1));
        // Get the ID of the product, since the user will not send it to us.
        lineItem.getProduct().setId(ProductController.getProductBySku(lineItem.getProduct().getSku()).getId());

        //Return line item
        return lineItem;

      } else {
        // Return null if line item has not been inserted into database
        return null;
      }
    }catch (SQLException e){
      e.printStackTrace();
    }
    //Returning null at this point
    return lineItem;
  }

  //Method forming line item
  public static LineItem formLineItem (ResultSet rs, Product product) {
    try {
      LineItem lineItem = new LineItem(rs.getInt("l_id"),product,
              rs.getInt("quantity"),
              rs.getFloat("l_price"));

      return lineItem;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
  
}
