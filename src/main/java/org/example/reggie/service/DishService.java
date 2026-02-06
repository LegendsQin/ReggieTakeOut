package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.Dish;
import org.example.reggie.entity.DishDto;

import java.util.List;

public interface DishService extends IService<Dish> {

        /**
         * 新增菜品，同时插入菜品对应的口味数据
         * @param dishDto
         */
        public boolean saveWithFlavor(DishDto dishDto);

        /**
         * 更新菜品，同时更新菜品对应的口味数据
         * @param dishDto
         * @return
         */
        public boolean updateWithFlavor(DishDto dishDto);

        /**
         * 删除菜品，同时删除菜品对应的口味数据
         * @param ids
         * @return
         */
        public boolean removeDishWithFlavorByIds(List<Long> ids);
}
