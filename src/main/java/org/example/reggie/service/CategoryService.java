package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.Category;

import java.util.List;


public interface CategoryService extends IService<Category> {
    /**
     * 删除分类，检查是否关联了菜品或套餐
     * @param id 分类ID
     */
    boolean removeWithDishAndSetmealCheck(Long id);

    /**
     * 根据类型获取分类名称列表
     * @param type 分类类型
     * @return 分类名称列表
     */
    List<String> getCategoryNameListByType(Integer type);
}
