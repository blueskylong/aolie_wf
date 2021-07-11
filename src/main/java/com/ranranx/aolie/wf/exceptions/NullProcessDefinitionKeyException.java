
package com.ranranx.aolie.wf.exceptions;


import com.ranranx.aolie.core.exceptions.BaseException;

/**
 * 流程定义关键词为空异常
 */
public class NullProcessDefinitionKeyException extends BaseException {
    private static final long serialVersionUID = 1L;

    public NullProcessDefinitionKeyException(String err) {
        super(err);
    }
}
