/**
 * Copyright (c) 2017 Nanjing Wenzheng Information Technolony Co.,Ltd. All Rights Reserved.
 * <p>
 * This software license agreement (the "Agreement") is a legal agreement between the user
 * ("You" or the "User") and Nanjing Wenzheng Information Technolony Co.,Ltd. ("Wenzheng")
 * for the software products (the "Software") and related services (the "Service") that
 * accompanies this Agreement, as may be updated or replaced by feature enhancements,
 * updates or maintenance releases and any services that may be provided by Wenzheng under this Agreement.
 * You are not allowed to download, install or use the Software or to use Services unless
 * you accept all the terms and conditions of this Agreement. Your downloading,
 * installation and use of the Software shall be regarded as your acceptance of the Agreement
 * and your agreement to be bound by all the terms and conditions of this Agreement.
 * <p>
 * The above notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * The software is provided "as is", without warranty of any kind, express or implied,
 * including but not limited to the warranties of merchantability, fitness for a particular
 * purpose and noninfringement. In no event shall the authors or copyright holders be
 * liable for any claim, damages or other liability, whether in an action of contract,
 * tort or otherwise, arising from, out of or in connection with the software or the use
 * or other dealings in the software.
 */
package com.ranranx.aolie.wf.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.exceptions.InvalidParamException;
import com.ranranx.aolie.wf.service.impl.WfManageService;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.editor.constants.ModelDataJsonConstants;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.ConflictingRequestException;
import org.flowable.ui.modeler.model.ModelKeyRepresentation;
import org.flowable.ui.modeler.model.ModelRepresentation;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * 模型编辑器
 *
 * @author yangxuefeng
 * @date 2017年6月8日 下午4:13:41
 */
@RequestMapping("/wf")
@RestController
public class ModelerController implements ModelDataJsonConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ModelerController.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ModelService modelService;


    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private IdentityService identityService;

    @Value("${bpm.charset:UTF-8}")
    private String BPM_CHAR_SET;

    @GetMapping(value = "/editor/stencilset", produces = "application/json;charset=utf-8")
    public JSONObject getStencilset() {
        InputStream stencilsetStream = this.getClass().getClassLoader().getResourceAsStream("stencilset.json");
        try {
            JSONObject modelNode = JSON.parseObject(IOUtils.toString(stencilsetStream, BPM_CHAR_SET));
            return modelNode;
        } catch (Exception e) {
            throw new InvalidParamException("Error while loading stencil set");
        }
    }


}
