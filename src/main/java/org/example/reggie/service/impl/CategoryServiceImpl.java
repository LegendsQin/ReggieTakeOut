package org.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.reggie.common.CustomException;
import org.example.reggie.entity.Category;
import org.example.reggie.entity.Dish;
import org.example.reggie.entity.Setmeal;
import org.example.reggie.mapper.CategoryMapper;
import org.example.reggie.service.CategoryService;
import org.example.reggie.service.DishService;
import org.example.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;


    @Override
    public boolean removeWithDishAndSetmealCheck(Long id) {
        // 检查分类是否关联了菜品或套餐
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        long dishCount = dishService.count(dishQueryWrapper);
        if (dishCount > 0) {
            throw new CustomException("分类关联了菜品，不能删除");
        }

        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        setmealQueryWrapper.eq(Setmeal::getCategoryId, id);
        long setmealCount = setmealService.count(setmealQueryWrapper);
        if (setmealCount > 0) {
            throw new CustomException("分类关联了套餐，不能删除");
        }

        // 删除分类
        return super.removeById(id);
    }

    @Override
    public List<String> getCategoryNameListByType(Integer type) {

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getType, type);
        queryWrapper.select(Category::getName);

        // 4. 执行查询：调用categoryService的list方法，返回符合条件的Category实体列表
        // list(queryWrapper) 等价于执行 SQL：SELECT name FROM category WHERE type = ?
        // 返回的是List<Category>，每个Category对象只有name字段有值（其他字段为null）
        List<Category> categoryNameList = categoryService.list(queryWrapper);

        // 5. 转换结果：把Category实体列表转为String列表（只保留name字段）
        // stream()：把列表转为流，方便批量处理
        // map(Category::getName)：遍历每个Category对象，提取它的name字段（比如把Category{name="川菜"}转为"川菜"）
        // collect(Collectors.toList())：把处理后的流重新转为List<String>
        return categoryNameList.stream().map(Category::getName).collect(Collectors.toList());
    }
}
