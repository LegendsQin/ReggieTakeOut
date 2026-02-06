package org.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.CustomException;
import org.example.reggie.entity.AddressBook;
import org.example.reggie.mapper.AddressBookMapper;
import org.example.reggie.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {

    @Autowired
    private AddressBookService addressBookService;


    /**
     * 新增地址并检查 addressBook表中该用户是否已经有地址记录,
     * 如果没有,则将本条记录设置为默认地址
     * @param addressBook
     */
    @Override
    public boolean saveAndCheckDefault(AddressBook addressBook) {

        Long userId = BaseContext.getCurrentUserId();
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, userId);
        int count = addressBookService.count(queryWrapper);

        addressBook.setUserId(userId);
        if (count == 0) {
            addressBook.setIsDefault(1);
        }
        else if(count > 0){
            addressBook.setIsDefault(0);
        }

        return addressBookService.save(addressBook);
    }

    /**
     * 更新地址为默认地址
     * @param addressBook: 包含用户 id 和地址记录 id 的 AddressBook 对象
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDefaultById(AddressBook addressBook) {

        // 找到这个 id 对应的地址记录
        AddressBook nowAddressBook = addressBookService.getById(addressBook.getId());
        if(nowAddressBook == null){
            return false;
        }


        // 将之前的默认地址设置为非默认地址
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, nowAddressBook.getUserId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);
        AddressBook originDefaultAddress = addressBookService.getOne(queryWrapper);
        originDefaultAddress.setIsDefault(0);
        addressBookService.updateById(originDefaultAddress);


        // 设置新的默认地址
        nowAddressBook.setIsDefault(1);
        return addressBookService.updateById(nowAddressBook);
    }



    /**
     * 根据用户 id 查询默认地址
     * @param userId
     * @return
     */
    @Override
    public AddressBook getDefaultByUserId(Long userId) {

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, userId);
        queryWrapper.eq(AddressBook::getIsDefault, 1);
        return addressBookService.getOne(queryWrapper);
    }

     /**
     * 根据 id 删除该地址记录
      * 如果该地址是默认地址,则将最近更新的地址设置为默认地址
     * @param ids
     * @return
     */
    @Override
    public boolean removeByIdAndChangeDefault(Long ids) {
        Long userId = BaseContext.getCurrentUserId();
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, userId);
        queryWrapper.eq(AddressBook::getId, ids);
        AddressBook addressBook = addressBookService.getOne(queryWrapper);
        if(addressBook.getIsDefault() == 1){
            LambdaQueryWrapper<AddressBook> queryWrapperNotDefault = new LambdaQueryWrapper<>();
            queryWrapperNotDefault.eq(AddressBook::getUserId, userId);
            queryWrapperNotDefault.eq(AddressBook::getIsDefault, 0);
            queryWrapperNotDefault.orderByDesc(AddressBook::getUpdateTime);
            queryWrapperNotDefault.last("limit 1");
            AddressBook addressBookNotDefault = addressBookService.getOne(queryWrapperNotDefault);
            if(addressBookNotDefault != null){

                // 删除地址记录
                addressBookService.removeById(ids);

                // 将最近更新的地址设置为默认地址
                addressBookNotDefault.setIsDefault(1);
                return addressBookService.updateById(addressBookNotDefault);
            }
            else{
                return addressBookService.removeById(ids);
            }
        }
        else{
            // 删除地址记录
            return addressBookService.removeById(ids);
        }

    }
}
