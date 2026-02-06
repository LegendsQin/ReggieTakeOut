package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.AddressBook;

public interface AddressBookService extends IService<AddressBook> {

     /**
     * 新增地址并检查默认地址
     * @param addressBook
     */
     boolean saveAndCheckDefault(AddressBook addressBook);

     /**
     * 更新地址为默认地址
     * @param addressBook
     * @return
     */
     boolean updateDefaultById(AddressBook addressBook);

     /**
     * 根据用户 id 查询默认地址
     * @param userId
     * @return
     */
    AddressBook getDefaultByUserId(Long userId);

     /**
     * 根据 id 删除该地址记录
      * 如果该地址是默认地址,则将其他地址设置为默认地址
     * @param ids
     * @return
     */
    boolean removeByIdAndChangeDefault(Long ids);
}
