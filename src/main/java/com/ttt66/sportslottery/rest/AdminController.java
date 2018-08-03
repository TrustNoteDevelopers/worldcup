package com.thingtrust.sportslottery.rest;

import com.thingtrust.sportslottery.common.model.ResponseResult;
import com.thingtrust.sportslottery.service.FifaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 〈后台管理接口〉
 *
 * @author WangYu
 * @create 2018/5/22
 * @since 1.0.0
 */
@Controller
@RequestMapping(value = "/community/admin")
public class AdminController {

    @Autowired
    private FifaService fifaService;

    /**
     * 批量打款
     *
     * @param
     * @return
     */
    @PostMapping(value = "/remit")
    public ResponseResult bulkMoney(final Long[] ids) {
        final List<Long> longList = Arrays.asList(ids);
        final ResponseResult responseResult = fifaService.bulkMoney(longList);
        return responseResult;
    }
}
