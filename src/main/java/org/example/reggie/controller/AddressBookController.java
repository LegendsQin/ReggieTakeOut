package org.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;
import org.example.reggie.entity.AddressBook;
import org.example.reggie.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addressBook")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;


    @PostMapping
    public R<String> save(@RequestBody AddressBook addressBook) {



        boolean isSuccess = addressBookService.saveAndCheckDefault(addressBook);
        if(isSuccess){
            return R.success("新增地址成功");
        }
        else{
            return R.error("新增地址失败");
        }
    }

    /**
     * 查询用户所有地址
     * @return
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list() {
        Long userId = BaseContext.getCurrentUserId();
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, userId);
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);
        List<AddressBook> addressBookList = addressBookService.list(queryWrapper);
        return R.success(addressBookList);
    }


    @PutMapping("/default")
    public R<String> setDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址的 id: {}", addressBook.getId());



        boolean isSuccess = addressBookService.updateDefaultById(addressBook);
        if(isSuccess){
            return R.success("设置默认地址成功");
        }
        else{
            return R.error("设置默认地址失败");
        }
    }

     /**
     * 查询用户默认地址
     * @return
     */
    @GetMapping("/default")
    public R<AddressBook> getDefaultAddressBook() {
        Long userId = BaseContext.getCurrentUserId();
        AddressBook addressBook = addressBookService.getDefaultByUserId(userId);
        if(addressBook == null){
            return R.error("用户没有默认地址");
        }
        else{
            return R.success(addressBook);
        }
    }


    /**
     * 根据 id 返回该地址记录的信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<AddressBook> getById(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        if(addressBook == null){
            return R.error("该地址记录不存在");
        }
        else{
            return R.success(addressBook);
        }
    }


    @PutMapping
    public R<String> update(@RequestBody AddressBook addressBook) {
        boolean isSuccess = addressBookService.updateById(addressBook);
        if(isSuccess){
            return R.success("更新地址成功");
        }
        else{
            return R.error("更新地址失败");
        }
    }

    /**
     * 根据 id 删除该地址记录
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long ids) {

        boolean isSuccess = addressBookService.removeByIdAndChangeDefault(ids);
        if(isSuccess){
            return R.success("删除地址成功");
        }
        else{
            return R.error("删除地址失败");
        }
    }




}
