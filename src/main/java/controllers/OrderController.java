package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import model.*;
import utils.Log;

public class OrderController {

  private static DatabaseController dbCon;

  public OrderController() {
    dbCon = new DatabaseController();
  }

  public static Order getOrder(int orderId) {

    Order order = null;
    ArrayList <LineItem> lineItemsList = new ArrayList<>();
    User user;
    LineItem lineItem;
    Address billingsAddress;
    Product product;
    Address shippingAddress;

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
      dbCon.getConnection().setAutoCommit(false);

      // Build SQL string to query
      String sql1 = "SELECT * " +
              "FROM address " +
              "LEFT JOIN orders ON address.a_id = orders.billing_address_id " +
              "LEFT JOIN user ON orders.user_id = user.u_id " +
              "LEFT JOIN line_item ON line_item.order_id = orders.o_id " +
              "LEFT JOIN product ON product.p_id = line_item.product_id " +
              "WHERE orders.o_id = " + orderId + " " +
              "ORDER BY address.a_id";

      // Do the query in the database
      ResultSet rs = dbCon.query(sql1);

      while (rs.next()) {
        if (order==null) {
          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingsAddress = AddressController.formAddress(rs);
          order = formOrder(rs, user, lineItemsList, billingsAddress);

        } else{
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          order.getLineItems().add(lineItem);
        }
      }

      //Making our second query
      String sql2 = "SELECT * FROM address WHERE address.a_id = " + (order.getBillingAddress().getId()+1);
      ResultSet rs2 = dbCon.query(sql2);

      if (rs2.next()){
        shippingAddress = AddressController.formAddress(rs2);
        order.setShippingAddress(shippingAddress);
      }

      dbCon.getConnection().commit();

      return order;

    } catch (SQLException | NullPointerException e) {
      e.printStackTrace();
      try {
        System.out.println("Rollback");
        dbCon.getConnection().rollback();
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    }finally {
      try {
        dbCon.getConnection().setAutoCommit(true);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    // Returns null
    return order;
  }

  /**
   * Get all orders in database
   *
   * @return
   */
  public static ArrayList<Order> getOrders() {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    String sql = "SELECT * " +
            "FROM address " +
            "LEFT JOIN orders ON address.a_id = orders.billing_address_id " +
            "LEFT JOIN user ON orders.user_id = user.u_id " +
            "LEFT JOIN line_item ON line_item.order_id = orders.o_id " +
            "LEFT JOIN product ON product.p_id = line_item.product_id " +
            "ORDER BY address.a_id";

    ResultSet rs = dbCon.query(sql);
    ArrayList<Order> orders = new ArrayList<>();

    try {
      while(rs.next()) {
        User user;
        LineItem lineItem;
        Address billingsAddress;
        Address shippingAddress;
        Product product;
        ArrayList <LineItem> lineItemsList = new ArrayList<>();

        //Creating order object if orders ArrayList is empty
        if(orders.isEmpty()) {
          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingsAddress = AddressController.formAddress(rs);
          orders.add(formOrder(rs, user, lineItemsList, billingsAddress));

          //Adding product(s) to existing order if the order id (o_id) in the next line in the result set is the same
          //as the previous
        } else if (rs.getInt("o_id") == orders.get(orders.size()-1).getId() && rs.getInt("o_id") != 0) {
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          orders.get(orders.size()-1).getLineItems().add(lineItem);

          //If the order id in the result set is null, it means the cursor is at the line where the corresponding
          //shipping address should be.
        } else if(rs.getInt("o_id") == 0) {
          shippingAddress = AddressController.formAddress(rs);
          orders.get(orders.size()-1).setShippingAddress(shippingAddress);

          //Creating a new order object
        } else{
          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingsAddress = AddressController.formAddress(rs);
          orders.add(formOrder(rs, user, lineItemsList, billingsAddress));
        }
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // return the orders
    return orders;
  }

  public static Order createOrder(Order order) {

    // Write in log that we've reach this step
    Log.writeLog(OrderController.class.getName(), order, "Actually creating a order in DB", 0);

    // Set creation and updated time for order.
    order.setCreatedAt(System.currentTimeMillis() / 1000L);
    order.setUpdatedAt(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
      dbCon.getConnection().setAutoCommit(false);

      // Save addresses to database and save them back to initial order instance
      order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
      order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

      // Save the user to the database and save them back to initial order instance
      order.setCustomer(UserController.getUser(order.getCustomer().getId()));

      // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts.: FIX

      // Insert the product in the DB
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

      order.setLineItems(items);

      dbCon.getConnection().commit();

    } catch (SQLException | NullPointerException e) {
      e.printStackTrace();
      if (dbCon.getConnection() != null) {
        try {
          dbCon.getConnection().rollback();
          System.out.println("Rollback");
        } catch (SQLException ex) {
          ex.printStackTrace();
        }
      }
    } finally {
      try {
        dbCon.getConnection().setAutoCommit(true);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    // Return order
    return order;
  }

  private static Order formOrder (ResultSet rs, User user, ArrayList<LineItem> lineItemsList, Address billingsAddress) {
    try {
      Order order = new Order(
                      rs.getInt("o_id"),
                      user,
                      lineItemsList,
                      billingsAddress,
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