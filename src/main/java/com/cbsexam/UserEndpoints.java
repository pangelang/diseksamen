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

@Path("user")
public class UserEndpoints {

  private static UserCache userCache = new UserCache();
  private static boolean forceUpdate = true;
  private static User currentUser = new User();

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

    // Return the user with the status code 200
    // TODO: What should happen if something breaks down?: FIX
    if (user != null) {
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(400).entity("Could not get user").build();
    }
  }

  /** @return Responses */
  @GET
  @Path("/")
  public Response getUsers() {

    // Write to log that we are here
    Log.writeLog(this.getClass().getName(), this, "Get all users", 0);

    // Get a list of users
    //Changed getUsers method to the one from UserCache
    ArrayList<User> users = userCache.getUsers(forceUpdate);

    // TODO: Add Encryption to JSON: FIX
    // Transfer users to json in order to return it to the user
    String json = new Gson().toJson(users);

    //Adds encryption
    json = Encryption.encryptDecryptXOR(json);

    //Setting forceUpdate to false so cache doesn't clear unnecessarily
    this.forceUpdate = false;

    // Return the users with the status code 200
    return Response.status(200).type(MediaType.APPLICATION_JSON).entity(json).build();
  }

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

    // Return the data to the user
    if (createUser != null) {

      //Setting forceUpdate to true, so cache clears when a user is created
      forceUpdate = true;

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(400).entity("Could not create user").build();
    }
  }

  // TODO: Make the system able to login users and assign them a token to use throughout the system.
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response loginUser(String body) {

    User user = new Gson().fromJson(body, User.class);

    User userToLogin = UserController.login(user);

    String json = new Gson().toJson(userToLogin);

    if (userToLogin != null) {
      currentUser = userToLogin;
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(400).entity("Could not find user").build();
    }
  }

  // TODO: Make the system able to delete users: FIX
  @DELETE
  @Path("/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteUser(@PathParam("idUser") int idUser) {

    if (currentUser.getToken() != null && currentUser.getId()==idUser) {
      // Write to log that we are here
      Log.writeLog(this.getClass().getName(), this, "Deleting a user", 0);

      // Use the ID to delete the user from the database via controller.
      boolean deleted = UserController.deleteUser(idUser);

      if (deleted) {
        forceUpdate = true;
        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity("User deleted").build();
      } else {
        // Return a response with status 200 and JSON as type
        return Response.status(400).entity("Could not delete user").build();
      }
    } else {
      return Response.status(400).entity("You're not logged in as the right user").build();
    }
  }

  // TODO: Make the system able to update users: FIX
  @PUT
  @Path("/update/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateUser(@PathParam("idUser") int idUser, String body) {

    if (currentUser.getToken() != null && currentUser.getId()==idUser) {
      User userToUpdate = new Gson().fromJson(body, User.class);

      boolean affected = UserController.updateUser(userToUpdate);

      if (affected) {
        String json = new Gson().toJson(userToUpdate);
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
      } else {
        return Response.status(400).entity("Could not update user").build();
      }
    } else {
      return Response.status(400).entity("You're not logged in as the right user").build();
    }
  }
}
