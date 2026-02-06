package org.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.CustomException;
import org.example.reggie.entity.Setmeal;
import org.example.reggie.entity.SetmealDish;
import org.example.reggie.entity.SetmealDto;
import org.example.reggie.mapper.SetmealMapper;
import org.example.reggie.service.SetmealDishService;
import org.example.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 保存套餐信息并关联菜品信息
     * @param setmealDto
     * @return
     */
    @Override
    public boolean saveSetmealWithDish(SetmealDto setmealDto) {
        this.save(setmealDto);

        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes();
        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            setmealDishList.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDto.getId());
            });
        }
        return setmealDishService.saveBatch(setmealDishList);
    }

    /**
     * 更新套餐信息并关联菜品信息
     * @param setmealDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSetmealWithDish(SetmealDto setmealDto) {


        Setmeal Setmeal = this.getById(setmealDto.getId());
        String originalSetmealImage = Setmeal.getImage();
        String newSetmealImage = setmealDto.getImage();
        // 3. 如果新图片与旧图片不同，删除旧图片
        if (!StringUtils.isEmpty(newSetmealImage) && !newSetmealImage.equals(originalSetmealImage)) {
            // 删除旧图片
            File oldFile = new File(basePath + originalSetmealImage);
            if (oldFile.exists()) {
                oldFile.delete();
            }
        }


        boolean updateSetmealResult = this.updateById(setmealDto);
        if(!updateSetmealResult){
            return false;
        }

        List<SetmealDish> nowSetmealDishList = setmealDto.getSetmealDishes();
        Map<Long, SetmealDish> nowSetmealDishMap= new HashMap<>();

        // 将当前套餐的菜品列表转换为 Map，键为菜品 ID，值为 SetmealDish 对象,这个Map里面只存修改的需要更新的菜品,新增的菜品由于没有ID,所以会被过滤掉
        if(!CollectionUtils.isEmpty(nowSetmealDishList)){
            nowSetmealDishMap = nowSetmealDishList.stream()
                    .filter(setmealDish -> setmealDish.getId()!=null)
                    .collect(Collectors.toMap(SetmealDish::getId, setmealDish -> setmealDish));
        }

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        List<SetmealDish> originalSetmealDishList = setmealDishService.list(queryWrapper);
        Set<Long> originalSetmealDishIdsSet = originalSetmealDishList.stream().map(SetmealDish::getId).collect(Collectors.toSet());


        // 处理新增菜品
        if(!CollectionUtils.isEmpty(nowSetmealDishList)){
            List<SetmealDish> newSetmealDishList = nowSetmealDishList.stream()
                    .filter(setmealDish -> setmealDish.getId()==null)
                    .peek(setmealDish -> setmealDish.setSetmealId(setmealDto.getId()))
                    .collect(Collectors.toList());
            if(!newSetmealDishList.isEmpty()){
                setmealDishService.saveBatch(newSetmealDishList);
            }
        }


        if(!CollectionUtils.isEmpty(originalSetmealDishIdsSet) && !CollectionUtils.isEmpty(nowSetmealDishMap)){
            for(Long originalSetmealDishId: originalSetmealDishIdsSet){

                // 如果当前套餐的菜品列表中不包含该菜品ID,则说明该菜品被删除了,需要从数据库中删除
                if(!nowSetmealDishMap.containsKey(originalSetmealDishId)){
                    setmealDishService.removeById(originalSetmealDishId);
                }
                else {
                    // 更新 SetmealDish信息
                    SetmealDish setmealDish = nowSetmealDishMap.get(originalSetmealDishId);
                    setmealDishService.updateById(setmealDish);
                }
            }
        }

        return true;
    }

    /**
     * 根据id 批量删除套餐信息,同时删除关联的菜品信息
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeSetmealWithSetmealDishByIds(List<Long> ids) {
        // 1. 校验是否有起售中的套餐
        LambdaQueryWrapper<Setmeal> queryWrapper_1 = new LambdaQueryWrapper<>();
        queryWrapper_1.in(Setmeal::getId, ids);
        queryWrapper_1.eq(Setmeal::getStatus, 1);
        int count = this.count(queryWrapper_1);
        if (count > 0) {
            throw new CustomException("存在起售中的套餐，不能删除");
        }

        // 2. 删除套餐（必须成功，否则返回false）
        // 2.1 先删除套餐的图片
        List<Setmeal> setmealList = this.listByIds(ids);
        if(!CollectionUtils.isEmpty(setmealList)){
            for(Setmeal setmeal : setmealList){
                // 获取套餐图片文件名
                String imageName = setmeal.getImage();
                if(StringUtils.hasLength(imageName)){
                    // 2.2 拼接完整图片路径
                    String imagePath = basePath + imageName;

                    File file = new File(imagePath);

                    if(file.exists()){
                        // 2.3 尝试删除图片文件
                        boolean deletePhotoResult = file.delete();
                        if(deletePhotoResult){
                            log.info("删除套餐图片成功: {}", imagePath);
                        }
                        else{
                            log.error("删除套餐图片失败: {}", imagePath);
                        }
                    }
                }
            }
        }

        boolean removeSetmealResult = this.removeByIds(ids);
        if(!removeSetmealResult){
            return false;
        }

        // 3. 删除套餐关联菜品（关键修复：只要SQL执行成功，不管有没有数据，都算成功）
        // 当 remove执行时,删除行数为0时(即没有可删的东西)就会返回false,所以我们做一个处理,只有 Sql执行失败的时候才返回false,删无可删时不算false
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getSetmealId, ids);
        try {
            // 执行删除操作，不依赖返回值（因为无数据时返回false，但操作是成功的）
            setmealDishService.remove(queryWrapper);
        } catch (Exception e) {
            // 只有当删除关联菜品时抛出异常（SQL执行失败），才返回false
            log.error("删除套餐关联菜品失败", e);
            return false;
        }

        // 4. 只要套餐删除成功，且关联菜品删除操作无异常，就返回true
        return true;
    }

}
