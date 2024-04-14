package com.ezone.ezproject.modules.swagger.controller;

import com.ezone.ezproject.es.entity.bean.CodeEventFilterConf;
import com.ezone.ezproject.es.entity.enums.OperationType;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * swagger只能处理好controller中的引用类，对于多态具体实现类处理不好，此处强制引用
 */
@ApiOperation("swagger-doc-models")
@RestController
@RequestMapping("/swagger-doc-models")
@Slf4j
@AllArgsConstructor
public class SwaggerModelController {

    @ApiOperation("swagger doc models")
    @GetMapping
    public SwaggerDocModals docModels() {
        return new SwaggerDocModals();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SwaggerDocModals {
        private CodeEventFilterConf codeEventFilterConf;
        private OperationType operationType;
    }
}
