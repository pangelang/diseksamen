package com.cbsexam;

import cache.OrderCache;
import com.google.gson.Gson;
import controllers.OrderController;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.Order;
import utils.Encryption;

@Path("order")
public class OrderEndpoints {

  //Instantiating an object of OrderCache
  private static OrderCache orderCache = new OrderCache();
  //Setting forceUpdate to true
  private static boolean forceUpdate = true;

  /**
   *
   * @param idOrder
   * @return Responses
   */
  @GET
  @Path("/{idOrder}")
  public Response getOrder(@PathParam("idOrder") int idOrder) {

    // Call our controller-layer in order to get the order from the DB
    //Changed getOrder method to the one from OrderCache
    Order order = orderCache.getOrder(forceUpdate, idOrder);

    // TODO: Add Encryption to JSON: FIX
    // We convert the java object to json with GSON library imported in Maven
    String json = new Gson().toJson(order);

    //Adds encryption
    json = Encryption.encryptDecryptXOR(json);

    //Return reponses
    if (order != null) {

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not get order").build();
    }
  }

  /**
   *
   * @return Responses
   */
  @GET
  @Path("/")
  public Response getOrders() {

    // Call our controller-layer in order to get the order from the DB
    //Changed getOrder method to the one from OrderCache
    ArrayList<Order> orders = orderCache.getOrders(forceUpdate);

    // TODO: Add Encryption to JSON: FIX
    // We convert the java object to json with GSON library imported in Maven
    String json = new Gson().toJson(orders);

    //Adds encryption
    json = Encryption.encryptDecryptXOR(json);

    //Return responses
    if (orders != null) {

      //Setting forceUpdate to true, so cache clears when a user is created
      forceUpdate = false;

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not get orders").build();
    }
  }

  /**
   *
   * @param body
   * @return Responses
   */
  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createOrder(String body) {

    // Read the json from body and transfer it to a order class
    Order newOrder = new Gson().fromJson(body, Order.class);

    // Use the controller to add the user
    Order createdOrder = OrderController.createOrder(newOrder);

    // Get the user back with the added ID and return it to the user
    String json = new Gson().toJson(createdOrder);

    // Return the data to the user
    if (createdOrder != null) {

      //Setting forceUpdate to true, so cache clears when a order is created
      forceUpdate = true;

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not create order").build();
    }
  }
}