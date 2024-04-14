package com.ezone.ezproject.modules.card.excel;

import com.alibaba.excel.exception.ExcelAnalysisException;
import com.ezone.ezproject.common.exception.CodedException;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class CustomerExcelAnalysisException extends ExcelAnalysisException {
    private Integer code;

    public CustomerExcelAnalysisException(HttpStatus statusCode, String message) {
        super(message);
        this.code = CodedException.ERROR_BASE_CODE + statusCode.value();
    }
}
