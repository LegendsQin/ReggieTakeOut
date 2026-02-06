package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.Setmeal;
import org.example.reggie.entity.SetmealDto;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 保存套餐信息并关联菜品信息
     * @param setmealDto
     * @return
     */
    public boolean saveSetmealWithDish(SetmealDto setmealDto);

    /**
     * 更新套餐信息并关联菜品信息
     * @param setmealDto
     * @return
     */
    public boolean updateSetmealWithDish(SetmealDto setmealDto);

     /**
     * 根据id 批量删除套餐信息,同时删除关联的菜品信息
     * @param ids
     * @return
     */
    public boolean removeSetmealWithSetmealDishByIds(List<Long> ids);
}
