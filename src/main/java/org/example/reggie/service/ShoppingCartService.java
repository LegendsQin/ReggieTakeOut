package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {

    /**
     * 保存购物车信息并检查是否存在重复项
     * 如果存在重复项,则更新数量
     * @param shoppingCart
     */
    ShoppingCart saveAndCheck(ShoppingCart shoppingCart);


    /**
     * 更新购物车项数量
     * @param shoppingCart
     * @return
     */
    ShoppingCart updateNum(ShoppingCart shoppingCart);
}
