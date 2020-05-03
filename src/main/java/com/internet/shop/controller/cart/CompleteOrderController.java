package com.internet.shop.controller.cart;

import com.internet.shop.model.Order;
import com.internet.shop.model.Product;
import com.internet.shop.model.ShoppingCart;
import com.internet.shop.service.OrderService;
import com.internet.shop.service.ShoppingCartService;
import com.internet.shop.service.UserService;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/orders/complete")
public class CompleteOrderController extends HttpServlet {
    private static final Long USER_ID = 1L;
    private static UserService userService;
    private static ShoppingCartService shoppingCartService;
    private static OrderService orderService;

    @Override
    public void init() {
        userService = (UserService)
                getServletContext().getAttribute("userService");
        shoppingCartService = (ShoppingCartService)
                getServletContext().getAttribute("shoppingCartService");
        orderService = (OrderService)
                getServletContext().getAttribute("orderService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ShoppingCart shoppingCart = shoppingCartService.getByUserId(USER_ID);
        List<Product> products = List.copyOf(shoppingCart.getProducts());
        shoppingCartService.clear(shoppingCart);
        Order order = orderService.completeOrder(products, userService.get(USER_ID));
        req.setAttribute("order", order);
        req.getRequestDispatcher("/views/orders/show.jsp").forward(req, resp);
    }
}
