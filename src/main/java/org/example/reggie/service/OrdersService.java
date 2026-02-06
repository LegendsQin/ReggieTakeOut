package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.DishDto;
import org.example.reggie.entity.Orders;
import org.example.reggie.entity.OrdersDto;

public interface OrdersService extends IService<Orders> {


    /**
     * 保存订单信息，同时填充订单中的其他信息
     * @param orders
     * @return
     */
    boolean saveWithOtherInformation(Orders orders);


    /**
     * 获取用户订单分页信息
     * @param page
     * @param pageSize
     * @return
     */
    Page<OrdersDto> getPage(int page, int pageSize);

    /**
     * 重新下单
     * @param orders
     * @return
     */
    boolean again(Orders orders);
}
