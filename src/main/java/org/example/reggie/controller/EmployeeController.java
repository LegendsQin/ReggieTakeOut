package org.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.entity.Employee;
import org.example.reggie.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.example.reggie.common.R;

import javax.servlet.http.HttpServletRequest;
import javax.xml.crypto.dsig.DigestMethod;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    /**
     * @Autowired: 扫描项目中所有实现了 EmployeeService  接口的类（比如 EmployeeServiceImpl）；
     * 如果这个实现类加了 @Service 注解（Spring 组件注解），Spring 会把它创建成实例并放入「容器」；
     * 把容器里的 EmployeeServiceImpl 实例，自动赋值给 employeeService 变量。
     */
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        if (emp == null) {
            return R.error("登录失败");
        }

        if(!password.equals(emp.getPassword())) {
            return R.error("密码不正确");
        }

        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }
        // 登录成功，将员工id存入Session并返回成功结果
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出登录
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        // 清空Session中的员工id
        request.getSession().removeAttribute("employee");
        String msg = "退出成功";
        return R.success(msg);
    }

    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增员工信息：{}", employee.toString());

        // 1. 设置初始密码（123456）并进行MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        //employee.setPassword("123456");

        // 2. 设置创建时间和更新时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());

        // 3. 获取当前登录用户的ID（创建人）
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        // 4. 保存员工信息到数据库
        boolean saveResult = employeeService.save(employee);

        if (saveResult) {
            log.info("员工添加成功，员工ID：{}", employee.getId());
            return R.success("员工添加成功");
        } else {
            log.error("员工添加失败");
            return R.error("员工添加失败");
        }
    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){

        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);
        // 1. 构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        // 2. 构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Employee::getName, name);  // Employee::getName: Lambda 表达式引用 Employee 实体类的 getName() 方法，MyBatis-Plus 会自动解析出对应的数据库字段名（比如实体类的 name 属性对应数据库的 name 字段）
        queryWrapper.orderByDesc(Employee::getUpdateTime);  // Desc = Descending，降序；对应的升序是 orderByAsc

        // 3. 执行查询
        employeeService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 更新员工信息
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {

        //log.info("更新员工状态：id = {}, status = {}", employee.getId(), employee.getStatus());
        log.info("更新员工信息：{}", employee.toString());

        Employee tempEmployee = employeeService.getById(employee.getId());

//        employee.setUpdateTime(LocalDateTime.now());

        // 获取当前登录用户的ID（创建人）
//        Long empId = (Long)request.getSession().getAttribute("employee");
//        employee.setUpdateUser(empId);

        boolean updateResult = employeeService.updateById(employee);
        if(updateResult)
        {
            return R.success(String.format("用户%s更新成功", tempEmployee.getName()));
        }
        else
        {
            return R.error(String.format("用户%s更新成功", tempEmployee.getName()));
        }
    }

    @GetMapping("/{id}")
    public R<Employee>  selectById(@PathVariable Long id) {
        log.info("根据ID查询员工信息：id = {}", id);
        Employee employee = employeeService.getById(id);
        if(employee != null)
            return R.success(employee);
        return R.error("查询失败");
    }
}
