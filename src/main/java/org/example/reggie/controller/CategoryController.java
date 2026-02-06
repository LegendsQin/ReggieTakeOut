package org.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.example.reggie.entity.Category;
import org.example.reggie.entity.Dish;
import org.example.reggie.service.CategoryService;
import org.example.reggie.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/category")
public class CategoryController {

    /**
     * 注入分类服务
     */
    @Autowired
    private CategoryService categoryService;

    /**
     * 注入菜品服务
     */
    @Autowired
    private DishService dishService;

    /**
     * 新增分类
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Category category){
        log.info("新增分类：{}", category.toString());

        boolean saveResult = categoryService.save(category);
        if(saveResult)
        {
            return R.success("新增分类成功");
        }
        else
        {
            return R.error("新增分类失败");
        }
    }

    /**
     * 分页查询分类
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize){
        log.info("分页查询分类，page = {}, pageSize = {}", page, pageSize);

        // 分页构造器
        Page<Category> pageinfo = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Category::getId);

        // 分页查询
        categoryService.page(pageinfo,queryWrapper);
        return R.success(pageinfo);
    }


    @DeleteMapping
    public R<String> delete(Long ids){
        log.info("删除分类，id = {}", ids);

        boolean deleteResult = categoryService.removeWithDishAndSetmealCheck(ids);
        if(deleteResult)
        {
            return R.success("删除分类成功");
        }
        else
        {
            return R.error("删除分类失败");
        }
    }

    @PutMapping
    public R<String> update(@RequestBody Category category){
        log.info("修改分类：{}", category.toString());
        boolean updateResult = categoryService.updateById(category);
        if(updateResult)
        {
            return R.success("修改分类成功");
        }
        else
        {
            return R.error("修改分类失败");
        }
    }

    @GetMapping("/list")
    public R<List<Category>> getCategoryByType(@RequestParam(required = false) Integer type){
        log.info("根据类型查询分类名称，type = {}", type);
        if (type == null){
            LambdaQueryWrapper<Category> queryWrapper= new LambdaQueryWrapper<>();
            queryWrapper.orderByAsc(Category::getSort);
            List<Category> categoryList = categoryService.list(queryWrapper);
            if(!categoryList.isEmpty())
                return R.success(categoryList);
            else
                return R.error("查询分类失败");
        }
        else{
            List<Category> categoryList = categoryService.list(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getType, type)
                            .orderByDesc(Category::getUpdateTime)
            );
            if(!categoryList.isEmpty())
                return R.success(categoryList);
            else
                return R.error(String.format("查询分类失败，type = %d", type));
        }

    }


}
