package com.ranranx.aolie.config;

import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.runtime.LoginUser;
import com.ranranx.aolie.wf.service.WfService;
import org.flowable.idm.api.Group;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/app")
public class FlowableController {

    @Autowired
    private WfService wfService;


    /**
     * 获取默认的管理员信息
     *
     * @return
     */
    @GetMapping(value = "/rest/account")
    public UserRepresentation getAccount() {
        LoginUser loginUser = SessionUtils.getLoginUser();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(loginUser.getUserId().toString());
        userRepresentation.setFullName(loginUser.getUsername());
        userRepresentation.setFirstName(loginUser.getAccountCode());
        List<String> privileges = new ArrayList<>();
        privileges.add(DefaultPrivileges.ACCESS_MODELER);
        privileges.add(DefaultPrivileges.ACCESS_IDM);
        privileges.add(DefaultPrivileges.ACCESS_ADMIN);
        privileges.add(DefaultPrivileges.ACCESS_TASK);
        privileges.add(DefaultPrivileges.ACCESS_REST_API);
        userRepresentation.setPrivileges(privileges);
        return userRepresentation;
    }

    @GetMapping(value = "/rest/editor-groups")
    public ResultListDataRepresentation getGroups(@RequestParam(required = false, value = "filter") String filter) {
        return wfService.getGroups(filter);
    }

    @GetMapping(value = "/rest/editor-users")
    public ResultListDataRepresentation getUsers(@RequestParam(value = "filter", required = false) String filter) {
        return wfService.getUsers(filter);
    }

}
