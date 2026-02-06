package org.example.reggie.entity;

import org.example.reggie.entity.Setmeal;
import org.example.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
