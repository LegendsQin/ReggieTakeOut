package org.example.reggie.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.example.reggie.entity.Category;
import org.example.reggie.entity.Dish;
import org.example.reggie.entity.DishDto;
import org.example.reggie.entity.DishFlavor;
import org.example.reggie.service.CategoryService;
import org.example.reggie.service.DishFlavorService;
import org.example.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;    // 菜品口味

    @Autowired
    private CategoryService  categoryService;


    /**
     * 菜品分页查询,由于菜品表中保存的是类别的ID,所以需要根据类别 ID查询类别名称,并将类别名称设置到 DishDto中
     * 由于前端接收的直接就是一个 page对象的 records,而 Dish表中没有 categoryName字段,因此我们创建一个新的实体类 DishDto
     * DishDto 中包含了 categoryName 字段,用于存储类别名称,并且该类集成于 Dish实体类,因此 DishDto 中包含了 Dish 实体类的所有字段
     * 而 即使 DishDto继承了 Dish,仍然需要通过 BeanUtils.copyProperties() 方法将 Dish 实体类的属性复制到 DishDto 中才能将 Dish中的每个字段查到的信息加入到 DishDto 中
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        // 分页构造器
        Page<Dish> dishPage = new Page<>(page, pageSize);

        // 查询条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null,Dish::getName,name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        // 执行分页查询
        dishService.page(dishPage, queryWrapper);

        Page<DishDto> dishDtoPage = new Page<>();

        // 这里是先把 Page对象的部分属性的值从一个 Page对象复制到另一个 Page对象
        BeanUtils.copyProperties(dishPage,dishDtoPage,"records");   //将除 records 以外的 dishPage的属性复制到 dishDtoPage

        // stream().map() 是 Java 8 中 Stream 流的核心方法之一，
        // 核心作用是对集合中的每个元素做 “转换 / 映射” 处理
        // 简单说就是 “遍历集合里的每一个元素，把它变成另一种形式”，比如把 Dish 对象列表转成菜品名称列表、把数字列表转成平方数列表。
        List<DishDto> dishDtoList = dishPage.getRecords().stream().map((item)->{
            DishDto dishDto = new DishDto();
            // 这里则是把一个对象中的全部属性的值从一个对象复制到另一个对象
            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                dishDto.setCategoryName(category.getName());
            }

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(dishDtoList);

        return R.success(dishDtoPage);
    }




    /**
     *
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {

        // 新增菜品
        boolean isSuccess = dishService.saveWithFlavor(dishDto);
        if(isSuccess){
            return R.success("新增菜品成功");
        }else{
            return R.error("新增菜品失败");
        }
    }

    /**
     * 根据ID查询菜品, 并返回菜品的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> getDishById(@PathVariable Long id) {
        // 1 根据传入的ID查Dish
        Dish dish = dishService.getById(id);
        // 2 根据传入的ID查DishFlavor, 并放入一个List中
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        queryWrapper.orderByDesc(DishFlavor::getUpdateTime);
        List<DishFlavor> dishFlavorList = dishFlavorService.list(queryWrapper);
        // 3 将1 和2 查到的信息集成到一个 DishDto 对象中
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);
        dishDto.setFlavors(dishFlavorList);
        // 4 返回 DishDto 对象
        return R.success(dishDto);
    }


    /**
     * 更新菜品, 并更新菜品的口味信息
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> updateWithFlavor(@RequestBody DishDto dishDto) {

        boolean isSuccess = dishService.updateWithFlavor(dishDto);
        if(isSuccess){
            return R.success("更新菜品成功");
        }else{
            return R.error("更新菜品失败");
        }
    }

    /**
     * 更新/批量更新 菜品状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        // 1. 检查 ids 是否为空
        if(CollectionUtils.isEmpty(ids)){
            return R.error("请选择要操作的菜品");
        }
        // 2. 更新菜品状态
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Dish::getStatus, status);
        updateWrapper.in(Dish::getId, ids);
        boolean isSuccess = dishService.update(updateWrapper);
        if(isSuccess){
            return R.success("更新菜品状态成功");
        }else {
            return R.error("更新菜品状态失败");
        }

    }


    @DeleteMapping
    // 如果 URL 参数名是 dish_name，但方法参数名是 name，必须用 @RequestParam 指定映射关系 : @RequestParam("dish_name") String name
    public R<String> delete(@RequestParam List<Long> ids) {
        if(CollectionUtils.isEmpty(ids)){
            return R.error("请选择要删除的菜品");
        }
        boolean isSuccess = dishService.removeDishWithFlavorByIds(ids);
        if(isSuccess){
            return R.success("删除菜品成功");
        }else{
            return R.error("删除菜品失败");



        }

    }

    @GetMapping("/list")
    public R<List<DishDto>> dishList(Long categoryId, @RequestParam(required = false) Integer status) {
        // 1. 检查 categoryId 是否为空
        if(categoryId == null){
            return R.error("请选择要查询的菜品分类");
        }

        List<Dish> dishList;

        // 2. 查询菜品列表
        if(status == null){
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dish::getCategoryId, categoryId);
            queryWrapper.orderByDesc(Dish::getUpdateTime);
            dishList = dishService.list(queryWrapper);
        }
        else {
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dish::getCategoryId, categoryId);
            queryWrapper.eq(Dish::getStatus, status);
            queryWrapper.orderByDesc(Dish::getUpdateTime);
            dishList = dishService.list(queryWrapper);
        }

        // 将查询到的菜品列表转换为 DishDto 列表
        List<DishDto> dishDtoList = dishList.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);

            LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorQueryWrapper.eq(DishFlavor::getDishId, item.getId());
            List<DishFlavor>  dishFlavorList = dishFlavorService.list(dishFlavorQueryWrapper);
            dishDto.setFlavors(dishFlavorList);

            return dishDto;
        }).collect(Collectors.toList());

        // 3. 返回 DishDto 列表
        return R.success(dishDtoList);
    }

}
