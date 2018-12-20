package controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import model.*;
import utils.DatabaseConnection;

public class OrderController {

  //For establishing connection to the database
  private static DatabaseConnection dbCon;

  public OrderController() {
    dbCon = new DatabaseConnection();
  }

  /**
   *
   * @param orderId
   * @return order
   */
  public static Order getOrder(int orderId) {

    //Initializing result set
    ResultSet rs = null;

    try{
      // check for connection
      if (dbCon == null || dbCon.getConnection().isClosed() ) {
        dbCon = new DatabaseConnection();
      }

      // Build SQL string to query
      String sql = "SELECT * FROM orders " +
              "INNER JOIN user ON orders.user_id = user.u_id " +
              "INNER JOIN line_item ON orders.o_id = line_item.order_id " +
              "INNER JOIN address AS ba ON orders.billing_address_id = ba.a_id " +
              "INNER JOIN address as sa ON orders.shipping_address_id = sa.a_id " +
              "INNER JOIN product ON line_item.product_id  = product.p_id " +
              "WHERE orders.o_id = ? ";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setInt(1, orderId);

      //Executing query
      rs = preparedStatement.executeQuery();

      //Declaring objects, initializing order to null and instantiating an ArrayList for line items
      Order order = null;
      User user;
      LineItem lineItem;
      ArrayList<LineItem> lineItemsList = new ArrayList<>();
      Product product;
      Address billingAddress;
      Address shippingAddress;

      while (rs.next()) {

        if (order == null) {
          //Setting objects needed to create an order
          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingAddress = AddressController.formBillingAddress(rs);
          shippingAddress = AddressController.formShippingAddress(rs);

          //Creating order
          order = formOrder(rs, user, lineItemsList, billingAddress, shippingAddress);

        }else {
          //If the result set is more than one line (ie. more products), this will add them to the order
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          order.getLineItems().add(lineItem);
        }
      }
      // Returns the build order
      return order;
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }finally {
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
   * @return order
   */
  public static ArrayList<Order> getOrders() {

    //Initializing result set
    ResultSet rs = null;

    try {
      // check for connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }

      // Build SQL string to query
      String sql = "SELECT * FROM orders " +
              "INNER JOIN user ON orders.user_id = user.u_id " +
              "INNER JOIN line_item ON orders.o_id = line_item.order_id " +
              "INNER JOIN address AS ba ON orders.billing_address_id = ba.a_id " +
              "INNER JOIN address as sa ON orders.shipping_address_id = sa.a_id " +
              "INNER JOIN product ON line_item.product_id  = product.p_id " +
              "ORDER BY orders.o_id";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);

      //Executing query
      rs = preparedStatement.executeQuery();

      //Instantiate an orders ArrayList
      ArrayList<Order> orders = new ArrayList<Order>();

      while (rs.next()) {

        //Declaring objects and instantiating an ArrayList for line items
        User user;
        LineItem lineItem;
        Address billingAddress;
        Address shippingAddress;
        Product product;
        ArrayList<LineItem> lineItemsList = new ArrayList<>();

        //Setting objects needed to create an order if there is no order created or the order id has changed
        if (orders.isEmpty() || rs.getInt("o_id") != orders.get(orders.size() - 1).getId()) {

          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingAddress = AddressController.formBillingAddress(rs);
          shippingAddress = AddressController.formShippingAddress(rs);

          //Creating order and adding it to the orders ArrayList
          Order order = formOrder(rs, user, lineItemsList, billingAddress, shippingAddress);
          orders.add(order);

          //If an order in the ArrayList matches the order in the resultset, this will add multiple products in case
          //it's needed
        } else if (rs.getInt("o_id") == orders.get(orders.size() - 1).getId()) {

          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          orders.get(orders.size() - 1).getLineItems().add(lineItem);
        }

      }

      if (orders != null)

        //Return the orders ArrayList with orders
        return orders;

    } catch (SQLException e){
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
    return null;
  }

  /**
   *
   * @param order
   * @return order
   */
  public static Order createOrder(Order order) {

    // Set creation and updated time for order.
    order.setCreatedAt(System.currentTimeMillis() / 1000L);
    order.setUpdatedAt(System.currentTimeMillis() / 1000L);

    try {
      //Check for connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseConnection();
      }

      //Setting autocommit to false
      DatabaseConnection.getConnection().setAutoCommit(false);

      // Save addresses to database and save them back to initial order instance
      order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
      order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

      // Save the user to the database and save them back to initial order instance
      order.setCustomer(UserController.getUser(order.getCustomer().getId()));

      // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts: FIX

      //Building SQL statement
      String sql = "INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, order_created_at, " +
              "order_updated_at) VALUES(?,?,?,?,?,?)";

      //Prepared statement
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setInt(1, order.getCustomer().getId());
      preparedStatement.setInt(2, order.getBillingAddress().getId());
      preparedStatement.setInt(3, order.getShippingAddress().getId());
      preparedStatement.setFloat(4, order.calculateOrderTotal());
      preparedStatement.setLong(5, order.getCreatedAt());
      preparedStatement.setLong(6, order.getUpdatedAt());

      //Executing update
      int orderID = preparedStatement.executeUpdate();

      // Get our key back in order to apply it to an object as ID
      ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
      if (generatedKeys.next()&&orderID==1) {
        order.setId(generatedKeys.getInt(1));
      }

      // Create an empty list in order to go trough items and then save them back with ID
      ArrayList<LineItem> items = new ArrayList<LineItem>();

      // Save line items to database with the respective order id
      for (LineItem item : order.getLineItems()) {
        item = LineItemController.createLineItem(item, order.getId());
        items.add(item);
      }

      //Add line items to the order, commit and return the order
      order.setLineItems(items);

      //Comitting transactions
      DatabaseConnection.getConnection().commit();

      //Returning order
      return  order;

      //Adding NullPointerException since we are using getUser() instead of createUser() and customers might not exist
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      if (dbCon.getConnection()!=null) {
        try {
          //Rolling back if exception is caught
          System.out.println("Rollback");
          DatabaseConnection.getConnection().rollback();
        } catch (SQLException ex) {
          ex.printStackTrace();
        }
      }
    } finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return  order;
  }

  //Method forming order
  private static Order formOrder (ResultSet rs, User user, ArrayList<LineItem> lineItemsList, Address billingAddress, Address shippingAddress) {
    try {
      Order order = new Order(
                      rs.getInt("o_id"),
                      user,
                      lineItemsList,
                      billingAddress,
                      shippingAddress,
                      rs.getFloat("order_total"),
                      rs.getLong("order_created_at"),
                      rs.getLong("order_updated_at"));

      return order;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

}