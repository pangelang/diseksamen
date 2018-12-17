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
    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL string to query
    String sql = "SELECT * FROM orders\n" +
            "inner join\n" +
            "user ON orders.user_id = user.u_id\n" +
            "inner join \n" +
            "line_item ON orders.o_id = line_item.order_id \n" +
            "inner join \n" +
            "address AS ba ON orders.billing_address_id = ba.a_id\n" +
            "inner join \n" +
            "address as sa ON orders.shipping_address_id = sa.a_id\n" +
            "inner join \n" +
            "product ON line_item.product_id  = product.p_id \n" +
            "where orders.o_id = " + orderId;

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);

    Order order = null;
    User user;
    LineItem lineItem;
    ArrayList<LineItem> lineItemsList = new ArrayList<>();
    Product product;
    Address billingAddress;
    Address shippingAddress;

    try {
      while (rs.next()) {

        if (order == null) {

          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingAddress = AddressController.formBillingAddress(rs);
          shippingAddress = AddressController.formShippingAddress(rs);
          order = formOrder(rs, user, lineItemsList, billingAddress, shippingAddress);

        } else {
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


  public static ArrayList<Order> getOrders() {
    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    String sql = "SELECT * FROM orders\n" +
            "             inner join\n" +
            "             user ON orders.user_id = user.u_id\n" +
            "             inner join \n" +
            "             line_item ON orders.o_id = line_item.order_id \n" +
            "             inner join \n" +
            "             address AS ba ON orders.billing_address_id = ba.a_id\n" +
            "             inner join \n" +
            "             address as sa ON orders.shipping_address_id = sa.a_id\n" +
            "             inner join \n" +
            "             product ON line_item.product_id  = product.p_id\n" +
            "             order by orders.o_id";

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);

    ArrayList<Order> orders = new ArrayList<>();

    try {
      while(rs.next()) {

        User user;
        LineItem lineItem;
        Address billingAddress;
        Address shippingAddress;
        Product product;
        ArrayList<LineItem> lineItemsList = new ArrayList<>();

        if (orders.isEmpty() || rs.getInt("o_id") != orders.get(orders.size()-1).getId()) {

          user = UserController.formUser(rs);
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          billingAddress = AddressController.formBillingAddress(rs);
          shippingAddress = AddressController.formShippingAddress(rs);
          Order order = formOrder(rs, user, lineItemsList, billingAddress, shippingAddress);

          orders.add(order);

        } else if (rs.getInt("o_id") == orders.get(orders.size()-1).getId()){
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);
          orders.get(orders.size()-1).getLineItems().add(lineItem);
        }

      }
      return orders;
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

      //if (order.getShippingAddress().getId() == order.getBillingAddress().getId() + 1) {

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

      } catch(SQLException | NullPointerException e){
        e.printStackTrace();
        if (dbCon.getConnection() != null) {
          try {
            dbCon.getConnection().rollback();
            System.out.println("Rollback");
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
      } finally{
        try {
          dbCon.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
      // Return order
      return order;
  }

  private static Order formOrder (ResultSet rs, User user, ArrayList<LineItem> lineItemsList, Address billingsAddress, Address shippingAddress) {
    try {
      Order order = new Order(
                      rs.getInt("o_id"),
                      user,
                      lineItemsList,
                      billingsAddress,
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