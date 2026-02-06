package org.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.CustomException;
import org.example.reggie.entity.Dish;
import org.example.reggie.entity.DishDto;
import org.example.reggie.entity.DishFlavor;
import org.example.reggie.mapper.DishMapper;
import org.example.reggie.service.DishFlavorService;
import org.example.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Value("${reggie.path}")
    private String basePath;
    @Autowired
    private DishService dishService;

    /**
     * 保存菜品到 Dish表 及其对应的口味信息到 DishFlavor表
     * @param dishDto 包含菜品信息和口味信息的 DTO对象
     */
    @Override
    public boolean saveWithFlavor(DishDto dishDto) {
        // 1. 保存菜品基本信息到菜品表
        this.save(dishDto);

        // 2. 保存菜品对应的口味信息到口味表
        List<DishFlavor> flavors = dishDto.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDto.getId());
            });
            // 批量插入口味数据
            return dishFlavorService.saveBatch(flavors);
        }
        return false;
    }

    /**
     * 更新菜品到 Dish表 及其对应的口味信息到 DishFlavor表
     * @param dishDto 包含菜品信息和口味信息的 DTO对象
     * @return
     */
    @Override
    // 加 @Transactional(rollbackFor = Exception.class)，如果菜品/口味更新任意一步失败，整个操作回滚，避免数据不一致
    // rollbackFor = Exception.class 确保所有异常（包括运行时异常）都能触发回滚
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithFlavor(DishDto dishDto) {


        Dish dish = dishService.getById(dishDto.getId());
        String originalDishImage = dish.getImage();
        String newDishImage = dishDto.getImage();
        // 3. 如果新图片与旧图片不同，删除旧图片
        if (!StringUtils.isEmpty(newDishImage) && !newDishImage.equals(originalDishImage)) {
            // 删除旧图片
            File oldFile = new File(basePath + originalDishImage);
            if (oldFile.exists()) {
                oldFile.delete();
            }
        }


        // 1. 更新菜品基本信息到菜品表
        // 更新菜品基本信息（返回是否更新成功）
        boolean dishUpdateSuccess = this.updateById(dishDto);
        if (!dishUpdateSuccess) {
            return false; // 菜品更新失败，直接返回
        }

        // 2. 更新菜品对应的口味信息到口味表
        List<DishFlavor> nowFlavors = dishDto.getFlavors();    // 新的口味数据
        Map<Long, DishFlavor> nowFlavorMap = new HashMap<>();

        LambdaQueryWrapper<DishFlavor>  queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        List<DishFlavor> originalFlavors = dishFlavorService.list(queryWrapper);    // 原有的口味数据
        Set<Long> originalFlavorIds = originalFlavors.stream().map(DishFlavor::getId).collect(Collectors.toSet());    // 原有的口味数据的 ID集合

        // 将所有 ID不为 null的口味数据收集到 nowFlavorMap中
        if(!CollectionUtils.isEmpty(nowFlavors)) {
            nowFlavorMap = nowFlavors.stream()
                    .filter(flavor -> flavor.getId() != null)   // 过滤掉 ID 为 null 的口味数据
                    .collect(Collectors.toMap(DishFlavor::getId, flavor -> flavor));
        }

        // 2.1 将所有 ID为 null的口味数据直接批量新增到口味表中
        if(!CollectionUtils.isEmpty(nowFlavors)) {
            List<DishFlavor> newFlavors = nowFlavors.stream()
                    .filter(flavor -> flavor.getId()==null)
                    .peek(flavor -> flavor.setDishId(dishDto.getId()))
                    .collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(newFlavors)){
                dishFlavorService.saveBatch(newFlavors);
            }
        }
        if(!CollectionUtils.isEmpty(originalFlavorIds) && !CollectionUtils.isEmpty(nowFlavorMap)){
            for (Long originalFlavorId : originalFlavorIds) {
                // 2.2 如果有删除的口味数据,则从口味表中删除
                if(!nowFlavorMap.containsKey(originalFlavorId)){
                    dishFlavorService.removeById(originalFlavorId);
                }
                // 2.3 如果有修改的口味数据,则更新到口味表中
                else {
                    DishFlavor flavor = nowFlavorMap.get(originalFlavorId);
                    log.info("口味ID: {}", flavor.getId());
                    dishFlavorService.updateById(flavor);
                }

            }
        }


        return true;
    }

    /**
     * 删除菜品，同时删除菜品对应的口味数据
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDishWithFlavorByIds(List<Long> ids) {
        // 1. 校验是否有起售中的菜品
        LambdaQueryWrapper<Dish> queryWrapper_1 = new LambdaQueryWrapper<>();
        queryWrapper_1.in(Dish::getId, ids);
        queryWrapper_1.eq(Dish::getStatus, 1);

        int count = this.count(queryWrapper_1);
        if (count > 0) {
            throw new CustomException("存在起售中的菜品，不能删除");
        }


        // 2. 删除 Dish表中的菜品
        // 2.1 先删除服务器上的该菜品的图片
        List<Dish> dishes = this.listByIds(ids);
        if (!CollectionUtils.isEmpty(dishes)) {
            for (Dish dish : dishes) {
                // 获取图片路径
                String image = dish.getImage();
                if (StringUtils.hasLength(image)) {
                    // 拼接完整的图片文件路径
                    String imagePath = basePath + image;
                    // 创建文件对象
                    File file = new File(imagePath);
                    // 如果文件存在，删除文件
                    if (file.exists()) {
                        boolean delete = file.delete();
                        if (delete) {
                            log.info("删除图片成功，路径：{}", imagePath);
                        } else {
                            log.info("删除图片失败，路径：{}", imagePath);
                        }
                    }
                }
            }
        }

        boolean dishRemoveSuccess = this.removeByIds(ids);
        if (!dishRemoveSuccess) {
            return false;
        }

        // 3. 删除 DishFlavor表中的菜品关联口味
        // 当 remove执行时,删除行数为0时(即没有可删的东西)就会返回false,所以我们做一个处理,只有 Sql执行失败的时候才返回false,删无可删时不算false
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId, ids);
        try {
            dishFlavorService.remove(queryWrapper);
        }
        catch (Exception e) {
            log.error("删除菜品关联口味失败", e);
            return false;
        }

        return true;
    }
}
