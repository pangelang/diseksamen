package cache;

import controllers.OrderController;
import model.Order;
import utils.Config;

import java.util.ArrayList;

//TODO: Build this cache and use it.: FIX
public class OrderCache {

    // List of orders
    private ArrayList<Order> orders;

    // Time cache should live
    private long ttl;

    // Sets when the cache has been created
    private long created;

    public OrderCache() {
        this.ttl = Config.getOrderTtl();
    }

    public ArrayList<Order> getOrders(Boolean forceUpdate) {

        // If we whish to clear cache, we can set force update.
        // Otherwise we look at the age of the cache and figure out if we should update.
        // If the list is empty we also check for new orders

        if (forceUpdate
                || ((this.created + this.ttl) <= (System.currentTimeMillis() / 1000L))
                || this.orders.isEmpty()) {

            // Get orders from controller, since we wish to update.
            ArrayList<Order> orders = OrderController.getOrders();

            System.out.println("Test");

            // Set orders for the instance and set created timestamp
            this.orders = orders;
            this.created = System.currentTimeMillis() / 1000L;
        }

        // Return the orders
        return this.orders;
    }

    public Order getOrder(Boolean forceUpdate, int idOrder) {

        // If we whish to clear cache, we can set force update.
        // Otherwise we look at the age of the cache and figure out if we should update.
        // If the list is empty we also check for new orders

        //Instantiating an order object
        Order order = new Order();

        if (forceUpdate) {

            // Get orders from controller, since we wish to update.
            order = OrderController.getOrder(idOrder);

            return order;
        } else {
            //If cache is up to date it checks the ArrayList until the right id is found
            for (Order o : orders) {
                if (idOrder == order.getId()) {
                    order = o;
                    return order;
                }
            }
        }
        return null;
    }
}
