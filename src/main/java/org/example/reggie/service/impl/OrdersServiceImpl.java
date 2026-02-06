package org.example.reggie.service.impl;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.CustomException;
import org.example.reggie.entity.*;
import org.example.reggie.mapper.OrdersMapper;
import org.example.reggie.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private UserService userService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 保存订单信息，同时填充订单中的其他信息
     * @param orders: 包含 addressBookId 、payMethod、remark
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithOtherInformation(Orders orders) {

        // 在orders里面填充 userId userName
        Long userId = BaseContext.getCurrentUserId();
        orders.setUserId(userId);
        LambdaQueryWrapper<User> queryWrapUser = new LambdaQueryWrapper<>();
        queryWrapUser.eq(User::getId, userId);
        User user = userService.getOne(queryWrapUser);
        orders.setUserName(user.getName());

        // 在orders里面填充 orderTime checkoutTime
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());

        // 在orders里面填充选的地址的那个人的 phone address consignee
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);

        long orderId = IdWorker.getId();

        orders.setNumber(String.valueOf(orderId));
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setStatus(2);


        // 获取购物车中的数据,载入到orderDetail中
        LambdaQueryWrapper<ShoppingCart>  queryWrapperShoppingCart = new LambdaQueryWrapper<>();
        queryWrapperShoppingCart.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart>  shoppingCartList = shoppingCartService.list(queryWrapperShoppingCart);

        //BigDecimal totalPrice = BigDecimal.ZERO;
        AtomicInteger amount = new AtomicInteger(0);
        for (ShoppingCart shoppingCart : shoppingCartList) {
            //totalPrice = totalPrice.add(shoppingCart.getAmount().multiply(BigDecimal.valueOf(shoppingCart.getNumber())));
            amount.addAndGet(shoppingCart.getAmount().multiply(new BigDecimal(shoppingCart.getNumber())).intValue());
        }

        if (shoppingCartList.isEmpty()) {
            throw new CustomException("购物车为空,不能下单");
        }

        //orders.setAmount(new BigDecimal(totalPrice.toString()));
        orders.setAmount(new BigDecimal(amount.get()));
        ordersService.save(orders);

        List<OrderDetail> orderDetailList = shoppingCartList.stream().map(
                shoppingCart -> {
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setOrderId(orders.getId());
//                    orderDetail.setNumber(shoppingCart.getNumber());
//                    orderDetail.setAmount(shoppingCart.getAmount());
//                    orderDetail.setImage(shoppingCart.getImage());

                    BeanUtils.copyProperties(shoppingCart, orderDetail,"id","userId","createTime");
                    //orderDetailService.save(orderDetail);
                    return orderDetail;
                }
        ).collect(Collectors.toList());

        // 批量保存到 orderDetail 中
        orderDetailService.saveBatch(orderDetailList);

        // 将购物车中的信息清空
        return shoppingCartService.remove(queryWrapperShoppingCart);
    }


    /**
     * 获取用户订单分页信息
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public Page<OrdersDto> getPage(int page, int pageSize) {

        // 先查 orders
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapperOrders = new LambdaQueryWrapper<>();
        queryWrapperOrders.eq(Orders::getUserId, BaseContext.getCurrentUserId());
        queryWrapperOrders.orderByDesc(Orders::getOrderTime);
        ordersService.page(ordersPage, queryWrapperOrders);


        // 复制 orders 到 dishDtoPage 中,将除records 之外的字段复制到 dishDtoPage 中
        Page<OrdersDto> ordersDtoPage = new Page<>();
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        List<OrdersDto> ordersDtoList = ordersPage.getRecords().stream().map(
                orders -> {
                    OrdersDto ordersDto = new OrdersDto();
                    BeanUtils.copyProperties(orders, ordersDto);
                    // 查 orderDetail
                    LambdaQueryWrapper<OrderDetail>  queryWrapperOrderDetail = new LambdaQueryWrapper<>();
                    queryWrapperOrderDetail.eq(OrderDetail::getOrderId, orders.getId());
                    List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapperOrderDetail);
                    // 复制 orderDetail 到 ordersDto 中
                    if (!orderDetailList.isEmpty())
                        ordersDto.setOrderDetails(orderDetailList);

                    return ordersDto;
                }
        ).collect(Collectors.toList());

        // 将 ordersDtoList 赋值给 ordersDtoPage 的 records 字段
        ordersDtoPage.setRecords(ordersDtoList);
        return ordersDtoPage;
    }

     /**
      * 重新下单
      * @param orders
      * @return
      */
    @Override
    public boolean again(Orders orders) {

        // 根据 orders.getId() 查 orderDetail
        LambdaQueryWrapper<OrderDetail>  queryWrapperOrderDetail = new LambdaQueryWrapper<>();
        queryWrapperOrderDetail.eq(OrderDetail::getOrderId, orders.getId());
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapperOrderDetail);
        if (orderDetailList.isEmpty()) {
            throw new CustomException("订单不存在");
        }

        // 从 orderDetailList 中获取 number, amount, image 字段, 并保存到 shoppingCart 中
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(
                orderDetail -> {
                    ShoppingCart shoppingCart = new ShoppingCart();
                    shoppingCart.setUserId(BaseContext.getCurrentUserId());
                    BeanUtils.copyProperties(orderDetail, shoppingCart, "id", "orderId");

                    return shoppingCart;
                }
        ).collect(Collectors.toList());

        // 批量保存到 shoppingCart 中
        return shoppingCartService.saveBatch(shoppingCartList);
    }
}
