package com.ezone.ezproject.modules.system.controller;

import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.ezproject.modules.system.service.SystemService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ApiOperation("系统操作")
@RestController
@RequestMapping("/project/api/system")
@Slf4j
@AllArgsConstructor
public class SystemApiController extends AbstractController {
    private SystemService systemService;

    /**
     * 注意控制资源消耗，避免影响系统正常业务
     */
    @ApiOperation("执行groovy")
    @PostMapping("groovy")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "String", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "Long", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public void runGroovy(@RequestParam String name, @RequestBody String script, HttpServletResponse response) throws IOException {
        log.info("-------start run groovy[{}] script:\n{}", name, script);
        systemService.runGroovy(script, response.getOutputStream());
        log.info("-------end groovy:[{}]", name);
    }
}
