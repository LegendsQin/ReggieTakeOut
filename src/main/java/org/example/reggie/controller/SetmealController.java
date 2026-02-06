package org.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.example.reggie.entity.*;
import org.example.reggie.service.CategoryService;
import org.example.reggie.service.SetmealDishService;
import org.example.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 分页查询套餐信息
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        // 1 创建分类构造器
        Page<Setmeal> setmealPageInfo = new Page<>(page, pageSize);

        // 2 构建条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 3 查询
        setmealService.page(setmealPageInfo, queryWrapper);

        // 4 创建子类分页构造器
        Page<SetmealDto> setmealDtoPageInfo = new Page<>();

        // 5 先将Page类中除records外的属性复制到子类分页构造器中
        BeanUtils.copyProperties(setmealPageInfo, setmealDtoPageInfo, "records");

        // 6 遍历 Setmeal的Page对象中的records中的每个Setmeal对象，将其转换为SetmealDto对象
        List<SetmealDto> setmealDtoList = setmealPageInfo.getRecords().stream().map(
                setmeal -> {
                    SetmealDto setmealDto = new SetmealDto();
                    BeanUtils.copyProperties(setmeal,setmealDto);
                    Long categoryId = setmeal.getCategoryId();
                    Category category = categoryService.getById(categoryId);
                    setmealDto.setCategoryName(category.getName());
                    return setmealDto;
                }
        ).collect(Collectors.toList());

        // 7 将转换后的SetmealDto对象列表设置到子类分页构造器的records属性中
        setmealDtoPageInfo.setRecords(setmealDtoList);

        return R.success(setmealDtoPageInfo);
    }


    /**
     * 保存套餐信息并关联菜品信息
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        boolean isSuccess = setmealService.saveSetmealWithDish(setmealDto);
        if(isSuccess){
            return R.success("保存成功");
        }else{
            return R.error("保存失败");
        }
    }

    /**
     * 根据id查询套餐信息并关联菜品信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){
        // 1 根据id查询套餐信息
        Setmeal setmeal = setmealService.getById(id);
        if(setmeal == null){
            return R.error("套餐不存在");
        }

        // 2 根据套餐id查询关联的菜品信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        queryWrapper.orderByDesc(SetmealDish::getUpdateTime);
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);

        // 3 将1 查到的套餐信息和2 查到的菜品信息封装到SetmealDto对象中
        SetmealDto setmealDto = new SetmealDto();
        setmealDto.setSetmealDishes(setmealDishList);
        BeanUtils.copyProperties(setmeal,setmealDto);

        return R.success(setmealDto);
    }


    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){

        boolean isSuccess = setmealService.updateSetmealWithDish(setmealDto);
        if(isSuccess){
            return R.success("更新成功");
        }else{
            return R.error("更新失败");
        }
    }


    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids){
        // 1 检查ids是否为空
        if(ids == null || ids.isEmpty()){
            return R.error("请选择要操作的套餐");
        }

        // 2 更新套餐状态
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Setmeal::getStatus, status);
        updateWrapper.in(Setmeal::getId, ids);
        boolean isSuccess = setmealService.update(updateWrapper);

        if(isSuccess){
            return R.success("更新成功");
        }else{
            return R.error("更新失败");
        }
    }

    /**
     * 根据id 批量删除套餐信息,同时删除关联的菜品信息
     * @param ids
     * @return
     */
    @DeleteMapping
    // 如果 URL 参数名是 dish_name，但方法参数名是 name，必须用 @RequestParam 指定映射关系 : @RequestParam("dish_name") String name
    public R<String> deleteById(@RequestParam List<Long> ids){
        if(ids == null || ids.isEmpty()){
            return R.error("请选择要删除的套餐");
        }

        boolean isSuccess = setmealService.removeSetmealWithSetmealDishByIds(ids);
        if(isSuccess){
            return R.success("删除成功");
        }else{
            return R.error("删除失败");
        }
    }

    @GetMapping("/list")
    public R<List<SetmealDto>> getSetmealList(Long categoryId, @RequestParam(required = false) Integer status){
        if(categoryId == null){
            return R.error("请选择要查询的套餐分类");
        }

        List<Setmeal> setmealList;

        // 2. 查询套餐列表
        if(status == null){
            LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Setmeal::getCategoryId, categoryId);
            queryWrapper.orderByDesc(Setmeal::getUpdateTime);
            setmealList = setmealService.list(queryWrapper);

        }
        else {
            LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Setmeal::getCategoryId, categoryId);
            queryWrapper.eq(Setmeal::getStatus, status);
            queryWrapper.orderByDesc(Setmeal::getUpdateTime);
            setmealList = setmealService.list(queryWrapper);
        }

        List<SetmealDto> setmealDtoList = setmealList.stream().map(
                setmeal->{
                    SetmealDto setmealDto = new SetmealDto();
                    BeanUtils.copyProperties(setmeal,setmealDto);

                    LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
                    setmealDishQueryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
                    List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishQueryWrapper);
                    setmealDto.setSetmealDishes(setmealDishList);
                    return setmealDto;
                }
        ).collect(Collectors.toList());


        return R.success(setmealDtoList);
    }

    @GetMapping("/dish/{id}")
    public R<SetmealDto> getDishList(@PathVariable Long id){
        // 1 根据套餐id查询关联的菜品信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        queryWrapper.orderByDesc(SetmealDish::getUpdateTime);
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);

        // 2 将1 查到的菜品信息封装到SetmealDto对象中
        Setmeal setmeal = setmealService.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);
        setmealDto.setSetmealDishes(setmealDishList);

        return R.success(setmealDto);
    }



}
