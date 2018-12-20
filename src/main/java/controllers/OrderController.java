package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import model.*;
import utils.Log;

public class OrderController {

  //For establishing connection to the database
  private static DatabaseController dbCon;

  public OrderController() {
    dbCon = new DatabaseController();
  }

  /**
   *
   * @param orderId
   * @return order
   */
  public static Order getOrder(int orderId) {
    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
    // Build SQL string to query
    String sql = "SELECT * FROM orders " +
            "INNER JOIN " +
            "user ON orders.user_id = user.u_id " +
            "INNER JOIN " +
            "line_item ON orders.o_id = line_item.order_id " +
            "INNER JOIN " +
            "address AS ba ON orders.billing_address_id = ba.a_id " +
            "INNER JOIN " +
            "address as sa ON orders.shipping_address_id = sa.a_id " +
            "INNER JOIN " +
            "product ON line_item.product_id  = product.p_id " +
            "WHERE orders.o_id = " + orderId;

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);

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

        } else {
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
    }
    return null;
  }

  /**
   *
   * @return order
   */
  public static ArrayList<Order> getOrders() {
    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    //Building SQL query
    String sql = "SELECT * FROM orders " +
            "INNER JOIN " +
            "user ON orders.user_id = user.u_id " +
            "INNER JOIN " +
            "line_item ON orders.o_id = line_item.order_id " +
            "INNER JOIN " +
            "address AS ba ON orders.billing_address_id = ba.a_id " +
            "INNER JOIN " +
            "address as sa ON orders.shipping_address_id = sa.a_id " +
            "INNER JOIN " +
            "product ON line_item.product_id  = product.p_id " +
            "ORDER BY orders.o_id";

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);
    //Instantiate an orders ArrayList
    ArrayList<Order> orders = new ArrayList<>();

    try {
      while(rs.next()) {

        //Declaring objects and instantiating an ArrayList for line items
        User user;
        LineItem lineItem;
        Address billingAddress;
        Address shippingAddress;
        Product product;
        ArrayList<LineItem> lineItemsList = new ArrayList<>();

        //Setting objects needed to create an order if there is no order created or the order id has changed
        if (orders.isEmpty() || rs.getInt("o_id") != orders.get(orders.size()-1).getId()) {

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
        } else if (rs.getInt("o_id") == orders.get(orders.size()-1).getId()){
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          orders.get(orders.size()-1).getLineItems().add(lineItem);
        }

      }
      //Return the build orders
      return orders;
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // return the orders (null)
    return orders;

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

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
      //Setting autocommit to false
      dbCon.getConnection().setAutoCommit(false);

      // Save addresses to database and save them back to initial order instance
      order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
      order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

      // Save the user to the database and save them back to initial order instance
      order.setCustomer(UserController.getUser(order.getCustomer().getId()));

        // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts.: FIX

        // Insert the order in the DB
        int orderID = dbCon.insert(
                "INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, order_created_at, order_updated_at) VALUES("
                        + order.getCustomer().getId()
                        + ", "
                        + order.getBillingAddress().getId()
                        + ", "
                        + order.getShippingAddress().getId()
                        + ", "
                        + order.calculateOrderTotal()
                        + ", "
                        + order.getCreatedAt()
                        + ", "
                        + order.getUpdatedAt()
                        + ")");

        if (orderID != 0) {
          //Update the productid of the product before returning
          order.setId(orderID);
        }

        // Create an empty list in order to go trough items and then save them back with ID
        ArrayList<LineItem> items = new ArrayList<LineItem>();

        // Save line items to database
        for (LineItem item : order.getLineItems()) {
          item = LineItemController.createLineItem(item, order.getId());
          items.add(item);
        }
        //Add line items to order
        order.setLineItems(items);

        //Commit transactions
        dbCon.getConnection().commit();

      } catch(SQLException | NullPointerException e){
        e.printStackTrace();
        if (dbCon.getConnection() != null) {
          try {
            //Rolling back if exception is caught
            dbCon.getConnection().rollback();
            System.out.println("Rollback");
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
      } finally{
        try {
          //Setting autocommit to true again
          dbCon.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
      // Return order
      return order;
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