package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import model.Address;
import utils.Log;

public class AddressController {

  private static DatabaseController dbCon;

  public AddressController() {
    dbCon = new DatabaseController();
  }

  public static Address createAddress(Address address) {

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the product in the DB
    int addressID = dbCon.insert(
        "INSERT INTO address(name, city, zipcode, street_address) VALUES('"
            + address.getName()
            + "', '"
            + address.getCity()
            + "', '"
            + address.getZipCode()
            + "', '"
            + address.getStreetAddress()
            + "')");

    if (addressID != 0) {
      // Update the productid of the product before returning
      address.setId(addressID);
    } else{
      // Return null if product has not been inserted into database
      return null;
    }

    // Return product, will be null at this point
    return address;
  }

  //Method forming billing address
  public static Address formBillingAddress(ResultSet rs) {
    try {
      Address address = new Address(rs.getInt("ba.a_id"),
              rs.getString("ba.name"),
              rs.getString("ba.street_address"),
              rs.getString("ba.city"),
              rs.getString("ba.zipcode"));

      return address;

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  //Method forming shipping address
  public static Address formShippingAddress(ResultSet rs) {
    try {
      Address address = new Address(rs.getInt("sa.a_id"),
              rs.getString("sa.name"),
              rs.getString("sa.street_address"),
              rs.getString("sa.city"),
              rs.getString("sa.zipcode"));

      return address;

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
}
