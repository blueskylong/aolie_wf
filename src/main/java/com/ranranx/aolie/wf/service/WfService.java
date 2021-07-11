package com.ranranx.aolie.wf.service;

import com.ranranx.aolie.core.handler.HandleResult;
import org.flowable.ui.common.model.ResultListDataRepresentation;

import java.util.Map;

public interface WfService {

    /**
     * 审核提交
     *
     * @param dsId
     * @param bussKey
     * @return
     */
    HandleResult commit(long dsId, long bussKey, Map<String, Object> mapValues);

    /**
     * 取得角色信息
     *
     * @param filter
     * @return
     */
    ResultListDataRepresentation getGroups(String filter);

    /**
     * 取得用户信息
     *
     * @param filter
     * @return
     */
    ResultListDataRepresentation getUsers(String filter);

    /**
     * 撤回上次的提交
     *
     * @param dsId
     * @param bussKey
     * @return
     */
    HandleResult rollBack(long dsId, Long bussKey);
}
