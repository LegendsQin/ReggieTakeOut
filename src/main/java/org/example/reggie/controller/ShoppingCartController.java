package org.example.reggie.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;
import org.example.reggie.entity.ShoppingCart;
import org.example.reggie.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;


    @GetMapping("/list")
    public R<List<ShoppingCart>> getShoppingCarts() {

        // 1 从ThreadLocal中获取当前用户ID
        Long userId = BaseContext.getCurrentUserId();
        // 2 根据用户ID查询购物车项
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            return R.error("购物车为空");
        }
        return R.success(shoppingCarts);
    }

    @PostMapping("/add")
    public R<ShoppingCart> save(@RequestBody ShoppingCart shoppingCart) {

        ShoppingCart cart = shoppingCartService.saveAndCheck(shoppingCart);
        return R.success(cart);
    }


    @DeleteMapping("/clean")
    public R<String> delete(){
        // 1 从ThreadLocal中获取当前用户ID
        Long userId = BaseContext.getCurrentUserId();
        // 2 根据用户ID删除购物车项
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        shoppingCartService.remove(queryWrapper);
        return R.success("购物车清空成功");
    }



    @PostMapping("/sub")
    public R<ShoppingCart> update(@RequestBody ShoppingCart shoppingCart) {

        ShoppingCart cart = shoppingCartService.updateNum(shoppingCart);
        if(cart == null) {
            return R.error("更新失败");
        }

        return R.success(cart);
    }


}
