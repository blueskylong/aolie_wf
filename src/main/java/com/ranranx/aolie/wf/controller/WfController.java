package com.ranranx.aolie.wf.controller;

import com.ranranx.aolie.core.annotation.CustomRightPermission;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.wf.service.WfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/6 0006 8:04
 **/
@RestController
@RequestMapping("/wf")
public class WfController {

    @Autowired
    private WfService wfService;

    @PostMapping("/commit/{dsId}/{bussKey}")
    public HandleResult commit(@PathVariable long dsId,
                               @PathVariable long bussKey, @RequestBody Map<String, Object> mapValues) {
        try {
            return wfService.commit(dsId, bussKey, mapValues);
        } catch (Exception e) {
            return HandleResult.failure(e.getMessage());
        }

    }

    @RequestMapping("/rollBack/{dsId}/{bussKey}")
    public HandleResult rollBack(@PathVariable long dsId,
                                 @PathVariable long bussKey) {

        try {
            return wfService.rollBack(dsId, bussKey);
        } catch (Exception e) {
            return HandleResult.failure(e.getMessage());
        }

    }

    @RequestMapping("/sayHello")
    @CustomRightPermission("MyRight")
    public String sayHello() {
        return "Hello,there!";
    }
}
