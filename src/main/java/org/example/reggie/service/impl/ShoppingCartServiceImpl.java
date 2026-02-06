package org.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;
import org.example.reggie.entity.ShoppingCart;
import org.example.reggie.mapper.ShoppingCartMapper;
import org.example.reggie.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 保存购物车信息并检查是否存在重复项
     * 如果存在重复项,则更新数量
     * @param shoppingCart
     */
    @Override
    public ShoppingCart saveAndCheck(ShoppingCart shoppingCart) {

        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        // 1 检查购物车中是否存在重复项
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        // 菜品重复项
        if(dishId != null && setmealId == null){
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        }
        // 套餐重复项
        else if(dishId == null && setmealId != null){
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentUserId());
        int count = shoppingCartService.count(queryWrapper);

        // 2 如果存在重复项,则更新数量
        if(count == 1){
            ShoppingCart cart = shoppingCartService.getOne(queryWrapper);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartService.updateById(cart);
        }
        // 3 如果不存在重复项,则新增购物车项
        else if(count == 0){
            shoppingCart.setNumber(1);
            shoppingCart.setUserId(BaseContext.getCurrentUserId());
            shoppingCartService.save(shoppingCart);
        }
        // 4 其他情况,返回false
        return shoppingCartService.getOne(queryWrapper);
    }


    /**
     * 更新购物车项数量
     * @param shoppingCart
     * @return
     */
    @Override
    public ShoppingCart updateNum(ShoppingCart shoppingCart) {
        // 1 从ThreadLocal中获取当前用户ID
        Long userId = BaseContext.getCurrentUserId();
        // 2 根据用户ID查询购物车项
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        if(shoppingCart.getDishId() != null && shoppingCart.getSetmealId() == null){
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }
        else if(shoppingCart.getDishId() == null && shoppingCart.getSetmealId() != null){
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        ShoppingCart cart = shoppingCartService.getOne(queryWrapper);

        // 3 更新购物车项
        // 更新数量
        int curNum = cart.getNumber();
        if(curNum == 1){
            // 数量为1时,删除购物车项
            shoppingCartService.remove(queryWrapper);
            ShoppingCart newCart = new ShoppingCart();
            newCart.setNumber(0);
            return newCart;
        }
        else if(curNum > 1){
            cart.setNumber(curNum - 1);
            shoppingCartService.updateById(cart);
            return cart;
        }
        else
            return null;
    }
}
