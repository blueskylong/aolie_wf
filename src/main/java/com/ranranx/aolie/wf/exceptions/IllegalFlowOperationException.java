package com.ranranx.aolie.wf.exceptions;

import com.ranranx.aolie.core.exceptions.IllegalOperatorException;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/9 0009 10:56
 **/
public class IllegalFlowOperationException extends IllegalOperatorException {
    public IllegalFlowOperationException(String errorInfo) {
        super(errorInfo);
    }
}
