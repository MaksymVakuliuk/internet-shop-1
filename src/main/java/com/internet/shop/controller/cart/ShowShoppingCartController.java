package com.internet.shop.controller.cart;

import com.internet.shop.model.Product;
import com.internet.shop.service.ShoppingCartService;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/cart/show")
public class ShowShoppingCartController extends HttpServlet {
    private static final Long USER_ID = 1L;
    private static ShoppingCartService shoppingCartService;

    @Override
    public void init() {
        shoppingCartService = (ShoppingCartService)
                getServletContext().getAttribute("shoppingCartService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Product> products = shoppingCartService.getByUserId(USER_ID).getProducts();
        req.setAttribute("products", products);
        req.getRequestDispatcher("/views/cart/show.jsp").forward(req, resp);
    }
}
