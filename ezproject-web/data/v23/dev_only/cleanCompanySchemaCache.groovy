/**
 * 20220429:
 * 由于对卡片类型（二级分类）增加了内置类型（一级分类），为了查询方便，在es的卡片信息中增加内置类型冗余字段。因为需对历史卡片处理
 */

import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def service = SpringBeanFactory.getBean(CompanyProjectSchemaQueryService.class)

def cleanCache = {
    service.cleanCompanyCardSchemaCache(1)
}

try {
    cleanCache()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"