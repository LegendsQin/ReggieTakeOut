package org.example.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;
import org.example.reggie.entity.DishDto;
import org.example.reggie.entity.Orders;
import org.example.reggie.entity.OrdersDto;
import org.example.reggie.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/order")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(number!=null, Orders::getNumber, number);
        queryWrapper.orderByDesc(Orders::getOrderTime);

        ordersService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }


    @PostMapping("/submit")
    public R<String> save(@RequestBody Orders orders) {
        log.info("orders: {}", orders.toString());
        boolean isSuccess = ordersService.saveWithOtherInformation(orders);
        if (isSuccess) {
            return R.success("下单成功");
        } else {
            return R.error("下单失败");
        }
    }


    @GetMapping("/userPage")
    public R<Page> orderPage(int page, int pageSize) {


        Page<OrdersDto> ordersDtoPage = ordersService.getPage(page, pageSize);

        return R.success(ordersDtoPage);
    }


    @PutMapping
    public R<String> update(@RequestBody Orders orders) {
        boolean isSuccess = ordersService.updateById(orders);
        if (isSuccess) {
            return R.success("更新成功");
        } else {
            return R.error("更新失败");
        }
    }


    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders) {
        boolean isSuccess = ordersService.again(orders);
        if (isSuccess) {
            return R.success("重新下单成功");
        } else {
            return R.error("重新下单失败");
        }
    }
}
