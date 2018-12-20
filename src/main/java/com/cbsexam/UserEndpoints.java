package com.cbsexam;

import cache.UserCache;
import com.google.gson.Gson;
import controllers.UserController;
import java.util.ArrayList;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.User;
import utils.Encryption;
import utils.Log;
import utils.Token;

@Path("user")
public class UserEndpoints {

  //Instantiating an object of UserCache
  private static UserCache userCache = new UserCache();
  //Setting forceUpdate to true
  private static boolean forceUpdate = true;

  /**
   * @param idUser
   * @return Responses
   */
  @GET
  @Path("/{idUser}")
  public Response getUser(@PathParam("idUser") int idUser) {

    // Use the ID to get the user from the controller.
    User user = UserController.getUser(idUser);

    // TODO: Add Encryption to JSON: FIX
    // Convert the user object to json in order to return the object
    String json = new Gson().toJson(user);

    //Adds encryption
    json = Encryption.encryptDecryptXOR(json);

    // Return responses
    // TODO: What should happen if something breaks down?: FIX
    if (user != null) {
      // Return the user with the status code 200
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not get user").build();
    }
  }

  /**
   *
   * @return Responses
   */
  @GET
  @Path("/")
  public Response getUsers() {

    // Get a list of users
    //Changed getUsers method to the one from UserCache
    ArrayList<User> users = userCache.getUsers(forceUpdate);

    // TODO: Add Encryption to JSON: FIX
    // Transfer users to json in order to return it to the user
    String json = new Gson().toJson(users);

    //Adds encryption
    json = Encryption.encryptDecryptXOR(json);

    // Return responses
    if (users != null) {

      //Setting forceUpdate to false so cache doesn't clear unnecessarily
      this.forceUpdate = false;

      // Return the users with the status code 200
      return Response.status(200).type(MediaType.APPLICATION_JSON).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not get users").build();
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
  public Response createUser(String body) {

    // Read the json from body and transfer it to a user class
    User newUser = new Gson().fromJson(body, User.class);

    // Use the controller to add the user
    User createUser = UserController.createUser(newUser);

    // Get the user back with the added ID and return it to the user
    String json = new Gson().toJson(createUser);

    // Return responses
    if (createUser != null) {

      //Setting forceUpdate to true, so cache clears when a user is created
      forceUpdate = true;

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not create user").build();
    }
  }

  /**
   *
   * @param body
   * @return Responses
   */
  // TODO: Make the system able to login users and assign them a token to use throughout the system.: FIX (added verification for delete and update user)
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response loginUser(String body) {

    // Read the json from body and transfer it to a user class
    User user = new Gson().fromJson(body, User.class);

    //Using controller to login user
    User userToLogin = UserController.login(user);

    //Return responses
    if (userToLogin != null) {

      //Return message with token
      String msg = "You have succesfully logged in, " + userToLogin.getFirstname() +". This is your token, please save"
              + " it:\n\n" + userToLogin.getToken() + "\n\nEnjoy your stay :-)";

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(msg).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not find user").build();
    }
  }

  /**
   *
   * @param idUser
   * @param body
   * @return Responses
   */
  // TODO: Make the system able to delete users: FIX
  @DELETE
  @Path("/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteUser(@PathParam("idUser") int idUser, String body) {

    // Read the json from body and transfer it to a user class
    User userToDelete = new Gson().fromJson(body, User.class);

    //Token verification
    if (Token.verifyToken(userToDelete.getToken(), userToDelete)) {

      // Use the ID to delete the user from the database via controller.
      boolean deleted = UserController.deleteUser(idUser);

      //Return responses
      if (deleted) {

        //Setting forceUpdate to false so cache doesn't clear unnecessarily
        forceUpdate = true;

        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity("User deleted").build();
      } else {
        // Return a response with status 400 and JSON as type
        return Response.status(400).entity("Could not delete user").build();
      }
    } else {
      // Return a response with status 401 and JSON as type
      return Response.status(401).entity("You're not authorized to do this").build();
    }
  }

  /**
   *
   * @param idUser
   * @param body
   * @return Responses
   */
  // TODO: Make the system able to update users: FIX
  @PUT
  @Path("/update/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateUser(@PathParam("idUser") int idUser, String body) {

    // Read the json from body and transfer it to a user class
    User userToUpdate = new Gson().fromJson(body, User.class);

    //Token verification
    if (Token.verifyToken(userToUpdate.getToken(), userToUpdate)) {

      // Use the ID to update the user from the database via controller
      boolean affected = UserController.updateUser(userToUpdate);

      //Return responses
      if (affected) {
        //Setting forceUpdate to false so cache doesn't clear unnecessarily
        forceUpdate = true;

        String json = new Gson().toJson(userToUpdate);

        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
      } else {
        // Return a response with status 400 and JSON as type
        return Response.status(400).entity("Could not update user").build();
      }
    } else {
      // Return a response with status 401 and JSON as type
      return Response.status(401).entity("You're not authorized to do this").build();
    }
  }
}
