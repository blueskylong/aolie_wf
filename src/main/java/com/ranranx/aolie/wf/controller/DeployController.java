package com.ranranx.aolie.wf.controller;

import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.wf.service.impl.WfServiceImpl;
import com.ranranx.aolie.wf.service.DeployService;
import org.apache.commons.io.FilenameUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/16 0016 20:45
 **/
@RestController
@RequestMapping("/wf")
public class DeployController {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private DeployService deployService;


    /**
     * 查询流程部署列表
     */
    @GetMapping
    public ResponseEntity<List<Deployment>> listDeployments() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().orderByDeploymentId().desc().list();

        if (deployments == null || deployments.isEmpty()) {
            return new ResponseEntity<List<Deployment>>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<List<Deployment>>(deployments, HttpStatus.OK);
    }

    /**
     * 查询指定的流程部署
     *
     * @param id 流程部署ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Deployment> getDeployment(@PathVariable String id) {
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(id).singleResult();

        if (deployment == null) {
            return new ResponseEntity<Deployment>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<Deployment>(deployment, HttpStatus.OK);
    }

    @RequestMapping("/deploy/{wfId}")
    public HandleResult deployWf(@PathVariable Long wfId) {
        return deployService.validateModelAndDeploy(wfId);
    }

    /**
     * 部署流程
     */
    @PostMapping
    public ResponseEntity<String> deploy(MultipartHttpServletRequest request) throws IOException {
        Iterator<String> itr = request.getFileNames();
        while (itr.hasNext()) {
            MultipartFile file = request.getFile(itr.next());
            String fileName = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(fileName);
            if (extension.equals("zip") || extension.equals("bar")) {
                ZipInputStream zip = new ZipInputStream(file.getInputStream());
                repositoryService.createDeployment().addZipInputStream(zip).deploy();
            } else {
                repositoryService.createDeployment().addInputStream(fileName, file.getInputStream()).deploy();
            }
        }
        return new ResponseEntity<String>(HttpStatus.OK);
    }


    /**
     * 删除部署的流程，级联删除流程实例
     *
     * @param id 流程部署ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        repositoryService.deleteDeployment(id, true);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    /**
     * 查询流程定义信息
     *
     * @return
     */
    @GetMapping("/getFlowInfoByVersion")
    public HandleResult getFlowInfoByVersion() {
        String version = SessionUtils.getLoginVersion();
        HandleResult success = HandleResult.success(1);
        success.setData(WfServiceImpl.findFlowInfoByVersion(version));
        return success;
    }

    /**
     * 查询一版本下,表和流程的关系信息
     *
     * @returndefineId
     */
    @GetMapping("/findTable2FlowByVersion")
    public HandleResult findTable2FlowByVersion() {
        String version = SessionUtils.getLoginVersion();
        HandleResult success = HandleResult.success(1);
        success.setData(WfServiceImpl.findTable2FlowByVersion(version));
        return success;
    }


    @RequestMapping(value = "/getFlowImage/{tableId}/{bussId}")
    public void getFlowImage(@PathVariable long tableId, @PathVariable long bussId, HttpServletResponse response) {
        response.setContentType("image/gif");
        try {
            OutputStream out = response.getOutputStream();
            byte[] b = deployService.getFlowImage(bussId, tableId, SessionUtils.getLoginVersion());
            out.write(b);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

}
